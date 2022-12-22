package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.Dependencies
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

data class State(
    val currentImageIndex: Int = 0,
    val miniatures: Map<Picture, ImageBitmap> = emptyMap(),
    val pictures: List<Picture> = emptyList(),
    val screen: ScreenState = ScreenState.Miniatures
)

sealed interface ScreenState {
    object Miniatures : ScreenState
    object FullScreen : ScreenState
}

val State.isContentReady get() = pictures.isNotEmpty()
val State.picture get():Picture? = pictures.getOrNull(currentImageIndex)

fun <T> MutableState<T>.modifyState(modification: T.() -> T) {
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
    dependencies.ioScope.launch {
        try {
            val pictures = dependencies.json.decodeFromString(
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
        } catch (e: CancellationException) {
            println("Rethrowing CancellationException with original cause")
            // https://kotlinlang.org/docs/exception-handling.html#exceptions-aggregation
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            dependencies.notification.notifyNoInternet()
        }
    }
}

fun MutableState<State>.setSelectedIndex(index: Int) = modifyState {
    copy(currentImageIndex = index)
}

fun MutableState<State>.toFullscreen(index: Int = value.currentImageIndex) = modifyState {
    copy(
        currentImageIndex = index,
        screen = ScreenState.FullScreen
    )
}
