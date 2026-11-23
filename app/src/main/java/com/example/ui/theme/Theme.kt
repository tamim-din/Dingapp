package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode(val labelBn: String) {
    AUTO("স্বয়ংক্রিয় মুড"),
    DAY("দিনের মুড"),
    NIGHT("রাতের মুড");

    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

@Immutable
data class NoorGlassColors(
    val isDark: Boolean,
    val background: Color,
    val glassCard: Color,
    val glassCardElevated: Color,
    val glassBorder: Color,
    val accentGold: Color,
    val emeraldPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val warning: Color,
    val error: Color
)

val LocalNoorGlassColors = staticCompositionLocalOf {
    NoorGlassColors(
        isDark = false,
        background = DayBackground,
        glassCard = DayGlassCard,
        glassCardElevated = DayGlassCardElevated,
        glassBorder = DayGlassBorder,
        accentGold = DayAccent,
        emeraldPrimary = DayPrimary,
        textPrimary = DayTextPrimary,
        textSecondary = DayTextSecondary,
        success = EmeraldSuccess,
        warning = WarmAmber,
        error = SoftCoralError
    )
}

val NoorShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun DcfbdNoorTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.AUTO -> systemDark
        ThemeMode.DAY -> false
        ThemeMode.NIGHT -> true
    }

    val animSpec = tween<Color>(durationMillis = 380)
    val bg by animateColorAsState(if (useDark) NightBackground else DayBackground, animSpec, label = "bg")
    val surface by animateColorAsState(if (useDark) NightSurface else DaySurface, animSpec, label = "surface")
    val surfaceVariant by animateColorAsState(if (useDark) NightSurfaceVariant else DaySurfaceVariant, animSpec, label = "surfaceVar")
    val primary by animateColorAsState(if (useDark) NightPrimary else DayPrimary, animSpec, label = "primary")
    val accent by animateColorAsState(if (useDark) NightAccent else DayAccent, animSpec, label = "accent")
    val textPrimary by animateColorAsState(if (useDark) NightTextPrimary else DayTextPrimary, animSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(if (useDark) NightTextSecondary else DayTextSecondary, animSpec, label = "textSecondary")
    val glassCard by animateColorAsState(if (useDark) NightGlassCard else DayGlassCard, animSpec, label = "glass")
    val glassElevated by animateColorAsState(if (useDark) NightGlassCardElevated else DayGlassCardElevated, animSpec, label = "glassElev")
    val glassBorder by animateColorAsState(if (useDark) NightGlassBorder else DayGlassBorder, animSpec, label = "border")
    val success by animateColorAsState(if (useDark) EmeraldSuccessNight else EmeraldSuccess, animSpec, label = "success")
    val error by animateColorAsState(if (useDark) SoftCoralErrorNight else SoftCoralError, animSpec, label = "error")

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = NightBackground,
            primaryContainer = NightSurfaceVariant,
            onPrimaryContainer = NightTextPrimary,
            secondary = NightPrimary,
            onSecondary = NightTextPrimary,
            secondaryContainer = NightSurface,
            onSecondaryContainer = NightAccent,
            tertiary = accent,
            onTertiary = NightBackground,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            outline = glassBorder,
            error = error,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = DaySurfaceVariant,
            onPrimaryContainer = DayPrimary,
            secondary = accent,
            onSecondary = DayTextPrimary,
            secondaryContainer = Color(0xFFF2EAD5),
            onSecondaryContainer = DayTextPrimary,
            tertiary = DayPrimaryLight,
            onTertiary = Color.White,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            outline = glassBorder,
            error = error,
            onError = Color.White
        )
    }

    val noorColors = NoorGlassColors(
        isDark = useDark,
        background = bg,
        glassCard = glassCard,
        glassCardElevated = glassElevated,
        glassBorder = glassBorder,
        accentGold = accent,
        emeraldPrimary = primary,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        success = success,
        warning = WarmAmber,
        error = error
    )

    CompositionLocalProvider(LocalNoorGlassColors provides noorColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoorTypography,
            shapes = NoorShapes,
            content = content
        )
    }
}
