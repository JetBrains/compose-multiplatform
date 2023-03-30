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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

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
            maxWidth = if (constraints.hasBoundedWidth) {
                constraints.maxWidth / 2
            } else {
                Constraints.Infinity
            },
            maxHeight = if (constraints.hasBoundedHeight) {
                constraints.maxHeight / 2
            } else {
                Constraints.Infinity
            }
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
fun LayoutWithProvidedIntrinsicsUsage(content: @Composable () -> Unit) {
    // We build a layout that will occupy twice as much space as its children,
    // and will position them to be bottom right aligned.
    val measurePolicy = object : MeasurePolicy {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            // measurables contains one element corresponding to each of our layout children.
            // constraints are the constraints that our parent is currently measuring us with.
            val childConstraints = Constraints(
                minWidth = constraints.minWidth / 2,
                minHeight = constraints.minHeight / 2,
                maxWidth = if (constraints.hasBoundedWidth) {
                    constraints.maxWidth / 2
                } else {
                    Constraints.Infinity
                },
                maxHeight = if (constraints.hasBoundedHeight) {
                    constraints.maxHeight / 2
                } else {
                    Constraints.Infinity
                }
            )
            // We measure the children with half our constraints, to ensure we can be double
            // the size of the children.
            val placeables = measurables.map { it.measure(childConstraints) }
            val layoutWidth = (placeables.maxByOrNull { it.width }?.width ?: 0) * 2
            val layoutHeight = (placeables.maxByOrNull { it.height }?.height ?: 0) * 2
            // We call layout to set the size of the current layout and to provide the positioning
            // of the children. The children are placed relative to the current layout place.
            return layout(layoutWidth, layoutHeight) {
                placeables.forEach {
                    it.placeRelative(layoutWidth - it.width, layoutHeight - it.height)
                }
            }
        }

        // The min intrinsic width of this layout will be twice the largest min intrinsic
        // width of a child. Note that we call minIntrinsicWidth with h / 2 for children,
        // since we should be double the size of the children.
        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = (measurables.map { it.minIntrinsicWidth(height / 2) }.maxByOrNull { it } ?: 0) * 2

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = (measurables.map { it.minIntrinsicHeight(width / 2) }.maxByOrNull { it } ?: 0) * 2

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = (measurables.map { it.maxIntrinsicHeight(height / 2) }.maxByOrNull { it } ?: 0) * 2

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = (measurables.map { it.maxIntrinsicHeight(width / 2) }.maxByOrNull { it } ?: 0) * 2
    }

    Layout(content = content, measurePolicy = measurePolicy)
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
fun LayoutModifierSample() {
    val verticalPadding = object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            // an example modifier that adds 50 pixels of vertical padding.
            val padding = 50
            val placeable = measurable.measure(constraints.offset(vertical = -padding))
            return layout(placeable.width, placeable.height + padding) {
                placeable.placeRelative(0, padding)
            }
        }
    }
    Box(Modifier.background(Color.Gray).then(verticalPadding)) {
        Box(Modifier.fillMaxSize().background(Color.DarkGray))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun LayoutModifierNodeSample() {
    class VerticalPadding(var padding: Dp) : LayoutModifierNode, Modifier.Node() {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val paddingPx = padding.roundToPx()
            val placeable = measurable.measure(constraints.offset(vertical = -paddingPx))
            return layout(placeable.width, placeable.height + paddingPx) {
                placeable.placeRelative(0, paddingPx)
            }
        }
    }
    data class VerticalPaddingElement(
        val padding: Dp
    ) : ModifierNodeElement<VerticalPadding>() {
        override fun create() = VerticalPadding(padding)
        override fun update(node: VerticalPadding): VerticalPadding {
            node.padding = padding
            return node
        }
        override fun InspectorInfo.inspectableProperties() {
            name = "verticalPadding"
            properties["padding"] = padding
        }
    }
    fun Modifier.verticalPadding(padding: Dp) = this then VerticalPaddingElement(padding)
    Box(Modifier.background(Color.Gray).verticalPadding(50.dp)) {
        Box(Modifier.fillMaxSize().background(Color.DarkGray))
    }
}

@Sampled
@Composable
fun ConvenienceLayoutModifierSample() {
    Box(
        Modifier.background(Color.Gray)
            .layout { measurable, constraints ->
                // an example modifier that adds 50 pixels of vertical padding.
                val padding = 50
                val placeable = measurable.measure(constraints.offset(vertical = -padding))
                layout(placeable.width, placeable.height + padding) {
                    placeable.placeRelative(0, padding)
                }
            }
    ) {
        Box(Modifier.fillMaxSize().background(Color.DarkGray))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun LayoutWithMultipleContentsUsage(
    content1: @Composable () -> Unit,
    content2: @Composable () -> Unit,
) {
    // We can provide pass a list of two composable lambdas in order to be able to treat
    // measureables from each lambda differently.
    Layout(listOf(content1, content2)) { (content1Measurables, content2Measurables), constraints ->
        val content1Placeables = content1Measurables.map { it.measure(constraints) }
        val content2Placeables = content2Measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            var currentX = 0
            var currentY = 0
            var currentMaxHeight = 0
            // we place placeables from content1 as a first line
            content1Placeables.forEach {
                it.place(currentX, currentY)
                currentX += it.width
                currentMaxHeight = maxOf(currentMaxHeight, it.height)
            }
            currentX = 0
            currentY = currentMaxHeight
            // and placeables from content2 composable as a second line
            content2Placeables.forEach {
                it.place(currentX, currentY)
                currentX += it.width
            }
        }
    }
}
