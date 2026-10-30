package com.dailybook.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsTest {

    private val today = LocalDate.of(2026, 10, 28)   // 周三

    private fun routine(
        id: Long,
        title: String,
        amount: Long = 0,
        kind: AmountKind = AmountKind.NONE,
        weekdays: Int = ALL_DAYS,
    ) = Routine(
        id = id,
        title = title,
        weekdays = weekdays,
        amount = amount,
        amountKind = kind,
        createdAt = 0L,
    )

    @Test
    fun `只有已完成的事务计入收支`() {
        val routines = listOf(routine(1, "签到", amount = 30, kind = AmountKind.INCOME))
        val tasks = listOf(
            DayTask(id = 10, date = today.key(), title = "买菜", done = true,
                amount = 2560, amountKind = AmountKind.EXPENSE),
            DayTask(id = 11, date = today.key(), title = "还没买", done = false,
                amount = 1000, amountKind = AmountKind.EXPENSE),
        )
        // 签到未打卡 → 不算
        val checks = emptyMap<String, RoutineCheck>()

        val state = Stats.dayState(today, routines, tasks.groupBy { it.date }, checks)

        assertEquals(0L, state.income)          // 签到没完成
        assertEquals(2560L, state.expense)      // 只有已完成的那笔
        assertEquals(-2560L, state.net)
        assertTrue(state.hasCash)
    }

    @Test
    fun `打卡后公共事务的金额才计入`() {
        val routines = listOf(routine(1, "签到", amount = 30, kind = AmountKind.INCOME))
        val checks = mapOf(
            Stats.checkKey(1, today) to RoutineCheck(1, today.key(), true, 0L, 30)
        )

        val state = Stats.dayState(today, routines, emptyMap(), checks)

        assertEquals(30L, state.income)
        assertEquals(0L, state.expense)
    }

    @Test
    fun `打卡记录里的金额优先于默认金额`() {
        // 默认 0.3 元，这次实际到账 0.5 元
        val routines = listOf(routine(1, "签到", amount = 30, kind = AmountKind.INCOME))
        val checks = mapOf(
            Stats.checkKey(1, today) to RoutineCheck(1, today.key(), true, 0L, 50)
        )

        val state = Stats.dayState(today, routines, emptyMap(), checks)

        assertEquals(50L, state.income)
    }

    @Test
    fun `打卡记录金额为 0 时回落到默认金额`() {
        val routines = listOf(routine(1, "签到", amount = 30, kind = AmountKind.INCOME))
        val checks = mapOf(
            Stats.checkKey(1, today) to RoutineCheck(1, today.key(), true, 0L, 0)
        )

        val state = Stats.dayState(today, routines, emptyMap(), checks)

        assertEquals(30L, state.income)
    }

    @Test
    fun `公共事务可以单独改某一天的金额`() {
        // 默认 0.3 元，今天实际到账 0.68 元
        val routines = listOf(routine(1, "某宝签到", amount = 30, kind = AmountKind.INCOME))
        val checks = mapOf(
            Stats.checkKey(1, today) to RoutineCheck(1, today.key(), true, 0L, 68)
        )

        val state = Stats.dayState(today, routines, emptyMap(), checks)
        assertEquals(68L, state.income)

        // 换成昨天，没改过 → 用默认值
        val yesterday = today.minusDays(1)
        val yesterdayChecks = mapOf(
            Stats.checkKey(1, yesterday) to RoutineCheck(1, yesterday.key(), true, 0L, 0)
        )
        val yesterdayState = Stats.dayState(yesterday, routines, emptyMap(), yesterdayChecks)
        assertEquals(30L, yesterdayState.income)
    }

    @Test
    fun `公共事务可以单独改某一天的方向`() {
        // 默认是收入，某天变成支出（比如退款扣回）
        val routines = listOf(routine(1, "返现", amount = 500, kind = AmountKind.INCOME))
        val checks = mapOf(
            Stats.checkKey(1, today) to RoutineCheck(
                1, today.key(), true, 0L, 500, AmountKind.EXPENSE
            )
        )

        val state = Stats.dayState(today, routines, emptyMap(), checks)

        assertEquals(0L, state.income)
        assertEquals(500L, state.expense)
    }

    @Test
    fun `改当天金额不影响默认值`() {
        val routines = listOf(routine(1, "签到", amount = 30, kind = AmountKind.INCOME))
        val checks = mapOf(
            Stats.checkKey(1, today) to RoutineCheck(1, today.key(), true, 0L, 99)
        )

        val item = Stats.dayState(today, routines, emptyMap(), checks).routines.first()

        assertEquals(99L, item.amount)          // 当天生效值
        assertEquals(30L, item.defaultAmount)   // 默认值不变
        assertTrue(item.amountOverridden)
    }

    @Test
    fun `取消勾选后金额从汇总里消失`() {
        val routines = listOf(routine(1, "签到", amount = 30, kind = AmountKind.INCOME))
        val tasks = listOf(
            DayTask(id = 10, date = today.key(), title = "买菜", done = true,
                amount = 1000, amountKind = AmountKind.EXPENSE)
        )
        val withCheck = mapOf(
            Stats.checkKey(1, today) to RoutineCheck(1, today.key(), true, 0L, 30)
        )

        val before = Stats.dayState(today, routines, tasks.groupBy { it.date }, withCheck)
        assertEquals(30L, before.income)

        // 删除打卡记录 → 收入归零
        val after = Stats.dayState(today, routines, tasks.groupBy { it.date }, emptyMap())
        assertEquals(0L, after.income)
        assertEquals(1000L, after.expense)
    }

    @Test
    fun `没有金额的事务 hasCash 为假`() {
        val routines = listOf(routine(1, "喝水"))
        val state = Stats.dayState(today, routines, emptyMap(), emptyMap())
        assertFalse(state.hasCash)
        assertEquals(0L, state.income)
    }

    @Test
    fun `dashboard 汇总本周与本月收支`() {
        val routines = listOf(
            routine(1, "每日签到", amount = 100, kind = AmountKind.INCOME)
        )
        // 本月 1 号到今天每天都打卡
        val checks = mutableListOf<RoutineCheck>()
        var d = today.withDayOfMonth(1)
        while (!d.isAfter(today)) {
            checks += RoutineCheck(1, d.key(), true, 0L, 100)
            d = d.plusDays(1)
        }
        val tasks = listOf(
            DayTask(id = 10, date = today.key(), title = "房租", done = true,
                amount = 50000, amountKind = AmountKind.EXPENSE)
        )

        val data = Stats.dashboard(today, routines, tasks, checks)

        val monthDays = today.dayOfMonth.toLong()
        assertEquals(monthDays * 100, data.monthIncome)
        assertEquals(50000L, data.monthExpense)
        // 本周从周一开始，2026-10-28 是周三 → 3 天
        assertEquals(3 * 100L, data.weekIncome)
        assertEquals(50000L, data.weekExpense)
        assertEquals(monthDays * 100, data.totalIncome)
        assertEquals(14, data.cashFlow14.size)
    }

    @Test
    fun `收支趋势按天拆分`() {
        val tasks = listOf(
            DayTask(id = 10, date = today.key(), title = "a", done = true,
                amount = 300, amountKind = AmountKind.INCOME),
            DayTask(id = 11, date = today.minusDays(1).key(), title = "b", done = true,
                amount = 700, amountKind = AmountKind.EXPENSE),
        )

        val data = Stats.dashboard(today, emptyList(), tasks, emptyList())
        val todayCash = data.cashFlow14.last()
        val yesterdayCash = data.cashFlow14[data.cashFlow14.size - 2]

        assertEquals(300L, todayCash.income)
        assertEquals(0L, todayCash.expense)
        assertEquals(700L, yesterdayCash.expense)
        assertEquals(-700L, yesterdayCash.net)
    }

    // ---------- 月度明细 ----------

    @Test
    fun `月度明细按天列出每一笔的来源`() {
        val routines = listOf(routine(1, "某宝签到", amount = 30, kind = AmountKind.INCOME))
        val checks = listOf(
            RoutineCheck(1, today.key(), true, 0L, 68),
            RoutineCheck(1, today.minusDays(1).key(), true, 0L, 0),
        )
        val tasks = listOf(
            DayTask(id = 10, date = today.key(), title = "午饭", done = true,
                amount = 1200, amountKind = AmountKind.EXPENSE),
            DayTask(id = 11, date = today.key(), title = "没做的", done = false,
                amount = 5000, amountKind = AmountKind.EXPENSE),
            DayTask(id = 12, date = today.minusDays(1).key(), title = "地铁", done = true,
                amount = 800, amountKind = AmountKind.EXPENSE),
        )

        val detail = Stats.monthCashDetail(
            java.time.YearMonth.of(2026, 10), routines, tasks, checks,
        )

        assertEquals(2, detail.days.size)
        // 倒序：今天在前
        val first = detail.days.first()
        assertEquals(today, first.date)
        assertEquals(2, first.items.size)                     // 未完成的不出现
        assertEquals(68L, first.income)                       // 用了当天的覆盖值
        assertEquals(1200L, first.expense)

        val second = detail.days.last()
        assertEquals(today.minusDays(1), second.date)
        assertEquals(30L, second.income)                      // 没有覆盖值 → 回落默认
        assertEquals(800L, second.expense)

        assertEquals(98L, detail.income)
        assertEquals(2000L, detail.expense)
        assertEquals(-1902L, detail.net)
        assertEquals(4, detail.count)
    }

    @Test
    fun `月度明细只统计当月`() {
        val tasks = listOf(
            DayTask(id = 10, date = "2026-09-30", title = "上个月", done = true,
                amount = 100, amountKind = AmountKind.EXPENSE),
            DayTask(id = 11, date = "2026-10-01", title = "本月", done = true,
                amount = 200, amountKind = AmountKind.EXPENSE),
        )

        val detail = Stats.monthCashDetail(
            java.time.YearMonth.of(2026, 10), emptyList(), tasks, emptyList(),
        )

        assertEquals(1, detail.days.size)
        assertEquals(LocalDate.of(2026, 10, 1), detail.days.first().date)
        assertEquals(200L, detail.expense)
    }

    @Test
    fun `没有收支的月份明细为空`() {
        val detail = Stats.monthCashDetail(
            java.time.YearMonth.of(2026, 10), emptyList(), emptyList(), emptyList(),
        )
        assertTrue(detail.empty)
        assertEquals(0L, detail.net)
        assertEquals(0, detail.count)
    }

    // ---------- 删除 / 归档只影响今天以后 ----------

    @Test
    fun `删除公共事务后今天起不再出现但历史照常`() {
        val removed = routine(1, "某宝签到", amount = 30, kind = AmountKind.INCOME)
            .copy(archived = true, deleted = true, endDate = today.minusDays(1).key())
        val routines = listOf(removed)
        val checks = mapOf(
            Stats.checkKey(1, today.minusDays(1)) to
                RoutineCheck(1, today.minusDays(1).key(), true, 0L, 68)
        )

        val yesterday = Stats.dayState(today.minusDays(1), routines, emptyMap(), checks)
        assertEquals(1, yesterday.routines.size)
        assertEquals(68L, yesterday.income)      // 昨天的钱还在

        val now = Stats.dayState(today, routines, emptyMap(), checks)
        assertTrue(now.isEmpty)                  // 今天不出现
        assertEquals(0L, now.income)
    }

    @Test
    fun `删除后月度与累计收支保留历史`() {
        val routines = listOf(
            routine(1, "签到", amount = 100, kind = AmountKind.INCOME)
                .copy(archived = true, deleted = true, endDate = today.minusDays(1).key())
        )
        val checks = listOf(
            RoutineCheck(1, today.minusDays(2).key(), true, 0L, 100),
            RoutineCheck(1, today.minusDays(1).key(), true, 0L, 100),
        )

        val data = Stats.dashboard(today, routines, emptyList(), checks)

        assertEquals(200L, data.monthIncome)
        assertEquals(200L, data.totalIncome)
        assertEquals(0L, data.todayIncome)
        assertTrue(data.routineStats.isEmpty())  // 坚持榜不再列已删除的
    }

    @Test
    fun `归档只影响今天以后`() {
        val archived = routine(1, "喝水").copy(archived = true, endDate = today.minusDays(1).key())

        assertEquals(1, Stats.dayState(today.minusDays(1), listOf(archived), emptyMap(), emptyMap()).routines.size)
        assertTrue(Stats.dayState(today, listOf(archived), emptyMap(), emptyMap()).isEmpty)
    }

    @Test
    fun `截止日当天仍然生效`() {
        val r = routine(1, "临时活动").copy(endDate = today.key())

        assertEquals(1, Stats.dayState(today, listOf(r), emptyMap(), emptyMap()).routines.size)
        assertTrue(Stats.dayState(today.plusDays(1), listOf(r), emptyMap(), emptyMap()).isEmpty)
    }
}
