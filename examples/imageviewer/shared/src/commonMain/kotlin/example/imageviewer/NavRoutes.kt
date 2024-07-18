package example.imageviewer

import kotlinx.serialization.Serializable

@Serializable
data object Gallery

@Serializable
data class Memory(val imageIndex: Int)

@Serializable
data class FullScreen(val imageIndex: Int)

@Serializable
data object Camera