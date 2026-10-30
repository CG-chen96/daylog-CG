package com.dailybook.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    // ---------- 公共事务 ----------
    @Query("SELECT * FROM routine ORDER BY archived ASC, sortOrder ASC, id ASC")
    fun observeRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routine ORDER BY archived ASC, sortOrder ASC, id ASC")
    suspend fun routines(): List<Routine>

    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Query("DELETE FROM routine_check WHERE routineId = :routineId")
    suspend fun deleteChecksOf(routineId: Long)

    /** 只清掉 fromDate 及以后的打卡，之前的历史留着 */
    @Query("DELETE FROM routine_check WHERE routineId = :routineId AND date >= :fromDate")
    suspend fun deleteChecksFrom(routineId: Long, fromDate: String)

    @Query("SELECT EXISTS(SELECT 1 FROM routine_check WHERE routineId = :routineId AND date < :before)")
    suspend fun hasChecksBefore(routineId: Long, before: String): Boolean

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM routine")
    suspend fun nextRoutineSort(): Int

    @Query("SELECT COALESCE(MAX(id), 0) FROM routine")
    suspend fun maxRoutineId(): Long

    // ---------- 当日事务 ----------
    @Query("SELECT * FROM day_task WHERE date = :date ORDER BY sortOrder ASC, id ASC")
    fun observeDayTasks(date: String): Flow<List<DayTask>>

    @Query("SELECT * FROM day_task ORDER BY date ASC, sortOrder ASC, id ASC")
    fun observeAllDayTasks(): Flow<List<DayTask>>

    @Query("SELECT * FROM day_task ORDER BY date ASC, sortOrder ASC, id ASC")
    suspend fun allDayTasks(): List<DayTask>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM day_task WHERE date = :date")
    suspend fun nextDaySort(date: String): Int

    @Query("SELECT * FROM day_task WHERE id = :id")
    suspend fun findDayTask(id: Long): DayTask?

    @Insert
    suspend fun insertDayTask(task: DayTask): Long

    @Update
    suspend fun updateDayTask(task: DayTask)

    @Delete
    suspend fun deleteDayTask(task: DayTask)

    // ---------- 打卡记录 ----------
    @Query("SELECT * FROM routine_check WHERE date = :date")
    fun observeChecks(date: String): Flow<List<RoutineCheck>>

    @Query("SELECT * FROM routine_check")
    fun observeAllChecks(): Flow<List<RoutineCheck>>

    @Query("SELECT * FROM routine_check")
    suspend fun allChecks(): List<RoutineCheck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheck(check: RoutineCheck)

    @Query("SELECT * FROM routine_check WHERE routineId = :routineId AND date = :date")
    suspend fun findCheck(routineId: Long, date: String): RoutineCheck?

    @Query("DELETE FROM routine_check WHERE routineId = :routineId AND date = :date")
    suspend fun deleteCheck(routineId: Long, date: String)

    // ---------- 备份 / 恢复 ----------
    @Insert
    suspend fun insertRoutines(list: List<Routine>)

    @Insert
    suspend fun insertDayTasks(list: List<DayTask>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecks(list: List<RoutineCheck>)

    @Query("DELETE FROM routine")
    suspend fun clearRoutines()

    @Query("DELETE FROM day_task")
    suspend fun clearDayTasks()

    @Query("DELETE FROM routine_check")
    suspend fun clearChecks()
}
