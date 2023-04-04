package example.imageviewer.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import example.imageviewer.LocalImageProvider
import example.imageviewer.model.PictureData

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun PreviewImage(
    picture: PictureData,
    onClick: () -> Unit,
) {
    val imageProvider = LocalImageProvider.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxWidth().height(393.dp).background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource, indication = null, onClick = onClick),
        ) {
            AnimatedContent(
                targetState = picture,
                transitionSpec = {
                    slideIntoContainer(
                        towards = AnimatedContentScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) with slideOutOfContainer(
                        towards = AnimatedContentScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )
                }
            ) { currentPicture ->
                var image: ImageBitmap? by remember(currentPicture) { mutableStateOf(null) }
                LaunchedEffect(currentPicture) {
                    image = imageProvider.getImage(currentPicture)
                }
                if (image != null) {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            bitmap = image!!,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        MemoryTextOverlay(currentPicture)
                    }
                } else {
                    Spacer(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
