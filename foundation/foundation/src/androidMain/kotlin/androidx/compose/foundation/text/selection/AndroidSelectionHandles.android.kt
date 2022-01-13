/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopLeft
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopMiddle
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal actual fun SelectionHandle(
    position: Offset,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    modifier: Modifier,
    content: @Composable (() -> Unit)?
) {
    val isLeft = isLeft(isStartHandle, direction, handlesCrossed)
    // The left selection handle's top right is placed at the given position, and vice versa.
    val handleReferencePoint = if (isLeft) {
        HandleReferencePoint.TopRight
    } else {
        HandleReferencePoint.TopLeft
    }

    HandlePopup(position = position, handleReferencePoint = handleReferencePoint) {
        if (content == null) {
            DefaultSelectionHandle(
                modifier = modifier
                    .semantics {
                        this[SelectionHandleInfoKey] = SelectionHandleInfo(
                            handle = if (isStartHandle) {
                                Handle.SelectionStart
                            } else {
                                Handle.SelectionEnd
                            },
                            position = position
                        )
                    },
                isStartHandle = isStartHandle,
                direction = direction,
                handlesCrossed = handlesCrossed
            )
        } else {
            content()
        }
    }
}

@Composable
/*@VisibleForTesting*/
internal fun DefaultSelectionHandle(
    modifier: Modifier,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean
) {
    Spacer(
        modifier.size(HandleWidth, HandleHeight)
            .drawSelectionHandle(isStartHandle, direction, handlesCrossed)
    )
}

@Suppress("ModifierInspectorInfo")
internal fun Modifier.drawSelectionHandle(
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean
) = composed {
    val handleColor = LocalTextSelectionColors.current.handleColor
    this.then(
        Modifier.drawWithCache {
            val radius = size.width / 2f
            val handleImage = createHandleImage(radius)
            val colorFilter = ColorFilter.tint(handleColor)
            onDrawWithContent {
                drawContent()
                val isLeft = isLeft(isStartHandle, direction, handlesCrossed)
                if (isLeft) {
                    // Flip the selection handle horizontally.
                    scale(scaleX = -1f, scaleY = 1f) {
                        drawImage(
                            image = handleImage,
                            colorFilter = colorFilter
                        )
                    }
                } else {
                    drawImage(
                        image = handleImage,
                        colorFilter = colorFilter
                    )
                }
            }
        }
    )
}

/**
 * The cache for the image mask created to draw selection/cursor handle, so that we don't need to
 * recreate them.
 */
private object HandleImageCache {
    var imageBitmap: ImageBitmap? = null
    var canvas: Canvas? = null
    var canvasDrawScope: CanvasDrawScope? = null
}

/**
 * Create an image bitmap for the basic shape of a selection handle or cursor handle. It is an
 * circle with a rectangle covering its left top part.
 *
 * To draw the right selection handle, directly draw this image bitmap.
 * To draw the left selection handle, mirror the canvas first and then draw this image bitmap.
 * To draw the cursor handle, translate and rotated the canvas 45 degrees, then draw this image
 * bitmap.
 *
 * @param radius the radius of circle in selection/cursor handle.
 * CanvasDrawScope objects so that we only recreate them when necessary.
 */
internal fun CacheDrawScope.createHandleImage(radius: Float): ImageBitmap {
    // The edge length of the square bounding box of the selection/cursor handle. This is also
    // the size of the bitmap needed for the bitmap mask.
    val edge = ceil(radius).toInt() * 2

    var imageBitmap = HandleImageCache.imageBitmap
    var canvas = HandleImageCache.canvas
    var drawScope = HandleImageCache.canvasDrawScope

    // If the cached bitmap is null or too small, we need to create new bitmap.
    if (
        imageBitmap == null ||
        canvas == null ||
        edge > imageBitmap.width ||
        edge > imageBitmap.height
    ) {
        imageBitmap = ImageBitmap(
            width = edge,
            height = edge,
            config = ImageBitmapConfig.Alpha8
        )
        HandleImageCache.imageBitmap = imageBitmap
        canvas = Canvas(imageBitmap)
        HandleImageCache.canvas = canvas
    }
    if (drawScope == null) {
        drawScope = CanvasDrawScope()
        HandleImageCache.canvasDrawScope = drawScope
    }

    drawScope.draw(
        this,
        layoutDirection,
        canvas,
        Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
    ) {
        // Clear the previously rendered portion within this ImageBitmap as we could
        // be re-using it
        drawRect(
            color = Color.Black,
            size = size,
            blendMode = BlendMode.Clear
        )

        // Draw the rectangle at top left.
        drawRect(
            color = Color(0xFF000000),
            topLeft = Offset.Zero,
            size = Size(radius, radius)
        )
        // Draw the circle
        drawCircle(
            color = Color(0xFF000000),
            radius = radius,
            center = Offset(radius, radius)
        )
    }
    return imageBitmap
}

@Composable
internal fun HandlePopup(
    position: Offset,
    handleReferencePoint: HandleReferencePoint,
    content: @Composable () -> Unit
) {
    val intOffset = IntOffset(position.x.roundToInt(), position.y.roundToInt())

    val popupPositioner = remember(handleReferencePoint, intOffset) {
        HandlePositionProvider(handleReferencePoint, intOffset)
    }

    Popup(
        popupPositionProvider = popupPositioner,
        properties = PopupProperties(
            excludeFromSystemGesture = true,
            clippingEnabled = false
        ),
        content = content
    )
}

/**
 * The enum that specifies how a selection/cursor handle is placed to its given position.
 * When this value is [TopLeft], the top left corner of the handle will be placed at the
 * given position.
 * When this value is [TopRight], the top right corner of the handle will be placed at the
 * given position.
 * When this value is [TopMiddle], the handle top edge's middle point will be placed at the given
 * position.
 */
internal enum class HandleReferencePoint {
    TopLeft,
    TopRight,
    TopMiddle
}

/**
 * This [PopupPositionProvider] for [HandlePopup]. It will position the selection handle
 * to the [offset] in its anchor layout.
 *
 * @see HandleReferencePoint
 */
/*@VisibleForTesting*/
internal class HandlePositionProvider(
    private val handleReferencePoint: HandleReferencePoint,
    private val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return when (handleReferencePoint) {
            HandleReferencePoint.TopLeft ->
                IntOffset(
                    x = anchorBounds.left + offset.x,
                    y = anchorBounds.top + offset.y
                )
            HandleReferencePoint.TopRight ->
                IntOffset(
                    x = anchorBounds.left + offset.x - popupContentSize.width,
                    y = anchorBounds.top + offset.y
                )
            HandleReferencePoint.TopMiddle ->
                IntOffset(
                    x = anchorBounds.left + offset.x - popupContentSize.width / 2,
                    y = anchorBounds.top + offset.y
                )
        }
    }
}

/**
 * Computes whether the handle's appearance should be left-pointing or right-pointing.
 */
private fun isLeft(
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean
): Boolean {
    return if (isStartHandle) {
        isHandleLtrDirection(direction, handlesCrossed)
    } else {
        !isHandleLtrDirection(direction, handlesCrossed)
    }
}

/**
 * This method is to check if the selection handles should use the natural Ltr pointing
 * direction.
 * If the context is Ltr and the handles are not crossed, or if the context is Rtl and the handles
 * are crossed, return true.
 *
 * In Ltr context, the start handle should point to the left, and the end handle should point to
 * the right. However, in Rtl context or when handles are crossed, the start handle should point to
 * the right, and the end handle should point to left.
 */
/*@VisibleForTesting*/
internal fun isHandleLtrDirection(
    direction: ResolvedTextDirection,
    areHandlesCrossed: Boolean
): Boolean {
    return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
        direction == ResolvedTextDirection.Rtl && areHandlesCrossed
}
