package com.resume.builder.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand
val Indigo = Color(0xFF4F46E5)
val IndigoDeep = Color(0xFF3730A3)
val Violet = Color(0xFF7C3AED)
val Magenta = Color(0xFFC026D3)
val Teal = Color(0xFF0D9488)
val Amber = Color(0xFFF59E0B)
val Coral = Color(0xFFF43F5E)
val Success = Color(0xFF16A34A)

// Light surfaces
val LightBackground = Color(0xFFF6F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF0F8)
val LightOutline = Color(0xFFD9DCE8)
val Ink = Color(0xFF14172B)
val InkMuted = Color(0xFF5E647A)

// Dark surfaces
val DarkBackground = Color(0xFF0C0E1A)
val DarkSurface = Color(0xFF151829)
val DarkSurfaceVariant = Color(0xFF1F2338)
val DarkOutline = Color(0xFF343954)
val Mist = Color(0xFFE8EAF5)
val MistMuted = Color(0xFFA3A8C2)

object Gradients {
    val brand = Brush.linearGradient(listOf(Indigo, Violet, Magenta))
    val hero = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFFA21CAF)))
    val teal = Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF14B8A6)))
    val sunset = Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEC4899)))
    val onboarding = Brush.verticalGradient(listOf(Color(0xFFEDEBFF), Color(0xFFF6F7FB), Color(0xFFFDF2FF)))
    val onboardingDark = Brush.verticalGradient(listOf(Color(0xFF1B1840), Color(0xFF0C0E1A), Color(0xFF22102B)))
}
