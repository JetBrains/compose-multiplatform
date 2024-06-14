package example.imageviewer.storage

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData
import example.imageviewer.toImageBitmap
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.writeToURL
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

private const val maxStorableImageSizePx = 1200
private const val storableThumbnailSizePx = 180
private const val jpegCompressionQuality = 60

class IosImageStorage(
    pictures: SnapshotStateList<PictureData>,
    private val ioScope: CoroutineScope
) : ImageStorage {

    private val savePictureDir =
        File(NSFileManager.defaultManager.DocumentDirectory, "ImageViewer/takenPhotos/")

    private val PictureData.Camera.jpgFile
        get() = File(savePictureDir, "$id.jpg")

    private val PictureData.Camera.thumbnailJpgFile
        get() = File(savePictureDir, "$id-thumbnail.jpg")

    private val PictureData.Camera.jsonFile
        get() = File(savePictureDir, "$id.json")

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

    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        ioScope.launch {
            with(image.rawValue) {
                picture.jpgFile.writeJpeg(fitInto(maxStorableImageSizePx))
                picture.thumbnailJpgFile.writeJpeg(fitInto(storableThumbnailSizePx))
            }
            picture.jsonFile.writeText(picture.toJson())
        }
    }

    override fun delete(picture: PictureData.Camera) {
        ioScope.launch {
            picture.jsonFile.delete()
            picture.jpgFile.delete()
            picture.thumbnailJpgFile.delete()
        }
    }

    override fun rewrite(picture: PictureData.Camera) {
        ioScope.launch {
            picture.jsonFile.delete()
            picture.jsonFile.writeText(picture.toJson())
        }
    }

    override suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap =
        withContext(ioScope.coroutineContext) {
            picture.thumbnailJpgFile.readBytes().toImageBitmap()
        }

    override suspend fun getImage(picture: PictureData.Camera): ImageBitmap =
        withContext(ioScope.coroutineContext) {
            picture.jpgFile.readBytes().toImageBitmap()
        }

    suspend fun getNSDataToShare(picture: PictureData): NSData = withContext(Dispatchers.IO) {
        when (picture) {
            is PictureData.Camera -> {
                picture.jpgFile
            }

            is PictureData.Resource -> {
                NSURL(
                    fileURLWithPath = NSBundle.mainBundle.resourcePath + "/" + picture.resource,
                    isDirectory = false
                )
            }
        }.readData()
    }

    suspend fun getNSURLToShare(picture: PictureData): NSURL = withContext(Dispatchers.IO) {
        when (picture) {
            is PictureData.Camera -> {
                picture.jpgFile
            }

            is PictureData.Resource -> {
                NSURL(
                    fileURLWithPath = NSBundle.mainBundle.resourcePath + "/" + picture.resource,
                    isDirectory = false
                )
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.fitInto(px: Int): UIImage {
    val targetScale = maxOf(
        px.toFloat() / size.useContents { width },
        px.toFloat() / size.useContents { height },
    )
    val newSize = size.useContents { CGSizeMake(width * targetScale, height * targetScale) }
    return resize(newSize)
}

@OptIn(ExperimentalForeignApi::class)
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
