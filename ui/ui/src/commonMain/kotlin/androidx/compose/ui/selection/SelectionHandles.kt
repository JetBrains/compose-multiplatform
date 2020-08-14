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
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val HANDLE_WIDTH = 25.dp
internal val HANDLE_HEIGHT = 25.dp
private val HANDLE_COLOR = Color(0xFF2B28F5.toInt())

@Composable
internal fun SelectionHandleLayout(modifier: Modifier, left: Boolean) {
    val selectionHandleCache = remember { SelectionHandleCache() }
    HandleDrawLayout(modifier = modifier, width = HANDLE_WIDTH, height = HANDLE_HEIGHT) {
        drawPath(selectionHandleCache.createPath(this, left), HANDLE_COLOR)
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
fun SelectionHandle(
    modifier: Modifier,
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean
) {
    SelectionHandleLayout(
        modifier,
        isLeft(isStartHandle, directions, handlesCrossed))
}

/**
 * Computes whether the handle's appearance should be left-pointing or right-pointing.
 */
internal fun isLeft(
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean
): Boolean {
    if (isStartHandle) {
        return isHandleLtrDirection(directions.first, handlesCrossed)
    } else {
        return !isHandleLtrDirection(directions.second, handlesCrossed)
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
internal fun isHandleLtrDirection(
    direction: ResolvedTextDirection,
    areHandlesCrossed: Boolean
): Boolean {
    return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
            direction == ResolvedTextDirection.Rtl && areHandlesCrossed
}
