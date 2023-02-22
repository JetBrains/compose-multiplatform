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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.model.Picture
import example.imageviewer.style.ImageviewerColors.kotlinHorizontalGradientBrush

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun PreviewImage(
    picture: Picture?,
    onClick: () -> Unit,
    getImage: suspend (Picture) -> ImageBitmap
) {
    var image by remember(picture) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(picture) {
        if (picture != null) {
            image = getImage(picture)
        }
    }

    Spacer(
        modifier = Modifier.height(5.dp).fillMaxWidth()
            .background(brush = kotlinHorizontalGradientBrush)
    )
    Card(
        modifier = Modifier.height(200.dp)
            .background(brush = kotlinHorizontalGradientBrush)
            .padding(10.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp, 10.dp, 10.dp, 10.dp),
    ) {
        AnimatedContent(
            targetState = image,
            transitionSpec = {
                slideInVertically(initialOffsetY = { it }) with slideOutVertically(targetOffsetY = { -it })
            }
        ) { imageBitmap ->
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
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
