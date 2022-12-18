package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.Dependencies
import example.imageviewer.utils.isInternetAvailable
import example.imageviewer.utils.ktorHttpClient
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class Picture(val big: String, val small: String)

val Picture.bigUrl get() = "$BASE_URL/$big"
val Picture.smallUrl get() = "$BASE_URL/$small"

val BASE_URL =
    "https://raw.githubusercontent.com/JetBrains/compose-jb/dima-avdeev/add-uikit-to-imageviewer/artwork/imageviewerrepo"

//todo dima-avdeev/add-uikit-to-imageviewer to master
val PICTURES_DATA_URL = "$BASE_URL/pictures.json"
val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
val jsonReader = Json {
    ignoreUnknownKeys = true
}

sealed interface ScreenState {
    object Miniatures : ScreenState
    object FullScreen : ScreenState
}

data class ContentStateData(
    val mainImage: ImageBitmap? = null,
    val currentImageIndex: Int = 0,
    val miniatures: Map<Picture, ImageBitmap> = emptyMap(),
    val pictures: List<Picture> = emptyList(),
    val screen: ScreenState = ScreenState.Miniatures
)

fun MutableState<ContentStateData>.modifyState(modification: ContentStateData.() -> ContentStateData) {
    value = value.modification()
}

fun MutableState<ContentStateData>.nextImage() = modifyState {
    var newIndex = currentImageIndex + 1
    if (newIndex > pictures.lastIndex) {
        newIndex = 0
    }
    copy(currentImageIndex = newIndex)
}

fun MutableState<ContentStateData>.previousImage() = modifyState {
    var newIndex = currentImageIndex - 1
    if (newIndex < 0) {
        newIndex = pictures.lastIndex
    }
    copy(currentImageIndex = newIndex)
}

val ContentStateData.isContentReady get() = pictures.isNotEmpty()
val ContentStateData.picture get():Picture? = pictures.getOrNull(currentImageIndex)

interface Notification {
    fun notifyInvalidRepo()
    fun notifyRepoIsEmpty()
    fun notifyNoInternet()
    fun notifyLoadImageUnavailable()
    fun notifyLastImage()
    fun notifyFirstImage()
    fun notifyRefreshUnavailable()
    fun notifyImageData(picture: Picture)
}

interface Localization {
    val back: String
    val appName: String
    val loading: String
}

class ContentState(
    val state: MutableState<ContentStateData>,
    val dependencies: Dependencies
) {
    fun modifyState(changeState: ContentStateData.() -> ContentStateData) {
        state.value = state.value.changeState()
    }

    fun initData() {
        backgroundScope.launch {
            try {
                val pictures = jsonReader.decodeFromString(
                    ListSerializer(Picture.serializer()),
                    ktorHttpClient.get(PICTURES_DATA_URL)
                )
                modifyState {
                    copy(pictures = pictures)
                }

                pictures.forEach { picture ->
                    launch {
                        val pair = picture to dependencies.imageRepository.loadContent(NetworkRequest(picture.smallUrl))
                        modifyState {
                            copy(miniatures = miniatures + pair)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dependencies.notification.notifyNoInternet()
                }
            }
        }
    }

    fun setMainImage(picture: Picture) {
        backgroundScope.launch {
            if (isInternetAvailable()) {
                val mainImage = dependencies.imageRepository.loadContent(NetworkRequest(picture.bigUrl))
                modifyState {
                    copy(
                        mainImage = mainImage
                    )
                }
            } else {
                withContext(Dispatchers.Main) {
                    dependencies.notification.notifyLoadImageUnavailable()
                }
            }
        }
    }

    fun updateMainImage() {
        val picture = state.value.picture
        if (picture != null) {
            setMainImage(picture)
        }
    }

    fun refresh() {
        backgroundScope.launch {
            if (isInternetAvailable()) {
                withContext(Dispatchers.Main) {
                    state.value = ContentStateData()
                    initData()
                }
            } else {
                withContext(Dispatchers.Main) {
                    dependencies.notification.notifyRefreshUnavailable()
                }
            }
        }
    }

    fun isContentReady(): Boolean = state.value.isContentReady

    private fun getName(): String = state.value.picture?.name ?: "no picture"

}
