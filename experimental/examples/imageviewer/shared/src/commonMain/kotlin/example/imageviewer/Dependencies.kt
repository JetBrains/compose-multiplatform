package example.imageviewer

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.model.*
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

//import kotlinx.serialization.json.Json

interface Dependencies {
    val pictures: SnapshotStateList<PictureData>
    val httpClient: WrappedHttpClient
    val ioScope: CoroutineScope
    fun getFilter(type: FilterType): BitmapFilter
    val localization: Localization
    val imageRepository: ContentRepository<ImageBitmap>
    val notification: Notification
//    val json: Json get() = jsonReader
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

//private val jsonReader: Json = Json {
//    ignoreUnknownKeys = true
//}

interface ImageProvider {
    suspend fun getImage(picture: PictureData): ImageBitmap
    suspend fun getThumbnail(picture: PictureData): ImageBitmap
    // fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage)
//    fun delete(picture: PictureData)
//    fun edit(picture: PictureData, name: String, description: String): PictureData
}

@OptIn(ExperimentalResourceApi::class)
val imageProvider: ImageProvider = object : ImageProvider {
    override suspend fun getImage(picture: PictureData): ImageBitmap = when (picture) {
        is PictureData.Resource -> {
            resource(picture.resource).readBytes().toImageBitmap()
        }

//        is PictureData.Camera -> {
//            imageStorage.getImage(picture)
//        }
    }

    override suspend fun getThumbnail(picture: PictureData): ImageBitmap = when (picture) {
        is PictureData.Resource -> {
            resource(picture.thumbnailResource).readBytes().toImageBitmap()
        }

//        is PictureData.Camera -> {
//            imageStorage.getThumbnail(picture)
//        }
    }

//    override fun saveImage(picture: PictureData.Camera, image: PlatformStorableImage) {
//        imageStorage.saveImage(picture, image)
//    }

//    override fun delete(picture: PictureData) {
//        pictures.remove(picture)
//        if (picture is PictureData.Camera) {
//            imageStorage.delete(picture)
//        }
//    }

//    override fun edit(picture: PictureData, name: String, description: String): PictureData {
//        when (picture) {
//            is PictureData.Resource -> {
//                val edited = picture.copy(
//                    name = name,
//                    description = description,
//                )
//                pictures[pictures.indexOf(picture)] = edited
//                return edited
//            }
//
//            is PictureData.Camera -> {
//                val edited = picture.copy(
//                    name = name,
//                    description = description,
//                )
//                pictures[pictures.indexOf(picture)] = edited
//                imageStorage.rewrite(edited)
//                return edited
//            }
//        }
//    }
}

internal val LocalImageProvider = staticCompositionLocalOf<ImageProvider> {
    imageProvider
//    noLocalProvidedFor("LocalImageProvider")
}

internal val LocalNotification = staticCompositionLocalOf<Notification> {
    noLocalProvidedFor("LocalNotification")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}