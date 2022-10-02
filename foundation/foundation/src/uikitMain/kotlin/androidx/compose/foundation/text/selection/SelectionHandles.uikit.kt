/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

/**
 * Clickable padding of handler
 */
private const val PADDING = 5f

/**
 * Radius of handle circle
 */
private const val RADIUS = 6f

/**
 * Thickness of handlers vertical line
 */
private const val THICKNESS = 2f

@Composable
internal actual fun SelectionHandle(
    position: Offset,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    lineHeight: Float,
    modifier: Modifier,
    content: @Composable (() -> Unit)?
) {
    val isLeft = isLeft(isStartHandle, direction, handlesCrossed)
    val y = if (isLeft) {
        position.y - PADDING - lineHeight - RADIUS * 2
    } else {
        position.y - PADDING
    }

    val positionState: State<IntOffset> = rememberUpdatedState(
        IntOffset(position.x.roundToInt(), y.roundToInt())
    )
    val handleColor = LocalTextSelectionColors.current.handleColor
    Popup(
        popupPositionProvider = remember {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ) = IntOffset(
                    x = anchorBounds.left + positionState.value.x - popupContentSize.width / 2,
                    y = anchorBounds.top + positionState.value.y
                )
            }
        }
    ) {
        Spacer(
            modifier.size((PADDING + RADIUS) * 2.dp)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        // vertical line
                        drawRect(
                            color = handleColor,
                            topLeft = Offset(
                                x = PADDING + RADIUS - THICKNESS / 2,
                                y = if (isLeft) PADDING + RADIUS else PADDING - lineHeight
                            ),
                            size = Size(THICKNESS, lineHeight + RADIUS)
                        )
                        // handle circle
                        drawCircle(
                            color = handleColor,
                            radius = RADIUS,
                            center = center
                        )
                    }
                }
        )
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
private fun isHandleLtrDirection(
    direction: ResolvedTextDirection,
    areHandlesCrossed: Boolean
): Boolean {
    return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
        direction == ResolvedTextDirection.Rtl && areHandlesCrossed
}
