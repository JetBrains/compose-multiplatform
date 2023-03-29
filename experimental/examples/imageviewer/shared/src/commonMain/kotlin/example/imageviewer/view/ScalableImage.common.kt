package example.imageviewer.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import example.imageviewer.model.ScalableState
import example.imageviewer.utils.onPointerEvent
import kotlin.math.min
import kotlin.math.pow

@Composable
internal fun ScalableImage(scalableState: ScalableState, image: ImageBitmap, modifier: Modifier = Modifier) {
    BoxWithConstraints {
        val areaSize = areaSize
        val imageSize = image.size
        val imageCenter = Offset(image.width / 2f, image.height / 2f)
        val areaCenter = Offset(areaSize.width / 2f, areaSize.height / 2f)

        DisposableEffect(Unit) {
            scalableState.setScale(
                min(areaSize.width / imageSize.width, areaSize.height / imageSize.height),
                areaCenter
            )
            onDispose { }
        }

        Box(
            modifier
                .fillMaxSize()
                .drawWithContent {
                    drawIntoCanvas {
                        it.withSave {
                            it.translate(areaCenter.x, areaCenter.y)
                            it.translate(scalableState.offset.x, scalableState.offset.y)
                            it.scale(scalableState.scale, scalableState.scale)
                            it.translate(-imageCenter.x, -imageCenter.y)
                            drawImage(image)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        scalableState.addPan(pan)
                        scalableState.addScale(zoom, centroid)
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    val gestureCentroid = it.changes[0].position
                    val delta = it.changes[0].scrollDelta
                    val gestureZoom = 1.2f.pow(-delta.y)
                    scalableState.addScale(gestureZoom, gestureCentroid - areaCenter)
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { position ->
                        scalableState.setScale(
                            if (scalableState.scale > 2.0) {
                                scalableState.scaleLimits.start
                            } else {
                                scalableState.scaleLimits.endInclusive
                            },
                            position
                        )
                    }) { }
                },
        )

        SideEffect {
            scalableState.limitTargetInsideArea(areaSize, imageSize)
        }
    }
}

private val ImageBitmap.size get() = Size(width.toFloat(), height.toFloat())

private val BoxWithConstraintsScope.areaSize
    @Composable get() = with(LocalDensity.current) {
        Size(maxWidth.toPx(), maxHeight.toPx())
    }