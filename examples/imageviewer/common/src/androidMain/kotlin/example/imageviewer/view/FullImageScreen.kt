package example.imageviewer.view

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import example.imageviewer.core.FilterType
import example.imageviewer.model.AppState
import example.imageviewer.model.ContentState
import example.imageviewer.model.ScreenType
import example.imageviewer.style.DarkGray
import example.imageviewer.style.DarkGreen
import example.imageviewer.style.Foreground
import example.imageviewer.style.MiniatureColor
import example.imageviewer.style.Transparent
import example.imageviewer.style.icBack
import example.imageviewer.style.icFilterGrayscaleOn
import example.imageviewer.style.icFilterGrayscaleOff
import example.imageviewer.style.icFilterPixelOn
import example.imageviewer.style.icFilterPixelOff
import example.imageviewer.style.icFilterBlurOn
import example.imageviewer.style.icFilterBlurOff
import example.imageviewer.style.icFilterUnknown
import example.imageviewer.utils.displayHeight
import example.imageviewer.utils.displayWidth
import example.imageviewer.utils.getDisplayBounds
import example.imageviewer.utils.adjustImageScale
import kotlin.math.abs
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
        Surface(color = MiniatureColor, modifier = Modifier.preferredHeight(44.dp)) {}
        Box {
            Surface(color = DarkGray, elevation = 4.dp, shape = CircleShape) {
                CircularProgressIndicator(
                    modifier = Modifier.preferredSize(50.dp).padding(3.dp, 3.dp, 4.dp, 4.dp),
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

    Surface(color = MiniatureColor, modifier = Modifier.preferredHeight(44.dp)) {
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
                            AppState.screenState(ScreenType.Main)
                        }
                    }) {
                    Image(
                        icBack(),
                        modifier = Modifier.preferredSize(38.dp)
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
                modifier = Modifier.preferredSize(154.dp, 38.dp)
                    .align(Alignment.CenterVertically),
                shape = CircleShape
            ) {
                ScrollableRow {
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
    modifier: Modifier = Modifier.preferredSize(38.dp)
) {
    Box(
        modifier = Modifier.background(color = Transparent).clip(CircleShape)
    ) {
        Clickable(
            onClick = { content.toggleFilter(type) }
        ) {
            Image(
                getFilterImage(type = type, content = content),
                modifier
            )
        }
    }

    Spacer(Modifier.width(20.dp))
}

@Composable
fun getFilterImage(type: FilterType, content: ContentState): ImageBitmap {

    return when (type) {
        FilterType.GrayScale -> if (content.isFilterEnabled(type)) icFilterGrayscaleOn() else icFilterGrayscaleOff()
        FilterType.Pixel -> if (content.isFilterEnabled(type)) icFilterPixelOn() else icFilterPixelOff()
        FilterType.Blur -> if (content.isFilterEnabled(type)) icFilterBlurOn() else icFilterBlurOff()
    }
}

@Composable
fun setImage(content: ContentState) {

    val drag = DragHandler()
    val scale = ScaleHandler()

    Surface(
        color = DarkGray,
        modifier = Modifier.fillMaxSize()
    ) {
        Draggable(onDrag = drag, modifier = Modifier.fillMaxSize()) {
            Scalable(onScale = scale, modifier = Modifier.fillMaxSize()) {
                val bitmap = imageByGesture(content, scale, drag)
                Image(
                    bitmap = bitmap.asImageBitmap(),
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
): Bitmap {
    val bitmap = cropBitmapByScale(content.getSelectedImage(), scale.factor.value, drag)

    if (scale.factor.value > 1f)
        return bitmap

    if (abs(drag.getDistance().x) > displayWidth() / 10) {
        if (drag.getDistance().x < 0) {
            content.swipeNext()
        } else {
            content.swipePrevious()
        }
        drag.onCancel()
    }

    return bitmap
}

private fun cropBitmapByScale(bitmap: Bitmap, scale: Float, drag: DragHandler): Bitmap {

    val crop = cropBitmapByBounds(
        bitmap,
        getDisplayBounds(bitmap),
        scale,
        drag
    )
    return Bitmap.createBitmap(
        bitmap,
        crop.left,
        crop.top,
        crop.right - crop.left,
        crop.bottom - crop.top
    )
}

private fun cropBitmapByBounds(
    bitmap: Bitmap,
    bounds: Rect,
    scaleFactor: Float,
    drag: DragHandler
): Rect {

    if (scaleFactor <= 1f)
        return Rect(0, 0, bitmap.width, bitmap.height)

    var scale = scaleFactor.toDouble().pow(1.4)

    var boundW = (bounds.width() / scale).roundToInt()
    var boundH = (bounds.height() / scale).roundToInt()

    scale *= displayWidth() / bounds.width().toDouble()

    val offsetX = drag.getAmount().x / scale
    val offsetY = drag.getAmount().y / scale

    if (boundW > bitmap.width) {
        boundW = bitmap.width
    }
    if (boundH > bitmap.height) {
        boundH = bitmap.height
    }

    val invisibleW = bitmap.width - boundW
    var leftOffset = (invisibleW / 2.0 - offsetX).roundToInt().toFloat()

    if (leftOffset > invisibleW) {
        leftOffset = invisibleW.toFloat()
        drag.getAmount().x = -((invisibleW / 2.0) * scale).roundToInt().toFloat()
    }
    if (leftOffset < 0) {
        drag.getAmount().x = ((invisibleW / 2.0) * scale).roundToInt().toFloat()
        leftOffset = 0f
    }

    val invisibleH = bitmap.height - boundH
    var topOffset = (invisibleH / 2 - offsetY).roundToInt().toFloat()

    if (topOffset > invisibleH) {
        topOffset = invisibleH.toFloat()
        drag.getAmount().y = -((invisibleH / 2.0) * scale).roundToInt().toFloat()
    }
    if (topOffset < 0) {
        drag.getAmount().y = ((invisibleH / 2.0) * scale).roundToInt().toFloat()
        topOffset = 0f
    }

    return Rect(
        leftOffset.toInt(),
        topOffset.toInt(),
        (leftOffset + boundW).toInt(),
        (topOffset + boundH).toInt()
    )
}
