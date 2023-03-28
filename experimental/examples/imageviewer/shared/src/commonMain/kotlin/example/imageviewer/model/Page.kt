package example.imageviewer.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import example.imageviewer.view.GalleryStyle

sealed class Page

class MemoryPage(val picture: PictureData) : Page() {

}

class CameraPage : Page()

class FullScreenPage(val picture: PictureData) : Page()

class GalleryPage(
    val pictures: SnapshotStateList<PictureData>
) : Page() {
    var galleryStyle by mutableStateOf(GalleryStyle.SQUARES)

    fun toggleGalleryStyle() {
        galleryStyle =
            if (galleryStyle == GalleryStyle.SQUARES) GalleryStyle.LIST else GalleryStyle.SQUARES
    }

    var currentPictureIndex by mutableStateOf(0)

    val picture get(): PictureData = pictures[currentPictureIndex]

    @Deprecated("")
    val galleryEntry: PictureData
        get() = pictures[currentPictureIndex]

    @Deprecated("")
    val pictureId
        get(): PictureData? = pictures.getOrNull(
            currentPictureIndex
        )

    fun nextImage() {
        currentPictureIndex =
            (currentPictureIndex + 1).mod(pictures.size)
    }

    fun previousImage() {
        currentPictureIndex =
            (currentPictureIndex - 1).mod(pictures.size)
    }

    fun selectPicture(picture: PictureData) {
        currentPictureIndex = pictures.indexOfFirst { it == picture }
    }
}
