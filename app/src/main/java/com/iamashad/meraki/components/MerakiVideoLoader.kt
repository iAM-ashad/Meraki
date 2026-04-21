package com.iamashad.meraki.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.iamashad.meraki.R

/**
 * A lifecycle-aware, full-screen video loading overlay that plays [R.raw.meraki_loader]
 * on a seamless loop, replacing all plain CircularProgressIndicator usages with a
 * premium, on-brand experience.
 *
 * Internal details:
 *  - ExoPlayer is initialised with [remember] so each composition gets exactly one instance.
 *  - [Player.REPEAT_MODE_ALL] keeps the animation looping without any gaps.
 *  - [RawResourceDataSource.buildRawResourceUri] points the player at the bundled MP4.
 *  - [DisposableEffect] releases the player the moment the composable leaves the composition,
 *    preventing audio/codec handle leaks across screen transitions.
 *  - [volume] is set to 0 — the loader is always silent.
 *
 * Usage:
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize()) {
 *     // Screen content
 *     if (isLoading) {
 *         MerakiVideoLoader()          // full-screen overlay
 *     }
 * }
 * ```
 *
 * For an in-line (non-overlay) loader, pass a constrained [modifier]:
 * ```kotlin
 * MerakiVideoLoader(modifier = Modifier.fillMaxWidth().height(200.dp))
 * ```
 */
@Composable
fun MerakiVideoLoader(modifier: Modifier = Modifier.fillMaxSize()) {
    val context = LocalContext.current

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            val rawUri = RawResourceDataSource.buildRawResourceUri(R.raw.meraki_loader)
            val mediaItem = MediaItem.fromUri(rawUri)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f          // silent — purely visual
            prepare()
            play()
        }
    }

    // Strict lifecycle cleanup: release ExoPlayer when this composable leaves the tree.
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        // Solid black background: shown in any letterboxed areas when the video
        // aspect ratio doesn't exactly match the screen, giving a clean edge instead
        // of a semi-transparent grey bleed-through.
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false  // hide all playback controls
                    // RESIZE_MODE_FIT scales the video to fit entirely within the screen,
                    // preserving its aspect ratio. The full animation is always visible —
                    // no cropping of the head silhouette or tree. Any remaining space is
                    // filled by the solid black Box background above.
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
