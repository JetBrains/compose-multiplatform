package example.imageviewer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

@OptIn(ExperimentalResourceApi::class)
abstract class Dependencies {
    abstract val httpClient: HttpClient
    abstract val ioScope: CoroutineScope
    abstract fun getFilter(type: FilterType): BitmapFilter
    abstract val localization: Localization
    abstract val notification: Notification
    abstract val imageStorage: ImageStorage
    val pictures: SnapshotStateList<PictureData> = mutableStateListOf(*resourcePictures)
    val imageProvider: ImageProvider = object : ImageProvider {
        override suspend fun getImage(picture: PictureData): ImageBitmap = when (picture) {
            is PictureData.Resource -> resource(picture.resource).readBytes().toImageBitmap()
            is PictureData.Camera -> imageStorage.getImage(picture)
        }

        override suspend fun getThumbnail(picture: PictureData): ImageBitmap = when (picture) {
            is PictureData.Resource -> resource(picture.resource).readBytes().toImageBitmap()//todo small
            is PictureData.Camera -> imageStorage.getThumbnail(picture)
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
}

interface ImageProvider {
    suspend fun getImage(picture: PictureData): ImageBitmap
    suspend fun getThumbnail(picture: PictureData): ImageBitmap
}

interface ImageStorage {
    suspend fun getImage(picture: PictureData.Camera): ImageBitmap
    suspend fun getThumbnail(picture: PictureData.Camera): ImageBitmap
    fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage)
}
