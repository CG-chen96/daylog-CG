package com.dailybook.app.data

import java.time.LocalDate

/**
 * 出游的统计口径。刻意和 [Stats] 分开：
 * 出游是独立模块，不参与完成率、连续达标和收支报表。
 */
object TripStats {

    fun summarize(trips: List<Trip>, items: List<TripItem>): List<TripSummary> {
        val byTrip = items.groupBy { it.tripId }
        return trips.map { t ->
            val own = byTrip[t.id].orEmpty()
            TripSummary(trip = t, total = own.sumOf { it.amount }, itemCount = own.size)
        }
    }

    /**
     * 详情：按行程日期铺满整个区间（没记录的天也留一行，方便按天补记），
     * 同一天内按时间排序，没填时间的排在最后。
     * 区间外的条目（缩短行程后遗留的）不计入，但也不会被删除。
     */
    fun detail(trip: Trip, items: List<TripItem>): TripDetail {
        val own = items
            .filter { it.tripId == trip.id }
            .filter { runCatching { trip.covers(LocalDate.parse(it.date)) }.getOrDefault(false) }
            .sortedWith(compareBy({ it.time.ifEmpty { "99:99" } }, { it.sortOrder }, { it.id }))
        val byDate = own.groupBy { it.date }

        val days = (0 until trip.dayCount).map { i ->
            val date = trip.start.plusDays(i.toLong())
            TripDay(date, byDate[date.key()].orEmpty())
        }

        val byKind = TripKind.entries
            .map { k ->
                val list = own.filter { it.kind == k }
                TripKindTotal(k, list.sumOf { it.amount }, list.size)
            }
            .filter { it.count > 0 }
            .sortedByDescending { it.total }

        return TripDetail(trip = trip, days = days, byKind = byKind)
    }
}
