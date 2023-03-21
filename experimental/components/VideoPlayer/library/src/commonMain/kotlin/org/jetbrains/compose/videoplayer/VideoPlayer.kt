package org.jetbrains.compose.videoplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier

data class Progress(val fraction: Float, val time/* millis */: Long)

@Composable
fun VideoPlayer(
    url: String,
    isResumed: Boolean,
    volume: Float = 1f,
    speed: Float = 1f,
    seek: Float = 0f,
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier,
    onFinish: (() -> Unit)? = null
) = VideoPlayerImpl(
    url = url,
    isResumed = isResumed,
    volume = volume,
    speed = speed,
    seek = seek,
    isFullscreen = isFullscreen,
    modifier = modifier,
    onFinish = onFinish
)

internal expect fun VideoPlayerImpl(
    url: String,
    isResumed: Boolean,
    volume: Float,
    speed: Float,
    seek: Float,
    isFullscreen: Boolean,
    modifier: Modifier,
    onFinish: (() -> Unit)?
): State<Progress>
