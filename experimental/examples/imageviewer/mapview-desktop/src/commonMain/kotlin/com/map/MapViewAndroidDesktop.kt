package com.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun MapViewAndroidDesktop(
    modifier: Modifier,
    isInTouchMode: Boolean,
    tiles: List<DisplayTileWithImage<TileImage>>,
    onZoom: (Pt?, Double) -> Unit,
    onClick: (Pt) -> Unit,
    onMove: (Int, Int) -> Unit,
    updateSize: (width: Int, height: Int) -> Unit
) {
    var previousMoveDownPos by remember { mutableStateOf<Offset?>(null) }
    var previousPressTime by remember { mutableStateOf(0L) }
    var previousPressPos by remember { mutableStateOf<Offset?>(null) }

    fun Modifier.applyPointerInput() = pointerInput(Unit) {
        while (true) {
            val event = awaitPointerEventScope {
                awaitPointerEvent()
            }
            val current = event.changes.firstOrNull()?.position
//            if (event.type == PointerEventType.Scroll) {
//                val scrollY: Float? = event.changes.firstOrNull()?.scrollDelta?.y
//                if (scrollY != null && scrollY != 0f) {
//                    onZoom(current?.toPt(), -scrollY * Config.SCROLL_SENSITIVITY_DESKTOP)
//                }
//            }
            when (event.type) {
                PointerEventType.Move -> {
                    if (event.buttons.isPrimaryPressed || isInTouchMode) {
                        val previous = previousMoveDownPos
                        if (previous != null && current != null) {
                            val dx = (current.x - previous.x).toInt()
                            val dy = (current.y - previous.y).toInt()
                            if (dx != 0 || dy != 0) {
                                onMove(dx, dy)
                            }
                        }
                        previousMoveDownPos = current
                    } else {
                        previousMoveDownPos = null
                    }
                }

                PointerEventType.Press -> {
                    previousPressTime = timeMs()
                    previousPressPos = current
                    previousMoveDownPos = current
                }

                PointerEventType.Release -> {
                    if (!isInTouchMode) {
                        if (timeMs() - previousPressTime < Config.CLICK_DURATION_MS) {
                            val previous = previousPressPos
                            if (current != null && previous != null) {
                                if (current.distanceTo(previous) < Config.CLICK_AREA_RADIUS_PX) {
                                    onClick(current.toPt())
                                }
                            }
                        }
                    }
                    previousPressTime = timeMs()
                    previousMoveDownPos = null
                }
            }
        }
    }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        previousMoveDownPos = null
        onMove(offsetChange.x.roundToInt(), offsetChange.y.roundToInt())
        onZoom(null, zoomChange.toDouble() - 1)
    }

    fun Modifier.applyTouchScreenHandlers(): Modifier {
        return transformable(
            transformableState
        ).pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    if (timeMs() - previousPressTime < Config.CLICK_DURATION_MS) {
                        val previous = previousPressPos
                        if (previous != null && previous.distanceTo(it) < Config.CLICK_AREA_RADIUS_PX) {
                            onClick(it.toPt())
                        }
                    }
                    previousPressTime = timeMs()
                    previousMoveDownPos = null
                }
            )
        }
    }

    Box(modifier) {
        Canvas(
            Modifier.fillMaxSize().applyPointerInput()
                .run {
                    if (isInTouchMode) {
                        applyTouchScreenHandlers()
                    } else {
                        this
                    }
                }
        ) {
            updateSize(size.width.toInt(), size.height.toInt())
            clipRect() {
                tiles.forEach { (t, img) ->
                    if (img != null) {
                        val size = IntSize(t.size, t.size)
                        val position = IntOffset(t.x, t.y)
                        drawImage(
                            img.extract(),
                            srcOffset = IntOffset(img.offsetX, img.offsetY),
                            srcSize = IntSize(img.cropSize, img.cropSize),
                            dstOffset = position,
                            dstSize = size
                        )
                    }
                }
            }
            drawPath(path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
            }, color = Color.Red, style = Stroke(4f))
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            ZoomBtn(Icons.Filled.ZoomIn, "ZoomIn") {
                onZoom(null, 2.0)
            }
            ZoomBtn(Icons.Filled.ZoomOut, "ZoomOut") {
                onZoom(null, -2.0)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter)) {
            LinkText("OpenStreetMap license", Config.OPENSTREET_MAP_LICENSE)
            LinkText("Usage policy", Config.OPENSTREET_MAP_POLICY)
        }
    }
}

@Composable
private fun ZoomBtn(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .clickable {
                onClick()
            }
    ) {
        Icon(icon, contentDescription, Modifier.fillMaxSize().padding(2.dp), Color.Blue)
    }
}

@Composable
private fun LinkText(text:String, link:String) {
    Text(
        text = text,
        color = Color.Blue,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable {
            navigateToUrl(link)
        }
            .padding(4.dp)
            .background(Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(5.dp))
            .padding(10.dp)
            .clip(RoundedCornerShape(5.dp))
    )
}
