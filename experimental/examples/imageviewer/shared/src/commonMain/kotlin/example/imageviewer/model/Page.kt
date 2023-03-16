package example.imageviewer.model

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import example.imageviewer.ExternalImageViewerEvent
import example.imageviewer.view.GalleryStyle
import kotlinx.coroutines.flow.Flow

sealed class Page

class MemoryPage(val picture: Picture) : Page() {
    val scrollState = ScrollState(0)
}

class CameraPage : Page()

class FullScreenPage(val picture: Picture) : Page()

class GalleryPage(
    val externalEvents: Flow<ExternalImageViewerEvent>
) : Page() {
    var galleryStyle by mutableStateOf(GalleryStyle.SQUARES)

    fun toggleGalleryStyle() {
        galleryStyle =
            if (galleryStyle == GalleryStyle.SQUARES) GalleryStyle.LIST else GalleryStyle.SQUARES
    }

    var currentPictureIndex by mutableStateOf(0)

    val picture get(): Picture? = globalPictures.getOrNull(currentPictureIndex)

    val galleryEntry: Picture?
        get() = globalPictures.getOrNull(currentPictureIndex)

    @Deprecated("")
    val pictureId
        get(): Picture? = globalPictures.getOrNull(
            currentPictureIndex
        )

    fun nextImage() {
        currentPictureIndex =
            (currentPictureIndex + 1).mod(globalPictures.lastIndex)
    }

    fun previousImage() {
        currentPictureIndex =
            (currentPictureIndex - 1).mod(globalPictures.lastIndex)
    }

    fun selectPicture(galleryId: Picture) {
        currentPictureIndex = globalPictures.indexOfFirst { it == galleryId }
    }
}