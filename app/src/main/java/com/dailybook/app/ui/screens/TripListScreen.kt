package com.dailybook.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailybook.app.data.Money
import com.dailybook.app.data.Trip
import com.dailybook.app.data.TripSummary
import com.dailybook.app.data.monthDayCn
import com.dailybook.app.ui.components.AddChip
import com.dailybook.app.ui.components.ConfirmDialog
import com.dailybook.app.ui.components.EmptyState
import com.dailybook.app.ui.components.PaperCard
import com.dailybook.app.ui.components.SectionHeader
import com.dailybook.app.ui.components.TripEditorDialog
import java.time.LocalDate

/**
 * 出游列表。独立模块：不生成待办、不进看板、不计入收支报表。
 */
@Composable
fun TripListScreen(
    trips: List<TripSummary>,
    onAdd: (title: String, destination: String, start: LocalDate, days: Int, note: String) -> Unit,
    onOpen: (Trip) -> Unit,
    onDelete: (Trip) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Trip?>(null) }
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Column {
                Text(
                    "出游记录",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "独立的旅行账本 · 不计入每日事务和报表",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "add") {
            PaperCard {
                SectionHeader(
                    title = "行程",
                    subtitle = if (trips.isEmpty()) "还没有记录" else "${trips.size} 次出游",
                ) { AddChip("新增") { showAdd = true } }
                Spacer(Modifier.height(12.dp))
                if (trips.isEmpty()) {
                    EmptyState("还没有出游记录", "记一次旅行：去哪儿、花了多少，按天列清楚")
                } else {
                    trips.forEachIndexed { index, summary ->
                        TripCard(
                            summary = summary,
                            accent = accent,
                            onOpen = { onOpen(summary.trip) },
                            onDelete = { deleting = summary.trip },
                        )
                        if (index != trips.lastIndex) {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        item(key = "hint") {
            Text(
                "出游数据单独存放，删掉它不会影响每日事务和看板统计",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }

    if (showAdd) {
        TripEditorDialog(
            heading = "新增出游",
            onDismiss = { showAdd = false },
            onConfirm = { title, destination, start, days, note ->
                showAdd = false
                onAdd(title, destination, start, days, note)
            },
        )
    }

    deleting?.let { trip ->
        ConfirmDialog(
            heading = "删除这次出游？",
            message = "「${trip.title}」里的所有行程记录会一起删掉，无法恢复。",
            confirmLabel = "删除",
            destructive = true,
            onDismiss = { deleting = null },
            onConfirm = {
                deleting = null
                onDelete(trip)
            },
        )
    }
}

@Composable
private fun TripCard(
    summary: TripSummary,
    accent: androidx.compose.ui.graphics.Color,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val trip = summary.trip
    var menu by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧色条：有记录才有颜色
        Box(
            Modifier
                .width(3.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (summary.empty) MaterialTheme.colorScheme.outlineVariant
                    else accent
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                trip.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                buildString {
                    if (trip.destination.isNotBlank()) append(trip.destination).append(" · ")
                    append("${trip.start.monthDayCn()} – ${trip.end.monthDayCn()}")
                    append(" · ${trip.dayCount} 天")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "¥${Money.format(summary.total)}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = if (summary.empty) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.secondary,
            )
            Text(
                "${summary.itemCount} 笔",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
        Box {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { menu = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("删除出游") },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                )
            }
        }
    }
}
