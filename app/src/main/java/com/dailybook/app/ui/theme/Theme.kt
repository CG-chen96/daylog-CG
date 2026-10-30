package com.dailybook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Paper = Color(0xFFFAF7F2)
private val Ink = Color(0xFF2A2622)
private val InkSoft = Color(0xFF7D7367)
private val HairLine = Color(0xFFEBE3D8)
private val Navy = Color(0xFF3E5A6E)
private val Clay = Color(0xFFB87A56)
private val Sage = Color(0xFF6F8B77)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE8EF),
    onPrimaryContainer = Color(0xFF1C3143),
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E4D8),
    onSecondaryContainer = Color(0xFF6B3D24),
    tertiary = Sage,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE9E0),
    onTertiaryContainer = Color(0xFF2A3D31),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3EDE4),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFB6AC9F),
    outlineVariant = HairLine,
    error = Color(0xFFB4544A),
    onError = Color.White,
    errorContainer = Color(0xFFF7DFDC),
    onErrorContainer = Color(0xFF6B2620),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFAF7),
    surfaceContainer = Color(0xFFF7F3EC),
    surfaceContainerHigh = Color(0xFFF2ECE3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DBBD0),
    onPrimary = Color(0xFF11283A),
    primaryContainer = Color(0xFF2A4356),
    onPrimaryContainer = Color(0xFFD3E4EF),
    secondary = Color(0xFFDCA985),
    onSecondary = Color(0xFF3C2314),
    secondaryContainer = Color(0xFF553A27),
    onSecondaryContainer = Color(0xFFF7E0D0),
    tertiary = Color(0xFFA6C4AE),
    onTertiary = Color(0xFF1E3327),
    tertiaryContainer = Color(0xFF33493C),
    onTertiaryContainer = Color(0xFFD6E8DB),
    background = Color(0xFF15140F),
    onBackground = Color(0xFFEDE7DD),
    surface = Color(0xFF1E1C17),
    onSurface = Color(0xFFEDE7DD),
    surfaceVariant = Color(0xFF2A2721),
    onSurfaceVariant = Color(0xFFAFA69A),
    outline = Color(0xFF5E574D),
    outlineVariant = Color(0xFF37332C),
    error = Color(0xFFE8A69E),
    onError = Color(0xFF4A130F),
    errorContainer = Color(0xFF6B2620),
    onErrorContainer = Color(0xFFF7DFDC),
    surfaceContainerLowest = Color(0xFF121110),
    surfaceContainerLow = Color(0xFF1B1915),
    surfaceContainer = Color(0xFF221F1A),
    surfaceContainerHigh = Color(0xFF2A2721),
)

/** 图表与状态用的语义色，统一从主题取，深浅色自动适配 */
@Immutable
data class ChartColors(
    val routine: Color,
    val daily: Color,
    val done: Color,
    val missed: Color,
    val grid: Color,
    /** 出游类别配色，顺序与 TripKind 的 ordinal 一致 */
    val tripKinds: List<Color>,
)

private val LightChart = ChartColors(
    routine = Navy,
    daily = Clay,
    done = Sage,
    missed = Color(0xFFE4D9CB),
    grid = HairLine,
    tripKinds = listOf(
        Navy,               // 交通
        Clay,               // 餐饮
        Sage,               // 住宿
        Color(0xFF8A7BA8),  // 购物
        Color(0xFFC09A3E),  // 景点
        Color(0xFF9A9086),  // 其他
    ),
)

private val DarkChart = ChartColors(
    routine = Color(0xFF9DBBD0),
    daily = Color(0xFFDCA985),
    done = Color(0xFFA6C4AE),
    missed = Color(0xFF35312A),
    grid = Color(0xFF37332C),
    tripKinds = listOf(
        Color(0xFF9DBBD0),
        Color(0xFFDCA985),
        Color(0xFFA6C4AE),
        Color(0xFFB6A6D4),
        Color(0xFFDCC36B),
        Color(0xFF8C8378),
    ),
)

val LocalChartColors = staticCompositionLocalOf { LightChart }

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp, lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 15.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(
        fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontSize = 11.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
    ),
)

val CardRadius = 22.dp
val RowRadius = 16.dp

@Composable
fun DaylogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    androidx.compose.runtime.CompositionLocalProvider(
        LocalChartColors provides if (darkTheme) DarkChart else LightChart,
    ) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
