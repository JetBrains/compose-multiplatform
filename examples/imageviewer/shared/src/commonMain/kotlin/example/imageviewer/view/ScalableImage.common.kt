package example.imageviewer.view

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import kotlin.math.pow

/**
 * Initial zoom of the image. 1.0f means the image fully fits the window.
 */
private const val INITIAL_ZOOM = 1.0f

/**
 * This zoom means that the image isn't significantly zoomed for the user yet.
 */
private const val SLIGHTLY_INCREASED_ZOOM = 1.5f

@Composable
fun ScalableImage(scalableState: ScalableState, image: ImageBitmap, modifier: Modifier = Modifier) {
    BoxWithConstraints {
        val areaSize = areaSize
        val imageSize = image.size
        val imageCenter = Offset(image.width / 2f, image.height / 2f)
        val areaCenter = Offset(areaSize.width / 2f, areaSize.height / 2f)

        Box(
            modifier
                .drawWithContent {
                    drawIntoCanvas {
                        it.withSave {
                            it.translate(areaCenter.x, areaCenter.y)
                            it.translate(
                                scalableState.transformation.offset.x,
                                scalableState.transformation.offset.y
                            )
                            it.scale(
                                scalableState.transformation.scale,
                                scalableState.transformation.scale
                            )
                            it.translate(-imageCenter.x, -imageCenter.y)
                            drawImage(image)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        scalableState.addPan(pan)
                        scalableState.addZoom(zoom, centroid - areaCenter)
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    val centroid = it.changes[0].position
                    val delta = it.changes[0].scrollDelta
                    val zoom = 1.2f.pow(-delta.y)
                    scalableState.addZoom(zoom, centroid - areaCenter)
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { position ->
                        // If a user zoomed significantly, the zoom should be the restored on double tap,
                        // otherwise the zoom should be increased
                        scalableState.setZoom(
                            if (scalableState.zoom > SLIGHTLY_INCREASED_ZOOM) {
                                INITIAL_ZOOM
                            } else {
                                scalableState.zoomLimits.endInclusive
                            },
                            position - areaCenter
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
