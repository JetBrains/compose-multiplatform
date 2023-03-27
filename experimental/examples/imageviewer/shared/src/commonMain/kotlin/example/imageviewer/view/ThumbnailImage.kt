package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import example.imageviewer.ImageProvider
import example.imageviewer.ImageProviderLocal
import example.imageviewer.model.PictureData

@Composable
internal fun ThumbnailImage(
    modifier: Modifier,
    picture: PictureData,
    filter: (ImageBitmap) -> ImageBitmap = remember { { it } },
) {
    val imageProvider = ImageProviderLocal.current
    var imageBitmap by remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        imageBitmap = imageProvider.getThumbnail(picture)
    }
    if (imageBitmap != null) {
        Image(
            bitmap = filter(imageBitmap!!),
            contentDescription = picture.name,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
