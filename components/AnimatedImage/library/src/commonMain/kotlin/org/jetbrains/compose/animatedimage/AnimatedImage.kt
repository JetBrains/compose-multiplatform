package org.jetbrains.compose.animatedimage

import androidx.compose.runtime.Composable

expect class AnimatedImage

expect suspend fun loadAnimatedImage(path: String): AnimatedImage

expect suspend fun loadResourceAnimatedImage(path: String): AnimatedImage

@Composable
expect fun asyncAnimatedImageLoaderState(path: String): AnimatedImageLoaderState

@Composable
expect fun asyncResourceAnimatedImageLoaderState(path: String): AnimatedImageLoaderState

@Composable
expect fun asyncAnimatedImageLoaderState(animatedImageLoader: AnimatedImageLoader): AnimatedImageLoaderState