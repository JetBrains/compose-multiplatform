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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
internal actual fun SelectionHandle(
    startHandlePosition: Offset?,
    endHandlePosition: Offset?,
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean,
    modifier: Modifier,
    content: @Composable (() -> Unit)?
) {
    SelectionHandlePopup(
        startHandlePosition = startHandlePosition,
        endHandlePosition = endHandlePosition,
        isStartHandle = isStartHandle,
        directions = directions,
        handlesCrossed = handlesCrossed
    ) {
        if (content == null) {
            DefaultSelectionHandle(
                modifier = modifier,
                isStartHandle = isStartHandle,
                directions = directions,
                handlesCrossed = handlesCrossed
            )
        } else content()
    }
}

@Composable
/*@VisibleForTesting*/
internal fun DefaultSelectionHandle(
    modifier: Modifier,
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean
) {
    val selectionHandleCache = remember { SelectionHandleCache() }
    val handleColor = LocalTextSelectionColors.current.handleColor
    HandleDrawLayout(modifier = modifier, width = HANDLE_WIDTH, height = HANDLE_HEIGHT) {
        drawPath(
            selectionHandleCache.createPath(
                this,
                isLeft(isStartHandle, directions, handlesCrossed)
            ),
            handleColor
        )
    }
}

/**
 * Class used to cache a Path object to represent a selection handle
 * based on the given handle direction
 */
private class SelectionHandleCache {
    private var path: Path? = null
    private var left: Boolean = false

    fun createPath(density: Density, left: Boolean): Path {
        return with(density) {
            val current = path
            if (this@SelectionHandleCache.left == left && current != null) {
                // If we have already created the Path for the correct handle direction
                // return it
                current
            } else {
                this@SelectionHandleCache.left = left
                // Otherwise, if this is the first time we are creating the Path
                // or the current handle direction is different than the one we
                // previously created, recreate the path and cache the result
                (current ?: Path().also { path = it }).apply {
                    reset()
                    addRect(
                        Rect(
                            top = 0f,
                            bottom = 0.5f * HANDLE_HEIGHT.toPx(),
                            left = if (left) {
                                0.5f * HANDLE_WIDTH.toPx()
                            } else {
                                0f
                            },
                            right = if (left) {
                                HANDLE_WIDTH.toPx()
                            } else {
                                0.5f * HANDLE_WIDTH.toPx()
                            }
                        )
                    )
                    addOval(
                        Rect(
                            top = 0f,
                            bottom = HANDLE_HEIGHT.toPx(),
                            left = 0f,
                            right = HANDLE_WIDTH.toPx()
                        )
                    )
                }
            }
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
private fun SelectionHandlePopup(
    startHandlePosition: Offset?,
    endHandlePosition: Offset?,
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean,
    content: @Composable () -> Unit
) {
    val offset = (if (isStartHandle) startHandlePosition else endHandlePosition) ?: return
    val left = isLeft(
        isStartHandle = isStartHandle,
        directions = directions,
        handlesCrossed = handlesCrossed
    )

    val intOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt())

    val popupPositioner = remember(left, intOffset) {
        SelectionHandlePositionProvider(left, intOffset)
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
 * This [PopupPositionProvider] for [SelectionHandlePopup]. It will position the selection handle
 * to the [offset] in its anchor layout. For left selection handle, the right top corner will be
 * positioned to [offset]. For right selection handle, the left top corner will be positioned to
 * [offset].
 */
/*@VisibleForTesting*/
internal class SelectionHandlePositionProvider(
    val isLeft: Boolean,
    val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return if (isLeft) {
            IntOffset(
                x = anchorBounds.left + offset.x - popupContentSize.width,
                y = anchorBounds.top + offset.y
            )
        } else {
            IntOffset(
                x = anchorBounds.left + offset.x,
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
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean
): Boolean {
    return if (isStartHandle) {
        isHandleLtrDirection(directions.first, handlesCrossed)
    } else {
        !isHandleLtrDirection(directions.second, handlesCrossed)
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
