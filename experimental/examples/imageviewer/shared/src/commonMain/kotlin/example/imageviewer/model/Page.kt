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

    private var currentPictureIndex by mutableStateOf(0)
    val picture get(): PictureData = pictures[currentPictureIndex]

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

    fun resetSelectedPicture() {
        currentPictureIndex = 0
    }
}
