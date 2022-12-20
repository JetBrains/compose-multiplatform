/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.LocalLayerContainer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.awt.MouseInfo
import javax.swing.SwingUtilities.convertPointFromScreen

/**
 * Provides [PopupPositionProvider] relative to the current mouse cursor position.
 *
 * @param offset [DpOffset] to be added to the position of the popup.
 * @param alignment The alignment of the popup relative to the current cursor position.
 * @param windowMargin Defines the area within the window that limits the placement of the popup.
 */
@Composable
fun rememberCursorPositionProvider(
    offset: DpOffset = DpOffset.Zero,
    alignment: Alignment = Alignment.BottomEnd,
    windowMargin: Dp = 4.dp
): PopupPositionProvider = with(LocalDensity.current) {
    val component = LocalLayerContainer.current
    val cursorPoint = remember {
        val awtMousePosition = MouseInfo.getPointerInfo().location
        convertPointFromScreen(awtMousePosition, component)
        IntOffset(
            (awtMousePosition.x * component.density.density).toInt(),
            (awtMousePosition.y * component.density.density).toInt()
        )
    }
    val offsetPx = IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
    val windowMarginPx = windowMargin.roundToPx()
    object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ) = with(density) {
            val anchor = IntRect(cursorPoint, IntSize.Zero)
            val tooltipArea = IntRect(
                IntOffset(
                    anchor.left - popupContentSize.width,
                    anchor.top - popupContentSize.height,
                ),
                IntSize(
                    popupContentSize.width * 2,
                    popupContentSize.height * 2
                )
            )
            val position = alignment.align(popupContentSize, tooltipArea.size, layoutDirection)
            var x = tooltipArea.left + position.x + offsetPx.x
            var y = tooltipArea.top + position.y + offsetPx.y
            if (x + popupContentSize.width > windowSize.width - windowMarginPx) {
                x -= popupContentSize.width
            }
            if (y + popupContentSize.height > windowSize.height - windowMarginPx) {
                y -= popupContentSize.height + anchor.height
            }
            if (x < windowMarginPx) {
                x = windowMarginPx
            }
            if (y < windowMarginPx) {
                y = windowMarginPx
            }
            IntOffset(x, y)
        }
    }
}

/**
 * Provides [PopupPositionProvider] relative to the current component bounds.
 *
 * @param anchor The anchor point relative to the current component bounds.
 * @param alignment The alignment of the popup relative to the [anchor] point.
 * @param offset [DpOffset] to be added to the position of the popup.
 */
@Composable
fun rememberComponentRectPositionProvider(
    anchor: Alignment = Alignment.BottomCenter,
    alignment: Alignment = Alignment.BottomCenter,
    offset: DpOffset = DpOffset.Zero
): PopupPositionProvider = with(LocalDensity.current) {
    val offsetPx = IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
    return object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ): IntOffset {
            val anchorPoint = anchor.align(IntSize.Zero, anchorBounds.size, layoutDirection)
            val tooltipArea = IntRect(
                IntOffset(
                    anchorBounds.left + anchorPoint.x - popupContentSize.width,
                    anchorBounds.top + anchorPoint.y - popupContentSize.height,
                ),
                IntSize(
                    popupContentSize.width * 2,
                    popupContentSize.height * 2
                )
            )
            val position = alignment.align(popupContentSize, tooltipArea.size, layoutDirection)
            return tooltipArea.topLeft + position + offsetPx
        }
    }
}
