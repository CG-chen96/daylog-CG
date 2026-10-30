package com.dailybook.app.data

/**
 * 金额一律以「分」为单位存 Long，避免浮点误差。
 * 解析和格式化都走字符串，不经过 Double。
 */
object Money {

    private val PATTERN = Regex("""^\d{0,9}(\.\d{0,2})?$""")

    /** 输入框文本 → 分。空串表示没有金额（0）；格式非法返回 null */
    fun parse(input: String): Long? {
        val text = input.trim().replace("，", "").replace(",", "")
        if (text.isEmpty()) return 0L
        if (!PATTERN.matches(text)) return null
        val dot = text.indexOf('.')
        val yuanPart = if (dot >= 0) text.substring(0, dot) else text
        val centPart = if (dot >= 0) text.substring(dot + 1) else ""
        val yuan = if (yuanPart.isEmpty()) 0L else yuanPart.toLong()
        val cents = when (centPart.length) {
            0 -> 0L
            1 -> centPart.toLong() * 10
            else -> centPart.toLong()
        }
        return yuan * 100 + cents
    }

    /** 分 → 编辑框里的初始文本，去掉多余的 .00 */
    fun toEditText(cents: Long): String {
        if (cents == 0L) return ""
        val abs = kotlin.math.abs(cents)
        val yuan = abs / 100
        val rest = abs % 100
        val sign = if (cents < 0) "-" else ""
        return if (rest == 0L) "$sign$yuan" else "$sign$yuan.${rest.toString().padStart(2, '0')}"
    }

    /** 分 → 展示文本，固定两位小数并带千分位，如 1234 → "12.34" */
    fun format(cents: Long): String {
        val abs = kotlin.math.abs(cents)
        val yuan = group(abs / 100)
        val rest = (abs % 100).toString().padStart(2, '0')
        val sign = if (cents < 0) "-" else ""
        return "$sign$yuan.$rest"
    }

    /** 带正负号，收入 +、支出 −（用真正的减号，排版更整齐） */
    fun formatSigned(cents: Long, kind: AmountKind): String = when (kind) {
        AmountKind.INCOME -> "+" + format(kotlin.math.abs(cents))
        AmountKind.EXPENSE -> "−" + format(kotlin.math.abs(cents))
        AmountKind.NONE -> format(cents)
    }

    private fun group(n: Long): String {
        val s = n.toString()
        val sb = StringBuilder()
        s.forEachIndexed { i, ch ->
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
            sb.append(ch)
        }
        return sb.toString()
    }
}
