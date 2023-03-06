package example.imageviewer.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import example.imageviewer.model.Picture

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun PreviewImage(
    picture: Picture?,
    onClick: () -> Unit,
    getImage: suspend (Picture) -> ImageBitmap
) {
    Box(Modifier.fillMaxWidth().height(393.dp).background(Color.Black), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxSize()
                .clickable { onClick() },
        ) {
            AnimatedContent(
                targetState = picture,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { it }, animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) with slideOutHorizontally(
                        targetOffsetX = { -it }, animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
//                    slideInVertically(initialOffsetY = { it }) with slideOutVertically(targetOffsetY = { -it })
                }
            ) { currentPicture ->
                var image by remember(currentPicture) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(currentPicture) {
                    if (currentPicture != null) {
                        image = getImage(currentPicture)
                    }
                }
                if (image != null) {
                    Image(
                        bitmap = image!!,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Spacer(
                        modifier = Modifier.fillMaxSize()

                    )
                }
            }
        }
    }

}
