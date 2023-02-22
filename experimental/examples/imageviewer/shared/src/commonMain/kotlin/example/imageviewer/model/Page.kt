package example.imageviewer.model

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import example.imageviewer.ExternalImageViewerEvent
import example.imageviewer.view.GalleryStyle
import kotlinx.coroutines.flow.Flow

sealed class Page

class MemoryPage(val pictureIndex: Int) : Page() {
    val scrollState = ScrollState(0)
}

class CameraPage : Page()

class FullScreenPage(val picture: Picture) : Page()

class GalleryPage(
    val galleryState: GalleryState,
    val externalEvents: Flow<ExternalImageViewerEvent>
) : Page() {
    var galleryStyle by mutableStateOf(GalleryStyle.SQUARES)

    fun toggleGalleryStyle() {
        galleryStyle = if(galleryStyle == GalleryStyle.SQUARES) GalleryStyle.LIST else GalleryStyle.SQUARES
    }

    var currentPictureIndex by mutableStateOf(0)

    val picture get(): Picture? = galleryState.picturesWithThumbnail.getOrNull(currentPictureIndex)?.picture

    fun nextImage() {
        currentPictureIndex =
            (currentPictureIndex + 1).mod(galleryState.picturesWithThumbnail.lastIndex)
    }

    fun previousImage() {
        currentPictureIndex =
            (currentPictureIndex - 1).mod(galleryState.picturesWithThumbnail.lastIndex)
    }

    fun selectPicture(picture: Picture) {
        currentPictureIndex =
            galleryState.picturesWithThumbnail.indexOfFirst { it.picture == picture }
    }
}