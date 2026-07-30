package example.imageviewer.model

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Page : NavKey

@Serializable
data object GalleryPage : Page

@Serializable
data class MemoryPage(val pictureIndex: Int) : Page

@Serializable
data class FullScreenPage(val pictureIndex: Int) : Page

@Serializable
data object CameraPage : Page
