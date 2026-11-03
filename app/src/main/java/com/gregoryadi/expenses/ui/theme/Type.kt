package com.gregoryadi.expenses.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// One system typeface, tight tracking on the big numbers. "tnum" gives digits equal widths,
// so counting animations don't jitter and amounts line up in lists.
private const val TABULAR = "tnum"

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.5).sp, fontFeatureSettings = TABULAR,
    ),
    displayMedium = TextStyle(
        fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.2).sp, fontFeatureSettings = TABULAR,
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.9).sp, fontFeatureSettings = TABULAR,
    ),
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR),
    titleMedium = TextStyle(
        fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR,
    ),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontFeatureSettings = TABULAR),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)
