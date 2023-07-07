/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.mpp.demo

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.min
import kotlin.math.pow

@Composable
fun ImageViewer() {
    ImageViewer(remember {
        ImageBitmap(500, 1000).apply {
            Canvas(this).apply {
                drawRect(0f, 0f, size.width, size.height, Paint().apply {
                    shader = LinearGradientShader(
                        from = Offset.Zero,
                        to = Offset(size.width, size.height),
                        colors = listOf(Color.Yellow, Color.Red, Color.Blue)
                    )
                })
            }
        }
    })
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImageViewer(image: ImageBitmap) {
    val cameraState = remember { CameraState() }

    BoxWithConstraints {
        val areaSize = areaSize
        val imageSize = image.size
        val imageCenter = Offset(image.width / 2f, image.height / 2f)
        val areaCenter = Offset(areaSize.width / 2f, areaSize.height / 2f)

        if (areaSize.width > 0 && areaSize.height > 0) {
            DisposableEffect(Unit) {
                cameraState.setScale(
                    min(areaSize.width / imageSize.width, areaSize.height / imageSize.height),
                    Offset.Zero,
                )
                onDispose { }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawIntoCanvas {
                        it.withSave {
                            it.translate(areaCenter.x, areaCenter.y)
                            it.translate(cameraState.offset.x, cameraState.offset.y)
                            it.scale(cameraState.scale, cameraState.scale)
                            it.translate(-imageCenter.x, -imageCenter.y)
                            drawImage(image)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        cameraState.addPan(pan)
                        cameraState.addScale(zoom, centroid - areaCenter)
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    val centroid = it.changes[0].position
                    val delta = it.changes[0].scrollDelta
                    val zoom = 1.2f.pow(-delta.y)
                    cameraState.addScale(zoom, centroid - areaCenter)
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { position ->
                        cameraState.setScale(
                            if (cameraState.scale > 2.0) {
                                cameraState.scaleLimits.start
                            } else {
                                cameraState.scaleLimits.endInclusive
                            },
                            position - areaCenter
                        )
                    }) { }
                },
        )

        SideEffect {
            cameraState.limitTargetInsideArea(areaSize, imageSize)
        }
    }
}

/**
 * Encapsulate all transformations about showing some target (an image, relative to its center)
 * scaled and shifted in some area (a window, relative to its center)
 */
private class CameraState {
    /**
     * Offset of the camera before scaling (an offset in pixels in the area coordinate system)
     */
    var offset by mutableStateOf(Offset.Zero)
        private set
    var scale by mutableStateOf(1f)
        private set

    private var areaSize: Size = Size.Unspecified
    private var targetSize: Size = Size.Zero

    val scaleLimits = 0.5f..5f
    private var offsetXLimits = Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY
    private var offsetYLimits = Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY

    /**
     * Limit the target center position, so:
     * - if the size of the target is less than area,
     *   the center of the target is bound to the center of the area
     * - if the size of the target is greater, then limit the center of it,
     *   so the target will be always in the area
     */
    fun limitTargetInsideArea(
        areaSize: Size,
        targetSize: Size,
    ) {
        this.areaSize = areaSize
        this.targetSize = targetSize
        applyLimits()
    }

    private fun applyLimits() {
        if (targetSize.isSpecified && areaSize.isSpecified) {
            offsetXLimits = centerLimits(targetSize.width * scale, areaSize.width)
            offsetYLimits = centerLimits(targetSize.height * scale, areaSize.height)
            offset = Offset(
                offset.x.coerceIn(offsetXLimits),
                offset.y.coerceIn(offsetYLimits),
            )
        } else {
            offsetXLimits = Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY
            offsetYLimits = Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY
        }
    }

    private fun centerLimits(imageSize: Float, areaSize: Float): ClosedFloatingPointRange<Float> {
        val areaCenter = areaSize / 2
        val imageCenter = imageSize / 2
        val extra = (imageCenter - areaCenter).coerceAtLeast(0f)
        return -extra / 2..extra / 2
    }

    fun addPan(pan: Offset) {
        offset += pan
        applyLimits()
    }

    /**
     * @param focus on which point the camera is focused in the area coordinate system.
     * After we apply the new scale, the camera should be focused on the same point in
     * the target coordinate system.
     */
    fun addScale(scaleMultiplier: Float, focus: Offset) {
        setScale(scale * scaleMultiplier, focus)
    }

    fun setScale(scale: Float, focus: Offset) {
        val newScale = scale.coerceIn(scaleLimits)
        val focusInTargetSystem = (focus - offset) / this.scale
        // calculate newOffset from this equation:
        // focusInTargetSystem = (focus - newOffset) / newScale
        offset = focus - focusInTargetSystem * newScale
        this.scale = newScale
        applyLimits()
    }
}

private val ImageBitmap.size get() = Size(width.toFloat(), height.toFloat())

private val BoxWithConstraintsScope.areaSize
    @Composable get() = with(LocalDensity.current) {
        Size(maxWidth.toPx(), maxHeight.toPx())
    }