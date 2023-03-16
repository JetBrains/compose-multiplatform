package example.imageviewer.model

import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.Dependencies
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.builtins.ListSerializer
import kotlin.jvm.JvmInline


//@JvmInline
//value class GalleryId(val l: Long)
//data class GalleryEntryWithMetadata(
//    val id: GalleryId,
//    val picture: Picture,
//    val thumbnail: ImageBitmap,
//)

class PhotoGallery(val deps: Dependencies) {
//    private val _galleryStateFlow = MutableStateFlow<List<GalleryEntryWithMetadata>>(listOf())
    val galleryStateFlow: List<Picture> = resourcePictures

//    init {
//        updatePictures()
//    }
//
//    fun updatePictures() {
//        deps.ioScope.launch {
//            try {
//                val pics = getNewPictures(deps)
//                _galleryStateFlow.emit(pics)
//            } catch (e: CancellationException) {
//                println("Rethrowing CancellationException with original cause")
//                // https://kotlinlang.org/docs/exception-handling.html#exceptions-aggregation
//                throw e
//            } catch (e: Exception) {
//                e.printStackTrace()
//                deps.notification.notifyNoInternet()
//            }
//        }
//    }

    private suspend fun getNewPictures(dependencies: Dependencies): List<Picture> {
        return resourcePictures
//        val pictures = dependencies.json.decodeFromString(
//            ListSerializer(Picture.serializer()),
//            dependencies.httpClient.get(PICTURES_DATA_URL).bodyAsText()
//        )
//        val miniatures = pictures
//            .map { picture ->
//                dependencies.ioScope.async {
//                    picture to dependencies.imageRepository.loadContent(picture.smallUrl)
//                }
//            }
//            .awaitAll()
//            .mapIndexed { index, pictureAndBitmap ->
//                val (pic, bit) = pictureAndBitmap
//                GalleryEntryWithMetadata(GalleryId(index.toLong()), pic, bit)
//            }
//        return miniatures
    }
}
