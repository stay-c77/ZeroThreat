package com.zerothreat.core.ui.theme

import androidx.compose.ui.graphics.Color

// ── Legacy palette (kept for backward compat with existing screens) ──────────
val PureBlack          = Color(0xFF0F1012)
val DarkBackground     = Color(0xFF011B1A)   // updated → Cyber Teal bg
val WhiteBackground    = DarkBackground
val PureWhite          = Color(0xFFFFFFFF)
val CardBackground     = Color(0xFF0B3231)   // updated → Cyber Teal card
val GlassyBlack        = Color(0xFF0B3231).copy(alpha = 0.98f)
val SurfaceVariant     = Color(0xFF162222)   // updated → panels
val PurpleGlow         = Color(0xFF162222)

// ── Cyber Teal accent palette ─────────────────────────────────────────────────
val NeonTeal           = Color(0xFF00F5D4)   // Primary accent
val NeonTealSecondary  = Color(0xFF00C2A8)   // Secondary accent
val NeonTealDim        = Color(0xFF00F5D4).copy(alpha = 0.15f)
val NeonTealGlow       = Color(0xFF00F5D4).copy(alpha = 0.25f)
val SidebarDeep        = Color(0xFF020A0C)   // Sidebar / deep panels
val BorderTeal         = Color(0xFF1F3D3C)   // Borders / dividers

// ── Legacy accent (kept for backward compat) ──────────────────────────────────
val ElectricPurple     = NeonTeal            // remapped → teal
val ElectricPurpleLight = NeonTealSecondary  // remapped → teal secondary
val PeachAccent        = NeonTeal
val PeachLight         = Color(0xFFDCE9FF)

// ── Text colors ───────────────────────────────────────────────────────────────
val TextPrimary        = Color(0xFFE6FFFF)   // updated → Cyber Teal primary text
val TextSecondary      = Color(0xFFB0D4D4)
val TextMuted          = Color(0xFF7FA6A6)   // updated → Cyber Teal muted text
val TextWhite          = Color(0xFFFFFFFF)

// ── Status colors ─────────────────────────────────────────────────────────────
val SafeGreen          = Color(0xFF00F5D4)   // teal = safe in this theme
val SafeGreenTrue      = Color(0xFF1DB954)   // pure green for explicit safe states
val WarningYellow      = Color(0xFFC8921F)
val DangerRed          = Color(0xFFD56B6B)

// ── Protection state colors ───────────────────────────────────────────────────
val ProtectedColor     = Color(0xFF00F5D4)   // PROTECTED  → neon teal
val PartialColor       = Color(0xFFFFB800)   // PARTIAL    → amber
val UnprotectedColor   = Color(0xFFE05252)   // UNPROTECTED → red