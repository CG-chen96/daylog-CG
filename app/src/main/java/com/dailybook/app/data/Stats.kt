package com.dailybook.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 所有统计口径集中在这里，纯函数、无副作用，方便单测与复用。
 *
 * 口径说明：
 * - 某天的「应做」= 当天命中的公共事务 + 当天手动新增的当日事务；
 * - 公共事务只统计它创建当天及之后的日期，不给"出生前"的日子记未完成；
 * - 「达标」= 当天应做数 > 0 且全部完成；
 * - 「收支」只统计已完成的事务，未完成的金额只是预期，不计入任何汇总。
 */
object Stats {

    fun checkKey(routineId: Long, date: LocalDate): String = "$routineId@${date.key()}"

    /** 公共事务当天的实际金额：打卡时记录过就用记录值，否则退回默认值 */
    private fun routineAmount(routine: Routine, check: RoutineCheck?): Long =
        if (check != null && check.amount != 0L) check.amount else routine.amount

    /** 公共事务当天的实际方向：当天改过就用当天的，否则退回默认值 */
    private fun routineKind(routine: Routine, check: RoutineCheck?): AmountKind =
        if (check != null && check.amountKind != AmountKind.NONE) check.amountKind
        else routine.amountKind

    fun dayState(
        date: LocalDate,
        routines: List<Routine>,
        tasksByDate: Map<String, List<DayTask>>,
        checks: Map<String, RoutineCheck>,
    ): DayState {
        val routineItems = routines
            .asSequence()
            .filter { it.activeOn(date) && !date.isBefore(it.createdDate) }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map { r ->
                val check = checks[checkKey(r.id, date)]
                TaskItem(
                    id = r.id,
                    kind = TaskKind.ROUTINE,
                    title = r.title,
                    note = r.note,
                    done = check?.done == true,
                    doneAt = check?.doneAt,
                    sortOrder = r.sortOrder,
                    amount = routineAmount(r, check),
                    amountKind = routineKind(r, check),
                    defaultAmount = r.amount,
                    defaultKind = r.amountKind,
                )
            }
            .toList()

        val dailyItems = tasksByDate[date.key()].orEmpty()
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .map {
                TaskItem(
                    id = it.id,
                    kind = TaskKind.DAILY,
                    title = it.title,
                    note = it.note,
                    done = it.done,
                    doneAt = it.doneAt,
                    sortOrder = it.sortOrder,
                    amount = it.amount,
                    amountKind = it.amountKind,
                )
            }

        return DayState(date, routineItems, dailyItems)
    }

    /** 某一天的收支，只算已完成的事务 */
    fun dayCash(
        date: LocalDate,
        routines: List<Routine>,
        tasksByDate: Map<String, List<DayTask>>,
        checks: Map<String, RoutineCheck>,
    ): DayCash {
        var income = 0L
        var expense = 0L

        routines.forEach { r ->
            if (!r.activeOn(date) || date.isBefore(r.createdDate)) return@forEach
            val check = checks[checkKey(r.id, date)] ?: return@forEach
            if (!check.done) return@forEach
            val amount = routineAmount(r, check)
            when (routineKind(r, check)) {
                AmountKind.INCOME -> income += amount
                AmountKind.EXPENSE -> expense += amount
                AmountKind.NONE -> Unit
            }
        }
        tasksByDate[date.key()].orEmpty().forEach { t ->
            if (!t.done) return@forEach
            when (t.amountKind) {
                AmountKind.INCOME -> income += t.amount
                AmountKind.EXPENSE -> expense += t.amount
                AmountKind.NONE -> Unit
            }
        }
        return DayCash(date, income, expense)
    }

    /**
     * 某个月的收支明细：把每天入账的每一笔都摊开，标明来自哪条事务。
     * 口径与 [dayCash] 一致——只算已完成、且填了金额和方向的。
     */
    fun monthCashDetail(
        yearMonth: YearMonth,
        routines: List<Routine>,
        dayTasks: List<DayTask>,
        checks: List<RoutineCheck>,
    ): MonthCashDetail {
        val checkMap = checks.associateBy { "${it.routineId}@${it.date}" }
        val tasksByDate = dayTasks.groupBy { it.date }
        val days = mutableListOf<DayCashDetail>()

        var cursor = yearMonth.atDay(1)
        val last = yearMonth.atEndOfMonth()
        while (!cursor.isAfter(last)) {
            val items = buildList {
                routines.forEach { r ->
                    if (!r.activeOn(cursor) || cursor.isBefore(r.createdDate)) return@forEach
                    val check = checkMap[checkKey(r.id, cursor)] ?: return@forEach
                    if (!check.done) return@forEach
                    val kind = routineKind(r, check)
                    val amount = routineAmount(r, check)
                    if (kind == AmountKind.NONE || amount == 0L) return@forEach
                    add(CashItem(r.title, TaskKind.ROUTINE, amount, kind, r.note))
                }
                tasksByDate[cursor.key()].orEmpty().forEach { t ->
                    if (!t.done || t.amountKind == AmountKind.NONE || t.amount == 0L) return@forEach
                    add(CashItem(t.title, TaskKind.DAILY, t.amount, t.amountKind, t.note))
                }
            }
            if (items.isNotEmpty()) days += DayCashDetail(cursor, items)
            cursor = cursor.plusDays(1)
        }
        return MonthCashDetail(yearMonth.year, yearMonth.monthValue, days.reversed())
    }

    private fun dayStat(
        date: LocalDate,
        routines: List<Routine>,
        tasksByDate: Map<String, List<DayTask>>,
        checks: Map<String, RoutineCheck>,
    ): DayStat {
        val hit = routines.filter { it.activeOn(date) && !date.isBefore(it.createdDate) }
        val done = hit.count { checks[checkKey(it.id, date)]?.done == true }
        val dailies = tasksByDate[date.key()].orEmpty()
        return DayStat(
            date = date,
            total = hit.size + dailies.size,
            done = done + dailies.count { it.done },
        )
    }

    fun series(
        endInclusive: LocalDate,
        days: Int,
        routines: List<Routine>,
        tasksByDate: Map<String, List<DayTask>>,
        checks: Map<String, RoutineCheck>,
    ): List<DayStat> {
        val start = endInclusive.minusDays((days - 1).toLong())
        return (0 until days).map { i ->
            dayStat(start.plusDays(i.toLong()), routines, tasksByDate, checks)
        }
    }

    /** 数据覆盖的最早日期，用于限定统计扫描范围 */
    private fun earliestDate(
        routines: List<Routine>,
        tasksByDate: Map<String, List<DayTask>>,
    ): LocalDate? {
        val candidates = buildList {
            routines.minOfOrNull { it.createdAt }?.let {
                add(java.time.Instant.ofEpochMilli(it)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            }
            tasksByDate.keys.minOrNull()?.let { add(LocalDate.parse(it)) }
        }
        return candidates.minOrNull()
    }

    /**
     * 连续达标天数：从今天往前数，遇到"应做但没做完"的日子就停。
     * 今天还没做完不算断（当天仍在进行中）；当天没有任务的日子跳过。
     */
    fun currentStreak(
        today: LocalDate,
        routines: List<Routine>,
        tasksByDate: Map<String, List<DayTask>>,
        checks: Map<String, RoutineCheck>,
    ): Int {
        var streak = 0
        var cursor = today
        val floor = today.minusDays(365)
        while (!cursor.isBefore(floor)) {
            val stat = dayStat(cursor, routines, tasksByDate, checks)
            when {
                stat.total == 0 -> Unit                    // 空日：跳过
                stat.allDone -> streak++
                cursor == today -> Unit                    // 今天进行中：不打断
                else -> break
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun bestStreak(
        today: LocalDate,
        routines: List<Routine>,
        tasksByDate: Map<String, List<DayTask>>,
        checks: Map<String, RoutineCheck>,
    ): Int {
        val start = earliestDate(routines, tasksByDate) ?: return 0
        val from = if (ChronoUnit.DAYS.between(start, today) > 365) today.minusDays(365) else start
        var best = 0
        var run = 0
        var cursor = from
        while (!cursor.isAfter(today)) {
            val stat = dayStat(cursor, routines, tasksByDate, checks)
            if (stat.allDone) {
                run++
                if (run > best) best = run
            } else if (stat.total > 0) {
                run = 0
            }
            cursor = cursor.plusDays(1)
        }
        return best
    }

    fun dashboard(
        today: LocalDate,
        routines: List<Routine>,
        dayTasks: List<DayTask>,
        checks: List<RoutineCheck>,
    ): DashboardData {
        val tasksByDate = dayTasks.groupBy { it.date }
        val checkMap = checks.associateBy { "${it.routineId}@${it.date}" }

        val todayState = dayState(today, routines, tasksByDate, checkMap)
        val last14 = series(today, 14, routines, tasksByDate, checkMap)

        // 热力图：本周一往前推 11 周，共 12 周 84 天
        val thisMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val heatStart = thisMonday.minusWeeks(11)
        val heatmap = series(today, ChronoUnit.DAYS.between(heatStart, today).toInt() + 1,
            routines, tasksByDate, checkMap)

        // 本周（周一至今天）
        val weekStats = (0..(today.dayOfWeek.value - 1)).map {
            dayStat(thisMonday.plusDays(it.toLong()), routines, tasksByDate, checkMap)
        }

        // 全量累计
        val start = earliestDate(routines, tasksByDate) ?: today
        val allSeries = series(today, ChronoUnit.DAYS.between(start, today).toInt() + 1,
            routines, tasksByDate, checkMap)

        // 公共事务近 30 天坚持度
        val routineFrom = today.minusDays(29)
        val routineStats = routines.filter { !it.archived && !it.deleted }.map { r ->
            var expected = 0
            var completed = 0
            var cursor = if (r.createdDate.isAfter(routineFrom)) r.createdDate else routineFrom
            while (!cursor.isAfter(today)) {
                if (r.activeOn(cursor)) {
                    expected++
                    if (checkMap[checkKey(r.id, cursor)]?.done == true) completed++
                }
                cursor = cursor.plusDays(1)
            }
            RoutineStat(r, expected, completed)
        }.sortedByDescending { it.ratio }

        // ---- 收支 ----
        val cashFlow14 = (0 until 14).map {
            dayCash(today.minusDays((13 - it).toLong()), routines, tasksByDate, checkMap)
        }
        val weekCash = (0..(today.dayOfWeek.value - 1)).map {
            dayCash(thisMonday.plusDays(it.toLong()), routines, tasksByDate, checkMap)
        }
        val monthFirst = today.withDayOfMonth(1)
        val monthCash = buildList {
            var cursor = monthFirst
            while (!cursor.isAfter(today)) {
                add(dayCash(cursor, routines, tasksByDate, checkMap))
                cursor = cursor.plusDays(1)
            }
        }

        // 累计直接遍历明细，避免按天重算
        var totalIncome = 0L
        var totalExpense = 0L
        dayTasks.forEach { t ->
            if (!t.done) return@forEach
            when (t.amountKind) {
                AmountKind.INCOME -> totalIncome += t.amount
                AmountKind.EXPENSE -> totalExpense += t.amount
                AmountKind.NONE -> Unit
            }
        }
        val routineMap = routines.associateBy { it.id }
        checks.forEach { c ->
            val r = routineMap[c.routineId] ?: return@forEach
            if (!c.done) return@forEach
            val amount = routineAmount(r, c)
            when (routineKind(r, c)) {
                AmountKind.INCOME -> totalIncome += amount
                AmountKind.EXPENSE -> totalExpense += amount
                AmountKind.NONE -> Unit
            }
        }

        return DashboardData(
            today = todayState,
            last14 = last14,
            heatmap = heatmap,
            streak = currentStreak(today, routines, tasksByDate, checkMap),
            bestStreak = bestStreak(today, routines, tasksByDate, checkMap),
            weekDone = weekStats.sumOf { it.done },
            weekTotal = weekStats.sumOf { it.total },
            totalDone = allSeries.sumOf { it.done },
            totalTasks = allSeries.sumOf { it.total },
            routineStats = routineStats,
            activeDays = allSeries.count { it.done > 0 },
            todayIncome = todayState.income,
            todayExpense = todayState.expense,
            weekIncome = weekCash.sumOf { it.income },
            weekExpense = weekCash.sumOf { it.expense },
            monthIncome = monthCash.sumOf { it.income },
            monthExpense = monthCash.sumOf { it.expense },
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            cashFlow14 = cashFlow14,
        )
    }

    /** 热力图列分组：按自然周（周一→周日）切分 */
    fun toWeeks(stats: List<DayStat>): List<List<DayStat?>> {
        if (stats.isEmpty()) return emptyList()
        val weeks = mutableListOf<MutableList<DayStat?>>()
        var current = MutableList<DayStat?>(7) { null }
        for (s in stats) {
            val idx = s.date.dayOfWeek.value - 1
            if (idx == 0 && current.any { it != null }) {
                weeks += current
                current = MutableList<DayStat?>(7) { null }
            }
            current[idx] = s
        }
        if (current.any { it != null }) weeks += current
        return weeks
    }

    fun DayOfWeek.shortCn(): String = when (value) {
        1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
        5 -> "周五"; 6 -> "周六"; else -> "周日"
    }
}
