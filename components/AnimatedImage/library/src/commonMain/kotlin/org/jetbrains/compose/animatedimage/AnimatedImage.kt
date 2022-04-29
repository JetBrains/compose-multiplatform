package org.jetbrains.compose.animatedimage

import androidx.compose.ui.graphics.ImageBitmap

expect class AnimatedImage

expect suspend fun loadAnimatedImage(path: String): AnimatedImage

expect suspend fun loadResourceAnimatedImage(path: String): AnimatedImage

expect fun AnimatedImage.animate(): ImageBitmap

private val BlankBitmap = ImageBitmap(1, 1)
val ImageBitmap.Companion.Blank get() = BlankBitmap