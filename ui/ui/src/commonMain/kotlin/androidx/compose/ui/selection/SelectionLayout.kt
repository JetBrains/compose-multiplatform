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

package androidx.compose.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Layout
import androidx.compose.ui.LayoutModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Placeable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.enforce
import androidx.compose.ui.unit.hasFixedHeight
import androidx.compose.ui.unit.hasFixedWidth
import androidx.compose.ui.window.Popup
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * @suppress
 */
@InternalTextApi
@Composable
fun SelectionHandleLayout(
    startHandlePosition: Offset?,
    endHandlePosition: Offset?,
    isStartHandle: Boolean,
    directions: Pair<ResolvedTextDirection, ResolvedTextDirection>,
    handlesCrossed: Boolean,
    handle: @Composable () -> Unit
) {
    val offset = (if (isStartHandle) startHandlePosition else endHandlePosition) ?: return

    SelectionLayout(AllowZeroSize) {
        val left = isLeft(
            isStartHandle = isStartHandle,
            directions = directions,
            handlesCrossed = handlesCrossed
        )
        val alignment = if (left) Alignment.TopEnd else Alignment.TopStart

        Popup(
            alignment = alignment,
            offset = IntOffset(offset.x.roundToInt(), offset.y.roundToInt()),
            content = handle
        )
    }
}

/**
 * Selection is transparent in terms of measurement and layout and passes the same constraints to
 * the children.
 * @suppress
 */
@InternalTextApi
@Composable
fun SelectionLayout(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(modifier = modifier, children = children) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val width = placeables.fold(0) { maxWidth, placeable ->
            max(maxWidth, (placeable.width))
        }

        val height = placeables.fold(0) { minWidth, placeable ->
            max(minWidth, (placeable.height))
        }

        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.place(0, 0)
            }
        }
    }
}

/**
 * A Container Box implementation used for selection children and handle layout
 */
@Composable
internal fun SimpleContainer(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    children: @Composable () -> Unit
) {
    Layout(children, modifier) { measurables, incomingConstraints ->
        val containerConstraints = Constraints()
            .copy(
                width?.toIntPx() ?: 0,
                width?.toIntPx() ?: Constraints.Infinity,
                height?.toIntPx() ?: 0,
                height?.toIntPx() ?: Constraints.Infinity
            )
            .enforce(incomingConstraints)
        val childConstraints = containerConstraints.copy(minWidth = 0, minHeight = 0)
        var placeable: Placeable? = null
        val containerWidth = if (
            containerConstraints.hasFixedWidth
        ) {
            containerConstraints.maxWidth
        } else {
            placeable = measurables.firstOrNull()?.measure(childConstraints)
            max((placeable?.width ?: 0), containerConstraints.minWidth)
        }
        val containerHeight = if (
            containerConstraints.hasFixedHeight
        ) {
            containerConstraints.maxHeight
        } else {
            if (placeable == null) {
                placeable = measurables.firstOrNull()?.measure(childConstraints)
            }
            max((placeable?.height ?: 0), containerConstraints.minHeight)
        }
        layout(containerWidth, containerHeight) {
            val p = placeable ?: measurables.firstOrNull()?.measure(childConstraints)
            p?.let {
                val position = Alignment.Center.align(
                    IntSize(
                        containerWidth - it.width,
                        containerHeight - it.height
                    )
                )
                it.placeRelative(
                    position.x,
                    position.y
                )
            }
        }
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
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        return layout(
            max(constraints.minWidth, placeable.width),
            max(constraints.minHeight, placeable.height)
        ) {
            placeable.placeRelative(0, 0)
        }
    }
}
