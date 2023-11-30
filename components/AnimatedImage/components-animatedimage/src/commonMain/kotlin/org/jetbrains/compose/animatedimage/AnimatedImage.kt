package org.jetbrains.compose.animatedimage

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Represents an image formed by a sequence of frames with a specific duration each one.
 */
expect class AnimatedImage

/**
 * Loads an [AnimatedImage] from a given path.
 * The image can be loaded from different sources depending on the platform (only desktop currently):
 * Desktop: From the network if it's a valid URL, from the local storage otherwise.
 */
expect suspend fun loadAnimatedImage(path: String): AnimatedImage

/**
 * Loads an [AnimatedImage] from the resources.
 */
expect suspend fun loadResourceAnimatedImage(path: String): AnimatedImage

/**
 * Animates an [AnimatedImage] by returning an [ImageBitmap] for each frame of the image.
 * The caller will be recomposed with each new frame that has been rendered.
 */
@Composable
expect fun AnimatedImage.animate(): ImageBitmap

private val BlankBitmap = ImageBitmap(1, 1)

/**
 * Object used to represent a blank ImageBitmap with the minimum possible size.
 */
val ImageBitmap.Companion.Blank get() = BlankBitmap