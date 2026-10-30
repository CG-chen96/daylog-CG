package com.dailybook.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailybook.app.data.AmountKind
import com.dailybook.app.data.CashItem
import com.dailybook.app.data.DayCashDetail
import com.dailybook.app.data.Money
import com.dailybook.app.data.MonthCashDetail
import com.dailybook.app.data.TaskKind
import com.dailybook.app.data.monthDayCn
import com.dailybook.app.data.weekdayCn
import com.dailybook.app.ui.components.EmptyState
import com.dailybook.app.ui.components.PaperCard
import java.time.LocalDate
import java.time.YearMonth

/**
 * 月度收支明细：按月列出每天的收入 / 支出，点开某天能看到每一笔来自哪条事务。
 * 数据全部来自 [MonthCashDetail]，页面本身不计算任何口径。
 */
@Composable
fun CashMonthScreen(
    month: YearMonth,
    detail: MonthCashDetail,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.secondary
    var expanded by remember(month) { mutableStateOf<LocalDate?>(null) }
    var seeded by remember(month) { mutableStateOf(false) }

    // 数据是异步到的，等第一份明细到了再把最近一天展开
    LaunchedEffect(detail) {
        if (!seeded && detail.days.isNotEmpty()) {
            expanded = detail.days.first().date
            seeded = true
        }
    }

    LazyColumn(
        modifier = modifier,
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
                Column {
                    Text(
                        "月度收支",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "${detail.count} 笔 · 只有已完成的事务才入账",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item(key = "switcher") {
            MonthSwitcher(month, onPrevMonth, onNextMonth)
        }

        item(key = "summary") {
            MonthSummaryCard(detail, incomeColor, expenseColor)
        }

        if (detail.empty) {
            item(key = "empty") {
                PaperCard {
                    EmptyState(
                        "${month.monthValue} 月还没有收支",
                        "给事务填上金额并完成，这里就会按天列出明细",
                    )
                }
            }
        } else {
            items(detail.days, key = { it.date.toString() }) { day ->
                DayCashCard(
                    day = day,
                    expanded = expanded == day.date,
                    onToggle = { expanded = if (expanded == day.date) null else day.date },
                    incomeColor = incomeColor,
                    expenseColor = expenseColor,
                )
            }
        }
    }
}

@Composable
private fun MonthSwitcher(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    val isThisMonth = month == YearMonth.now()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArrowButton(Icons.Filled.KeyboardArrowLeft, "上个月", onPrev)
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${month.year}年${month.monthValue}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (isThisMonth) {
                Text(
                    "本月",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ArrowButton(Icons.Filled.KeyboardArrowRight, "下个月", onNext)
    }
}

@Composable
private fun ArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MonthSummaryCard(
    detail: MonthCashDetail,
    incomeColor: Color,
    expenseColor: Color,
) {
    PaperCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("净结余", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(
                    Money.formatSigned(
                        detail.net,
                        if (detail.net >= 0) AmountKind.INCOME else AmountKind.EXPENSE,
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = if (detail.net >= 0) incomeColor else expenseColor,
                )
            }
            SummaryCell("收入", detail.income, incomeColor, Alignment.End)
            Spacer(Modifier.width(18.dp))
            SummaryCell("支出", detail.expense, expenseColor, Alignment.End)
        }

        Spacer(Modifier.height(14.dp))
        val total = detail.income + detail.expense
        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        ) {
            if (total > 0L) {
                if (detail.income > 0L) {
                    Box(
                        Modifier
                            .weight(detail.income.toFloat())
                            .fillMaxHeight()
                            .background(incomeColor)
                    )
                }
                if (detail.expense > 0L) {
                    Box(
                        Modifier
                            .weight(detail.expense.toFloat())
                            .fillMaxHeight()
                            .background(expenseColor)
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
    }
}

@Composable
private fun SummaryCell(
    label: String,
    cents: Long,
    color: Color,
    align: Alignment.Horizontal,
) {
    Column(horizontalAlignment = align) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            Money.format(cents),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun DayCashCard(
    day: DayCashDetail,
    expanded: Boolean,
    onToggle: () -> Unit,
    incomeColor: Color,
    expenseColor: Color,
) {
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    val netColor = if (day.net >= 0) incomeColor else expenseColor

    PaperCard(modifier = Modifier.clickable(onClick = onToggle)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        day.date.monthDayCn(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        day.date.weekdayCn(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    buildString {
                        append("${day.items.size} 笔")
                        if (day.income > 0L) append(" · 收 ${Money.format(day.income)}")
                        if (day.expense > 0L) append(" · 支 ${Money.format(day.expense)}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                Money.formatSigned(day.net, if (day.net >= 0) AmountKind.INCOME else AmountKind.EXPENSE),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = netColor,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(18.dp)
                    .rotate(arrow),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                )
                Spacer(Modifier.height(4.dp))
                day.items.forEach { item ->
                    CashItemRow(item, incomeColor, expenseColor)
                }
            }
        }
    }
}

@Composable
private fun CashItemRow(item: CashItem, incomeColor: Color, expenseColor: Color) {
    val color = if (item.amountKind == AmountKind.INCOME) incomeColor else expenseColor
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        if (item.kind == TaskKind.ROUTINE) {
            Text(
                "公共",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            Money.formatSigned(item.amount, item.amountKind),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
