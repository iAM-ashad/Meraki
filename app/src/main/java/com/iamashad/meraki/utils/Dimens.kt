package com.iamashad.meraki.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Dimens(
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val fontSmall: TextUnit,
    val fontMedium: TextUnit,
    val fontLarge: TextUnit,
    val cornerRadius: Dp,
    val avatarSize: Dp
)

val SmallScreenDimens = Dimens(
    paddingSmall = 6.dp,
    paddingMedium = 12.dp,
    paddingLarge = 18.dp,
    fontSmall = 8.sp,
    fontMedium = 12.sp,
    fontLarge = 16.sp,
    cornerRadius = 18.dp,
    avatarSize = 250.dp
)

val MediumScreenDimens = Dimens(
    paddingSmall = 8.dp,
    paddingMedium = 16.dp,
    paddingLarge = 24.dp,
    fontSmall = 12.sp,
    fontMedium = 18.sp,
    fontLarge = 24.sp,
    cornerRadius = 24.dp,
    avatarSize = 300.dp
)

val LargeScreenDimens = Dimens(
    paddingSmall = 12.dp,
    paddingMedium = 20.dp,
    paddingLarge = 32.dp,
    fontSmall = 16.sp,
    fontMedium = 20.sp,
    fontLarge = 28.sp,
    cornerRadius = 32.dp,
    avatarSize = 350.dp
)

val LocalDimens = staticCompositionLocalOf { MediumScreenDimens }

@Composable
fun ProvideDimens(screenWidthDp: Int, screenHeightDp: Int, content: @Composable () -> Unit) {
    val dimens = when {
        screenWidthDp < 360 || screenHeightDp < 600 -> SmallScreenDimens
        screenWidthDp < 720 || screenHeightDp < 800 -> MediumScreenDimens
        else -> LargeScreenDimens
    }

    CompositionLocalProvider(LocalDimens provides dimens, content = content)
}

