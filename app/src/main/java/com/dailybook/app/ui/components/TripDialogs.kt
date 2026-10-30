package com.dailybook.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dailybook.app.data.Money
import com.dailybook.app.data.Trip
import com.dailybook.app.data.TripKind
import com.dailybook.app.data.monthDayCn
import com.dailybook.app.data.weekdayCn
import com.dailybook.app.ui.theme.LocalChartColors
import java.time.LocalDate

/** 新建 / 编辑一次出游：标题、目的地、出发日、天数 */
@Composable
fun TripEditorDialog(
    heading: String,
    initialTitle: String = "",
    initialDestination: String = "",
    initialStart: LocalDate = LocalDate.now(),
    initialDays: Int = 1,
    initialNote: String = "",
    confirmLabel: String = "保存",
    onDismiss: () -> Unit,
    onConfirm: (title: String, destination: String, start: LocalDate, days: Int, note: String) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var destination by remember { mutableStateOf(initialDestination) }
    var start by remember { mutableStateOf(initialStart) }
    var days by remember { mutableStateOf(initialDays.coerceAtLeast(1)) }
    var note by remember { mutableStateOf(initialNote) }
    val focus = LocalFocusManager.current
    val canSave = title.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(heading, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "例如：新疆七日",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    placeholder = {
                        Text(
                            "目的地（可选），例如：乌鲁木齐 · 吐鲁番",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    StepperRow(
                        label = "出发",
                        value = "${start.monthDayCn()} ${start.weekdayCn()}",
                        onMinus = { start = start.minusDays(1) },
                        onPlus = { start = start.plusDays(1) },
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    )
                    StepperRow(
                        label = "天数",
                        value = "$days 天",
                        minusEnabled = days > 1,
                        plusEnabled = days < 60,
                        onMinus = { if (days > 1) days-- },
                        onPlus = { if (days < 60) days++ },
                    )
                }
                Spacer(Modifier.height(6.dp))
                val end = start.plusDays((days - 1).toLong())
                Text(
                    "${start.monthDayCn()} – ${end.monthDayCn()} · 共 $days 天",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = {
                        Text(
                            "备注（可选）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))
                DialogActions(
                    confirmLabel = confirmLabel,
                    confirmEnabled = canSave,
                    onCancel = onDismiss,
                    onConfirm = {
                        focus.clearFocus()
                        onConfirm(title, destination, start, days, note)
                    },
                )
            }
        }
    }
}

/** 一条行程记录：类型、第几天、时间、标题、补充说明、花费 */
@Composable
fun TripItemEditorDialog(
    heading: String,
    trip: Trip,
    initialDate: LocalDate,
    initialTime: String = "",
    initialKind: TripKind = TripKind.OTHER,
    initialTitle: String = "",
    initialDetail: String = "",
    initialAmount: Long = 0,
    confirmLabel: String = "保存",
    onDismiss: () -> Unit,
    onConfirm: (
        date: LocalDate,
        time: String,
        kind: TripKind,
        title: String,
        detail: String,
        amount: Long,
    ) -> Unit,
) {
    val kindColors = LocalChartColors.current.tripKinds
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }
    var kind by remember { mutableStateOf(initialKind) }
    var title by remember { mutableStateOf(initialTitle) }
    var detail by remember { mutableStateOf(initialDetail) }
    var amountText by remember { mutableStateOf(Money.toEditText(initialAmount)) }
    val focus = LocalFocusManager.current
    val parsed = Money.parse(amountText)
    val canSave = title.isNotBlank() && parsed != null
    val tripDays = (0 until trip.dayCount).map { trip.start.plusDays(it.toLong()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(heading, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                // 类型
                TripKind.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { k ->
                            val on = kind == k
                            val color = kindColors[k.ordinal % kindColors.size]
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (on) color.copy(alpha = 0.16f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (on) color else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(50),
                                    )
                                    .clickable { kind = k },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    k.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (on) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // 第几天
                if (tripDays.size > 1) {
                    Spacer(Modifier.height(2.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tripDays) { d ->
                            val on = d == date
                            val index = trip.dayIndex(d)
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (on) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { date = d }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    "第 $index 天 · ${d.monthValue}/${d.dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (on) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "做了什么，例如：乌鲁木齐站 → 酒店",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it.filter { c -> c.isDigit() || c == ':' }.take(5) },
                        placeholder = {
                            Text(
                                "时间",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        colors = fieldColors(),
                        modifier = Modifier.width(104.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = {
                            Text(
                                "花费",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        },
                        prefix = {
                            Text(
                                "¥",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        singleLine = true,
                        isError = !(parsed != null),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (parsed == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "金额最多两位小数",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    placeholder = {
                        Text(
                            "补充说明（可选），例如：地铁 8 元，约 70 分钟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))
                DialogActions(
                    confirmLabel = confirmLabel,
                    confirmEnabled = canSave,
                    onCancel = onDismiss,
                    onConfirm = {
                        focus.clearFocus()
                        onConfirm(date, time, kind, title, detail, parsed ?: 0L)
                    },
                )
            }
        }
    }
}

/** 一行「− 值 +」的小步进器 */
@Composable
private fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
        )
        StepButton(plus = false, enabled = minusEnabled, onClick = onMinus)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        StepButton(plus = true, enabled = plusEnabled, onClick = onPlus)
    }
}

@Composable
private fun StepButton(plus: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (plus) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp),
            )
        } else {
            // icons-core 里没有减号，用一条短线代替
            Box(
                Modifier
                    .size(width = 11.dp, height = 1.5.dp)
                    .background(tint)
            )
        }
    }
}
