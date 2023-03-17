package example.imageviewer

import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.model.PictureData

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
