package example.imageviewer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.filter.PlatformContext
import example.imageviewer.model.PictureData
import imageviewer.shared.generated.resources.Res
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

abstract class Dependencies {
    abstract val notification: Notification
    abstract val imageStorage: ImageStorage
    abstract val sharePicture: SharePicture
    val pictures: SnapshotStateList<PictureData> = mutableStateListOf(*resourcePictures)
    open val externalEvents: Flow<ExternalImageViewerEvent> = emptyFlow()
    val localization: Localization = getCurrentLocalization()
    val imageProvider: ImageProvider = object : ImageProvider {
        override suspend fun getImage(picture: PictureData): ImageBitmap = when (picture) {
            is PictureData.Resource -> {
                Res.readBytes(picture.resource).toImageBitmap()
            }

            is PictureData.Camera -> {
                imageStorage.getImage(picture)
            }
        }

        override suspend fun getThumbnail(picture: PictureData): ImageBitmap = when (picture) {
            is PictureData.Resource -> {
                Res.readBytes(picture.thumbnailResource).toImageBitmap()
            }

            is PictureData.Camera -> {
                imageStorage.getThumbnail(picture)
            }
        }

        override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
            pictures.add(0, picture)
            imageStorage.saveImage(picture, image)
        }

        override fun delete(picture: PictureData) {
            pictures.remove(picture)
            if (picture is PictureData.Camera) {
                imageStorage.delete(picture)
            }
        }

        override fun edit(picture: PictureData, name: String, description: String): PictureData {
            when (picture) {
                is PictureData.Resource -> {
                    val edited = picture.copy(
                        name = name,
                        description = description,
                    )
                    pictures[pictures.indexOf(picture)] = edited
                    return edited
                }

                is PictureData.Camera -> {
                    val edited = picture.copy(
                        name = name,
                        description = description,
                    )
                    pictures[pictures.indexOf(picture)] = edited
                    imageStorage.rewrite(edited)
                    return edited
                }
            }
        }
    }
}

interface Notification {
    fun notifyImageData(picture: PictureData)
}

abstract class PopupNotification(private val localization: Localization) : Notification {
    abstract fun showPopUpMessage(text: String)
    override fun notifyImageData(picture: PictureData) = showPopUpMessage(
        "${localization.picture} ${picture.name}"
    )
}

interface Localization {
    val appName: String
    val back: String
    val picture: String
    val takePhoto: String
    val addPhoto: String
    val kotlinConfName: String
    val kotlinConfDescription: String
    val newPhotoName: String
    val newPhotoDescription: String
}

interface ImageProvider {
    suspend fun getImage(picture: PictureData): ImageBitmap
    suspend fun getThumbnail(picture: PictureData): ImageBitmap
    fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage)
    fun delete(picture: PictureData)
    fun edit(picture: PictureData, name: String, description: String): PictureData
}

interface ImageStorage {
    fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage)
    fun delete(picture: PictureData.Camera)
    fun rewrite(picture: PictureData.Camera)
    suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap
    suspend fun getImage(picture: PictureData.Camera): ImageBitmap
}

interface SharePicture {
    fun share(context: PlatformContext, picture: PictureData)
}

internal val LocalLocalization = staticCompositionLocalOf<Localization> {
    noLocalProvidedFor("LocalLocalization")
}

internal val LocalNotification = staticCompositionLocalOf<Notification> {
    noLocalProvidedFor("LocalNotification")
}

internal val LocalImageProvider = staticCompositionLocalOf<ImageProvider> {
    noLocalProvidedFor("LocalImageProvider")
}

internal val LocalInternalEvents = staticCompositionLocalOf<Flow<ExternalImageViewerEvent>> {
    noLocalProvidedFor("LocalInternalEvents")
}

internal val LocalSharePicture = staticCompositionLocalOf<SharePicture> {
    noLocalProvidedFor("LocalSharePicture")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
