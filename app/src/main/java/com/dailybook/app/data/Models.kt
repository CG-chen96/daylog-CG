package com.dailybook.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 星期位掩码：bit0=周一 … bit6=周日 */
const val ALL_DAYS = 0b1111111

/** 金额方向。存数据库时是 Int，见 [Converters] */
enum class AmountKind(val value: Int) {
    NONE(0),
    INCOME(1),
    EXPENSE(2);

    companion object {
        fun of(value: Int): AmountKind = entries.firstOrNull { it.value == value } ?: NONE
    }
}

/**
 * 公共事务：每天（或指定星期）自动出现在当日清单里。
 * 只存"规则"，不预生成每日记录，避免历史被后来的改动污染。
 *
 * [amount] 是默认金额，打卡时写入 routine_check 固定下来，
 * 之后改默认金额不会影响已打卡的历史。
 */
@Entity(tableName = "routine")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val weekdays: Int = ALL_DAYS,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    /** 创建时间，用于计算历史统计时不给"出生前"的日子记未完成 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 默认金额，单位：分 */
    val amount: Long = 0,
    val amountKind: AmountKind = AmountKind.NONE,
    /**
     * 最后一个生效日（yyyy-MM-dd，含当天），null 表示一直有效。
     * 删除/归档只把截止日定到昨天，之前的日子照旧算这项事务。
     */
    val endDate: String? = null,
    /** 已删除：只在「事务」页隐藏，历史日期仍按 [endDate] 正常回溯 */
    val deleted: Boolean = false,
) {
    val createdDate: LocalDate
        get() = java.time.Instant.ofEpochMilli(createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

    private val lastDay: LocalDate? by lazy {
        endDate?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    fun activeOn(day: LocalDate): Boolean =
        (weekdays and (1 shl (day.dayOfWeek.value - 1))) != 0 &&
            (lastDay == null || !day.isAfter(lastDay))

    /** 每日重复 */
    val isEveryday: Boolean get() = weekdays == ALL_DAYS
}

/** 当日事务：手动新增，绑定某一天 */
@Entity(tableName = "day_task", indices = [Index("date")])
data class DayTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** yyyy-MM-dd */
    val date: String,
    val title: String,
    val note: String = "",
    val done: Boolean = false,
    val doneAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** 金额，单位：分 */
    val amount: Long = 0,
    val amountKind: AmountKind = AmountKind.NONE,
)

/** 公共事务在某一天的完成状态（每天一行，未完成时不落库） */
@Entity(tableName = "routine_check", primaryKeys = ["routineId", "date"])
data class RoutineCheck(
    val routineId: Long,
    /** yyyy-MM-dd */
    val date: String,
    val done: Boolean = true,
    val doneAt: Long? = System.currentTimeMillis(),
    /** 当天实际入账的金额，单位：分。0 表示沿用 [Routine.amount] */
    val amount: Long = 0,
    /** 当天实际方向，NONE 表示沿用 [Routine.amountKind] */
    val amountKind: AmountKind = AmountKind.NONE,
)

enum class TaskKind { ROUTINE, DAILY }

/**
 * 出游条目的类别。只影响图标、配色和分类小计，
 * 与每日事务的 [TaskKind] 无关——两个模块互不引用。
 */
enum class TripKind(val value: Int, val label: String) {
    TRANSPORT(0, "交通"),
    FOOD(1, "餐饮"),
    STAY(2, "住宿"),
    SHOPPING(3, "购物"),
    SIGHT(4, "景点"),
    OTHER(5, "其他");

    companion object {
        fun of(value: Int): TripKind = entries.firstOrNull { it.value == value } ?: OTHER
    }
}

/**
 * 一次出游。和每日事务完全独立：不生成待办、不进看板、不计入收支报表。
 * 只描述"某段时间去了哪、花了多少"。
 */
@Entity(tableName = "trip")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String = "",
    /** yyyy-MM-dd */
    val startDate: String,
    /** yyyy-MM-dd，与 startDate 相同表示当天往返 */
    val endDate: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val start: LocalDate get() = LocalDate.parse(startDate)
    val end: LocalDate get() = LocalDate.parse(endDate.ifEmpty { startDate })

    /** 行程天数（含首尾） */
    val dayCount: Int
        get() = (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)

    fun covers(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    /** 第几天（从 1 开始），不在范围内返回 0 */
    fun dayIndex(date: LocalDate): Int =
        if (covers(date)) (ChronoUnit.DAYS.between(start, date).toInt() + 1) else 0
}

/** 出游里的一条记录：一段路、一顿饭、一次购物、一个景点…… */
@Entity(tableName = "trip_item", indices = [Index("tripId"), Index("date")])
data class TripItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    /** yyyy-MM-dd，落在所属出游的日期范围内 */
    val date: String,
    /** 可选时间点，如 "11:20"；为空表示不关心具体时刻 */
    val time: String = "",
    val kind: TripKind = TripKind.OTHER,
    val title: String,
    /** 补充说明：交通方式、耗时、买了什么 */
    val detail: String = "",
    /** 花费，单位：分。0 表示这一项没花钱 */
    val amount: Long = 0,
    val sortOrder: Int = 0,
)

/** 某一天的清单条目，由「公共事务规则 + 当日事务 + 打卡记录」合成 */
data class TaskItem(
    val id: Long,
    val kind: TaskKind,
    val title: String,
    val note: String,
    val done: Boolean,
    val doneAt: Long?,
    val sortOrder: Int = 0,
    val amount: Long = 0,
    val amountKind: AmountKind = AmountKind.NONE,
    /** 公共事务的默认金额，仅用于「恢复默认」提示，当日事务恒为 0 */
    val defaultAmount: Long = 0,
    val defaultKind: AmountKind = AmountKind.NONE,
) {
    val key: String get() = (if (kind == TaskKind.ROUTINE) "r" else "d") + id
    val hasAmount: Boolean get() = amount != 0L && amountKind != AmountKind.NONE

    /** 当天金额是否已被单独改过 */
    val amountOverridden: Boolean
        get() = kind == TaskKind.ROUTINE && (amount != defaultAmount || amountKind != defaultKind)
}

data class DayState(
    val date: LocalDate = LocalDate.now(),
    val routines: List<TaskItem> = emptyList(),
    val dailies: List<TaskItem> = emptyList(),
) {
    val total: Int get() = routines.size + dailies.size
    val doneCount: Int get() = routines.count { it.done } + dailies.count { it.done }
    val progress: Float get() = if (total == 0) 0f else doneCount.toFloat() / total
    val isEmpty: Boolean get() = total == 0

    private val all: List<TaskItem> get() = routines + dailies
    private val settled: List<TaskItem> get() = all.filter { it.done && it.hasAmount }

    /** 只有已完成的事务才计入收支，未完成只表示"预期" */
    val income: Long get() = settled.filter { it.amountKind == AmountKind.INCOME }.sumOf { it.amount }
    val expense: Long get() = settled.filter { it.amountKind == AmountKind.EXPENSE }.sumOf { it.amount }
    val net: Long get() = income - expense
    val hasCash: Boolean get() = all.any { it.hasAmount }
}

data class DayStat(val date: LocalDate, val total: Int, val done: Int) {
    val ratio: Float get() = if (total == 0) 0f else done.toFloat() / total
    val allDone: Boolean get() = total > 0 && done >= total
}

data class DayCash(val date: LocalDate, val income: Long, val expense: Long) {
    val net: Long get() = income - expense
    val empty: Boolean get() = income == 0L && expense == 0L
}

data class RoutineStat(
    val routine: Routine,
    val expected: Int,
    val completed: Int,
) {
    val ratio: Float get() = if (expected == 0) 0f else completed.toFloat() / expected
}

data class DashboardData(
    val today: DayState = DayState(),
    val last14: List<DayStat> = emptyList(),
    val heatmap: List<DayStat> = emptyList(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val weekDone: Int = 0,
    val weekTotal: Int = 0,
    val totalDone: Int = 0,
    val totalTasks: Int = 0,
    val routineStats: List<RoutineStat> = emptyList(),
    val activeDays: Int = 0,
    // ---- 金额（单位：分）----
    val todayIncome: Long = 0,
    val todayExpense: Long = 0,
    val weekIncome: Long = 0,
    val weekExpense: Long = 0,
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val cashFlow14: List<DayCash> = emptyList(),
) {
    val weekRatio: Float get() = if (weekTotal == 0) 0f else weekDone.toFloat() / weekTotal
    val overallRatio: Float get() = if (totalTasks == 0) 0f else totalDone.toFloat() / totalTasks

    val weekNet: Long get() = weekIncome - weekExpense
    val monthNet: Long get() = monthIncome - monthExpense
    val totalNet: Long get() = totalIncome - totalExpense
    val hasCash: Boolean get() = totalIncome != 0L || totalExpense != 0L
}

// ---------- 月度收支明细 ----------

/** 一笔有来源的收支：来自哪条事务、多少、什么方向 */
data class CashItem(
    val title: String,
    val kind: TaskKind,
    val amount: Long,
    val amountKind: AmountKind,
    val note: String = "",
)

data class DayCashDetail(val date: LocalDate, val items: List<CashItem>) {
    val income: Long get() = items.filter { it.amountKind == AmountKind.INCOME }.sumOf { it.amount }
    val expense: Long get() = items.filter { it.amountKind == AmountKind.EXPENSE }.sumOf { it.amount }
    val net: Long get() = income - expense
}

/** 某个月的收支明细，只包含真正有钱进出的日子，按日期倒序 */
data class MonthCashDetail(
    val year: Int = 0,
    val month: Int = 0,
    val days: List<DayCashDetail> = emptyList(),
) {
    val income: Long get() = days.sumOf { it.income }
    val expense: Long get() = days.sumOf { it.expense }
    val net: Long get() = income - expense
    val count: Int get() = days.sumOf { it.items.size }
    val empty: Boolean get() = days.isEmpty()
}

// ---------- 出游 ----------

/** 列表页用：一次出游 + 它的花费概况 */
data class TripSummary(
    val trip: Trip,
    val total: Long = 0,
    val itemCount: Int = 0,
) {
    val empty: Boolean get() = itemCount == 0
}

data class TripKindTotal(val kind: TripKind, val total: Long, val count: Int)

/** 出游里的一天 */
data class TripDay(val date: LocalDate, val items: List<TripItem>) {
    val total: Long get() = items.sumOf { it.amount }
}

data class TripDetail(
    val trip: Trip,
    val days: List<TripDay> = emptyList(),
    val byKind: List<TripKindTotal> = emptyList(),
) {
    val total: Long get() = days.sumOf { it.total }
    val itemCount: Int get() = days.sumOf { it.items.size }
    val empty: Boolean get() = itemCount == 0
    val perDay: Long get() = if (days.isEmpty()) 0 else total / days.size
}
