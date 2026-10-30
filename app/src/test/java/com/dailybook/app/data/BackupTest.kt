package com.dailybook.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {

    private val snapshot = Snapshot(
        routines = listOf(
            Routine(id = 1, title = "喝水", note = "8 杯", weekdays = ALL_DAYS, sortOrder = 0),
            Routine(id = 2, title = "跑步", weekdays = 0b0010100, sortOrder = 1, archived = true),
        ),
        dayTasks = listOf(
            DayTask(id = 7, date = "2026-10-28", title = "回电话", done = true, doneAt = 123L),
            DayTask(id = 8, date = "2026-10-28", title = "买牛奶", done = false),
        ),
        checks = listOf(
            RoutineCheck(routineId = 1, date = "2026-10-28", done = true, doneAt = 999L),
        ),
    )

    @Test
    fun `明文备份可以原样往返`() {
        val bytes = Backup.encode(snapshot, password = "")
        val back = Backup.decode(bytes, password = "")

        assertEquals(2, back.routines.size)
        assertEquals("喝水", back.routines[0].title)
        assertEquals(0b0010100, back.routines[1].weekdays)
        assertTrue(back.routines[1].archived)
        assertEquals(2, back.dayTasks.size)
        assertTrue(back.dayTasks[0].done)
        assertEquals(123L, back.dayTasks[0].doneAt)
        assertFalse(back.dayTasks[1].done)
        assertEquals(1, back.checks.size)
        assertEquals("2026-10-28", back.checks[0].date)
    }

    @Test
    fun `加密备份需要正确密码`() {
        val bytes = Backup.encode(snapshot, password = "s3cret")

        val back = Backup.decode(bytes, password = "s3cret")
        assertEquals(2, back.routines.size)
        assertEquals(1, back.checks.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `错误密码会被拒绝`() {
        val bytes = Backup.encode(snapshot, password = "s3cret")
        Backup.decode(bytes, password = "wrong")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `空密码读加密文件会被拒绝`() {
        val bytes = Backup.encode(snapshot, password = "s3cret")
        Backup.decode(bytes, password = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `非备份文件会被拒绝`() {
        Backup.decode("{\"foo\":1}".toByteArray(), password = "")
    }

    @Test
    fun `相同内容两次加密结果不同`() {
        val a = Backup.encode(snapshot, password = "s3cret")
        val b = Backup.encode(snapshot, password = "s3cret")
        // salt 与 iv 随机，密文不应相同
        assertFalse(a.contentEquals(b))
    }
}
