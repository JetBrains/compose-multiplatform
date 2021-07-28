package example.imageviewer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowSize
import example.imageviewer.core.FilterType
import example.imageviewer.model.AppState
import example.imageviewer.model.ContentState
import example.imageviewer.model.ScreenType
import example.imageviewer.style.DarkGray
import example.imageviewer.style.DarkGreen
import example.imageviewer.style.Foreground
import example.imageviewer.style.MiniatureColor
import example.imageviewer.style.TranslucentBlack
import example.imageviewer.style.Transparent
import example.imageviewer.style.icBack
import example.imageviewer.style.icFilterBlurOff
import example.imageviewer.style.icFilterBlurOn
import example.imageviewer.style.icFilterGrayscaleOff
import example.imageviewer.style.icFilterGrayscaleOn
import example.imageviewer.style.icFilterPixelOff
import example.imageviewer.style.icFilterPixelOn
import example.imageviewer.utils.cropImage
import example.imageviewer.utils.getDisplayBounds
import example.imageviewer.utils.toByteArray
import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun setImageFullScreen(
    content: ContentState
) {
    if (content.isContentReady()) {
        Column {
            setToolBar(content.getSelectedImageName(), content)
            setImage(content)
        }
    } else {
        setLoadingScreen()
    }
}

@Composable
private fun setLoadingScreen() {

    Box {
        Surface(color = MiniatureColor, modifier = Modifier.height(44.dp)) {}
        Box(modifier = Modifier.align(Alignment.Center)) {
            Surface(color = DarkGray, elevation = 4.dp, shape = CircleShape) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp).padding(3.dp, 3.dp, 4.dp, 4.dp),
                    color = DarkGreen
                )
            }
        }
    }
}

@Composable
fun setToolBar(
    text: String,
    content: ContentState
) {
    val backButtonHover = remember { mutableStateOf(false) }
    Surface(
        color = MiniatureColor,
        modifier = Modifier.height(44.dp)
    ) {
        Row(modifier = Modifier.padding(end = 30.dp)) {
            Surface(
                color = Transparent,
                modifier = Modifier.padding(start = 20.dp).align(Alignment.CenterVertically),
                shape = CircleShape
            ) {
                Clickable(
                    modifier = Modifier.hover(
                        onEnter = {
                            backButtonHover.value = true
                            false
                        },
                        onExit = {
                            backButtonHover.value = false
                            false
                    })
                    .background(color = if (backButtonHover.value) TranslucentBlack else Transparent),
                    onClick = {
                        if (content.isContentReady()) {
                            content.restoreMainImage()
                            AppState.screenState(ScreenType.Main)
                        }
                    }) {
                    Image(
                        icBack(),
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
                val state = rememberScrollState(0)
                Row(modifier = Modifier.horizontalScroll(state)) {
                    Row {
                        for (type in FilterType.values()) {
                            FilterButton(content, type)
                        }
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
    val filterButtonHover = remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.background(color = Transparent).clip(CircleShape)
    ) {
        Clickable(
            modifier = Modifier.hover(
                onEnter = {
                    filterButtonHover.value = true
                    false
                },
                onExit = {
                    filterButtonHover.value = false
                    false
            })
            .background(color = if (filterButtonHover.value) TranslucentBlack else Transparent),
            onClick = { content.toggleFilter(type)}
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

@Composable
fun getFilterImage(type: FilterType, content: ContentState): Painter {

    return when (type) {
        FilterType.GrayScale -> if (content.isFilterEnabled(type)) icFilterGrayscaleOn() else icFilterGrayscaleOff()
        FilterType.Pixel -> if (content.isFilterEnabled(type)) icFilterPixelOn() else icFilterPixelOff()
        FilterType.Blur -> if (content.isFilterEnabled(type)) icFilterBlurOn() else icFilterBlurOff()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun setImage(content: ContentState) {
    val drag = remember { DragHandler() }
    val scale = remember { ScaleHandler() }

    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        Draggable(dragHandler = drag, modifier = Modifier.fillMaxSize()) {
            Zoomable(
                onScale = scale,
                modifier = Modifier.fillMaxSize()
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyUp) {
                            when (it.key) {
                                Key.DirectionLeft -> content.swipePrevious()
                                Key.DirectionRight -> content.swipeNext()
                            }
                        }
                        false
                    }
            ) {
                val bitmap = imageByGesture(content, scale, drag)
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit
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
    val bitmap = cropBitmapByScale(content.getSelectedImage(), content.windowState.size, scale.factor.value, drag)
    return org.jetbrains.skija.Image.makeFromEncoded(toByteArray(bitmap)).asImageBitmap()
}

private fun cropBitmapByScale(
    bitmap: BufferedImage,
    size: WindowSize,
    scale: Float,
    drag: DragHandler
): BufferedImage {
    val crop = cropBitmapByBounds(
        bitmap,
        getDisplayBounds(bitmap, size),
        size,
        scale,
        drag
    )
    return cropImage(
        bitmap,
        Rectangle(crop.x, crop.y, crop.width - crop.x, crop.height - crop.y)
    )
}

private fun cropBitmapByBounds(
    bitmap: BufferedImage,
    bounds: Rectangle,
    size: WindowSize,
    scaleFactor: Float,
    drag: DragHandler
): Rectangle {

    if (scaleFactor <= 1f) {
        return Rectangle(0, 0, bitmap.width, bitmap.height)
    }

    var scale = scaleFactor.toDouble().pow(1.4)

    var boundW = (bounds.width / scale).roundToInt()
    var boundH = (bounds.height / scale).roundToInt()

    scale *= size.width.value / bounds.width.toDouble()

    val offsetX = drag.getAmount().x / scale
    val offsetY = drag.getAmount().y / scale

    if (boundW > bitmap.width) {
        boundW = bitmap.width
    }
    if (boundH > bitmap.height) {
        boundH = bitmap.height
    }

    val invisibleW = bitmap.width - boundW
    var leftOffset = (invisibleW / 2.0 - offsetX).roundToInt()

    if (leftOffset > invisibleW) {
        leftOffset = invisibleW
        drag.getAmount().x = -((invisibleW / 2.0) * scale).roundToInt().toFloat()
    }
    if (leftOffset < 0) {
        drag.getAmount().x = ((invisibleW / 2.0) * scale).roundToInt().toFloat()
        leftOffset = 0
    }

    val invisibleH = bitmap.height - boundH
    var topOffset = (invisibleH / 2 - offsetY).roundToInt()

    if (topOffset > invisibleH) {
        topOffset = invisibleH
        drag.getAmount().y = -((invisibleH / 2.0) * scale).roundToInt().toFloat()
    }
    if (topOffset < 0) {
        drag.getAmount().y = ((invisibleH / 2.0) * scale).roundToInt().toFloat()
        topOffset = 0
    }

    return Rectangle(leftOffset, topOffset, leftOffset + boundW, topOffset + boundH)
}
