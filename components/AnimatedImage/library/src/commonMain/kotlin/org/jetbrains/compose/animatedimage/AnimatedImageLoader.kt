package org.jetbrains.compose.animatedimage

interface AnimatedImageLoader {
    suspend fun loadBytes(): ByteArray
}