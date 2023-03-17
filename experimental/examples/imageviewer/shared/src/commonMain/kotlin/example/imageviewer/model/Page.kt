package example.imageviewer.model

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import example.imageviewer.Dependencies
import example.imageviewer.ExternalImageViewerEvent
import example.imageviewer.view.GalleryStyle
import kotlinx.coroutines.flow.Flow

sealed class Page

class MemoryPage(val picture: PictureData) : Page() {
    val scrollState = ScrollState(0)
}

class CameraPage : Page()

class FullScreenPage(val picture: PictureData) : Page()

class GalleryPage(//todo delete
    val dependencies: Dependencies,
    val externalEvents: Flow<ExternalImageViewerEvent>
) : Page() {
    var galleryStyle by mutableStateOf(GalleryStyle.SQUARES)

    fun toggleGalleryStyle() {
        galleryStyle =
            if (galleryStyle == GalleryStyle.SQUARES) GalleryStyle.LIST else GalleryStyle.SQUARES
    }

    var currentPictureIndex by mutableStateOf(0)

    val picture get(): PictureData? = dependencies.pictures.getOrNull(currentPictureIndex)

    val galleryEntry: PictureData?
        get() = dependencies.pictures.getOrNull(currentPictureIndex)

    @Deprecated("")
    val pictureId
        get(): PictureData? = dependencies.pictures.getOrNull(
            currentPictureIndex
        )

    fun nextImage() {
        currentPictureIndex =
            (currentPictureIndex + 1).mod(dependencies.pictures.lastIndex)
    }

    fun previousImage() {
        currentPictureIndex =
            (currentPictureIndex - 1).mod(dependencies.pictures.lastIndex)
    }

    fun selectPicture(galleryId: PictureData) {
        currentPictureIndex = dependencies.pictures.indexOfFirst { it == galleryId }
    }
}
