package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors (Vibrant Orange & Dark Navy)
val BrandOrange = Color(0xFFFF6D00)      // Sleek energetic Orange accent
val SecondaryOrange = Color(0xFFFF851B)  // Lighter secondary orange
val DeepDarkNavy = Color(0xFF050A14)     // Pure dark background canvas
val SleekNavyCard = Color(0xFF0D1B2D)    // Premium dark blue card shape
val NavySurface = Color(0xFF11223F)      // Lighter accent surface navy
val AccentGold = Color(0xFFFFB703)       // Warm Amber/Gold highlight

// Compatibility Aliases to keep existing code compiling perfectly
val PrimaryBlue = Color(0xFFFF6D00)      // Map to BrandOrange!
val SecondaryEmerald = Color(0xFF38BDF8) // High-tech light blue / cyan for active items
val AccentAmber = Color(0xFFFFB703)      // Warm golden accent

// Light Color Scheme Shades
val LightBackground = Color(0xFFF8FAFC)  // Clean grayish slate
val LightSurface = Color(0xFFFFFFFF)     // True White
val LightTextPrimary = Color(0xFF0F172A) // Slate 900
val LightTextSecondary = Color(0xFF475569) // Slate 600

// Dark Color Scheme Shades
val DarkBackground = DeepDarkNavy
val DarkSurface = SleekNavyCard
val DarkPrimary = BrandOrange
val DarkSecondary = SecondaryOrange
val DarkTextPrimary = Color(0xFFFFFFFF)  // White
val DarkTextSecondary = Color(0xFF94A3B8) // Slate 400

// Retro CRT Terminal Colors (used in Nova Solicitação: Cliente, Máquina, Ocorrência)
val TerminalBackground = Color(0xFF0A0A0A)      // Solid black CRT background
val TerminalBorder = Color(0xFF1F3D1F)          // Dark green border
val TerminalGreen = Color(0xFF2EE85C)           // Standard text green
val TerminalGreenBright = Color(0xFF33FF66)     // Selected/highlighted item green
val TerminalSelectedBg = Color(0xFF102010)      // Slightly lighter bg for selected item
val TerminalHint = Color(0xFF1F8F3E)            // Dim green for placeholder/hint text
