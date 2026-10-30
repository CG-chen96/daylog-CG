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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailybook.app.data.Money
import com.dailybook.app.data.TripDay
import com.dailybook.app.data.TripDetail
import com.dailybook.app.data.TripItem
import com.dailybook.app.data.TripKind
import com.dailybook.app.data.monthDayCn
import com.dailybook.app.data.weekdayCn
import com.dailybook.app.ui.components.ConfirmDialog
import com.dailybook.app.ui.components.PaperCard
import com.dailybook.app.ui.components.TripEditorDialog
import com.dailybook.app.ui.components.TripItemEditorDialog
import com.dailybook.app.ui.theme.LocalChartColors
import java.time.LocalDate

/**
 * 出游详情：按天铺开整段行程，每条记录标明类型和花费。
 * 每天都能单独补记，即使那天还没记录也会留一行。
 */
@Composable
fun TripDetailScreen(
    detail: TripDetail,
    onBack: () -> Unit,
    onUpdateTrip: (String, String, LocalDate, Int, String) -> Unit,
    onAddItem: (LocalDate, String, TripKind, String, String, Long) -> Unit,
    onUpdateItem: (TripItem, LocalDate, String, TripKind, String, String, Long) -> Unit,
    onDeleteItem: (TripItem) -> Unit,
) {
    val trip = detail.trip
    val kindColors = LocalChartColors.current.tripKinds
    var editingTrip by remember { mutableStateOf(false) }
    var addingOn by remember { mutableStateOf<LocalDate?>(null) }
    var editingItem by remember { mutableStateOf<TripItem?>(null) }
    var deletingItem by remember { mutableStateOf<TripItem?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        trip.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                    Text(
                        buildString {
                            if (trip.destination.isNotBlank()) {
                                append(trip.destination).append(" · ")
                            }
                            append("${trip.start.monthDayCn()} – ${trip.end.monthDayCn()}")
                            append(" · ${trip.dayCount} 天")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    "编辑",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { editingTrip = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        item(key = "summary") {
            PaperCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "总花费",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "¥${Money.format(detail.total)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    MiniStat("天数", "${detail.days.size}")
                    Spacer(Modifier.width(16.dp))
                    MiniStat("记录", "${detail.itemCount}")
                    Spacer(Modifier.width(16.dp))
                    MiniStat("日均", Money.format(detail.perDay))
                }
                if (trip.note.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        trip.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (detail.byKind.isNotEmpty()) {
            item(key = "kinds") {
                PaperCard {
                    Text(
                        "花费分布",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    val max = detail.byKind.maxOf { it.total }.coerceAtLeast(1L)
                    detail.byKind.forEachIndexed { index, k ->
                        if (index > 0) Spacer(Modifier.height(9.dp))
                        val color = kindColors[k.kind.ordinal % kindColors.size]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                k.kind.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((k.total.toFloat() / max).coerceIn(0.03f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(color)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                Money.format(k.total),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = color,
                            )
                        }
                    }
                }
            }
        }

        items(detail.days, key = { it.date.toString() }) { day ->
            TripDayCard(
                day = day,
                index = trip.dayIndex(day.date),
                kindColors = kindColors,
                onAdd = { addingOn = day.date },
                onEdit = { editingItem = it },
                onDelete = { deletingItem = it },
            )
        }

        item(key = "footer") {
            Text(
                "这次出游共 ${detail.itemCount} 条记录，合计 ¥${Money.format(detail.total)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
    }

    if (editingTrip) {
        TripEditorDialog(
            heading = "编辑出游",
            initialTitle = trip.title,
            initialDestination = trip.destination,
            initialStart = trip.start,
            initialDays = trip.dayCount,
            initialNote = trip.note,
            onDismiss = { editingTrip = false },
            onConfirm = { title, dest, start, days, note ->
                editingTrip = false
                onUpdateTrip(title, dest, start, days, note)
            },
        )
    }

    addingOn?.let { date ->
        TripItemEditorDialog(
            heading = "添加记录",
            trip = trip,
            initialDate = date,
            confirmLabel = "添加",
            onDismiss = { addingOn = null },
            onConfirm = { d, time, kind, title, detail, amount ->
                addingOn = null
                onAddItem(d, time, kind, title, detail, amount)
            },
        )
    }

    editingItem?.let { item ->
        TripItemEditorDialog(
            heading = "编辑记录",
            trip = trip,
            initialDate = runCatching { LocalDate.parse(item.date) }.getOrDefault(trip.start),
            initialTime = item.time,
            initialKind = item.kind,
            initialTitle = item.title,
            initialDetail = item.detail,
            initialAmount = item.amount,
            onDismiss = { editingItem = null },
            onConfirm = { d, time, kind, title, detail, amount ->
                editingItem = null
                onUpdateItem(item, d, time, kind, title, detail, amount)
            },
        )
    }

    deletingItem?.let { item ->
        ConfirmDialog(
            heading = "删除这条记录？",
            message = "「${item.title}」会被移除，无法恢复。",
            confirmLabel = "删除",
            destructive = true,
            onDismiss = { deletingItem = null },
            onConfirm = {
                deletingItem = null
                onDeleteItem(item)
            },
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TripDayCard(
    day: TripDay,
    index: Int,
    kindColors: List<Color>,
    onAdd: () -> Unit,
    onEdit: (TripItem) -> Unit,
    onDelete: (TripItem) -> Unit,
) {
    PaperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "第 $index 天",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${day.date.monthDayCn()} ${day.date.weekdayCn()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (day.items.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${day.items.size} 条 · ¥${Money.format(day.total)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "添加",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        if (day.items.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "这天还没有记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        } else {
            Spacer(Modifier.height(6.dp))
            day.items.forEach { item ->
                TripItemRow(
                    item = item,
                    color = kindColors[item.kind.ordinal % kindColors.size],
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) },
                )
            }
        }
    }
}

@Composable
private fun TripItemRow(
    item: TripItem,
    color: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.width(38.dp)) {
            if (item.time.isNotBlank()) {
                Text(
                    item.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.13f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        item.kind.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
            if (item.detail.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    item.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (item.amount > 0L) {
            Text(
                "¥${Money.format(item.amount)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { menu = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("编辑") },
                    onClick = {
                        menu = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                )
            }
        }
    }
}
