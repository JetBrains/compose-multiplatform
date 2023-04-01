package example.imageviewer.storage

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData
import example.imageviewer.toImageBitmap
import kotlinx.cinterop.CValue
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.memcpy

private const val maxStorableImageSizePx = 1200
private const val storableThumbnailSizePx = 180
private const val jpegCompressionQuality = 60

class IosImageStorage(
    private val pictures: SnapshotStateList<PictureData>,
    private val ioScope: CoroutineScope
) : ImageStorage {
    private val savePictureDir = File(NSFileManager.defaultManager.DocumentDirectory, "ImageViewer/takenPhotos/")

    private val PictureData.Camera.jpgFile get() = File(savePictureDir, "$id.jpg")
    private val PictureData.Camera.thumbnailJpgFile get() = File(savePictureDir, "$id-thumbnail.jpg")
    private val PictureData.Camera.jsonFile get() = File(savePictureDir, "$id.json")

    init {
        if (savePictureDir.isDirectory) {
            val files = savePictureDir.listFiles { _, name: String ->
                name.endsWith(".json")
            } ?: emptyArray()
            pictures.addAll(
                index = 0,
                elements = files
                    .map {
                        it.readText().toCameraMetadata()
                    }.sortedByDescending {
                        it.timeStampSeconds
                    }
            )
        } else {
            savePictureDir.mkdirs()
        }
    }

    override fun saveImage(pictureData: PictureData.Camera, image: PlatformStorableImage) {
        ioScope.launch {
            with(image.rawValue) {
                pictureData.jpgFile.writeJpeg(fitInto(maxStorableImageSizePx))
                pictureData.thumbnailJpgFile.writeJpeg(fitInto(storableThumbnailSizePx))
            }
            pictures.add(0, pictureData)
            pictureData.jsonFile.writeText(pictureData.toJson())
        }
    }

    override fun delete(picture: PictureData.Camera) {
        ioScope.launch {
            picture.jsonFile.delete()
            picture.jpgFile.delete()
            picture.thumbnailJpgFile.delete()
        }
    }

    override suspend fun getThumbnail(pictureData: PictureData.Camera): ImageBitmap =
        withContext(ioScope.coroutineContext) {
            pictureData.thumbnailJpgFile.readBytes().toImageBitmap()
        }

    override suspend fun getImage(pictureData: PictureData.Camera): ImageBitmap =
        withContext(ioScope.coroutineContext) {
            pictureData.jpgFile.readBytes().toImageBitmap()
        }
}

private fun UIImage.fitInto(px: Int): UIImage {
    val targetScale = maxOf(
        px.toFloat() / size.useContents { width },
        px.toFloat() / size.useContents { height },
    )
    val newSize = size.useContents { CGSizeMake(width * targetScale, height * targetScale) }
    return resize(newSize)
}

private fun UIImage.resize(targetSize: CValue<CGSize>): UIImage {
    val currentSize = this.size
    val widthRatio = targetSize.useContents { width } / currentSize.useContents { width }
    val heightRatio = targetSize.useContents { height } / currentSize.useContents { height }

    val newSize: CValue<CGSize> = if (widthRatio > heightRatio) {
        CGSizeMake(
            width = currentSize.useContents { width } * heightRatio,
            height = currentSize.useContents { height } * heightRatio
        )
    } else {
        CGSizeMake(
            width = currentSize.useContents { width } * widthRatio,
            height = currentSize.useContents { height } * widthRatio
        )
    }
    val newRect = CGRectMake(
        x = 0.0,
        y = 0.0,
        width = newSize.useContents { width },
        height = newSize.useContents { height }
    )
    UIGraphicsBeginImageContextWithOptions(size = newSize, opaque = false, scale = 1.0)
    this.drawInRect(newRect)
    val newImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return newImage!!
}

private fun PictureData.Camera.toJson(): String =
    Json.Default.encodeToString(this)

private fun String.toCameraMetadata(): PictureData.Camera =
    Json.Default.decodeFromString(this)

private fun NSURL.writeJpeg(image: UIImage, compressionQuality: Int = jpegCompressionQuality) {
    UIImageJPEGRepresentation(image, compressionQuality / 100.0)
        ?.writeToURL(this, true)
}
