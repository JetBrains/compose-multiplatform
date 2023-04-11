package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import example.imageviewer.LocalImageProvider
import example.imageviewer.model.PictureData

@Composable
fun ThumbnailImage(
    modifier: Modifier,
    picture: PictureData,
    filter: (ImageBitmap) -> ImageBitmap = remember { { it } },
) {
    val imageProvider = LocalImageProvider.current
    var imageBitmap by remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(picture) {
        imageBitmap = imageProvider.getThumbnail(picture)
    }
    imageBitmap?.let {
        Image(
            bitmap = filter(it),
            contentDescription = picture.name,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
