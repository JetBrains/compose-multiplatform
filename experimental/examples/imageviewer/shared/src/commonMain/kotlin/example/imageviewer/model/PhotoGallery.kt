package example.imageviewer.model

import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.Dependencies
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline


@JvmInline
value class GalleryId(val l: Long)
data class GalleryEntryWithMetadata(
    val id: GalleryId,
    val picture: Picture,
    val thumbnail: ImageBitmap,
)

class PhotoGallery(val deps: Dependencies) {
    private val _galleryStateFlow = MutableStateFlow<List<GalleryEntryWithMetadata>>(listOf())
    val galleryStateFlow: StateFlow<List<GalleryEntryWithMetadata>> = _galleryStateFlow

    init {
        updatePictures()
    }

    fun updatePictures() {
        deps.ioScope.launch {
            try {
                val pics = getNewPictures(deps)
                _galleryStateFlow.emit(pics)
            } catch (e: CancellationException) {
                println("Rethrowing CancellationException with original cause")
                // https://kotlinlang.org/docs/exception-handling.html#exceptions-aggregation
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                deps.notification.notifyNoInternet()
            }
        }
    }

    private val mockedResponse = """
        [
          {"big":  "1.jpg", "small": "small/1.jpg"},
          {"big":  "2.jpg", "small": "small/2.jpg"},
          {"big":  "3.jpg", "small": "small/3.jpg"},
          {"big":  "4.jpg", "small": "small/4.jpg"},
          {"big":  "5.jpg", "small": "small/5.jpg"},
          {"big":  "6.jpg", "small": "small/6.jpg"},
          {"big":  "7.jpg", "small": "small/7.jpg"},
          {"big":  "8.jpg", "small": "small/8.jpg"},
          {"big":  "9.jpg", "small": "small/9.jpg"},
          {"big":  "10.jpg", "small": "small/10.jpg"},
          {"big":  "11.jpg", "small": "small/11.jpg"},
          {"big":  "12.jpg", "small": "small/12.jpg"},
          {"big":  "13.jpg", "small": "small/13.jpg"}
        ]
    """.trimIndent()

    private suspend fun getNewPictures(dependencies: Dependencies): List<GalleryEntryWithMetadata> {
//        val pictures = dependencies.json.decodeFromString(
//            ListSerializer(Picture.serializer()),
//            dependencies.httpClient.get(PICTURES_DATA_URL).bodyAsText()
//        )
        val pictures = (1..13).map {
            Picture("$it.jpg", "small/$it.jpg")
        }

        val miniatures = pictures
            .map { picture ->
                dependencies.ioScope.async {
                    picture to dependencies.imageRepository.loadContent(picture.smallUrl)
                }
            }
            .awaitAll()
            .mapIndexed { index, pictureAndBitmap ->
                val (pic, bit) = pictureAndBitmap
                GalleryEntryWithMetadata(GalleryId(index.toLong()), pic, bit)
            }
        return miniatures
    }
}