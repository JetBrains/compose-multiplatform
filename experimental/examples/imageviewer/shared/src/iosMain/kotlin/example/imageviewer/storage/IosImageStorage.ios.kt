package example.imageviewer.storage

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData
import kotlinx.cinterop.CValue
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
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

private const val maxStorableImageSize = 30
private const val storableThumbnailSize = 30

class IosImageStorage(
    private val pictures: SnapshotStateList<PictureData>,
    private val ioScope: CoroutineScope
) : ImageStorage {

    private val fileManager = NSFileManager.defaultManager
    private val savePictureDir = fileManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        create = true,
        appropriateForURL = null,
        error = null
    )!!.URLByAppendingPathComponent("ImageViewer/takenPhotos/")!!

    init {
        val directoryContent = fileManager.contentsOfDirectoryAtPath(savePictureDir.path!!, null)
        if (directoryContent != null) {
            pictures.addAll(
                index = 0,
                elements = directoryContent.map { it.toString() }
                    .filter { it.endsWith(".json") }
                    .map {
                        val jsonStr = readStringFromFile(it)
                        Json.Default.decodeFromString<PictureData.Camera>(jsonStr)
                    }.sortedByDescending {
                        it.timeStampSeconds
                    }
            )
        } else {
            fileManager.createDirectoryAtURL(savePictureDir, true, null, null)
        }
    }

    private fun makeFileUrl(fileName: String) =
        savePictureDir.URLByAppendingPathComponent(fileName)!!

    private fun readStringFromFile(fileName: String): String =
        NSString.stringWithContentsOfURL(
            url = makeFileUrl(fileName),
            encoding = NSUTF8StringEncoding,
            error = null,
        ) as String

    private fun String.writeToFile(fileName: String) =
        writeToURL(makeFileUrl(fileName))

    private fun readPngFromFile(fileName: String) =
        NSData.dataWithContentsOfURL(makeFileUrl(fileName))

    private fun NSData.writeToFile(fileName: String) =
        writeToURL(makeFileUrl(fileName), true)

    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        ioScope.launch {
            UIImageJPEGRepresentation(image.rawValue.resizeToThumbnail(), 0.6)
                ?.writeToFile(picture.thumbnailPngFile)
            pictures.add(0, picture)
            UIImageJPEGRepresentation(image.rawValue.resizeToBig(), 0.6)
                ?.writeToFile(picture.pngFile)
            val jsonStr = Json.Default.encodeToString(picture)
            jsonStr.writeToFile(picture.jsonFile)
        }
    }

    override suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap =
        ioScope.async {
            val jpgRepresentation = readPngFromFile(picture.thumbnailPngFile)!!
            val byteArray: ByteArray = ByteArray(jpgRepresentation.length.toInt()).apply {
                usePinned {
                    memcpy(it.addressOf(0), jpgRepresentation.bytes, jpgRepresentation.length)
                }
            }
            Image.makeFromEncoded(byteArray).toComposeImageBitmap()
        }.await()

    override suspend fun getImage(picture: PictureData.Camera): ImageBitmap =
        ioScope.async {
            fun getFileContent() = readPngFromFile(picture.pngFile)
            var jpgRepresentation: NSData? = getFileContent()
            while (jpgRepresentation == null) {
                yield()
                jpgRepresentation = getFileContent()
            }
            val byteArray: ByteArray = ByteArray(jpgRepresentation.length.toInt()).apply {
                usePinned {
                    memcpy(it.addressOf(0), jpgRepresentation.bytes, jpgRepresentation.length)
                }
            }
            Image.makeFromEncoded(byteArray).toComposeImageBitmap()
        }.await()

}

private fun UIImage.resizeToThumbnail(): UIImage {
    val targetScale = maxOf(
        storableThumbnailSize.toFloat() / size.useContents { width },
        storableThumbnailSize.toFloat() / size.useContents { height },
    )
    val newSize = size.useContents { CGSizeMake(width * targetScale, height * targetScale) }
    return resize(newSize)
}

private fun UIImage.resizeToBig(): UIImage {
    val targetScale = maxOf(
        maxStorableImageSize.toFloat() / size.useContents { width },
        maxStorableImageSize.toFloat() / size.useContents { height },
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

private val PictureData.Camera.pngFile get():String = id + ".jpg"
private val PictureData.Camera.thumbnailPngFile get():String = id + "-thumbnail.jpg"
private val PictureData.Camera.jsonFile get():String = id + ".json"
private fun String.writeToURL(url: NSURL) = (this as NSString).writeToURL(
    url = url,
    atomically = true,
    encoding = NSUTF8StringEncoding,
    error = null
)
