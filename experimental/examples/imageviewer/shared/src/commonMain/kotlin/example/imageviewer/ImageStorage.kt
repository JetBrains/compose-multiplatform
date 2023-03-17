package example.imageviewer

import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.model.DiskPicture
import example.imageviewer.model.PictureData
import example.imageviewer.model.ResourcePicture
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource

interface ImageStorage<P : PictureData> {
    suspend fun getImage(picture: P): ImageBitmap
    suspend fun getThumbnail(picture: P): ImageBitmap = getImage(picture)
}

object ResourcesStorage : ImageStorage<ResourcePicture> {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getImage(picture: ResourcePicture): ImageBitmap {
        return resource(picture.resource).readBytes().toImageBitmap()
    }
}

object InMemoryStorage : ImageStorage<DiskPicture> {
    private val map: MutableMap<DiskPicture, ImageBitmap> = mutableMapOf()
    override suspend fun getImage(picture: DiskPicture): ImageBitmap {
        return map[picture]!!
    }
}

typealias BitmapStorage = suspend (PictureData) -> ImageBitmap?

inline fun <reified T : PictureData> MutableList<BitmapStorage>.addStorageAdapter(
    crossinline adapter: suspend (picture: T) -> ImageBitmap
) = add { if (it is T) adapter(it) else null }

suspend fun List<BitmapStorage>.getImage(picture: PictureData): ImageBitmap {
    for (storage in this) {
        return storage(picture) ?: continue
    }
    error("ImageBitmap not found for picture $picture")
}

suspend fun List<BitmapStorage>.getThumbnail(picture: PictureData) = getImage(picture)//todo
