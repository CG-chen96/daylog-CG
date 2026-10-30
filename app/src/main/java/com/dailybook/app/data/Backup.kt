package com.dailybook.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class Snapshot(
    val routines: List<Routine>,
    val dayTasks: List<DayTask>,
    val checks: List<RoutineCheck>,
    /** v3 起包含出游；旧备份没有这两项，读出来是空列表 */
    val trips: List<Trip> = emptyList(),
    val tripItems: List<TripItem> = emptyList(),
) {
    val isEmpty: Boolean
        get() = routines.isEmpty() && dayTasks.isEmpty() && checks.isEmpty() &&
            trips.isEmpty() && tripItems.isEmpty()
}

/**
 * 本地备份。格式参考 verifin 的做法：AES-GCM 加密 + PBKDF2 派生密钥，
 * 整个 JSON 一次性加密后落盘，不依赖任何服务端。
 *
 * 文件布局：MAGIC(6) | version(1) | salt(16) | iv(12) | ciphertext
 */
object Backup {

    private val MAGIC = byteArrayOf('D'.code.toByte(), 'A'.code.toByte(), 'Y'.code.toByte(),
        'L'.code.toByte(), 'O'.code.toByte(), 'G'.code.toByte())
    private const val VERSION: Byte = 1
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_LEN = 16
    private const val IV_LEN = 12

    fun write(context: Context, uri: Uri, password: String, snapshot: Snapshot) {
        val payload = encode(snapshot, password)
        context.contentResolver.openOutputStream(uri)?.use { it.write(payload) }
            ?: error("无法写入所选文件")
    }

    fun read(context: Context, uri: Uri, password: String): Snapshot {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取所选文件")
        return decode(bytes, password)
    }

    fun encode(snapshot: Snapshot, password: String): ByteArray {
        val json = toJson(snapshot).toString().toByteArray(Charsets.UTF_8)
        return if (password.isEmpty()) {
            MAGIC + byteArrayOf(VERSION) + byteArrayOf(0) + json
        } else {
            val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
            MAGIC + byteArrayOf(VERSION) + byteArrayOf(1) + salt + iv + cipher.doFinal(json)
        }
    }

    fun decode(bytes: ByteArray, password: String): Snapshot {
        require(bytes.size > MAGIC.size + 2) { "文件内容不完整" }
        require(MAGIC.indices.all { bytes[it] == MAGIC[it] }) { "不是日常的备份文件" }
        val encrypted = bytes[MAGIC.size + 1].toInt() == 1
        val body = bytes.copyOfRange(MAGIC.size + 2, bytes.size)

        val jsonBytes = if (!encrypted) {
            body
        } else {
            require(password.isNotEmpty()) { "该备份已加密，请输入密码" }
            require(body.size > SALT_LEN + IV_LEN) { "文件内容不完整" }
            val salt = body.copyOfRange(0, SALT_LEN)
            val iv = body.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
            val data = body.copyOfRange(SALT_LEN + IV_LEN, body.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
            try {
                cipher.doFinal(data)
            } catch (e: Exception) {
                throw IllegalArgumentException("密码不正确，或文件已损坏")
            }
        }
        return fromJson(JSONObject(String(jsonBytes, Charsets.UTF_8)))
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun toJson(s: Snapshot): JSONObject = JSONObject().apply {
        put("format", "daylog")
        put("version", 1)
        put("exportedAt", System.currentTimeMillis())
        put("routines", JSONArray().apply {
            s.routines.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id); put("title", r.title); put("note", r.note)
                    put("weekdays", r.weekdays); put("sortOrder", r.sortOrder)
                    put("archived", r.archived); put("createdAt", r.createdAt)
                    put("amount", r.amount); put("amountKind", r.amountKind.value)
                    put("endDate", r.endDate ?: JSONObject.NULL); put("deleted", r.deleted)
                })
            }
        })
        put("dayTasks", JSONArray().apply {
            s.dayTasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("date", t.date); put("title", t.title)
                    put("note", t.note); put("done", t.done)
                    put("doneAt", t.doneAt ?: JSONObject.NULL)
                    put("sortOrder", t.sortOrder); put("createdAt", t.createdAt)
                    put("amount", t.amount); put("amountKind", t.amountKind.value)
                })
            }
        })
        put("checks", JSONArray().apply {
            s.checks.forEach { c ->
                put(JSONObject().apply {
                    put("routineId", c.routineId); put("date", c.date)
                    put("done", c.done); put("doneAt", c.doneAt ?: JSONObject.NULL)
                    put("amount", c.amount); put("amountKind", c.amountKind.value)
                })
            }
        })
        put("trips", JSONArray().apply {
            s.trips.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("title", t.title)
                    put("destination", t.destination)
                    put("startDate", t.startDate); put("endDate", t.endDate)
                    put("note", t.note); put("createdAt", t.createdAt)
                })
            }
        })
        put("tripItems", JSONArray().apply {
            s.tripItems.forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id); put("tripId", i.tripId); put("date", i.date)
                    put("time", i.time); put("kind", i.kind.value)
                    put("title", i.title); put("detail", i.detail)
                    put("amount", i.amount); put("sortOrder", i.sortOrder)
                })
            }
        })
    }

    private fun fromJson(root: JSONObject): Snapshot {
        require(root.optString("format") == "daylog") { "备份格式不匹配" }

        val routines = root.optJSONArray("routines").orEmpty().map { o ->
            Routine(
                id = o.getLong("id"),
                title = o.getString("title"),
                note = o.optString("note"),
                weekdays = o.optInt("weekdays", ALL_DAYS),
                sortOrder = o.optInt("sortOrder"),
                archived = o.optBoolean("archived"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                // v1 备份没有金额字段，缺省按 0 处理
                amount = o.optLong("amount", 0L),
                amountKind = AmountKind.of(o.optInt("amountKind", 0)),
                // v4 之前没有截止日，缺省表示一直有效
                endDate = o.optString("endDate").takeIf { it.isNotBlank() },
                deleted = o.optBoolean("deleted"),
            )
        }
        val tasks = root.optJSONArray("dayTasks").orEmpty().map { o ->
            DayTask(
                id = o.getLong("id"),
                date = o.getString("date"),
                title = o.getString("title"),
                note = o.optString("note"),
                done = o.optBoolean("done"),
                doneAt = o.optLongOrNull("doneAt"),
                sortOrder = o.optInt("sortOrder"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                amount = o.optLong("amount", 0L),
                amountKind = AmountKind.of(o.optInt("amountKind", 0)),
            )
        }
        val checks = root.optJSONArray("checks").orEmpty().map { o ->
            RoutineCheck(
                routineId = o.getLong("routineId"),
                date = o.getString("date"),
                done = o.optBoolean("done", true),
                doneAt = o.optLongOrNull("doneAt"),
                amount = o.optLong("amount", 0L),
                amountKind = AmountKind.of(o.optInt("amountKind", 0)),
            )
        }
        // v3 之前的备份没有出游，缺省为空
        val trips = root.optJSONArray("trips").orEmpty().map { o ->
            Trip(
                id = o.getLong("id"),
                title = o.getString("title"),
                destination = o.optString("destination"),
                startDate = o.getString("startDate"),
                endDate = o.optString("endDate"),
                note = o.optString("note"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
        val tripItems = root.optJSONArray("tripItems").orEmpty().map { o ->
            TripItem(
                id = o.getLong("id"),
                tripId = o.getLong("tripId"),
                date = o.getString("date"),
                time = o.optString("time"),
                kind = TripKind.of(o.optInt("kind", TripKind.OTHER.value)),
                title = o.getString("title"),
                detail = o.optString("detail"),
                amount = o.optLong("amount", 0L),
                sortOrder = o.optInt("sortOrder"),
            )
        }
        return Snapshot(routines, tasks, checks, trips, tripItems)
    }

    private fun JSONArray?.orEmpty(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).map { optJSONObject(it) }.filterNotNull()
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (isNull(key) || !has(key)) return null
        return optLong(key)
    }
}
