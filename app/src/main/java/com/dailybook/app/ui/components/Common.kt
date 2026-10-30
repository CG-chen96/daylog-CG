package com.dailybook.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dailybook.app.data.ALL_DAYS
import com.dailybook.app.data.AmountKind
import com.dailybook.app.data.Money
import com.dailybook.app.data.TaskItem
import com.dailybook.app.data.TaskKind
import com.dailybook.app.data.WEEK_LABELS
import com.dailybook.app.ui.theme.CardRadius
import com.dailybook.app.ui.theme.LocalChartColors

/** 统一的圆角卡片：细描边代替重阴影，安静一些 */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) { content() }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** 圆形勾选框，带一点回弹 */
@Composable
fun CheckCircle(
    done: Boolean,
    color: Color,
    onClick: () -> Unit,
    size: Int = 24,
) {
    val scale by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "checkScale",
    )
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .then(
                if (done) Modifier.background(color)
                else Modifier.border(1.6.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "已完成",
                tint = Color.White,
                modifier = Modifier
                    .size((size * 0.62).dp)
                    .scale(scale),
            )
        }
    }
}

/** 一条任务 */
@Composable
fun TaskRow(
    item: TaskItem,
    accent: Color,
    onToggle: () -> Unit,
    actions: List<Pair<String, () -> Unit>> = emptyList(),
) {
    var menuOpen by remember { mutableStateOf(false) }
    val textColor by animateFloatAsState(if (item.done) 0.45f else 1f, label = "alpha")
    val moneyColor = when (item.amountKind) {
        AmountKind.INCOME -> MaterialTheme.colorScheme.tertiary
        AmountKind.EXPENSE -> MaterialTheme.colorScheme.secondary
        AmountKind.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 9.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(done = item.done, color = accent, onClick = onToggle)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textColor),
                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.note.isNotBlank()) {
                Text(
                    item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.hasAmount) {
            Spacer(Modifier.width(8.dp))
            Text(
                Money.formatSigned(item.amount, item.amountKind),
                style = MaterialTheme.typography.labelLarge,
                color = moneyColor.copy(alpha = if (item.done) 0.55f else 0.9f),
                maxLines = 1,
            )
        }
        if (item.kind == TaskKind.ROUTINE) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (item.done) 0.25f else 0.55f))
            )
        }
        if (actions.isNotEmpty()) {
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(17.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    actions.forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = { menuOpen = false; action() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, hint: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            hint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

/** 小型标签 */
@Composable
fun Pill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * 新建 / 编辑任务对话框。
 * showWeekdays 为 true 时显示星期选择（公共事务专用）。
 */
@Composable
fun TaskEditorDialog(
    heading: String,
    initialTitle: String = "",
    initialNote: String = "",
    initialWeekdays: Int = ALL_DAYS,
    initialAmount: Long = 0,
    initialKind: AmountKind = AmountKind.EXPENSE,
    showWeekdays: Boolean = false,
    confirmLabel: String = "保存",
    onDismiss: () -> Unit,
    onConfirm: (title: String, note: String, weekdays: Int, amount: Long, kind: AmountKind) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var note by remember { mutableStateOf(initialNote) }
    var weekdays by remember { mutableStateOf(initialWeekdays) }
    var amountText by remember { mutableStateOf(Money.toEditText(initialAmount)) }
    var kind by remember {
        mutableStateOf(if (initialAmount == 0L) AmountKind.EXPENSE else initialKind)
    }
    val focus = LocalFocusManager.current
    val parsedAmount = Money.parse(amountText)
    val amountValid = parsedAmount != null
    val canSave = title.isNotBlank() && amountValid

    val expenseColor = MaterialTheme.colorScheme.secondary
    val incomeColor = MaterialTheme.colorScheme.tertiary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(heading, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            if (showWeekdays) "例如：早餐后称体重" else "例如：给客户回电话",
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

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KindChip(
                        label = "支出",
                        selected = kind == AmountKind.EXPENSE,
                        accent = expenseColor,
                        onClick = { kind = AmountKind.EXPENSE },
                    )
                    Spacer(Modifier.width(6.dp))
                    KindChip(
                        label = "收入",
                        selected = kind == AmountKind.INCOME,
                        accent = incomeColor,
                        onClick = { kind = AmountKind.INCOME },
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = {
                            Text(
                                "0.00",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        },
                        prefix = {
                            Text(
                                "¥",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (kind == AmountKind.INCOME) incomeColor else expenseColor,
                            )
                        },
                        singleLine = true,
                        isError = !amountValid,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!amountValid) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "金额最多两位小数",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

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
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (showWeekdays) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "重复",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WEEK_LABELS.forEachIndexed { index, label ->
                            val on = weekdays and (1 shl index) != 0
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (on) Modifier.background(MaterialTheme.colorScheme.primary)
                                        else Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape,
                                        )
                                    )
                                    .clickable { weekdays = weekdays xor (1 shl index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (on) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (weekdays == 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "未选任何星期，将不会出现在清单里",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                DialogActions(
                    confirmLabel = confirmLabel,
                    confirmEnabled = canSave,
                    onCancel = onDismiss,
                    onConfirm = {
                        focus.clearFocus()
                        onConfirm(title, note, weekdays, parsedAmount ?: 0L, kind)
                    },
                )
            }
        }
    }
}

/** 支出 / 收入切换 */
@Composable
fun KindChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) accent.copy(alpha = 0.14f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 只改「某一天的金额」，用于公共事务每天到账不一致的情况。
 * 保存 0 元表示恢复使用公共事务的默认金额。
 */
@Composable
fun AmountDialog(
    subtitle: String,
    initialAmount: Long,
    initialKind: AmountKind,
    defaultAmount: Long,
    defaultKind: AmountKind,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, kind: AmountKind) -> Unit,
) {
    var amountText by remember { mutableStateOf(Money.toEditText(initialAmount)) }
    var kind by remember {
        mutableStateOf(
            when {
                initialAmount != 0L && initialKind != AmountKind.NONE -> initialKind
                defaultKind != AmountKind.NONE -> defaultKind
                else -> AmountKind.EXPENSE
            }
        )
    }
    val parsed = Money.parse(amountText)
    val valid = parsed != null
    // 金额或方向任一偏离默认值，就提供「恢复默认」；只看金额会漏掉「同额改方向」的情况
    val overridden = (parsed ?: initialAmount) != defaultAmount || kind != defaultKind
    val expenseColor = MaterialTheme.colorScheme.secondary
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val accent = if (kind == AmountKind.INCOME) incomeColor else expenseColor
    val focus = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "今日金额",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                AmountField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    accent = accent,
                    onDone = { focus.clearFocus() },
                )
                if (!valid) {
                    Text(
                        "金额最多两位小数",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(14.dp))
                KindSwitch(
                    kind = kind,
                    expenseColor = expenseColor,
                    incomeColor = incomeColor,
                    onChange = { kind = it },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (defaultAmount != 0L) "默认 ${Money.formatSigned(defaultAmount, defaultKind)}"
                    else "未设置默认金额",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))
                DialogActions(
                    confirmEnabled = valid,
                    leading = if (overridden) {
                        {
                            Text(
                                "恢复默认",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onConfirm(0L, AmountKind.NONE) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onCancel = onDismiss,
                    onConfirm = {
                        focus.clearFocus()
                        onConfirm(parsed ?: 0L, kind)
                    },
                )
            }
        }
    }
}

/** 大号金额输入：小号 ¥ + 34sp 数字 + 一条 accent 下划线 */
@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    onDone: () -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.displaySmall.copy(color = accent),
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "¥",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent.copy(alpha = 0.55f),
                        modifier = Modifier.padding(bottom = 7.dp, end = 5.dp),
                    )
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                "0.00",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                            )
                        }
                        inner()
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.35f))
                )
            }
        },
    )
}

/** 两段式收支切换，比两个独立 chip 更紧凑 */
@Composable
private fun KindSwitch(
    kind: AmountKind,
    expenseColor: Color,
    incomeColor: Color,
    onChange: (AmountKind) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(AmountKind.EXPENSE to "支出", AmountKind.INCOME to "收入").forEach { (k, label) ->
            val on = kind == k
            val color = if (k == AmountKind.INCOME) incomeColor else expenseColor
            Box(
                Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) color else Color.Transparent)
                    .clickable { onChange(k) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) onAccent(color) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 深色底上用白字、浅色底上用深字 */
private fun onAccent(color: Color): Color =
    if (color.luminance() > 0.55f) Color(0xFF231F1A) else Color.White

/** 统一的对话框底部操作区：比默认按钮小一号 */
@Composable
fun DialogActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "保存",
    confirmEnabled: Boolean = true,
    confirmColor: Color = MaterialTheme.colorScheme.primary,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onCancel)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "取消",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(50))
                .background(if (confirmEnabled) confirmColor else confirmColor.copy(alpha = 0.25f))
                .clickable(enabled = confirmEnabled, onClick = onConfirm)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                confirmLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    heading: String,
    message: String,
    confirmLabel: String = "确定",
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(heading, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                DialogActions(
                    confirmLabel = confirmLabel,
                    confirmColor = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    onCancel = onDismiss,
                    onConfirm = {
                        onDismiss()
                        onConfirm()
                    },
                )
            }
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)

/** 小的"+"按钮，用于分区右上角 */
@Composable
fun AddChip(label: String, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.09f),
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
        }
    }
}
