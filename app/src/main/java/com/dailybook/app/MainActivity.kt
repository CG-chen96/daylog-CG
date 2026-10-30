package com.dailybook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailybook.app.ui.DiaryViewModel
import com.dailybook.app.ui.screens.CashMonthScreen
import com.dailybook.app.ui.screens.DashboardScreen
import com.dailybook.app.ui.screens.ManageScreen
import com.dailybook.app.ui.screens.TodayScreen
import com.dailybook.app.ui.screens.TripDetailScreen
import com.dailybook.app.ui.screens.TripListScreen
import com.dailybook.app.ui.theme.DaylogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaylogTheme {
                DaylogRoot()
            }
        }
    }
}

private val TAB_LABELS = listOf("今日", "看板", "出游", "事务")

@Composable
private fun DaylogRoot(vm: DiaryViewModel = viewModel()) {
    val day by vm.day.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    val routines by vm.routines.collectAsStateWithLifecycle()
    val cashMonth by vm.cashMonth.collectAsStateWithLifecycle()
    val monthDetail by vm.monthDetail.collectAsStateWithLifecycle()
    val trips by vm.trips.collectAsStateWithLifecycle()
    val tripDetail by vm.tripDetail.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbarHost.showSnackbar(it) }
    }

    // 回到前台时若已跨天，自动切到新的今天
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshToday()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 月度明细是全屏页，打开时把底部导航收起来
    val showCashMonth = cashMonth != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = { if (!showCashMonth) FloatingBar(tab) { tab = it } },
    ) { inner ->
        val content = Modifier
            .padding(inner)
            .fillMaxSize()

        val month = cashMonth
        if (month != null) {
            CashMonthScreen(
                month = month,
                detail = monthDetail,
                onPrevMonth = { vm.shiftCashMonth(-1) },
                onNextMonth = { vm.shiftCashMonth(1) },
                onBack = { vm.closeCashMonth() },
                modifier = content,
            )
        } else {
            Crossfade(
                targetState = tab,
                animationSpec = tween(220),
                modifier = content,
                label = "tab",
            ) { index ->
                when (index) {
                    0 -> TodayScreen(
                        date = date,
                        day = day,
                        onPrev = { vm.shiftDate(-1) },
                        onNext = { vm.shiftDate(1) },
                        onToday = { vm.goToday() },
                        onToggle = { vm.toggle(it) },
                        onSetDayAmount = { item, a, k -> vm.setDayAmount(item, a, k) },
                        onAddDaily = { t, n, a, k -> vm.addDaily(t, n, a, k) },
                        onEditDaily = { id, t, n, a, k -> vm.updateDaily(id, t, n, a, k) },
                        onDeleteDaily = { vm.deleteDaily(it) },
                        onMoveNext = { vm.moveToNextDay(it) },
                        onAddRoutine = { t, n, w, a, k -> vm.addRoutine(t, n, w, a, k) },
                    )
                    1 -> DashboardScreen(
                        data = dashboard,
                        onOpenCashMonth = { vm.openCashMonth() },
                    )
                    2 -> {
                        val detail = tripDetail
                        if (detail == null) {
                            TripListScreen(
                                trips = trips,
                                onAdd = { t, dest, start, days, note ->
                                    vm.addTrip(t, dest, start, days, note)
                                },
                                onOpen = { vm.openTrip(it.id) },
                                onDelete = { vm.deleteTrip(it) },
                            )
                        } else {
                            TripDetailScreen(
                                detail = detail,
                                onBack = { vm.closeTrip() },
                                onUpdateTrip = { title, dest, start, days, note ->
                                    vm.updateTrip(detail.trip, title, dest, start, days, note)
                                },
                                onAddItem = { d, time, kind, title, extra, amount ->
                                    vm.addTripItem(detail.trip.id, d, time, kind, title, extra, amount)
                                },
                                onUpdateItem = { item, d, time, kind, title, extra, amount ->
                                    vm.updateTripItem(item, d, time, kind, title, extra, amount)
                                },
                                onDeleteItem = { vm.deleteTripItem(it) },
                            )
                        }
                    }
                    else -> ManageScreen(
                        routines = routines,
                        onAdd = { t, n, w, a, k -> vm.addRoutine(t, n, w, a, k) },
                        onEdit = { r, t, n, w, a, k -> vm.updateRoutine(r, t, n, w, a, k) },
                        onArchive = { r, archived -> vm.setArchived(r, archived) },
                        onDelete = { vm.deleteRoutine(it) },
                        onExport = { uri, pwd -> vm.export(uri, pwd) },
                        onImport = { uri, pwd, replace -> vm.import(uri, pwd, replace) },
                    )
                }
            }
        }
    }
}

/** 浮起的胶囊导航栏：选中项展开文字，其余只留图标 */
@Composable
private fun FloatingBar(current: Int, onSelect: (Int) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant
            ),
        ) {
            Row(
                Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TAB_LABELS.forEachIndexed { index, label ->
                    val selected = current == index
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { onSelect(index) }
                            .animateContentSize()
                            .padding(
                                horizontal = if (selected) 16.dp else 14.dp,
                                vertical = 11.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TabIcon(
                            index = index,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (selected) {
                            Spacer(Modifier.width(7.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIcon(index: Int, tint: Color) {
    when (index) {
        0 -> Icon(
            Icons.Filled.CheckCircle, contentDescription = TAB_LABELS[0],
            tint = tint, modifier = Modifier.size(20.dp),
        )
        1 -> Icon(
            painterResource(R.drawable.ic_stats), contentDescription = TAB_LABELS[1],
            tint = tint, modifier = Modifier.size(20.dp),
        )
        2 -> Icon(
            Icons.Filled.Place, contentDescription = TAB_LABELS[2],
            tint = tint, modifier = Modifier.size(20.dp),
        )
        else -> Icon(
            Icons.Filled.List, contentDescription = TAB_LABELS[3],
            tint = tint, modifier = Modifier.size(20.dp),
        )
    }
}
