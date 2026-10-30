package com.dailybook.app.data

import java.time.LocalDate

/** LocalDate 的存储键，与 SQLite 中的 date 列一致 */
fun LocalDate.key(): String = toString()

fun LocalDate.weekdayCn(): String = when (dayOfWeek.value) {
    1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
    5 -> "周五"; 6 -> "周六"; else -> "周日"
}

fun LocalDate.monthDayCn(): String = "${monthValue}月${dayOfMonth}日"

val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

fun weekdaysText(mask: Int): String {
    if (mask == ALL_DAYS) return "每天"
    if (mask == 0) return "未选"
    if (mask == 0b0011111) return "工作日"
    if (mask == 0b1100000) return "周末"
    val picked = (0..6).filter { mask and (1 shl it) != 0 }.map { WEEK_LABELS[it] }
    return "周" + picked.joinToString("·")
}

fun todayOr(date: LocalDate): String =
    if (date == LocalDate.now()) "今天"
    else if (date == LocalDate.now().minusDays(1)) "昨天"
    else if (date == LocalDate.now().plusDays(1)) "明天"
    else date.monthDayCn()
