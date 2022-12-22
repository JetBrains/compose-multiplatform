package example.imageviewer

import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.ContentRepository
import example.imageviewer.model.Picture
import example.imageviewer.model.name
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

interface Dependencies {
    val httpClient: HttpClient
    val ioScope: CoroutineScope
    fun getFilter(type: FilterType): BitmapFilter
    val localization: Localization
    val imageRepository: ContentRepository<ImageBitmap>
    val notification: Notification
    val json: Json get() = jsonReader
}

interface Notification {
    fun notifyInvalidRepo()
    fun notifyRepoIsEmpty()
    fun notifyNoInternet()
    fun notifyLoadImageUnavailable()
    fun notifyLastImage()
    fun notifyFirstImage()
    fun notifyImageData(picture: Picture)
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
    override fun notifyImageData(picture: Picture) = showPopUpMessage(
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

private val jsonReader: Json = Json {
    ignoreUnknownKeys = true
}
