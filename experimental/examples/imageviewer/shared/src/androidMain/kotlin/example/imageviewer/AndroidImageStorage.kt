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
    val mapWithBigImages = mutableMapOf<PictureData.Camera, ImageBitmap>()
    val mapWithThumbnails = mutableMapOf<PictureData.Camera, ImageBitmap>()
    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        if (image.imageBitmap.width == 0 || image.imageBitmap.height == 0) {
            return
        }
        ioScope.launch {
            val androidBitmap = image.imageBitmap.asAndroidBitmap()

            val targetScale = maxOf(
                maxStorableImageSizePx.toFloat() / androidBitmap.width,
                maxStorableImageSizePx.toFloat() / androidBitmap.height
            )
            mapWithBigImages[picture] =
                if (targetScale < 1.0) {
                    androidBitmap.scale(
                        width = (androidBitmap.width * targetScale).toInt(),
                        height = (androidBitmap.height * targetScale).toInt()
                    ).asImageBitmap()
                } else {
                    image.imageBitmap
                }

            val targetThumbnailScale = maxOf(
                storableThumbnailSizePx.toFloat() / androidBitmap.width,
                storableThumbnailSizePx.toFloat() / androidBitmap.height
            )
            mapWithThumbnails[picture] = androidBitmap.scale(
                width = (androidBitmap.width * targetThumbnailScale).toInt(),
                height = (androidBitmap.height * targetThumbnailScale).toInt()
            ).asImageBitmap()

            pictures.add(0, picture)
        }
    }

    override suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap {
        return mapWithThumbnails[picture]!!
    }

    override suspend fun getImage(picture: PictureData.Camera): ImageBitmap {
        return mapWithBigImages[picture]!!
    }

}
