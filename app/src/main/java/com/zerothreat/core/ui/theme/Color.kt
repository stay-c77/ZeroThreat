package com.zerothreat.core.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark Violet Security Theme ────────────────────────────────
// Neutral dark base — NO purple tint in backgrounds
val DarkBackground    = Color(0xFF0F0F10)   // primary bg — near-black neutral
val CardBackground    = Color(0xFF1A1A1C)   // card bg
val SurfaceVariant    = Color(0xFF202022)   // elevated surface / list items / stat pills
val DeepPanel         = Color(0xFF0F0F10)   // same as bg for deep insets
val BorderColor       = Color(0xFF2C2C2E)   // subtle neutral border between cards

// ── Legacy aliases (names kept, values updated) ───────────────
val PureBlack         = Color(0xFF0F0F10)
val WhiteBackground   = DarkBackground
val PureWhite         = Color(0xFFFFFFFF)
val GlassyBlack       = CardBackground
val PurpleGlow        = Color(0xFF8B5CF6).copy(alpha = 0.20f)  // violet glow behind shield

// ── Primary Accent — Tailwind Violet-500 ─────────────────────
// Used for: icons, ring, borders, nav active, "Scan now" labels
val NeonTeal          = Color(0xFF8B5CF6)   // #8B5CF6 violet-500
val CyberTeal         = Color(0xFFA78BFA)   // #A78BFA violet-400 (lighter, for gradients/glow)

// ── Extra gradient helpers ────────────────────────────────────
val VioletDeep        = Color(0xFF7C3AED)   // violet-600 for sweep gradient start
val VioletSoft        = Color(0xFFC4B5FD)   // violet-300 for very subtle highlights

// ── Legacy accent aliases ─────────────────────────────────────
val ElectricPurple      = NeonTeal
val ElectricPurpleLight = CyberTeal
val PeachAccent         = NeonTeal
val PeachLight          = Color(0xFF8B5CF6).copy(alpha = 0.15f)

// ── Text Colors ───────────────────────────────────────────────
val TextPrimary   = Color(0xFFFFFFFF)   // #FFFFFF pure white
val TextSecondary = Color(0xFF9CA3AF)   // #9CA3AF Tailwind gray-400
val TextMuted     = Color(0xFF9CA3AF)
val TextWhite     = Color(0xFFFFFFFF)

// ── Status Colors ─────────────────────────────────────────────
val SafeGreen     = Color(0xFF22C55E)   // #22C55E green-500  (matches screenshot tick)
val WarningYellow = Color(0xFFFACC15)   // #FACC15 yellow-400 (matches screenshot dot)
val DangerRed     = Color(0xFFEF4444)   // #EF4444 red-500    (matches screenshot dot)