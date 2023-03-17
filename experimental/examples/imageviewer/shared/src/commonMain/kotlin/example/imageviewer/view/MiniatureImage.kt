package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import example.imageviewer.ImageProvider
import example.imageviewer.model.PictureData

@Composable
fun MiniatureImage(
    modifier: Modifier,
    picture: PictureData,
    storage: ImageProvider,
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
