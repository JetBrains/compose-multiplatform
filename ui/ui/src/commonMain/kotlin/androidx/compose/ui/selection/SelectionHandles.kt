/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.util.annotation.VisibleForTesting
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.max
import kotlin.math.roundToInt

internal val HANDLE_WIDTH = 25.dp
internal val HANDLE_HEIGHT = 25.dp

/**
 * @suppress
 */
@InternalTextApi
@Composable
fun SelectionHandle(
    startHandlePosition: Offset?,
    endHandlePosition: Offset?,
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean,
    modifier: Modifier,
    handle: (@Composable () -> Unit)?
) {
    SelectionHandlePopup(
        startHandlePosition = startHandlePosition,
        endHandlePosition = endHandlePosition,
        isStartHandle = isStartHandle,
        directions = directions,
        handlesCrossed = handlesCrossed
    ) {
        if (handle == null) {
            DefaultSelectionHandle(
                modifier = modifier,
                isStartHandle = isStartHandle,
                directions = directions,
                handlesCrossed = handlesCrossed
            )
        } else handle()
    }
}

/**
 * Adjust coordinates for given text offset.
 *
 * Currently [android.text.Layout.getLineBottom] returns y coordinates of the next
 * line's top offset, which is not included in current line's hit area. To be able to
 * hit current line, move up this y coordinates by 1 pixel.
 *
 * @suppress
 */
@InternalTextApi
fun getAdjustedCoordinates(position: Offset): Offset {
    return Offset(position.x, position.y - 1f)
}

/**
 * @suppress
 */
@InternalTextApi
@Composable
@VisibleForTesting
internal fun DefaultSelectionHandle(
    modifier: Modifier,
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean
) {
    val selectionHandleCache = remember { SelectionHandleCache() }
    val handleColor = AmbientTextSelectionColors.current.handleColor
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
    Layout(emptyContent(), modifier.drawBehind(onCanvas)) { _, _ ->
        // take width and height space of the screen and allow draw modifier to draw inside of it
        layout(width.toIntPx(), height.toIntPx()) {
            // this layout has no children, only draw modifier.
        }
    }
}

/**
 * @suppress
 */
@InternalTextApi
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

    SimpleLayout(AllowZeroSize) {
        val left = isLeft(
            isStartHandle = isStartHandle,
            directions = directions,
            handlesCrossed = handlesCrossed
        )
        val alignment = if (left) AbsoluteAlignment.TopRight else AbsoluteAlignment.TopLeft

        val intOffset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt())

        val popupPositioner = remember(alignment, intOffset) {
            SelectionHandlePositionProvider(alignment, intOffset)
        }

        Popup(
            popupPositionProvider = popupPositioner,
            content = content
        )
    }
}

/**
 * This modifier allows the content to measure at its desired size without regard for the incoming
 * measurement [minimum width][Constraints.minWidth] or [minimum height][Constraints.minHeight]
 * constraints.
 *
 * The same as "wrapContentSize" in foundation-layout, which we cannot use in this module.
 */
private object AllowZeroSize : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        return layout(
            max(constraints.minWidth, placeable.width),
            max(constraints.minHeight, placeable.height)
        ) {
            placeable.place(0, 0)
        }
    }
}

/**
 * This is a copy of "AlignmentOffsetPositionProvider" class in Popup, with some
 * change at "resolvedOffset" value.
 *
 * This is for [SelectionHandlePopup] only.
 */
@VisibleForTesting
internal class SelectionHandlePositionProvider(
    val alignment: Alignment,
    val offset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        parentGlobalBounds: IntBounds,
        windowGlobalBounds: IntBounds,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // TODO: Decide which is the best way to round to result without reimplementing Alignment.align
        var popupGlobalPosition = IntOffset(0, 0)

        // Get the aligned point inside the parent
        val parentAlignmentPoint = alignment.align(
            IntSize.Zero,
            IntSize(parentGlobalBounds.width, parentGlobalBounds.height),
            layoutDirection
        )
        // Get the aligned point inside the child
        val relativePopupPos = alignment.align(
            IntSize.Zero,
            IntSize(popupContentSize.width, popupContentSize.height),
            layoutDirection
        )

        // Add the global position of the parent
        popupGlobalPosition += IntOffset(parentGlobalBounds.left, parentGlobalBounds.top)

        // Add the distance between the parent's top left corner and the alignment point
        popupGlobalPosition += parentAlignmentPoint

        // Subtract the distance between the children's top left corner and the alignment point
        popupGlobalPosition -= IntOffset(relativePopupPos.x, relativePopupPos.y)

        // Add the user offset
        val resolvedOffset = IntOffset(offset.x, offset.y)
        popupGlobalPosition += resolvedOffset

        return popupGlobalPosition
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
@VisibleForTesting
internal fun isHandleLtrDirection(
    direction: ResolvedTextDirection,
    areHandlesCrossed: Boolean
): Boolean {
    return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
        direction == ResolvedTextDirection.Rtl && areHandlesCrossed
}
