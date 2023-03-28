package example.imageviewer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import kotlinx.coroutines.CoroutineScope
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
    val localization: Localization = when (getCurrentLanguage()) {
        AvailableLanguages.EN -> EnglishLocalization
        AvailableLanguages.DE -> DeutschhLocalization
    }
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
    fun notifyInvalidRepo()
    fun notifyRepoIsEmpty()
    fun notifyNoInternet()
    fun notifyLoadImageUnavailable()
    fun notifyLastImage()
    fun notifyFirstImage()
    fun notifyImageData(picture: PictureData)
    fun notifyRefreshUnavailable()
}

abstract class PopupNotification(private val localization: Localization) : Notification {
    abstract fun showPopUpMessage(text: String)

    override fun notifyInvalidRepo() = showPopUpMessage(localization.repoInvalid)
    override fun notifyRepoIsEmpty() = showPopUpMessage(localization.repoEmpty)
    override fun notifyNoInternet() = showPopUpMessage(localization.noInternet)
    override fun notifyLoadImageUnavailable() =
        showPopUpMessage(
            """
                ${localization.noInternet}
                ${localization.loadImageUnavailable}
            """.trimIndent()
        )

    override fun notifyLastImage() = showPopUpMessage(localization.lastImage)
    override fun notifyFirstImage() = showPopUpMessage(localization.firstImage)
    override fun notifyImageData(picture: PictureData) = showPopUpMessage(
        "${localization.picture} ${picture.name}"
    )

    override fun notifyRefreshUnavailable() = showPopUpMessage(
        """
            ${localization.noInternet}
            ${localization.refreshUnavailable}
        """.trimIndent()
    )
}

interface Localization {
    val back: String
    val appName: String
    val loading: String
    val repoInvalid: String
    val repoEmpty: String
    val noInternet: String
    val loadImageUnavailable: String
    val lastImage: String
    val firstImage: String
    val picture: String
    val size: String
    val pixels: String
    val refreshUnavailable: String
    val takePhoto: String
    val addPhoto: String
}

interface ImageProvider {
    suspend fun getImage(picture: PictureData): ImageBitmap
    suspend fun getThumbnail(picture: PictureData): ImageBitmap
}

interface ImageStorage {
    fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage)
    suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap
    suspend fun getImage(picture: PictureData.Camera): ImageBitmap
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
