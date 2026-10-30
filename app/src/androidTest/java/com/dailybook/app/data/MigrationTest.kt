package com.dailybook.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 用 app/schemas 里导出的 v1 schema 建库，验证升级到 v2 后数据不丢、新列可用 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_保留旧数据并加上金额列() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO routine (id, title, note, weekdays, sortOrder, archived, createdAt) " +
                    "VALUES (1, '喝水', '八杯', 127, 0, 0, 1000)"
            )
            db.execSQL(
                "INSERT INTO day_task (id, date, title, note, done, doneAt, sortOrder, createdAt) " +
                    "VALUES (10, '2026-10-28', '买菜', '', 1, 2000, 0, 1000)"
            )
            db.execSQL(
                "INSERT INTO routine_check (routineId, date, done, doneAt) " +
                    "VALUES (1, '2026-10-28', 1, 3000)"
            )
        }

        // 校验迁移后的 schema 与 v2 实体完全一致
        val db = helper.runMigrationsAndValidate(dbName, 2, true, AppDatabase.MIGRATION_1_2)

        db.query("SELECT title, note, amount, amountKind FROM routine").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("喝水", c.getString(0))
            assertEquals("八杯", c.getString(1))
            assertEquals(0L, c.getLong(2))
            assertEquals(0, c.getInt(3))
        }
        db.query("SELECT title, done, amount, amountKind FROM day_task").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("买菜", c.getString(0))
            assertEquals(1, c.getInt(1))
            assertEquals(0L, c.getLong(2))
        }
        db.query("SELECT routineId, done, amount, amountKind FROM routine_check").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1L, c.getLong(0))
            assertEquals(1, c.getInt(1))
            assertEquals(0L, c.getLong(2))
            assertEquals(0, c.getInt(3))
        }
    }

    @Test
    fun migrate1To2_旧数据写入金额后能读回() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO day_task (id, date, title, note, done, doneAt, sortOrder, createdAt) " +
                    "VALUES (10, '2026-10-28', '旧事务', '', 0, NULL, 0, 1000)"
            )
        }
        val db = helper.runMigrationsAndValidate(dbName, 2, true, AppDatabase.MIGRATION_1_2)

        db.execSQL("UPDATE day_task SET amount = 1234, amountKind = 2 WHERE id = 10")
        db.query("SELECT amount, amountKind FROM day_task WHERE id = 10").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1234L, c.getLong(0))
            assertEquals(2, c.getInt(1))
        }
    }

    @Test
    fun migrate2To3_新增出游表且不影响原有数据() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO routine (id, title, note, weekdays, sortOrder, archived, createdAt, " +
                    "amount, amountKind) VALUES (1, '喝水', '', 127, 0, 0, 1000, 30, 1)"
            )
            db.execSQL(
                "INSERT INTO day_task (id, date, title, note, done, doneAt, sortOrder, createdAt, " +
                    "amount, amountKind) VALUES (10, '2026-10-28', '买菜', '', 1, 2000, 0, 1000, 2560, 2)"
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, AppDatabase.MIGRATION_2_3)

        // 老数据还在，金额也没被清掉
        db.query("SELECT title, amount, amountKind FROM day_task WHERE id = 10").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("买菜", c.getString(0))
            assertEquals(2560L, c.getLong(1))
            assertEquals(2, c.getInt(2))
        }

        // 新表可用
        db.execSQL(
            "INSERT INTO trip (id, title, destination, startDate, endDate, note, createdAt) " +
                "VALUES (1, '新疆', '乌鲁木齐', '2026-10-20', '2026-10-22', '', 1000)"
        )
        db.execSQL(
            "INSERT INTO trip_item (id, tripId, date, time, kind, title, detail, amount, sortOrder) " +
                "VALUES (1, 1, '2026-10-20', '07:30', 0, '打车去机场', '', 800, 0)"
        )
        db.query("SELECT title, kind, amount FROM trip_item WHERE tripId = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("打车去机场", c.getString(0))
            assertEquals(0, c.getInt(1))
            assertEquals(800L, c.getLong(2))
        }
    }

    @Test
    fun migrate3To4_加上截止日与删除标记且历史不丢() {
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL(
                "INSERT INTO routine (id, title, note, weekdays, sortOrder, archived, createdAt, " +
                    "amount, amountKind) VALUES (1, '某宝签到', '', 127, 0, 0, 1000, 30, 1)"
            )
            db.execSQL(
                "INSERT INTO routine (id, title, note, weekdays, sortOrder, archived, createdAt, " +
                    "amount, amountKind) VALUES (2, '已归档的', '', 127, 1, 1, 1000, 0, 0)"
            )
            db.execSQL(
                "INSERT INTO routine_check (routineId, date, done, doneAt, amount, amountKind) " +
                    "VALUES (1, '2026-10-27', 1, 3000, 68, 1)"
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4)

        db.query("SELECT title, amount, amountKind, endDate, deleted FROM routine WHERE id = 1")
            .use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("某宝签到", c.getString(0))
                assertEquals(30L, c.getLong(1))
                assertEquals(1, c.getInt(2))
                assertTrue(c.isNull(3))     // 未删除 → 无截止日
                assertEquals(0, c.getInt(4))
            }
        // 打卡历史（含当天金额）原样保留
        db.query("SELECT date, amount, amountKind FROM routine_check WHERE routineId = 1")
            .use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("2026-10-27", c.getString(0))
                assertEquals(68L, c.getLong(1))
                assertEquals(1, c.getInt(2))
            }
        // 老的归档事务补上截止日，避免升级后又在清单里复活
        db.query("SELECT endDate FROM routine WHERE id = 2").use { c ->
            assertTrue(c.moveToFirst())
            assertFalse(c.isNull(0))
        }
    }
}
