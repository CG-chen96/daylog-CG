package com.dailybook.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.dailybook.app.BuildConfig
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dailybook.app.data.AmountKind
import com.dailybook.app.data.Money
import com.dailybook.app.data.Routine
import com.dailybook.app.data.weekdaysText
import com.dailybook.app.ui.components.AddChip
import com.dailybook.app.ui.components.ConfirmDialog
import com.dailybook.app.ui.components.EmptyState
import com.dailybook.app.ui.components.PaperCard
import com.dailybook.app.ui.components.SectionHeader
import com.dailybook.app.ui.components.TaskEditorDialog
import com.dailybook.app.ui.components.fieldColors
import com.dailybook.app.ui.theme.CardRadius
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ManageScreen(
    routines: List<Routine>,
    onAdd: (String, String, Int, Long, AmountKind) -> Unit,
    onEdit: (Routine, String, String, Int, Long, AmountKind) -> Unit,
    onArchive: (Routine, Boolean) -> Unit,
    onDelete: (Routine) -> Unit,
    onExport: (Uri, String) -> Unit,
    onImport: (Uri, String, Boolean) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Routine?>(null) }
    var deleting by remember { mutableStateOf<Routine?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var replaceAll by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    // 已删除的事务只留作历史，不在列表里露头
    val active = routines.filter { !it.archived && !it.deleted }
    val archived = routines.filter { it.archived && !it.deleted }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) onExport(uri, password) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            password = ""
            pendingImport = uri
            showImportDialog = true
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "title") {
            Column {
                Text(
                    "事务",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "公共事务会自动出现在每一天的清单里",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------- 公共事务 ----------
        item(key = "routines") {
            PaperCard {
                SectionHeader(
                    title = "公共事务",
                    subtitle = "${active.size} 项进行中",
                ) { AddChip("新增") { showAdd = true } }
                Spacer(Modifier.height(10.dp))
                if (active.isEmpty()) {
                    EmptyState("还没有公共事务", "每天固定要做的事放这里")
                } else {
                    active.forEachIndexed { index, routine ->
                        RoutineRow(
                            routine = routine,
                            onEdit = { editing = routine },
                            onArchive = { onArchive(routine, true) },
                            onDelete = { deleting = routine },
                        )
                        if (index != active.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }
        }

        // ---------- 已归档 ----------
        if (archived.isNotEmpty()) {
            item(key = "archived") {
                PaperCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showArchived = !showArchived },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "已归档 ${archived.size} 项",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (showArchived) "收起" else "展开",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (showArchived) {
                        Spacer(Modifier.height(8.dp))
                        archived.forEach { routine ->
                            RoutineRow(
                                routine = routine,
                                dimmed = true,
                                onEdit = { editing = routine },
                                onArchive = { onArchive(routine, false) },
                                onDelete = { deleting = routine },
                            )
                        }
                    }
                }
            }
        }

        // ---------- 数据与备份 ----------
        item(key = "backup") {
            PaperCard {
                SectionHeader("数据与备份", "全部保存在这台设备上")
                Spacer(Modifier.height(6.dp))
                SettingRow(
                    title = "导出备份",
                    subtitle = "生成加密文件，可存到网盘或电脑",
                    onClick = {
                        password = ""
                        showExportDialog = true
                    },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
                SettingRow(
                    title = "从备份恢复",
                    subtitle = "选择之前导出的 .daylog 文件",
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                )
            }
        }

        // ---------- 关于 ----------
        item(key = "about") {
            PaperCard {
                SectionHeader("关于")
                Spacer(Modifier.height(10.dp))
                InfoLine("版本", BuildConfig.VERSION_NAME)
                InfoLine("存储", "本地 SQLite，无网络权限")
                InfoLine("备份加密", "AES-GCM + PBKDF2")
                Spacer(Modifier.height(10.dp))
                CreatorContact()
                Spacer(Modifier.height(8.dp))
                Text(
                    "没有账号、没有服务端、没有埋点。卸载前请先导出备份。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }

    // ---------- 对话框 ----------
    if (showAdd) {
        TaskEditorDialog(
            heading = "新增公共事务",
            showWeekdays = true,
            onDismiss = { showAdd = false },
            onConfirm = { title, note, weekdays, amount, kind ->
                showAdd = false
                onAdd(title, note, weekdays, amount, kind)
            },
        )
    }

    editing?.let { routine ->
        TaskEditorDialog(
            heading = "编辑公共事务",
            initialTitle = routine.title,
            initialNote = routine.note,
            initialWeekdays = routine.weekdays,
            initialAmount = routine.amount,
            initialKind = routine.amountKind,
            showWeekdays = true,
            onDismiss = { editing = null },
            onConfirm = { title, note, weekdays, amount, kind ->
                editing = null
                onEdit(routine, title, note, weekdays, amount, kind)
            },
        )
    }

    deleting?.let { routine ->
        val today = LocalDate.now()
        val last = today.minusDays(1)
        val hasHistory = routine.createdDate.isBefore(today)
        ConfirmDialog(
            heading = "删除「${routine.title}」？",
            message = if (hasHistory) {
                "从今天起不再出现在清单里，今天及以后的记录会一并移除；" +
                    "${last.monthValue}月${last.dayOfMonth}日 及更早的打卡和金额都会保留。"
            } else {
                "这项事务是今天刚建的，还没有历史记录，删除后不可恢复。"
            },
            confirmLabel = "删除",
            destructive = true,
            onDismiss = { deleting = null },
            onConfirm = { onDelete(routine) },
        )
    }

    if (showExportDialog) {
        PasswordDialog(
            heading = "导出备份",
            message = "设置一个密码用于加密备份文件。留空则导出明文文件（方便查看，但不要外传）。",
            password = password,
            onPasswordChange = { password = it },
            confirmLabel = "选择保存位置",
            onDismiss = { showExportDialog = false },
            onConfirm = {
                showExportDialog = false
                val name = "daylog-" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                    ".daylog"
                exportLauncher.launch(name)
            },
        )
    }

    if (showImportDialog) {
        PasswordDialog(
            heading = "从备份恢复",
            message = "如果备份设置了密码，请在这里输入。",
            password = password,
            onPasswordChange = { password = it },
            confirmLabel = "开始导入",
            extra = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("覆盖现有数据", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "关闭则与原数据合并",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = replaceAll,
                        onCheckedChange = { replaceAll = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            },
            onDismiss = { showImportDialog = false },
            onConfirm = {
                showImportDialog = false
                pendingImport?.let { onImport(it, password, replaceAll) }
            },
        )
    }
}

@Composable
private fun RoutineRow(
    routine: Routine,
    dimmed: Boolean = false,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val alpha = if (dimmed) 0.5f else 1f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f * alpha))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                routine.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                weekdaysText(routine.weekdays) +
                    if (routine.note.isNotBlank()) " · ${routine.note}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (routine.amount != 0L && routine.amountKind != AmountKind.NONE) {
            Spacer(Modifier.width(8.dp))
            Text(
                Money.formatSigned(routine.amount, routine.amountKind),
                style = MaterialTheme.typography.labelLarge,
                color = (if (routine.amountKind == AmountKind.INCOME)
                    MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.secondary).copy(alpha = alpha),
                maxLines = 1,
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("编辑", style = MaterialTheme.typography.bodyMedium) },
                    onClick = { menu = false; onEdit() },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (dimmed) "恢复" else "归档",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = { menu = false; onArchive() },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "删除",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private const val CREATOR_VX = "cg09112286"

/** 创作者联系方式，点一下复制微信号 */
@Composable
private fun CreatorContact() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable {
                val cm = context.getSystemService(ClipboardManager::class.java)
                cm?.setPrimaryClip(ClipData.newPlainText("创作者微信", CREATOR_VX))
                Toast.makeText(context, "微信号已复制", Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "创作者微信",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "点击复制",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            CREATOR_VX,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "使用过程中有什么不方便的地方，欢迎加微信找我；有什么建议也欢迎指教。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun PasswordDialog(
    heading: String,
    message: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmLabel: String,
    extra: @Composable (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant
            ),
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(heading, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = {
                        Text(
                            "密码（可留空）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                extra?.invoke()
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.TextButton(onClick = onConfirm) {
                        Text(confirmLabel, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
