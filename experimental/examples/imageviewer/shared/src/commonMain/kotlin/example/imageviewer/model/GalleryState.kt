package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.Dependencies
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

data class PictureWithThumbnail(val picture: Picture, val thumbnail: ImageBitmap)

data class GalleryState(
    val currentPictureIndex: Int = 0,
    val pictures: List<PictureWithThumbnail> = emptyList(),
    val screen: ScreenState = ScreenState.Miniatures
)

sealed interface ScreenState {
    object Miniatures : ScreenState
    object FullScreen : ScreenState
}

val GalleryState.isContentReady get() = pictures.isNotEmpty()
val GalleryState.picture get(): Picture? = pictures.getOrNull(currentPictureIndex)?.picture

fun <T> MutableState<T>.modifyState(modification: T.() -> T) {
    value = value.modification()
}

fun MutableState<GalleryState>.nextImage() = modifyState {
    var newIndex = currentPictureIndex + 1
    if (newIndex > pictures.lastIndex) {
        newIndex = 0
    }
    copy(currentPictureIndex = newIndex)
}

fun MutableState<GalleryState>.previousImage() = modifyState {
    var newIndex = currentPictureIndex - 1
    if (newIndex < 0) {
        newIndex = pictures.lastIndex
    }
    copy(currentPictureIndex = newIndex)
}

fun MutableState<GalleryState>.refresh(dependencies: Dependencies) {
    dependencies.ioScope.launch {
        try {
            val pictures = dependencies.json.decodeFromString(
                ListSerializer(Picture.serializer()),
                dependencies.httpClient.get(PICTURES_DATA_URL).bodyAsText()
            )
            val miniatures = pictures
                .map { picture ->
                    async {
                        picture to dependencies.imageRepository.loadContent(picture.smallUrl)
                    }
                }
                .awaitAll()
                .map { (pic, bit) -> PictureWithThumbnail(pic, bit) }

            modifyState {
                // TODO: Unless I'm crazy, this might happen from a background thread.
                // Investigate.
                copy(pictures = miniatures)
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

fun MutableState<GalleryState>.setSelectedIndex(index: Int) = modifyState {
    copy(currentPictureIndex = index)
}

fun MutableState<GalleryState>.setSelectedPicture(picture: Picture) = modifyState {
    copy(currentPictureIndex = pictures.indexOfFirst { it.picture == picture })
}

fun MutableState<GalleryState>.toFullscreen(index: Int = value.currentPictureIndex) = modifyState {
    copy(
        currentPictureIndex = index,
        screen = ScreenState.FullScreen
    )
}
