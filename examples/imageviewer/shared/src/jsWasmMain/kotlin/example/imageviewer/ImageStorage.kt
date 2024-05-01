package example.imageviewer

import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.model.PictureData

// TODO: Rework it with some web service to store the images
class WebImageStorage : ImageStorage {
    private val pictures = HashMap<String, SavedPicture>()

    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        pictures[picture.id] = SavedPicture(picture, image.imageBitmap)
    }

    override fun delete(picture: PictureData.Camera) {
        pictures.remove(picture.id)
    }

    override fun rewrite(picture: PictureData.Camera) {
        pictures[picture.id]?.let {
            pictures[picture.id] = it.copy(data = picture)
        }
    }

    override suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap {
        return pictures[picture.id]?.bitmap ?: error("Picture was not found")
    }

    override suspend fun getImage(picture: PictureData.Camera): ImageBitmap {
        return pictures[picture.id]?.bitmap ?: error("Picture was not found")
    }

    private data class SavedPicture(
        val data: PictureData,
        val bitmap: ImageBitmap
    )
}