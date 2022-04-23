package org.jetbrains.compose.animatedimage
expect abstract class AnimatedImageLoader {
    suspend fun loadAnimatedImage(): AnimatedImage
    abstract suspend fun generateByteArray(): ByteArray
}

sealed interface AnimatedImageLoaderState {
    object Loading : AnimatedImageLoaderState
    data class Success(val animatedImage: AnimatedImage) : AnimatedImageLoaderState
    data class Error(val ex: Exception) : AnimatedImageLoaderState
}