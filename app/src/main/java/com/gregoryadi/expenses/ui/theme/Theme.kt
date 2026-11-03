package com.gregoryadi.expenses.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * "Ink": a dark-first theme. Near-black ground, one lime accent, and colour otherwise reserved for
 * payment methods and categories, so a colour on screen always means something.
 */
object Ink {
    val Ground = Color(0xFF0F100D)
    val Sunken = Color(0xFF0A0B09)
    val Sheet = Color(0xFF151712)
    val Card = Color(0xFF1A1C17)
    val Raised = Color(0xFF23261F)
    val Line = Color(0xFF2B2E27)
    val Text = Color(0xFFF2F3EE)
    val Muted = Color(0xFF9EA295)
    val Lime = Color(0xFFD4F45A)
    val LimeDeep = Color(0xFF2B3313)
    val Coral = Color(0xFFFF8A6B)
}

private val InkColors = darkColorScheme(
    primary = Ink.Lime,
    onPrimary = Ink.Ground,
    primaryContainer = Ink.LimeDeep,
    onPrimaryContainer = Ink.Lime,
    inversePrimary = Ink.LimeDeep,
    secondary = Ink.Text,
    onSecondary = Ink.Ground,
    secondaryContainer = Ink.Raised,
    onSecondaryContainer = Ink.Text,
    tertiary = Ink.Lime,
    onTertiary = Ink.Ground,
    background = Ink.Ground,
    onBackground = Ink.Text,
    surface = Ink.Ground,
    onSurface = Ink.Text,
    surfaceVariant = Ink.Raised,
    onSurfaceVariant = Ink.Muted,
    surfaceTint = Ink.Ground,
    inverseSurface = Ink.Text,
    inverseOnSurface = Ink.Ground,
    surfaceBright = Ink.Raised,
    surfaceDim = Ink.Ground,
    surfaceContainerLowest = Ink.Sunken,
    surfaceContainerLow = Ink.Sheet,
    surfaceContainer = Ink.Card,
    surfaceContainerHigh = Ink.Raised,
    surfaceContainerHighest = Ink.Line,
    outline = Color(0xFF3A3E35),
    outlineVariant = Ink.Line,
    error = Ink.Coral,
    onError = Ink.Ground,
    errorContainer = Color(0xFF3A1F17),
    onErrorContainer = Ink.Coral,
    scrim = Color(0xFF000000),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ExpensesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
