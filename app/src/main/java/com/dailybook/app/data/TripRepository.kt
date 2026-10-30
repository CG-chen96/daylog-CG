package com.dailybook.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class TripRepository(private val dao: TripDao) {

    // ---------- 查询 ----------

    fun observeSummaries(): Flow<List<TripSummary>> = combine(
        dao.observeTrips(),
        dao.observeAllItems(),
    ) { trips, items -> TripStats.summarize(trips, items) }

    fun observeDetail(tripId: Long): Flow<TripDetail?> = combine(
        dao.observeTrips(),
        dao.observeAllItems(),
    ) { trips, items ->
        val trip = trips.firstOrNull { it.id == tripId }
        trip?.let { TripStats.detail(it, items) }
    }

    // ---------- 出游 ----------

    suspend fun addTrip(
        title: String,
        destination: String,
        start: LocalDate,
        end: LocalDate,
        note: String = "",
    ): Long {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return 0
        val safeEnd = if (end.isBefore(start)) start else end
        return dao.insertTrip(
            Trip(
                title = trimmed,
                destination = destination.trim(),
                startDate = start.key(),
                endDate = safeEnd.key(),
                note = note.trim(),
            )
        )
    }

    suspend fun updateTrip(
        trip: Trip,
        title: String,
        destination: String,
        start: LocalDate,
        end: LocalDate,
        note: String,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val safeEnd = if (end.isBefore(start)) start else end
        dao.updateTrip(
            trip.copy(
                title = trimmed,
                destination = destination.trim(),
                startDate = start.key(),
                endDate = safeEnd.key(),
                note = note.trim(),
            )
        )
    }

    /** 删出游连同它的条目一起删，不留孤儿 */
    suspend fun deleteTrip(trip: Trip) {
        dao.deleteItemsOf(trip.id)
        dao.deleteTrip(trip)
    }

    // ---------- 行程条目 ----------

    suspend fun addItem(
        tripId: Long,
        date: LocalDate,
        time: String,
        kind: TripKind,
        title: String,
        detail: String,
        amount: Long,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.insertItem(
            TripItem(
                tripId = tripId,
                date = date.key(),
                time = time.trim(),
                kind = kind,
                title = trimmed,
                detail = detail.trim(),
                amount = amount,
                sortOrder = dao.nextItemSort(tripId, date.key()),
            )
        )
    }

    suspend fun updateItem(
        item: TripItem,
        date: LocalDate,
        time: String,
        kind: TripKind,
        title: String,
        detail: String,
        amount: Long,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val moved = date.key() != item.date
        dao.updateItem(
            item.copy(
                date = date.key(),
                time = time.trim(),
                kind = kind,
                title = trimmed,
                detail = detail.trim(),
                amount = amount,
                // 换了日期就重新排到当天末尾，避免和原有顺序打架
                sortOrder = if (moved) dao.nextItemSort(item.tripId, date.key()) else item.sortOrder,
            )
        )
    }

    suspend fun deleteItem(item: TripItem) = dao.deleteItem(item)

    suspend fun findItem(id: Long): TripItem? = dao.findItem(id)

    // ---------- 备份 ----------

    suspend fun snapshot(): Pair<List<Trip>, List<TripItem>> = dao.allTrips() to dao.allItems()

    suspend fun restore(trips: List<Trip>, items: List<TripItem>, replace: Boolean) {
        if (replace) {
            dao.clearItems()
            dao.clearTrips()
            if (trips.isNotEmpty()) dao.insertTrips(trips)
            if (items.isNotEmpty()) dao.insertItems(items)
            return
        }
        // 合并：出游重新分配 id，条目跟着重映射
        var nextId = dao.maxTripId() + 1
        val idMap = HashMap<Long, Long>(trips.size)
        val fresh = trips.map { t ->
            val newId = nextId++
            idMap[t.id] = newId
            t.copy(id = newId)
        }
        if (fresh.isNotEmpty()) dao.insertTrips(fresh)
        val remapped = items.mapNotNull { i -> idMap[i.tripId]?.let { i.copy(id = 0, tripId = it) } }
        if (remapped.isNotEmpty()) dao.insertItems(remapped)
    }
}
