package com.dailybook.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.dailybook.app.data.DayCash
import com.dailybook.app.data.DayStat
import com.dailybook.app.data.Stats
import com.dailybook.app.data.WEEK_LABELS
import com.dailybook.app.ui.theme.LocalChartColors

/** 环形进度，中心可放任意内容 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 54.dp,
    strokeWidth: Dp = 5.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.16f),
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "ring",
    )
    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.001f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** 近 N 天完成率柱状图（布局实现，无需第三方图表库） */
@Composable
fun CompletionBars(
    stats: List<DayStat>,
    modifier: Modifier = Modifier,
    barMaxHeight: Dp = 92.dp,
    highlightToday: Boolean = true,
) {
    val chart = LocalChartColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        stats.forEachIndexed { index, stat ->
            val isToday = highlightToday && index == stats.lastIndex
            val ratio = if (stat.total == 0) 0.06f else stat.ratio.coerceAtLeast(0.06f)
            val barHeight by animateFloatAsState(
                targetValue = ratio,
                animationSpec = tween(500, delayMillis = index * 18),
                label = "bar",
            )
            val barColor = when {
                stat.total == 0 -> chart.missed.copy(alpha = 0.5f)
                stat.allDone -> chart.done
                stat.ratio >= 0.5f -> chart.routine.copy(alpha = 0.75f)
                else -> chart.routine.copy(alpha = 0.35f)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barMaxHeight * barHeight)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(if (isToday) MaterialTheme.colorScheme.secondary else barColor)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isToday) "今" else "${stat.date.dayOfMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** 近 N 天收支双向柱：收入向上、支出向下，共用一条中轴 */
@Composable
fun CashFlowBars(
    data: List<DayCash>,
    modifier: Modifier = Modifier,
    halfHeight: Dp = 58.dp,
) {
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val peak = data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        data.forEachIndexed { index, day ->
            val isToday = index == data.lastIndex
            // 非零但比例很小的柱子也给一点高度，否则看不见
            val incomeRatio = when {
                peak == 0L || day.income == 0L -> 0f
                else -> maxOf(day.income.toFloat() / peak, 0.03f)
            }
            val expenseRatio = when {
                peak == 0L || day.expense == 0L -> 0f
                else -> maxOf(day.expense.toFloat() / peak, 0.03f)
            }
            val inH by animateFloatAsState(
                targetValue = incomeRatio,
                animationSpec = tween(500, delayMillis = index * 18),
                label = "in",
            )
            val outH by animateFloatAsState(
                targetValue = expenseRatio,
                animationSpec = tween(500, delayMillis = index * 18),
                label = "out",
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(halfHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (inH > 0.001f) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(halfHeight * inH)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(incomeColor.copy(alpha = 0.85f))
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(gridColor)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(halfHeight),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (outH > 0.001f) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(halfHeight * outH)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(expenseColor.copy(alpha = 0.85f))
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isToday) "今" else "${day.date.dayOfMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** 12 周日历热力图 */
@Composable
fun Heatmap(
    stats: List<DayStat>,
    modifier: Modifier = Modifier,
    cellSize: Dp = 13.dp,
) {
    val chart = LocalChartColors.current
    val weeks = Stats.toWeeks(stats)
    Row(modifier = modifier) {
        // 星期轴
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(end = 6.dp),
        ) {
            WEEK_LABELS.forEachIndexed { i, label ->
                Box(Modifier.height(cellSize), contentAlignment = Alignment.Center) {
                    Text(
                        if (i % 2 == 0) label else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { stat ->
                        val color = when {
                            stat == null -> Color.Transparent
                            stat.total == 0 -> chart.grid
                            stat.allDone -> chart.done
                            stat.ratio >= 0.6f -> chart.done.copy(alpha = 0.78f)
                            stat.ratio >= 0.25f -> chart.done.copy(alpha = 0.52f)
                            else -> chart.done.copy(alpha = 0.3f)
                        }
                        Box(
                            Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}
