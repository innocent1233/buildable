package com.libraryx.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Replaces the `fontFamily.display` / `fontFamily.mono` Tailwind tokens used for the
 * neon "cyber" headings across the app. No custom font file ships in this project
 * (see migration report); `FontFamily.Monospace` approximates the original's
 * monospaced display headings until a real font asset is added under res/font.
 */
val DisplayFontFamily = FontFamily.Monospace
val BodyFontFamily = FontFamily.SansSerif

val StudyLabTypography = Typography(
    headlineLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)
