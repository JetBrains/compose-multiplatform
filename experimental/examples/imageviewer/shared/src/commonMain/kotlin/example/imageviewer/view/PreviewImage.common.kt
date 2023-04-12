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
import example.imageviewer.model.GalleryEntryWithMetadata
import example.imageviewer.model.Picture
import example.imageviewer.model.PictureData
import kotlinx.coroutines.yield

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun PreviewImage(
    picture: PictureData,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val imageProvider = LocalImageProvider.current
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
                var image by remember(currentPicture) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(currentPicture) {
                    yield() // To ensure the animation starts first
                    // Wait until the animation is finished, because getImage is quite heavy at the moment,
                    // so the animation can be not smooth (when running in a browser)
                    while (transition.isRunning) { yield() }
                    if (currentPicture != null) {
                        image = imageProvider.getImage(picture)
                    }
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
                        MemoryTextOverlay()
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
