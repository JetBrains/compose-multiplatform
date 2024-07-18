package example.imageviewer.storage

import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData

interface ImageStorage {
    fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage)
    fun delete(picture: PictureData.Camera)
    fun rewrite(picture: PictureData.Camera)
    suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap
    suspend fun getImage(picture: PictureData.Camera): ImageBitmap
}