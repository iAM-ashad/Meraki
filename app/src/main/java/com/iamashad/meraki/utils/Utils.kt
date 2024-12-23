package com.iamashad.meraki.utils

import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide

@Composable
fun VideoAnimation(
    modifier: Modifier = Modifier,
    res: Int
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = "android.resource://${context.packageName}/${res}"
            val mediaItem = androidx.media3.common.MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    this.player = exoPlayer
                    this.useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun LoadImageWithGlide(
    imageScale: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = imageScale
            }
        },
        modifier = modifier,
        update = { imageView ->
            // Use Glide to load the image
            Glide.with(imageView.context)
                .load(imageUrl)
                .into(imageView)
        }
    )
}

