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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailybook.app.data.AmountKind
import com.dailybook.app.data.DashboardData
import com.dailybook.app.data.Money
import com.dailybook.app.data.RoutineStat
import com.dailybook.app.data.monthDayCn
import com.dailybook.app.data.weekdaysText
import com.dailybook.app.ui.components.CashFlowBars
import com.dailybook.app.ui.components.CompletionBars
import com.dailybook.app.ui.components.EmptyState
import com.dailybook.app.ui.components.Heatmap
import com.dailybook.app.ui.components.PaperCard
import com.dailybook.app.ui.components.ProgressRing
import com.dailybook.app.ui.components.SectionHeader
import com.dailybook.app.ui.theme.CardRadius
import java.time.LocalDate

@Composable
fun DashboardScreen(data: DashboardData, onOpenCashMonth: () -> Unit = {}) {
    val today = LocalDate.now()
    val routineAccent = MaterialTheme.colorScheme.primary
    val dailyAccent = MaterialTheme.colorScheme.secondary
    val doneAccent = MaterialTheme.colorScheme.tertiary

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "title") {
            Column {
                Text(
                    "数据看板",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${today.monthDayCn()} · 今天完成 ${data.today.doneCount}/${data.today.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------- 今日 + 三个指标 ----------
        item(key = "metrics") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "连续达标",
                    value = "${data.streak}",
                    unit = "天",
                    accent = dailyAccent,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "本周完成",
                    value = "${(data.weekRatio * 100).toInt()}",
                    unit = "%",
                    accent = routineAccent,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "累计完成",
                    value = "${data.totalDone}",
                    unit = "项",
                    accent = doneAccent,
                )
            }
        }

        // ---------- 今日完成环 ----------
        item(key = "ring") {
            PaperCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(
                        progress = data.today.progress,
                        ringSize = 76.dp,
                        strokeWidth = 7.dp,
                        color = doneAccent,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(data.today.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = doneAccent,
                            )
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "今日完成度",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "已完成 ${data.today.doneCount} 项 · 未完成 ${data.today.total - data.today.doneCount} 项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniTag("公共 ${data.today.routines.size}", routineAccent)
                            MiniTag("当日 ${data.today.dailies.size}", dailyAccent)
                        }
                    }
                }
            }
        }

        // ---------- 本月收支 ----------
        item(key = "cash") {
            PaperCard(modifier = Modifier.clickable(onClick = onOpenCashMonth)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("本月收支", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "明细 ›",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "${today.monthValue}月1日 至今",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "净结余",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            Money.formatSigned(
                                data.monthNet,
                                if (data.monthNet >= 0) AmountKind.INCOME else AmountKind.EXPENSE,
                            ),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = if (data.monthNet >= 0) doneAccent else dailyAccent,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                val monthTotal = data.monthIncome + data.monthExpense
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    if (monthTotal > 0L) {
                        if (data.monthIncome > 0L) {
                            Box(
                                Modifier
                                    .weight(data.monthIncome.toFloat())
                                    .fillMaxHeight()
                                    .background(doneAccent)
                            )
                        }
                        if (data.monthExpense > 0L) {
                            Box(
                                Modifier
                                    .weight(data.monthExpense.toFloat())
                                    .fillMaxHeight()
                                    .background(dailyAccent)
                            )
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    CashLegend("收入", data.monthIncome, doneAccent, Modifier.weight(1f))
                    CashLegend("支出", data.monthExpense, dailyAccent, Modifier.weight(1f))
                }
            }
        }

        // ---------- 14 天趋势 ----------
        item(key = "trend") {
            PaperCard {
                SectionHeader("近 14 天", "每天的完成率")
                Spacer(Modifier.height(16.dp))
                CompletionBars(data.last14, barMaxHeight = 88.dp)
            }
        }

        // ---------- 14 天收支 ----------
        item(key = "cashflow") {
            PaperCard {
                SectionHeader("近 14 天收支", "上为收入，下为支出")
                Spacer(Modifier.height(16.dp))
                if (data.cashFlow14.any { !it.empty }) {
                    CashFlowBars(data.cashFlow14)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CashLegend("收入", data.cashFlow14.sumOf { it.income }, doneAccent)
                        Spacer(Modifier.width(16.dp))
                        CashLegend("支出", data.cashFlow14.sumOf { it.expense }, dailyAccent)
                    }
                } else {
                    EmptyState("这 14 天还没有收支记录", "给事务填上金额，这里就会有走势")
                }
            }
        }

        // ---------- 热力图 ----------
        item(key = "heatmap") {
            PaperCard {
                SectionHeader("近 12 周", "颜色越深，当天完成得越多")
                Spacer(Modifier.height(14.dp))
                Heatmap(data.heatmap)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "少",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    listOf(0.18f, 0.4f, 0.65f, 1f).forEach { a ->
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(doneAccent.copy(alpha = a))
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "多",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "累计活跃 ${data.activeDays} 天",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ---------- 公共事务坚持榜 ----------
        item(key = "rank") {
            PaperCard {
                SectionHeader("公共事务坚持榜", "近 30 天完成率")
                Spacer(Modifier.height(12.dp))
                if (data.routineStats.isEmpty()) {
                    EmptyState("还没有公共事务", "添加之后这里会记录你的坚持")
                } else {
                    data.routineStats.forEachIndexed { index, stat ->
                        RoutineRankRow(
                            index = index + 1,
                            stat = stat,
                            accent = routineAccent,
                        )
                        if (index != data.routineStats.lastIndex) {
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }
            }
        }

        item(key = "footer") {
            Column(Modifier.padding(top = 2.dp)) {
                Text(
                    "最长连续 ${data.bestStreak} 天 · 累计应做 ${data.totalTasks} 项",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "数据只保存在这台设备上，不上传任何服务器",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    accent: Color,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(CardRadius))
            .background(accent.copy(alpha = 0.09f))
            .padding(vertical = 14.dp, horizontal = 12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 27.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp),
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MiniTag(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** 圆点 + 名称 + 金额 */
@Composable
private fun CashLegend(
    label: String,
    cents: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            Money.format(cents),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun RoutineRankRow(index: Int, stat: RoutineStat, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$index",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.width(16.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stat.routine.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    "${(stat.ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (stat.ratio >= 0.8f) MaterialTheme.colorScheme.tertiary else accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(stat.ratio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (stat.ratio >= 0.8f) MaterialTheme.colorScheme.tertiary else accent
                        )
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                "${weekdaysText(stat.routine.weekdays)} · 完成 ${stat.completed}/${stat.expected} 次",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}
