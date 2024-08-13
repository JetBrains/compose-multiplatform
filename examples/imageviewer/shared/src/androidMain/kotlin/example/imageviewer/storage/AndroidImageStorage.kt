package example.imageviewer.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import example.imageviewer.ImageStorage
import example.imageviewer.PlatformStorableImage
import example.imageviewer.model.PictureData
import example.imageviewer.toImageBitmap
import imageviewer.shared.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val savePictureDir = File(context.filesDir, "taken_photos")
    private val sharedImagesDir = File(context.filesDir, "share_images")

    private val PictureData.Camera.jpgFile get() = File(savePictureDir, "$id.jpg")
    private val PictureData.Camera.thumbnailJpgFile
        get() = File(
            savePictureDir,
            "$id-thumbnail.jpg"
        )
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

    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
        if (image.imageBitmap.width == 0 || image.imageBitmap.height == 0) {
            return
        }
        ioScope.launch {
            with(image.imageBitmap) {
                picture.jpgFile.writeJpeg(fitInto(maxStorableImageSizePx))
                picture.thumbnailJpgFile.writeJpeg(fitInto(storableThumbnailSizePx))

            }
            pictures.add(0, picture)
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

    suspend fun getUri(context: Context, picture: PictureData): Uri = withContext(Dispatchers.IO) {
        if (!sharedImagesDir.exists()) {
            sharedImagesDir.mkdirs()
        }
        val tempFileToShare: File = sharedImagesDir.resolve("share_picture.jpg")
        when (picture) {
            is PictureData.Camera -> {
                picture.jpgFile.copyTo(tempFileToShare, overwrite = true)
            }

            is PictureData.Resource -> {
                if (!tempFileToShare.exists()) {
                    tempFileToShare.createNewFile()
                }
                tempFileToShare.writeBytes(Res.readBytes(picture.resource))
            }
        }
        FileProvider.getUriForFile(
            context,
            "example.imageviewer.fileprovider",
            tempFileToShare
        )
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
