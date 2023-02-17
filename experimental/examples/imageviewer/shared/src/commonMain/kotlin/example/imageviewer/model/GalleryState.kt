package example.imageviewer.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

class GalleryScreenState {
    var currentPictureIndex by mutableStateOf(0)
    val picturesWithThumbnail = mutableStateListOf<PictureWithThumbnail>()
    var screen by mutableStateOf<ScreenState>(ScreenState.Miniatures)
    val isContentReady get() = picturesWithThumbnail.isNotEmpty()

    val picture get(): Picture? = picturesWithThumbnail.getOrNull(currentPictureIndex)?.picture

    fun nextImage() {
        currentPictureIndex = (currentPictureIndex + 1).mod(picturesWithThumbnail.lastIndex)
    }

    fun previousImage() {
        currentPictureIndex = (currentPictureIndex - 1).mod(picturesWithThumbnail.lastIndex)
    }

    fun selectPicture(picture: Picture) {
        currentPictureIndex = picturesWithThumbnail.indexOfFirst { it.picture == picture }
    }

    fun toFullscreen(idx: Int = currentPictureIndex) {
        currentPictureIndex = idx
        screen = ScreenState.FullScreen
    }

    fun refresh(dependencies: Dependencies) {
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

                picturesWithThumbnail.clear()
                picturesWithThumbnail.addAll(miniatures)
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
}

sealed interface ScreenState {
    object Miniatures : ScreenState
    object FullScreen : ScreenState
}