package example.imageviewer.model

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import example.imageviewer.ExternalImageViewerEvent
import example.imageviewer.view.GalleryStyle
import kotlinx.coroutines.flow.Flow

sealed class Page

class MemoryPage(val pictureData: PictureData) : Page() {
    val scrollState = ScrollState(0)
}

class CameraPage : Page()

class FullScreenPage(val pictureData: PictureData) : Page()

class GalleryPage(
    val photoGallery: PhotoGallery,
    val externalEvents: Flow<ExternalImageViewerEvent>
) : Page() {
    var galleryStyle by mutableStateOf(GalleryStyle.SQUARES)

    fun toggleGalleryStyle() {
        galleryStyle =
            if (galleryStyle == GalleryStyle.SQUARES) GalleryStyle.LIST else GalleryStyle.SQUARES
    }

    var currentPictureIndex by mutableStateOf(0)

    val picture get(): Picture? = photoGallery.galleryStateFlow.value.getOrNull(currentPictureIndex)?.picture

    val galleryEntry: GalleryEntryWithMetadata?
        get() = photoGallery.galleryStateFlow.value.getOrNull(
            currentPictureIndex
        )

    val pictureId
        get(): GalleryId? = photoGallery.galleryStateFlow.value.getOrNull(
            currentPictureIndex
        )?.id

    fun nextImage() {
        currentPictureIndex =
            (currentPictureIndex + 1).mod(photoGallery.galleryStateFlow.value.lastIndex)
    }

    fun previousImage() {
        currentPictureIndex =
            (currentPictureIndex - 1).mod(photoGallery.galleryStateFlow.value.lastIndex)
    }

    fun selectPicture(galleryId: GalleryId) {
        currentPictureIndex = photoGallery.galleryStateFlow.value.indexOfFirst { it.id == galleryId }
    }
}