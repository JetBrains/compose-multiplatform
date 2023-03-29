package example.imageviewer

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import example.imageviewer.model.PictureData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val maxStorableImageSizePx = 2000
private const val storableThumbnailSizePx = 200

class AndroidImageStorage(
    private val pictures: SnapshotStateList<PictureData>,
    private val ioScope: CoroutineScope
) : ImageStorage {
    private val largeImages = mutableMapOf<PictureData.Camera, ImageBitmap>()
    private val thumbnails = mutableMapOf<PictureData.Camera, ImageBitmap>()

    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        if (image.imageBitmap.width == 0 || image.imageBitmap.height == 0) {
            return
        }
        ioScope.launch {
            largeImages[picture] = image.imageBitmap.fitInto(maxStorableImageSizePx)
            thumbnails[picture] = image.imageBitmap.fitInto(storableThumbnailSizePx)
            pictures.add(0, picture)
        }
    }

    override suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap {
        return thumbnails[picture]!!
    }

    override suspend fun getImage(picture: PictureData.Camera): ImageBitmap {
        return largeImages[picture]!!
    }
}

private fun ImageBitmap.fitInto(px: Int): ImageBitmap {
    val targetScale = maxOf(
        px.toFloat() / width,
        px.toFloat() / height
    )
    return if (targetScale < 1.0) {
        asAndroidBitmap().scale(
            width = (width * targetScale).toInt(),
            height = (height * targetScale).toInt()
        ).asImageBitmap()
    } else {
        this
    }
}
