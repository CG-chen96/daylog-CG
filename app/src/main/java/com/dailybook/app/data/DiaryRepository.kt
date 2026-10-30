package com.dailybook.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

class DiaryRepository(private val dao: DiaryDao) {

    // ---------- 查询 ----------

    fun observeDay(date: LocalDate): Flow<DayState> = combine(
        dao.observeRoutines(),
        dao.observeDayTasks(date.key()),
        dao.observeChecks(date.key()),
    ) { routines, tasks, checks ->
        Stats.dayState(
            date = date,
            routines = routines,
            tasksByDate = tasks.groupBy { it.date },
            checks = checks.associateBy { Stats.checkKey(it.routineId, date) },
        )
    }

    fun observeRoutines(): Flow<List<Routine>> = dao.observeRoutines()

    fun observeDashboard(today: LocalDate = LocalDate.now()): Flow<DashboardData> = combine(
        dao.observeRoutines(),
        dao.observeAllDayTasks(),
        dao.observeAllChecks(),
    ) { routines, tasks, checks ->
        Stats.dashboard(today, routines, tasks, checks)
    }

    /** 月度收支明细：只读汇总，不落库 */
    fun observeMonthDetail(month: YearMonth): Flow<MonthCashDetail> = combine(
        dao.observeRoutines(),
        dao.observeAllDayTasks(),
        dao.observeAllChecks(),
    ) { routines, tasks, checks ->
        Stats.monthCashDetail(month, routines, tasks, checks)
    }

    // ---------- 勾选 ----------

    /** 切换完成状态；公共事务写打卡表，当日事务改自己那一行 */
    suspend fun toggle(item: TaskItem, date: LocalDate) {
        val now = System.currentTimeMillis()
        when (item.kind) {
            TaskKind.ROUTINE -> {
                if (item.done) {
                    dao.deleteCheck(item.id, date.key())
                } else {
                    // 打卡时把当天金额固定下来，之后改默认金额不影响历史
                    dao.upsertCheck(
                        RoutineCheck(
                            routineId = item.id,
                            date = date.key(),
                            done = true,
                            doneAt = now,
                            amount = item.amount,
                            amountKind = item.amountKind,
                        )
                    )
                }
            }
            TaskKind.DAILY -> {
                val task = dao.allDayTasks().firstOrNull { it.id == item.id } ?: return
                dao.updateDayTask(
                    task.copy(done = !task.done, doneAt = if (task.done) null else now)
                )
            }
        }
    }

    /**
     * 单独改某一天的实际金额，不影响公共事务的默认值。
     * 未打卡时也会写入一条未完成记录，之后勾选就按这个金额入账。
     */
    suspend fun setRoutineDayAmount(
        routineId: Long,
        date: LocalDate,
        amount: Long,
        kind: AmountKind,
    ) {
        val existing = dao.findCheck(routineId, date.key())
        val resolvedKind = if (amount == 0L) AmountKind.NONE else kind
        dao.upsertCheck(
            existing?.copy(amount = amount, amountKind = resolvedKind)
                ?: RoutineCheck(
                    routineId = routineId,
                    date = date.key(),
                    done = false,
                    doneAt = null,
                    amount = amount,
                    amountKind = resolvedKind,
                )
        )
    }

    // ---------- 当日事务 ----------

    suspend fun addDaily(
        date: LocalDate,
        title: String,
        note: String = "",
        amount: Long = 0,
        amountKind: AmountKind = AmountKind.NONE,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.insertDayTask(
            DayTask(
                date = date.key(),
                title = trimmed,
                note = note.trim(),
                sortOrder = dao.nextDaySort(date.key()),
                amount = amount,
                amountKind = if (amount == 0L) AmountKind.NONE else amountKind,
            )
        )
    }

    suspend fun updateDaily(
        task: DayTask,
        title: String,
        note: String,
        amount: Long = task.amount,
        amountKind: AmountKind = task.amountKind,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.updateDayTask(
            task.copy(
                title = trimmed,
                note = note.trim(),
                amount = amount,
                amountKind = if (amount == 0L) AmountKind.NONE else amountKind,
            )
        )
    }

    suspend fun deleteDaily(task: DayTask) = dao.deleteDayTask(task)

    suspend fun findDayTask(id: Long): DayTask? = dao.findDayTask(id)

    suspend fun moveToNextDay(task: DayTask) {
        val next = LocalDate.parse(task.date).plusDays(1)
        dao.updateDayTask(task.copy(date = next.key(), sortOrder = dao.nextDaySort(next.key())))
    }

    // ---------- 公共事务 ----------

    suspend fun addRoutine(
        title: String,
        note: String,
        weekdays: Int,
        amount: Long = 0,
        amountKind: AmountKind = AmountKind.NONE,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.insertRoutine(
            Routine(
                title = trimmed,
                note = note.trim(),
                weekdays = weekdays,
                sortOrder = dao.nextRoutineSort(),
                amount = amount,
                amountKind = if (amount == 0L) AmountKind.NONE else amountKind,
            )
        )
    }

    suspend fun updateRoutine(
        routine: Routine,
        title: String,
        note: String,
        weekdays: Int,
        amount: Long = routine.amount,
        amountKind: AmountKind = routine.amountKind,
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.updateRoutine(
            routine.copy(
                title = trimmed,
                note = note.trim(),
                weekdays = weekdays,
                amount = amount,
                amountKind = if (amount == 0L) AmountKind.NONE else amountKind,
            )
        )
    }

    /** 归档 = 从今天起不再出现，昨天及更早的历史原样保留；取消归档则恢复 */
    suspend fun setArchived(routine: Routine, archived: Boolean, today: LocalDate) =
        dao.updateRoutine(
            routine.copy(
                archived = archived,
                endDate = if (archived) today.minusDays(1).key() else null,
            )
        )

    /**
     * 删除 = 从今天起摘掉，今天及以后的打卡清掉，之前的历史（含金额）全部保留。
     * 今天刚建、且没有任何历史打卡的事务没什么可留的，直接物理删除。
     *
     * @return true 表示保留了历史，false 表示彻底删掉了
     */
    suspend fun deleteRoutine(routine: Routine, today: LocalDate): Boolean {
        val keepHistory = routine.createdDate.isBefore(today) ||
            dao.hasChecksBefore(routine.id, today.key())
        if (!keepHistory) {
            dao.deleteChecksOf(routine.id)
            dao.deleteRoutine(routine)
            return false
        }
        dao.deleteChecksFrom(routine.id, today.key())
        dao.updateRoutine(
            routine.copy(
                archived = true,
                deleted = true,
                endDate = today.minusDays(1).key(),
            )
        )
        return true
    }

    suspend fun reorderRoutines(ordered: List<Routine>) {
        ordered.forEachIndexed { index, r -> dao.updateRoutine(r.copy(sortOrder = index)) }
    }

    // ---------- 备份 ----------

    suspend fun exportSnapshot(): Triple<List<Routine>, List<DayTask>, List<RoutineCheck>> =
        Triple(dao.routines(), dao.allDayTasks(), dao.allChecks())

    suspend fun restore(
        routines: List<Routine>,
        tasks: List<DayTask>,
        checks: List<RoutineCheck>,
        replace: Boolean,
    ) {
        if (replace) {
            dao.clearChecks()
            dao.clearDayTasks()
            dao.clearRoutines()
            if (routines.isNotEmpty()) dao.insertRoutines(routines)
            if (tasks.isNotEmpty()) dao.insertDayTasks(tasks)
            if (checks.isNotEmpty()) dao.insertChecks(checks)
            return
        }

        // 合并模式：公共事务重新分配 id，打卡记录跟着重映射，避免主键冲突
        var nextId = dao.maxRoutineId() + 1
        val idMap = HashMap<Long, Long>(routines.size)
        val fresh = routines.map { r ->
            val newId = nextId++
            idMap[r.id] = newId
            r.copy(id = newId)
        }
        if (fresh.isNotEmpty()) dao.insertRoutines(fresh)
        // 当日事务 id 置 0 交给自增，不会覆盖已有记录
        if (tasks.isNotEmpty()) dao.insertDayTasks(tasks.map { it.copy(id = 0) })
        val remapped = checks.mapNotNull { c -> idMap[c.routineId]?.let { c.copy(routineId = it) } }
        if (remapped.isNotEmpty()) dao.insertChecks(remapped)
    }
}
