package com.dailybook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailybook.app.data.AmountKind
import com.dailybook.app.data.DayState
import com.dailybook.app.data.Money
import com.dailybook.app.data.TaskItem
import com.dailybook.app.data.weekdayCn
import com.dailybook.app.ui.components.AddChip
import com.dailybook.app.ui.components.AmountDialog
import com.dailybook.app.ui.components.ConfirmDialog
import com.dailybook.app.ui.components.EmptyState
import com.dailybook.app.ui.components.PaperCard
import com.dailybook.app.ui.components.ProgressRing
import com.dailybook.app.ui.components.SectionHeader
import com.dailybook.app.ui.components.TaskEditorDialog
import com.dailybook.app.ui.components.TaskRow
import com.dailybook.app.ui.theme.CardRadius
import java.time.LocalDate

@Composable
fun TodayScreen(
    date: LocalDate,
    day: DayState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onToggle: (TaskItem) -> Unit,
    onSetDayAmount: (TaskItem, Long, AmountKind) -> Unit,
    onAddDaily: (String, String, Long, AmountKind) -> Unit,
    onEditDaily: (Long, String, String, Long, AmountKind) -> Unit,
    onDeleteDaily: (Long) -> Unit,
    onMoveNext: (Long) -> Unit,
    onAddRoutine: (String, String, Int, Long, AmountKind) -> Unit,
) {
    val today = LocalDate.now()
    val isToday = date == today

    var showAddDaily by remember { mutableStateOf(false) }
    var showAddRoutine by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskItem?>(null) }
    var deleting by remember { mutableStateOf<TaskItem?>(null) }
    var amountEditing by remember { mutableStateOf<TaskItem?>(null) }

    val routineAccent = MaterialTheme.colorScheme.primary
    val dailyAccent = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------- 日期导航 ----------
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NavArrow(Icons.Filled.KeyboardArrowLeft, onPrev)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${date.monthValue}月${date.dayOfMonth}日",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            date.weekdayCn(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!isToday) {
                        Text(
                            "回到今天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onToday)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    } else {
                        Text(
                            "今天",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                NavArrow(Icons.Filled.KeyboardArrowRight, onNext)
            }
        }

        // ---------- 进度总览 ----------
        item(key = "progress") {
            ProgressSummary(day = day, accent = routineAccent, accent2 = dailyAccent)
        }

        // ---------- 公共事务 ----------
        item(key = "routine") {
            PaperCard {
                SectionHeader(
                    title = "公共事务",
                    subtitle = if (day.routines.isEmpty()) "每天自动出现在清单里"
                    else "${day.routines.size} 项 · 自动加入每一天",
                ) { AddChip("新增") { showAddRoutine = true } }
                Spacer(Modifier.height(4.dp))
                if (day.routines.isEmpty()) {
                    EmptyState("还没有公共事务", "比如「喝水 8 杯」「拉伸 10 分钟」")
                } else {
                    day.routines.forEach { item ->
                        TaskRow(
                            item = item,
                            accent = routineAccent,
                            onToggle = { onToggle(item) },
                            actions = listOf("修改今日金额" to { amountEditing = item }),
                        )
                    }
                }
            }
        }

        // ---------- 当日事务 ----------
        item(key = "daily") {
            PaperCard {
                SectionHeader(
                    title = "当日事务",
                    subtitle = if (isToday) "只属于今天，需要手动添加"
                    else "补记 ${date.monthValue}月${date.dayOfMonth}日",
                ) { AddChip("新增") { showAddDaily = true } }
                Spacer(Modifier.height(4.dp))
                if (day.dailies.isEmpty()) {
                    EmptyState(
                        "今天还没有额外安排",
                        "临时的事情就记在这里",
                    )
                } else {
                    day.dailies.forEach { item ->
                        TaskRow(
                            item = item,
                            accent = dailyAccent,
                            onToggle = { onToggle(item) },
                            actions = listOf(
                                "编辑" to { editing = item },
                                "顺延到明天" to { onMoveNext(item.id) },
                                "删除" to { deleting = item },
                            ),
                        )
                    }
                }
            }
        }

        item(key = "footer") {
            Text(
                if (day.isEmpty) "今天还是空白的一天"
                else "已完成 ${day.doneCount} 项，还剩 ${day.total - day.doneCount} 项",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }

    // ---------- 对话框 ----------
    if (showAddDaily) {
        TaskEditorDialog(
            heading = if (isToday) "新增当日事务" else "补记 ${date.monthValue}月${date.dayOfMonth}日",
            onDismiss = { showAddDaily = false },
            onConfirm = { title, note, _, amount, kind ->
                showAddDaily = false
                onAddDaily(title, note, amount, kind)
            },
        )
    }

    if (showAddRoutine) {
        TaskEditorDialog(
            heading = "新增公共事务",
            showWeekdays = true,
            onDismiss = { showAddRoutine = false },
            onConfirm = { title, note, weekdays, amount, kind ->
                showAddRoutine = false
                onAddRoutine(title, note, weekdays, amount, kind)
            },
        )
    }

    editing?.let { item ->
        TaskEditorDialog(
            heading = "编辑事务",
            initialTitle = item.title,
            initialNote = item.note,
            initialAmount = item.amount,
            initialKind = item.amountKind,
            onDismiss = { editing = null },
            onConfirm = { title, note, _, amount, kind ->
                editing = null
                onEditDaily(item.id, title, note, amount, kind)
            },
        )
    }

    deleting?.let { item ->
        ConfirmDialog(
            heading = "删除这条事务？",
            message = "「${item.title}」将被移除，无法恢复。",
            confirmLabel = "删除",
            destructive = true,
            onDismiss = { deleting = null },
            onConfirm = { onDeleteDaily(item.id) },
        )
    }

    amountEditing?.let { item ->
        AmountDialog(
            subtitle = "${item.title} · ${date.monthValue}月${date.dayOfMonth}日",
            initialAmount = item.amount,
            initialKind = item.amountKind,
            defaultAmount = item.defaultAmount,
            defaultKind = item.defaultKind,
            onDismiss = { amountEditing = null },
            onConfirm = { amount, kind ->
                amountEditing = null
                onSetDayAmount(item, amount, kind)
            },
        )
    }
}

@Composable
private fun ProgressSummary(
    day: DayState,
    accent: androidx.compose.ui.graphics.Color,
    accent2: androidx.compose.ui.graphics.Color,
) {
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.secondary

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.10f),
                        accent2.copy(alpha = 0.07f),
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    progress = day.progress,
                    ringSize = 58.dp,
                    strokeWidth = 5.5.dp,
                    color = accent,
                ) {
                    Text(
                        "${(day.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (day.isEmpty) "今天还没有任务"
                        else if (day.doneCount == day.total) "全部完成，今天很棒"
                        else "今日进度",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(day.progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(accent)
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "${day.doneCount} / ${day.total} 已完成",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 只有存在金额时才显示收支，避免空占一行
            if (day.hasCash) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    CashCell(
                        label = "今日收入",
                        cents = day.income,
                        kind = AmountKind.INCOME,
                        color = incomeColor,
                        modifier = Modifier.weight(1f),
                    )
                    CashCell(
                        label = "今日支出",
                        cents = day.expense,
                        kind = AmountKind.EXPENSE,
                        color = expenseColor,
                        modifier = Modifier.weight(1f),
                    )
                    CashCell(
                        label = "结余",
                        cents = day.net,
                        kind = if (day.net >= 0) AmountKind.INCOME else AmountKind.EXPENSE,
                        color = if (day.net >= 0) incomeColor else expenseColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CashCell(
    label: String,
    cents: Long,
    kind: AmountKind,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            if (cents == 0L) "—" else Money.formatSigned(cents, kind),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (cents == 0L) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            else color,
            maxLines = 1,
        )
    }
}

@Composable
private fun NavArrow(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(24.dp),
        )
    }
}
