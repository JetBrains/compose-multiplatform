package example.imageviewer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
abstract class Dependencies {
    abstract val notification: Notification
    abstract val imageStorage: ImageStorage
    val pictures: SnapshotStateList<PictureData> = mutableStateListOf(*resourcePictures)
    open val externalEvents: Flow<ExternalImageViewerEvent> = emptyFlow()
    val localization: Localization = getCurrentLocalization()
    val imageProvider: ImageProvider = object : ImageProvider {
        override suspend fun getImage(picture: PictureData): ImageBitmap = when (picture) {
            is PictureData.Resource -> {
                resource(picture.resource).readBytes().toImageBitmap()
            }

            is PictureData.Camera -> {
                imageStorage.getImage(picture)
            }
        }

        override suspend fun getThumbnail(picture: PictureData): ImageBitmap = when (picture) {
            is PictureData.Resource -> {
                resource(picture.thumbnailResource).readBytes().toImageBitmap()
            }

            is PictureData.Camera -> {
                imageStorage.getThumbnail(picture)
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
}

interface ImageStorage {
    fun saveImage(pictureData: PictureData.Camera, image: PlatformStorableImage)
    suspend fun getThumbnail(pictureData: PictureData.Camera): ImageBitmap
    suspend fun getImage(pictureData: PictureData.Camera): ImageBitmap
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

internal val LocalImageStorage = staticCompositionLocalOf<ImageStorage> {
    noLocalProvidedFor("LocalImageStorage")
}

internal val LocalInternalEvents = staticCompositionLocalOf<Flow<ExternalImageViewerEvent>> {
    noLocalProvidedFor("LocalInternalEvents")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
