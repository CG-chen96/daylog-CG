package com.dailybook.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailybook.app.data.AmountKind
import com.dailybook.app.data.AppDatabase
import com.dailybook.app.data.Backup
import com.dailybook.app.data.DashboardData
import com.dailybook.app.data.DayState
import com.dailybook.app.data.DayTask
import com.dailybook.app.data.DiaryRepository
import com.dailybook.app.data.MonthCashDetail
import com.dailybook.app.data.Routine
import com.dailybook.app.data.Snapshot
import com.dailybook.app.data.TaskItem
import com.dailybook.app.data.Trip
import com.dailybook.app.data.TripDetail
import com.dailybook.app.data.TripItem
import com.dailybook.app.data.TripKind
import com.dailybook.app.data.TripRepository
import com.dailybook.app.data.TripSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = DiaryRepository(db.dao())
    private val tripRepo = TripRepository(db.tripDao())

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date

    /** 看板永远以"真实的今天"为准，跨天后由 refreshToday 推进 */
    private val _today = MutableStateFlow(LocalDate.now())
    private var lastKnownToday = LocalDate.now()

    val day: StateFlow<DayState> = _date
        .flatMapLatest { repo.observeDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayState())

    val dashboard: StateFlow<DashboardData> = _today
        .flatMapLatest { repo.observeDashboard(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardData())

    val routines: StateFlow<List<Routine>> = repo.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- 月度收支明细（null = 未打开）----------

    private val _cashMonth = MutableStateFlow<YearMonth?>(null)
    val cashMonth: StateFlow<YearMonth?> = _cashMonth

    val monthDetail: StateFlow<MonthCashDetail> = _cashMonth
        .flatMapLatest { m -> if (m == null) flowOf(MonthCashDetail()) else repo.observeMonthDetail(m) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthCashDetail())

    fun openCashMonth() {
        _cashMonth.value = YearMonth.now()
    }

    fun closeCashMonth() {
        _cashMonth.value = null
    }

    fun shiftCashMonth(months: Long) {
        _cashMonth.value = _cashMonth.value?.plusMonths(months)
    }

    // ---------- 出游（null = 在列表页）----------

    val trips: StateFlow<List<TripSummary>> = tripRepo.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _openTripId = MutableStateFlow<Long?>(null)
    val openTripId: StateFlow<Long?> = _openTripId

    val tripDetail: StateFlow<TripDetail?> = _openTripId
        .flatMapLatest { id -> if (id == null) flowOf(null) else tripRepo.observeDetail(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun openTrip(id: Long) {
        _openTripId.value = id
    }

    fun closeTrip() {
        _openTripId.value = null
    }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private fun notify(text: String) {
        _messages.tryEmit(text)
    }

    // ---------- 日期 ----------

    fun setDate(target: LocalDate) {
        _date.value = target
    }

    fun shiftDate(days: Long) {
        _date.value = _date.value.plusDays(days)
    }

    fun goToday() {
        _date.value = LocalDate.now()
    }

    /** 应用回到前台时调用：跨天了就自动跳到新的今天 */
    fun refreshToday() {
        val now = LocalDate.now()
        if (now == lastKnownToday) return
        if (_date.value == lastKnownToday) _date.value = now
        lastKnownToday = now
        _today.value = now
    }

    // ---------- 勾选 ----------

    fun toggle(item: TaskItem) {
        viewModelScope.launch { repo.toggle(item, _date.value) }
    }

    // ---------- 当日事务 ----------

    fun addDaily(title: String, note: String, amount: Long, kind: AmountKind) {
        viewModelScope.launch {
            repo.addDaily(_date.value, title, note, amount, kind)
            notify(if (amount > 0) "已加入当天，并记了一笔" else "已加入当天")
        }
    }

    fun updateDaily(id: Long, title: String, note: String, amount: Long, kind: AmountKind) {
        viewModelScope.launch {
            val task = repo.findDayTask(id) ?: return@launch
            repo.updateDaily(task, title, note, amount, kind)
        }
    }

    fun deleteDaily(id: Long) {
        viewModelScope.launch {
            val task = repo.findDayTask(id) ?: return@launch
            repo.deleteDaily(task)
            notify("已删除")
        }
    }

    fun moveToNextDay(id: Long) {
        viewModelScope.launch {
            val task = repo.findDayTask(id) ?: return@launch
            repo.moveToNextDay(task)
            notify("已顺延到明天")
        }
    }

    /** 把某条当日事务原样复制到另一天 */
    fun copyDaily(id: Long, target: LocalDate) {
        viewModelScope.launch {
            val task = repo.findDayTask(id) ?: return@launch
            repo.addDaily(target, task.title, task.note)
            notify("已复制到 ${target.monthValue}月${target.dayOfMonth}日")
        }
    }

    // ---------- 公共事务 ----------

    /** 改公共事务在某一天的实际金额，不影响默认值 */
    fun setDayAmount(item: TaskItem, amount: Long, kind: AmountKind) {
        viewModelScope.launch {
            repo.setRoutineDayAmount(item.id, _date.value, amount, kind)
            notify(if (amount > 0) "今日金额已更新" else "已恢复默认金额")
        }
    }

    fun addRoutine(title: String, note: String, weekdays: Int, amount: Long, kind: AmountKind) {
        viewModelScope.launch {
            repo.addRoutine(title, note, weekdays, amount, kind)
            notify("公共事务已添加，今天就能看到")
        }
    }

    fun updateRoutine(
        routine: Routine,
        title: String,
        note: String,
        weekdays: Int,
        amount: Long,
        kind: AmountKind,
    ) {
        viewModelScope.launch { repo.updateRoutine(routine, title, note, weekdays, amount, kind) }
    }

    fun setArchived(routine: Routine, archived: Boolean) {
        viewModelScope.launch {
            repo.setArchived(routine, archived, LocalDate.now())
            notify(if (archived) "已归档，今天起不再出现" else "已恢复")
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            val kept = repo.deleteRoutine(routine, LocalDate.now())
            notify(if (kept) "已删除，今天之前的记录保留" else "已删除")
        }
    }

    // ---------- 出游 ----------

    fun addTrip(title: String, destination: String, start: LocalDate, days: Int, note: String) {
        viewModelScope.launch {
            val end = start.plusDays((days - 1).toLong().coerceAtLeast(0))
            val id = tripRepo.addTrip(title, destination, start, end, note)
            if (id > 0) {
                notify("出游已创建，可以按天记录了")
                _openTripId.value = id
            }
        }
    }

    fun updateTrip(
        trip: Trip,
        title: String,
        destination: String,
        start: LocalDate,
        days: Int,
        note: String,
    ) {
        viewModelScope.launch {
            val end = start.plusDays((days - 1).toLong().coerceAtLeast(0))
            tripRepo.updateTrip(trip, title, destination, start, end, note)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            tripRepo.deleteTrip(trip)
            if (_openTripId.value == trip.id) _openTripId.value = null
            notify("出游已删除")
        }
    }

    fun addTripItem(
        tripId: Long,
        date: LocalDate,
        time: String,
        kind: TripKind,
        title: String,
        detail: String,
        amount: Long,
    ) {
        viewModelScope.launch {
            tripRepo.addItem(tripId, date, time, kind, title, detail, amount)
        }
    }

    fun updateTripItem(
        item: TripItem,
        date: LocalDate,
        time: String,
        kind: TripKind,
        title: String,
        detail: String,
        amount: Long,
    ) {
        viewModelScope.launch {
            tripRepo.updateItem(item, date, time, kind, title, detail, amount)
        }
    }

    fun deleteTripItem(item: TripItem) {
        viewModelScope.launch {
            tripRepo.deleteItem(item)
            notify("已删除这条记录")
        }
    }

    // ---------- 备份 ----------

    fun export(uri: Uri, password: String) {
        viewModelScope.launch {
            val result = runCatching {
                val (r, t, c) = repo.exportSnapshot()
                val (trips, tripItems) = tripRepo.snapshot()
                withContext(Dispatchers.IO) {
                    Backup.write(
                        getApplication(), uri, password,
                        Snapshot(r, t, c, trips, tripItems),
                    )
                }
                if (password.isEmpty()) "已导出（未加密）" else "已导出加密备份"
            }
            notify(result.getOrElse { "导出失败：${it.message}" })
        }
    }

    fun import(uri: Uri, password: String, replace: Boolean) {
        viewModelScope.launch {
            val result = runCatching {
                val snapshot = withContext(Dispatchers.IO) {
                    Backup.read(getApplication(), uri, password)
                }
                if (snapshot.isEmpty) error("备份里没有数据")
                repo.restore(snapshot.routines, snapshot.dayTasks, snapshot.checks, replace)
                tripRepo.restore(snapshot.trips, snapshot.tripItems, replace)
                buildString {
                    append("已恢复 ${snapshot.routines.size} 项公共事务、${snapshot.dayTasks.size} 项当日事务")
                    if (snapshot.trips.isNotEmpty()) append("、${snapshot.trips.size} 次出游")
                }
            }
            notify(result.getOrElse { "导入失败：${it.message}" })
        }
    }
}
