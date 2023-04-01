package example.imageviewer.storage

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.scale
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData
import example.imageviewer.toAndroidBitmap
import example.imageviewer.toImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val maxStorableImageSizePx = 2000
private const val storableThumbnailSizePx = 200
private const val jpegCompressionQuality = 60

class AndroidImageStorage(
    private val pictures: SnapshotStateList<PictureData>,
    private val ioScope: CoroutineScope,
    context: Context
) : ImageStorage {
    private val savePictureDir = File(context.filesDir, "takenPhotos")

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
                elements = files.map {
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
        if (image.imageBitmap.width == 0 || image.imageBitmap.height == 0) {
            return
        }
        ioScope.launch {
            with(image.imageBitmap) {
                pictureData.jpgFile.writeJpeg(fitInto(maxStorableImageSizePx))
                pictureData.thumbnailJpgFile.writeJpeg(fitInto(storableThumbnailSizePx))

            }
            pictures.add(0, pictureData)
            pictureData.jsonFile.writeText(pictureData.toJson())
        }
    }

    override fun delete(picture: PictureData.Camera) {
        picture.jsonFile.delete()
        picture.jpgFile.delete()
        picture.thumbnailJpgFile.delete()
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

private fun PictureData.Camera.toJson(): String =
    Json.Default.encodeToString(this)

private fun String.toCameraMetadata(): PictureData.Camera =
    Json.Default.decodeFromString(this)

private fun File.writeJpeg(image: ImageBitmap, compressionQuality: Int = jpegCompressionQuality) {
    outputStream().use {
        image.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, compressionQuality, it)
    }
}
