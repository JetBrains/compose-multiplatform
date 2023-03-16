package example.imageviewer.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import example.imageviewer.Dependencies
import example.imageviewer.toImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.resource

interface ImageStorage<P : Picture> {
    suspend fun getImage(picture: P): ImageBitmap

    @Composable
    fun getThumbnail(picture: P): Painter

    fun saveImage(picture: P, imageBitmap: ImageBitmap)
}

object ResourcesStorage : ImageStorage<ResourcePicture> {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getImage(picture: ResourcePicture): ImageBitmap {
        return resource(picture.resource).readBytes().toImageBitmap()
    }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun getThumbnail(picture: ResourcePicture): Painter {
        return painterResource(picture.resource)
    }

    override fun saveImage(picture: ResourcePicture, imageBitmap: ImageBitmap) {
        error("Can't save ResourcePicture")
    }

}

object InMemoryStorage : ImageStorage<InMemoryPicture> {
    val map: MutableMap<InMemoryPicture, ImageBitmap> = mutableMapOf()
    override suspend fun getImage(picture: InMemoryPicture): ImageBitmap {
        return map.get(picture)!!
    }

    @Composable
    override fun getThumbnail(picture: InMemoryPicture): Painter {
        return BitmapPainter(map.get(picture)!!)
    }

    override fun saveImage(picture: InMemoryPicture, imageBitmap: ImageBitmap) {
        map.put(picture, imageBitmap)
    }

}

class ImageStorageFacade(private val inMemoryStorage: ImageStorage<InMemoryPicture>) : ImageStorage<Picture> {

    override suspend fun getImage(picture: Picture): ImageBitmap {
        return when (picture) {
            is ResourcePicture -> {
                ResourcesStorage.getImage(picture)
            }
            is InMemoryPicture -> {
                inMemoryStorage.getImage(picture)
            }
        }
    }

    @Composable
    override fun getThumbnail(picture: Picture): Painter {
        return when (picture) {
            is ResourcePicture -> {
                ResourcesStorage.getThumbnail(picture)
            }
            is InMemoryPicture -> {
                inMemoryStorage.getThumbnail(picture)
            }
        }
    }

    override fun saveImage(picture: Picture, imageBitmap: ImageBitmap) {
        when (picture) {
            is ResourcePicture -> {
                ResourcesStorage.saveImage(picture, imageBitmap)
            }
            is InMemoryPicture -> {
                inMemoryStorage.saveImage(picture, imageBitmap)
            }
        }
    }

}
