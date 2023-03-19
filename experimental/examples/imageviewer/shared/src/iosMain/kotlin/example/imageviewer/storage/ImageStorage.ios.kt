package example.imageviewer.storage

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image
import platform.Foundation.*
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

class IosImageStorage(
    val pictures: SnapshotStateList<PictureData>,
    val ioScope: CoroutineScope
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
                directoryContent.map { it.toString() }
                    .filter { it.endsWith(".json") }
                    .map {
                        val jsonStr = NSString.stringWithContentsOfURL(
                            url = makeFileUrl(it),
                            encoding = NSUTF8StringEncoding,
                            error = null,
                        ) as String
                        Json.Default.decodeFromString<PictureData.Camera>(jsonStr)
                    }
            )
        } else {
            fileManager.createDirectoryAtURL(savePictureDir, true, null, null)
        }
    }

    fun makeFileUrl(fileName: String): NSURL = savePictureDir.URLByAppendingPathComponent(fileName)!!

    override suspend fun getImage(picture: PictureData.Camera): ImageBitmap =
        ioScope.async {
            fun getFileContent() = NSData.dataWithContentsOfURL(makeFileUrl(picture.pngFile))
            var pngRepresentation: NSData? = getFileContent()
            while (pngRepresentation == null) {
                yield()
                pngRepresentation = getFileContent()
            }
            val byteArray: ByteArray = ByteArray(pngRepresentation.length.toInt()).apply {
                usePinned {
                    memcpy(it.addressOf(0), pngRepresentation.bytes, pngRepresentation.length)
                }
            }
            Image.makeFromEncoded(byteArray).toComposeImageBitmap()
        }.await()

    override suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap =
        ioScope.async {
            val pngRepresentation = NSData.dataWithContentsOfURL(makeFileUrl(picture.thumbnailPngFile))!!
            val byteArray: ByteArray = ByteArray(pngRepresentation.length.toInt()).apply {
                usePinned {
                    memcpy(it.addressOf(0), pngRepresentation.bytes, pngRepresentation.length)
                }
            }
            Image.makeFromEncoded(byteArray).toComposeImageBitmap()
        }.await()

    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        ioScope.launch {
            val uiImage = UIImage(image.data)
            UIImagePNGRepresentation(uiImage.resizeToSmall())
                ?.writeToURL(makeFileUrl(picture.thumbnailPngFile), true)
            pictures.add(picture)

            delay(3000) // for hand testing
            UIImagePNGRepresentation(uiImage.resizeToBig())
                ?.writeToURL(makeFileUrl(picture.pngFile), true)

            Json.Default.encodeToString(picture)
                .writeToURL(makeFileUrl(picture.jsonFile))
        }
    }

}

private fun UIImage.resizeToSmall(): UIImage {
    //todo
    return this
}

private fun UIImage.resizeToBig(): UIImage {
    //todo
    return this
}

private val PictureData.Camera.pngFile get():String = id + ".png"
private val PictureData.Camera.thumbnailPngFile get():String = id + "-thumbnail.png"
private val PictureData.Camera.jsonFile get():String = id + ".json"
private fun String.writeToURL(url: NSURL) = (this as NSString).writeToURL(url, true)
