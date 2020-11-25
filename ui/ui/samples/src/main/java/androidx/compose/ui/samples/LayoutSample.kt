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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.measureBlocksOf
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.offset

@Sampled
@Composable
fun LayoutWithProvidedIntrinsicsUsage(content: @Composable () -> Unit) {
    // We build a layout that will occupy twice as much space as its children,
    // and will position them to be bottom right aligned.
    Layout(
        content,
        minIntrinsicWidthMeasureBlock = { measurables, h ->
            // The min intrinsic width of this layout will be twice the largest min intrinsic
            // width of a child. Note that we call minIntrinsicWidth with h / 2 for children,
            // since we should be double the size of the children.
            (measurables.map { it.minIntrinsicWidth(h / 2) }.maxByOrNull { it } ?: 0) * 2
        },
        minIntrinsicHeightMeasureBlock = { measurables, w ->
            (measurables.map { it.minIntrinsicHeight(w / 2) }.maxByOrNull { it } ?: 0) * 2
        },
        maxIntrinsicWidthMeasureBlock = { measurables, h ->
            (measurables.map { it.maxIntrinsicHeight(h / 2) }.maxByOrNull { it } ?: 0) * 2
        },
        maxIntrinsicHeightMeasureBlock = { measurables, w ->
            (measurables.map { it.maxIntrinsicHeight(w / 2) }.maxByOrNull { it } ?: 0) * 2
        }
    ) { measurables, constraints ->
        // measurables contains one element corresponding to each of our layout children.
        // constraints are the constraints that our parent is currently measuring us with.
        val childConstraints = Constraints(
            minWidth = constraints.minWidth / 2,
            minHeight = constraints.minHeight / 2,
            maxWidth = constraints.maxWidth / 2,
            maxHeight = constraints.maxHeight / 2
        )
        // We measure the children with half our constraints, to ensure we can be double
        // the size of the children.
        val placeables = measurables.map { it.measure(childConstraints) }
        val layoutWidth = (placeables.maxByOrNull { it.width }?.width ?: 0) * 2
        val layoutHeight = (placeables.maxByOrNull { it.height }?.height ?: 0) * 2
        // We call layout to set the size of the current layout and to provide the positioning
        // of the children. The children are placed relative to the current layout place.
        layout(layoutWidth, layoutHeight) {
            placeables.forEach {
                it.placeRelative(layoutWidth - it.width, layoutHeight - it.height)
            }
        }
    }
}

@Sampled
@Composable
@OptIn(ExperimentalLayoutNodeApi::class)
fun LayoutWithMeasureBlocksWithIntrinsicUsage(content: @Composable () -> Unit) {
    val measureBlocks = measureBlocksOf(
        minIntrinsicWidthMeasureBlock = { measurables, h ->
            // The min intrinsic width of this layout will be twice the largest min intrinsic
            // width of a child. Note that we call minIntrinsicWidth with h / 2 for children,
            // since we should be double the size of the children.
            (measurables.map { it.minIntrinsicWidth(h / 2) }.maxByOrNull { it } ?: 0) * 2
        },
        minIntrinsicHeightMeasureBlock = { measurables, w ->
            (measurables.map { it.minIntrinsicHeight(w / 2) }.maxByOrNull { it } ?: 0) * 2
        },
        maxIntrinsicWidthMeasureBlock = { measurables, h ->
            (measurables.map { it.maxIntrinsicHeight(h / 2) }.maxByOrNull { it } ?: 0) * 2
        },
        maxIntrinsicHeightMeasureBlock = { measurables, w ->
            (measurables.map { it.maxIntrinsicHeight(w / 2) }.maxByOrNull { it } ?: 0) * 2
        }
    ) { measurables, constraints ->
        // measurables contains one element corresponding to each of our layout children.
        // constraints are the constraints that our parent is currently measuring us with.
        val childConstraints = Constraints(
            minWidth = constraints.minWidth / 2,
            minHeight = constraints.minHeight / 2,
            maxWidth = constraints.maxWidth / 2,
            maxHeight = constraints.maxHeight / 2
        )
        // We measure the children with half our constraints, to ensure we can be double
        // the size of the children.
        val placeables = measurables.map { it.measure(childConstraints) }
        val layoutWidth = (placeables.maxByOrNull { it.width }?.width ?: 0) * 2
        val layoutHeight = (placeables.maxByOrNull { it.height }?.height ?: 0) * 2
        // We call layout to set the size of the current layout and to provide the positioning
        // of the children. The children are placed relative to the current layout place.
        layout(layoutWidth, layoutHeight) {
            placeables.forEach {
                it.placeRelative(layoutWidth - it.width, layoutHeight - it.height)
            }
        }
    }
    Layout(content = content, measureBlocks = measureBlocks)
}

@Sampled
@Composable
fun LayoutUsage(content: @Composable () -> Unit) {
    // We build a layout that will occupy twice as much space as its children,
    // and will position them to be bottom right aligned.
    Layout(content) { measurables, constraints ->
        // measurables contains one element corresponding to each of our layout children.
        // constraints are the constraints that our parent is currently measuring us with.
        val childConstraints = Constraints(
            minWidth = constraints.minWidth / 2,
            minHeight = constraints.minHeight / 2,
            maxWidth = constraints.maxWidth / 2,
            maxHeight = constraints.maxHeight / 2
        )
        // We measure the children with half our constraints, to ensure we can be double
        // the size of the children.
        val placeables = measurables.map { it.measure(childConstraints) }
        val layoutWidth = (placeables.maxByOrNull { it.width }?.width ?: 0) * 2
        val layoutHeight = (placeables.maxByOrNull { it.height }?.height ?: 0) * 2
        // We call layout to set the size of the current layout and to provide the positioning
        // of the children. The children are placed relative to the current layout place.
        layout(layoutWidth, layoutHeight) {
            placeables.forEach {
                it.placeRelative(layoutWidth - it.width, layoutHeight - it.height)
            }
        }
    }
}

@Sampled
@Composable
fun LayoutTagChildrenUsage(header: @Composable () -> Unit, footer: @Composable () -> Unit) {
    Layout({
        // Here the Containers are only needed to apply the modifiers. You could use the
        // modifier on header and footer directly if they are composables accepting modifiers.
        Box(Modifier.layoutId("header")) { header() }
        Box(Modifier.layoutId("footer")) { footer() }
    }) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            when (measurable.layoutId) {
                // You should use appropriate constraints. Here we measure fake constraints.
                "header" -> measurable.measure(Constraints.fixed(100, 100))
                "footer" -> measurable.measure(constraints)
                else -> error("Unexpected tag")
            }
        }
        // Size should be derived from children measured sizes on placeables,
        // but this is simplified for the purposes of the example.
        layout(100, 100) {
            placeables.forEach { it.placeRelative(0, 0) }
        }
    }
}

@Sampled
@Composable
fun ConvenienceLayoutModifierSample() {
    Box(
        modifier = Modifier
            .background(Color.Gray)
            .layout { measurable, constraints ->
                // an example modifier that adds 50 pixels of vertical padding
                val padding = 50
                val placeable = measurable.measure(constraints.offset(vertical = -padding))
                this.layout(placeable.width, placeable.height + padding) {
                    placeable.placeRelative(0, padding)
                }
            }
    ) {
        Box(Modifier.fillMaxSize().background(Color.DarkGray)) {}
    }
}
