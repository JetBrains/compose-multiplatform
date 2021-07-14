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

package androidx.compose.foundation.text

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

private const val Sqrt2 = 1.41421356f
internal val CursorHandleHeight = 25.dp
internal val CursorHandleWidth = CursorHandleHeight * 2f / (1 + Sqrt2)

@Composable
internal actual fun CursorHandle(
    handlePosition: Offset,
    modifier: Modifier,
    content: @Composable (() -> Unit)?
) {
    CursorHandlePopup(
        handlePosition = handlePosition
    ) {
        if (content == null) {
            DefaultCursorHandle(
                modifier = modifier,
            )
        } else content()
    }
}

@Composable
/*@VisibleForTesting*/
internal fun DefaultCursorHandle(
    modifier: Modifier,
) {
    val handleColor = LocalTextSelectionColors.current.handleColor
    HandleDrawLayout(
        modifier = modifier,
        width = CursorHandleWidth,
        height = CursorHandleHeight
    ) {
        drawPath(CursorHandleCache.createPath(this), handleColor)
    }
}

/**
 * Class used to cache a Path object to represent a selection handle
 * based on the given handle direction
 */
private class CursorHandleCache {
    companion object {
        private var cachedPath: Path? = null

        fun createPath(density: Density): Path {
            // Use the cached path if we've already created one before.
            val path = cachedPath ?: Path().apply {
                // This is the first time we create the Path.
                with(density) {
                    val height = CursorHandleHeight.toPx()
                    val radius = height / (1 + Sqrt2)
                    val edge = radius * Sqrt2 / 2
                    moveTo(x = radius - edge, y = height - radius - edge)
                    lineTo(x = radius, y = 0f)
                    lineTo(x = radius + edge, y = height - radius - edge)
                    arcTo(
                        rect = Rect(
                            top = height - 2f * radius,
                            left = 0f,
                            bottom = height,
                            right = 2 * radius
                        ),
                        startAngleDegrees = -45f,
                        sweepAngleDegrees = 270f,
                        forceMoveTo = true
                    )
                }
            }
            cachedPath = path
            return path
        }
    }
}

/**
 * Simple container to perform drawing of selection handles. This layout takes size on the screen
 * according to [width] and [height] params and performs drawing in this space as specified in
 * [onCanvas]
 */
@Composable
private fun HandleDrawLayout(
    modifier: Modifier,
    width: Dp,
    height: Dp,
    onCanvas: DrawScope.() -> Unit
) {
    Layout({}, modifier.drawBehind(onCanvas)) { _, _ ->
        // take width and height space of the screen and allow draw modifier to draw inside of it
        layout(width.roundToPx(), height.roundToPx()) {
            // this layout has no children, only draw modifier.
        }
    }
}

@Composable
private fun CursorHandlePopup(
    handlePosition: Offset,
    content: @Composable () -> Unit
) {
    val intOffset = IntOffset(handlePosition.x.roundToInt(), handlePosition.y.roundToInt())

    val popupPositioner = remember(intOffset) {
        CursorHandlePositionProvider(intOffset)
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
 * This [PopupPositionProvider] for [CursorHandlePopup]. It will position the cursor handle
 * to the [offset] in its anchor layout. For the cursor handle, the middle of the top will be
 * positioned to [offset].
 */
/*@VisibleForTesting*/
internal class CursorHandlePositionProvider(val offset: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return IntOffset(
            x = anchorBounds.left + offset.x - popupContentSize.width / 2,
            y = anchorBounds.top + offset.y
        )
    }
}