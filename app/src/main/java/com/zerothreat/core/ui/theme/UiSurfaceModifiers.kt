package com.zerothreat.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

typealias UiSurfaceState = Unit

val LocalUiSurfaceState = staticCompositionLocalOf<UiSurfaceState?> { null }

@Composable
fun rememberUiSurfaceState(): UiSurfaceState = remember { Unit }

fun Modifier.appBackground(hazeState: UiSurfaceState?): Modifier = this

fun Modifier.appContainer(
    hazeState: UiSurfaceState?,
    cornerRadius: Dp = 24.dp,
    thin: Boolean = false
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    this.background(CardBackground, shape)
}
