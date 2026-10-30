package com.dailybook.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    // ---------- 出游 ----------
    @Query("SELECT * FROM trip ORDER BY startDate DESC, id DESC")
    fun observeTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trip ORDER BY startDate DESC, id DESC")
    suspend fun allTrips(): List<Trip>

    @Query("SELECT * FROM trip WHERE id = :id")
    suspend fun findTrip(id: Long): Trip?

    @Query("SELECT COALESCE(MAX(id), 0) FROM trip")
    suspend fun maxTripId(): Long

    @Insert
    suspend fun insertTrip(trip: Trip): Long

    @Insert
    suspend fun insertTrips(list: List<Trip>)

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trip")
    suspend fun clearTrips()

    // ---------- 行程条目 ----------
    @Query("SELECT * FROM trip_item ORDER BY date ASC, sortOrder ASC, id ASC")
    fun observeAllItems(): Flow<List<TripItem>>

    @Query("SELECT * FROM trip_item ORDER BY date ASC, sortOrder ASC, id ASC")
    suspend fun allItems(): List<TripItem>

    @Query("SELECT * FROM trip_item WHERE tripId = :tripId ORDER BY date ASC, sortOrder ASC, id ASC")
    suspend fun itemsOf(tripId: Long): List<TripItem>

    @Query("SELECT * FROM trip_item WHERE id = :id")
    suspend fun findItem(id: Long): TripItem?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM trip_item WHERE tripId = :tripId AND date = :date")
    suspend fun nextItemSort(tripId: Long, date: String): Int

    @Insert
    suspend fun insertItem(item: TripItem): Long

    @Insert
    suspend fun insertItems(list: List<TripItem>)

    @Update
    suspend fun updateItem(item: TripItem)

    @Delete
    suspend fun deleteItem(item: TripItem)

    @Query("DELETE FROM trip_item WHERE tripId = :tripId")
    suspend fun deleteItemsOf(tripId: Long)

    @Query("DELETE FROM trip_item")
    suspend fun clearItems()
}
