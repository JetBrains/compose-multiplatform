package example.imageviewer.model

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.Dependencies
import example.imageviewer.ExternalImageViewerEvent
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

data class PictureWithThumbnail(val picture: Picture, val thumbnail: ImageBitmap)

class GalleryState {
    val picturesWithThumbnail = mutableStateListOf<PictureWithThumbnail>()

    val isContentReady get() = picturesWithThumbnail.isNotEmpty()

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