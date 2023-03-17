package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import example.imageviewer.model.BitmapStorage
import example.imageviewer.model.ImageStorage
import example.imageviewer.model.PictureData
import example.imageviewer.model.getThumbnail

@Composable
fun MiniatureImage(
    modifier: Modifier,
    picture: PictureData,
    storage: List<BitmapStorage>,
) {
    var imageBitmap by remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        imageBitmap = storage.getThumbnail(picture)
    }
    if (imageBitmap != null) {
        Tooltip(picture.name) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = picture.name,
                modifier = modifier,
                contentScale = ContentScale.Crop,
            )
        }
    }
}
