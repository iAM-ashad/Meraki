package com.iamashad.meraki.utils

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass

// Define a data class for dimensions
data class Dimens(
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val fontSmall: TextUnit,
    val fontMedium: TextUnit,
    val fontLarge: TextUnit,
    val cornerRadius: Dp,
    val avatarSize: Dp,
    val elevation: Dp
)

val CompactDimens = Dimens(
    paddingSmall = 8.dp,
    paddingMedium = 16.dp,
    paddingLarge = 24.dp,
    fontSmall = 16.sp,
    fontMedium = 24.sp,
    fontLarge = 32.sp,
    cornerRadius = 24.dp,
    avatarSize = 225.dp,
    elevation = 8.dp
)

val MediumDimens = Dimens(
    paddingSmall = 12.dp,
    paddingMedium = 24.dp,
    paddingLarge = 36.dp,
    fontSmall = 24.sp,
    fontMedium = 32.sp,
    fontLarge = 40.sp,
    cornerRadius = 36.dp,
    avatarSize = 400.dp,
    elevation = 16.dp
)

val ExpandedDimens = Dimens(
    paddingSmall = 12.dp,
    paddingMedium = 24.dp,
    paddingLarge = 36.dp,
    fontSmall = 24.sp,
    fontMedium = 32.sp,
    fontLarge = 40.sp,
    cornerRadius = 40.dp,
    avatarSize = 400.dp,
    elevation = 20.dp
)

// CompositionLocal for dimensions
val LocalDimens = staticCompositionLocalOf { MediumDimens }

// Phase 4: ProvideDimens now accepts WindowAdaptiveInfo (material3-adaptive) instead of the
// legacy M3 WindowSizeClass.  Breakpoints map directly to
// androidx.window.core.layout.WindowWidthSizeClass COMPACT / MEDIUM / EXPANDED constants.
@Composable
fun ProvideDimens(windowAdaptiveInfo: WindowAdaptiveInfo, content: @Composable () -> Unit) {
    val dimens = when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> CompactDimens
        WindowWidthSizeClass.MEDIUM -> MediumDimens
        WindowWidthSizeClass.EXPANDED -> ExpandedDimens
        else -> MediumDimens // Fallback
    }
    CompositionLocalProvider(LocalDimens provides dimens, content = content)
}
