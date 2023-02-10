package example.imageviewer.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.model.GalleryState
import example.imageviewer.model.Picture
import example.imageviewer.model.toFullscreen
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun PreviewImage(
    galleryState: MutableState<GalleryState>,
    getImage: suspend (Picture) -> ImageBitmap
) {
    val pictures = galleryState.value.pictures
    val index = galleryState.value.currentPictureIndex
    val imageState = remember(pictures, index) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pictures, index) {
        val picture = pictures.getOrNull(index)
        if (picture != null) {
            imageState.value = getImage(picture)
        }
    }

    val image = imageState.value
    Spacer(
        modifier = Modifier.height(5.dp).fillMaxWidth()
            .background(brush = kotlinHorizontalGradientBrush)
    )
    Card(
//        colors = CardDefaults.cardColors()
//        backgroundColor = MaterialTheme.colors.background,
        modifier = Modifier.height(200.dp)
            .background(brush = kotlinHorizontalGradientBrush)
            .padding(10.dp)
            .clickable { galleryState.toFullscreen() },
        shape = RoundedCornerShape(10.dp, 10.dp, 10.dp, 10.dp),
//        elevation = 1.dp
    ) {
        AnimatedContent(
            targetState = image,// ?: resource("empty.png").rememberImageBitmap().orEmpty(),
            transitionSpec = {
                slideInVertically(initialOffsetY = { it }) with slideOutVertically(targetOffsetY = { -it })
            }
        ) { imageBitmap ->
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()//.padding(start = 1.dp, top = 1.dp, end = 1.dp, bottom = 5.dp),
                    ,
                    contentScale = ContentScale.Crop
                )
            } else {
                Spacer(
                    modifier = Modifier.fillMaxSize()
                        .background(brush = kotlinHorizontalGradientBrush)
                )
            }
        }
    }
}

@Composable
internal expect fun needShowPreview(): Boolean
