package example.imageviewer

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import example.imageviewer.filter.scaleBitmapAspectRatio
import example.imageviewer.model.PictureData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val maxStorableImageSizePx = 2000
private const val storableThumbnailSizePx = 200

class DesktopImageStorage(
    private val pictures: SnapshotStateList<PictureData>,
    private val ioScope: CoroutineScope
) : ImageStorage {
    private val largeImages = mutableMapOf<PictureData.Camera, ImageBitmap>()
    private val thumbnails = mutableMapOf<PictureData.Camera, ImageBitmap>()

    override fun saveImage(pictureData: PictureData.Camera, image: PlatformStorableImage) {
        if (image.imageBitmap.width == 0 || image.imageBitmap.height == 0) {
            return
        }
        ioScope.launch {
            largeImages[pictureData] = image.imageBitmap.fitInto(maxStorableImageSizePx)
            thumbnails[pictureData] = image.imageBitmap.fitInto(storableThumbnailSizePx)
            pictures.add(0, pictureData)
        }
    }

    override suspend fun getThumbnail(pictureData: PictureData.Camera): ImageBitmap {
        return thumbnails[pictureData]!!
    }

    override suspend fun getImage(pictureData: PictureData.Camera): ImageBitmap {
        return largeImages[pictureData]!!
    }
}

private fun ImageBitmap.fitInto(px: Int): ImageBitmap {
    val targetScale = maxOf(
        px.toFloat() / width,
        px.toFloat() / height
    )
    return if (targetScale < 1.0) {
        scaleBitmapAspectRatio(
            toAwtImage(),
            width = (width * targetScale).toInt(),
            height = (height * targetScale).toInt()
        ).toComposeImageBitmap()
    } else {
        this
    }
}
