package example.imageviewer

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
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
    val largeImages = mutableMapOf<PictureData.Camera, ImageBitmap>()
    val thumbnails = mutableMapOf<PictureData.Camera, ImageBitmap>()
    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        if (image.imageBitmap.width == 0 || image.imageBitmap.height == 0) {
            return
        }
        ioScope.launch {
            val awtImage = image.imageBitmap.toAwtImage()

            val targetScale = maxOf(
                maxStorableImageSizePx.toFloat() / awtImage.width,
                maxStorableImageSizePx.toFloat() / awtImage.height
            )
            largeImages[picture] =
                if (targetScale < 1.0) {
                    scaleBitmapAspectRatio(
                        awtImage,
                        width = (awtImage.width * targetScale).toInt(),
                        height = (awtImage.height * targetScale).toInt(),
                    ).toComposeImageBitmap()
                } else {
                    image.imageBitmap
                }

            val targetThumbnailScale = maxOf(
                storableThumbnailSizePx.toFloat() / awtImage.width,
                storableThumbnailSizePx.toFloat() / awtImage.height
            )
            thumbnails[picture] = scaleBitmapAspectRatio(
                awtImage,
                width = (awtImage.width * targetThumbnailScale).toInt(),
                height = (awtImage.height * targetThumbnailScale).toInt(),
            ).toComposeImageBitmap()

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
