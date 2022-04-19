package org.jetbrains.compose.animatedimage

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale

@Composable
expect fun AnimatedImage(
    loader: AnimatedImageLoader,
    contentDescription: String?,
    contentModifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    placeHolder: @Composable BoxScope.() -> Unit = {}
)

@Composable
expect fun rememberAnimatedImage(url: String): AnimatedImageLoader