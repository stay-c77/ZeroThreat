package com.zerothreat.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Unified spacing constants for consistent UI across all screens.
 * Based on 8dp baseline grid system for Material Design 3.
 */
object Spacing {
    // Baseline units
    val xs = 4.dp      // 4dp - micro spacing
    val sm = 8.dp      // 8dp
    val md = 16.dp     // 16dp
    val lg = 24.dp     // 24dp
    val xl = 28.dp     // extended spacing
    val xxl = 32.dp    // section spacing
    val xxxl = 32.dp   // 32dp - triple extra large

    // Dialog/Popup spacing
    object Dialog {
        val contentPadding = 24.dp      // Interior padding
        val iconSize = 72.dp            // Icon/animation size
        val iconSpacing = 16.dp         // Space below icon
        val titleSpacing = 8.dp         // Space below title
        val urlCardSpacing = 12.dp      // Space below URL card
        val descriptionSpacing = 24.dp  // Space before buttons
        val buttonSpacing = 12.dp       // Space between buttons
        val cardCornerRadius = 12.dp    // Card border radius
        val urlCardRadius = 12.dp       // URL card border radius
    }

    // Button spacing
    object Button {
        val height = 50.dp              // Standard button height
        val cornerRadius = 12.dp        // Button corner radius
        val horizontalPadding = 16.dp   // Button horizontal padding
        val verticalSpacing = 12.dp     // Space between buttons
    }

    // Screen spacing
    object Screen {
        val topSpacing = 24.dp          // Top padding
        val horizontalPadding = 24.dp   // Side padding
        val sectionSpacing = 16.dp      // Space between sections
        val cardSpacing = 8.dp          // Space between cards
    }

    // Card spacing
    object Card {
        val borderRadius = 12.dp        // Card border radius
        val smallRadius = 12.dp         // Small card radius
        val elevation = 4.dp            // Shadow elevation
    }
}

/**
 * Animation timing constants for consistent motion across screens.
 */
object AnimationTiming {
    // Duration in milliseconds
    const val FAST = 150              // Fast transitions (150ms)
    const val NORMAL = 300            // Normal transitions (300ms)
    const val SLOW = 500              // Slow transitions (500ms)

    // For Lottie animations
    const val LOTTIE_INFINITE_LOOP = -1  // Infinite loop duration

    // Easing curves
    const val EASE_IN_OUT = "easeInOut"
}
