package com.iamashad.meraki.utils

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable

// Phase 4: Replaced rememberWindowSizeClass() (M3 legacy WindowSizeClass wrapper) with
// rememberWindowAdaptiveInfo() returning WindowAdaptiveInfo directly.
// currentWindowAdaptiveInfo() is the canonical API from material3-adaptive; it requires no
// Activity reference and returns WindowAdaptiveInfo which embeds
// androidx.window.core.layout.WindowSizeClass (COMPACT / MEDIUM / EXPANDED constants).
@Composable
fun rememberWindowAdaptiveInfo(): WindowAdaptiveInfo = currentWindowAdaptiveInfo()
