package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import example.imageviewer.core.FilterType
import example.imageviewer.model.AppState
import example.imageviewer.model.ContentState
import example.imageviewer.model.ScreenType
import example.imageviewer.style.DarkGray
import example.imageviewer.style.Foreground
import example.imageviewer.style.MiniatureColor
import example.imageviewer.style.Transparent
import example.imageviewer.utils.adjustImageScale
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource

@Composable
fun FullscreenImage(
    content: ContentState
) {
    Column {
        ToolBar(content.getSelectedImageName(), content)
        Image(content)
    }
    if (!content.isContentReady()) {
        LoadingScreen()
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ToolBar(
    text: String,
    content: ContentState
) {
    val scrollState = rememberScrollState()
    Surface(color = MiniatureColor, modifier = Modifier.height(44.dp)) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            Surface(
                color = Transparent,
                modifier = Modifier.padding(start = 20.dp).align(Alignment.CenterVertically),
                shape = CircleShape
            ) {
                Clickable(
                    onClick = {
                        if (content.isContentReady()) {
                            content.restoreMainImage()
                            AppState.screenState(ScreenType.MainScreen)
                        }
                    }) {
                    Image(
                        resource("back.png").rememberImageBitmap().orEmpty(),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            Text(
                text,
                color = Foreground,
                maxLines = 1,
                modifier = Modifier.padding(start = 30.dp).weight(1f)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.typography.body1
            )

            Surface(
                color = Color(255, 255, 255, 40),
                modifier = Modifier.size(154.dp, 38.dp)
                    .align(Alignment.CenterVertically),
                shape = CircleShape
            ) {
                Row(Modifier.horizontalScroll(scrollState)) {
                    for (type in FilterType.values()) {
                        FilterButton(content, type)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    content: ContentState,
    type: FilterType,
    modifier: Modifier = Modifier.size(38.dp)
) {
    Box(
        modifier = Modifier.background(color = Transparent).clip(CircleShape)
    ) {
        Clickable(
            onClick = { content.toggleFilter(type) }
        ) {
            Image(
                getFilterImage(type = type, content = content),
                contentDescription = null,
                modifier
            )
        }
    }

    Spacer(Modifier.width(20.dp))
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun getFilterImage(type: FilterType, content: ContentState): ImageBitmap {
    return when (type) {
        FilterType.GrayScale -> if (content.isFilterEnabled(type)) {
            resource("grayscale_on.png").rememberImageBitmap().orEmpty()
        } else {
            resource("grayscale_off.png").rememberImageBitmap().orEmpty()
        }
        FilterType.Pixel -> if (content.isFilterEnabled(type)) {
            resource("pixel_on.png").rememberImageBitmap().orEmpty()
        } else {
            resource("pixel_off.png").rememberImageBitmap().orEmpty()
        }
        FilterType.Blur -> if (content.isFilterEnabled(type)) {
            resource("blur_on.png").rememberImageBitmap().orEmpty()
        } else {
            resource("blur_off.png").rememberImageBitmap().orEmpty()
        }
    }
}

@Composable
fun Image(content: ContentState) {
    val drag = remember { DragHandler() }
    val scale = remember { ScaleHandler() }

    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        Draggable(dragHandler = drag, modifier = Modifier.fillMaxSize()) {
            Scalable(onScale = scale, modifier = Modifier.fillMaxSize()) {
                val bitmap = imageByGesture(content, scale, drag)
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = adjustImageScale(bitmap)
                )
            }
        }
    }
}

@Composable
fun imageByGesture(
    content: ContentState,
    scale: ScaleHandler,
    drag: DragHandler
): ImageBitmap {
    return content.getSelectedImage()
//    val bitmap = cropBitmapByScale(content.getSelectedImage(), scale.factor.value, drag)//todo crop
//
//    if (scale.factor.value > 1f)
//        return bitmap
//
//    if (abs(drag.getDistance().x) > displayWidth() / 10) {
//        if (drag.getDistance().x < 0) {
//            content.swipeNext()
//        } else {
//            content.swipePrevious()
//        }
//        drag.cancel()
//    }
//
//    return bitmap
}