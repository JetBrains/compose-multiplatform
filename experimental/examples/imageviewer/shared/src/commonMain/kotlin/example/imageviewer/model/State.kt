package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.Dependencies
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class Picture(val big: String, val small: String)

val Picture.bigUrl get() = "$BASE_URL/$big"
val Picture.smallUrl get() = "$BASE_URL/$small"

//todo dima-avdeev/add-uikit-to-imageviewer to master
val BASE_URL =
    "https://raw.githubusercontent.com/JetBrains/compose-jb/dima-avdeev/add-uikit-to-imageviewer/artwork/imageviewerrepo"

val PICTURES_DATA_URL = "$BASE_URL/pictures.json"
val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
val jsonReader = Json {
    ignoreUnknownKeys = true
}

sealed interface ScreenState {
    object Miniatures : ScreenState
    object FullScreen : ScreenState
}

data class State(
    val mainImage: ImageBitmap? = null,
    val currentImageIndex: Int = 0,
    val miniatures: Map<Picture, ImageBitmap> = emptyMap(),
    val pictures: List<Picture> = emptyList(),
    val screen: ScreenState = ScreenState.Miniatures
)

fun MutableState<State>.modifyState(modification: State.() -> State) {
    value = value.modification()
}

fun MutableState<State>.nextImage() = modifyState {
    var newIndex = currentImageIndex + 1
    if (newIndex > pictures.lastIndex) {
        newIndex = 0
    }
    copy(currentImageIndex = newIndex)
}

fun MutableState<State>.previousImage() = modifyState {
    var newIndex = currentImageIndex - 1
    if (newIndex < 0) {
        newIndex = pictures.lastIndex
    }
    copy(currentImageIndex = newIndex)
}

fun MutableState<State>.refresh(dependencies: Dependencies) {
    backgroundScope.launch {
        try {
            val pictures = jsonReader.decodeFromString(
                ListSerializer(Picture.serializer()),
                dependencies.httpClient.get(PICTURES_DATA_URL).bodyAsText()
            )
            val miniatures = pictures.map { picture ->
                async {
                    picture to dependencies.imageRepository.loadContent(picture.smallUrl)
                }
            }.awaitAll().toMap()

            modifyState {
                copy(pictures = pictures, miniatures = miniatures)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dependencies.notification.notifyNoInternet()
        }
    }
}

fun MutableState<State>.setMainImage(picture: Picture, dependencies: Dependencies) {
    backgroundScope.launch {
        val mainImage = dependencies.imageRepository.loadContent(picture.bigUrl)
        modifyState {
            copy(mainImage = mainImage)
        }
    }
}

val State.isContentReady get() = pictures.isNotEmpty()
val State.picture get():Picture? = pictures.getOrNull(currentImageIndex)

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
