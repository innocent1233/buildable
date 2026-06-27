package com.libraryx.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mirrors the CSS custom properties in src/index.css / src/tailwind.config.ts —
 * the dark "cyberpunk lab" palette used throughout AppLayout.tsx, Dashboard.tsx, etc.
 */
object StudyLabColors {
    val Background = Color(0xFF0A0E1F)
    val Surface = Color(0xFF111527)
    val SurfaceVariant = Color(0xFF161B30)
    val Border = Color(0xFF22273F)

    val NeonGreen = Color(0xFF39FF8C)
    val NeonCyan = Color(0xFF22D3EE)
    val NeonPink = Color(0xFFFF3DAD)
    val NeonOrange = Color(0xFFFF8A3D)
    val NeonPurple = Color(0xFFB45CFF)

    val TextPrimary = Color(0xFFE7ECF7)
    val TextMuted = Color(0xFF8A93B2)

    val StatusPaid = NeonGreen
    val StatusUnpaid = NeonOrange
    val StatusOverdue = NeonPink

    // Light-theme counterparts for ThemeToggle.tsx parity (less neon, daylight readable).
    val LightBackground = Color(0xFFF6F7FB)
    val LightSurface = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFE2E5F0)
    val LightTextPrimary = Color(0xFF131726)
    val LightTextMuted = Color(0xFF5C6480)
}
