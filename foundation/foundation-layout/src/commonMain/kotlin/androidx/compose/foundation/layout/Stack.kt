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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.Layout
import androidx.compose.ui.Measurable
import androidx.compose.ui.Modifier
import androidx.compose.ui.ParentDataModifier
import androidx.compose.ui.Placeable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

/**
 * A composable that positions its children relative to its edges.
 * The component is useful for drawing children that overlap. The children will always be
 * drawn in the order they are specified in the body of the [Stack].
 * Use [StackScope.gravity] modifier to define the position of the target element inside the
 * [Stack] box.
 *
 * Example usage:
 *
 * @sample androidx.compose.foundation.layout.samples.SimpleStack
 */
@Composable
fun Stack(
    modifier: Modifier = Modifier,
    children: @Composable StackScope.() -> Unit
) {
    val stackChildren: @Composable () -> Unit = { StackScope().children() }

    Layout(stackChildren, modifier = modifier) { measurables, constraints ->
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        // First measure aligned children to get the size of the layout.
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        (0 until measurables.size).filter { i -> !measurables[i].stretch }.forEach { i ->
            placeables[i] = measurables[i].measure(childConstraints)
        }
        val (stackWidth, stackHeight) = with(placeables.filterNotNull()) {
            Pair(
                max(maxByOrNull { it.width }?.width ?: 0, constraints.minWidth),
                max(maxByOrNull { it.height }?.height ?: 0, constraints.minHeight)
            )
        }

        // Now measure stretch children.
        (0 until measurables.size).filter { i -> measurables[i].stretch }.forEach { i ->
            // infinity check is needed for intrinsic measurements
            val minWidth = if (stackWidth != Constraints.Infinity) stackWidth else 0
            val minHeight = if (stackHeight != Constraints.Infinity) stackHeight else 0
            placeables[i] = measurables[i].measure(
                Constraints(minWidth, stackWidth, minHeight, stackHeight)
            )
        }

        // Position the children.
        layout(stackWidth, stackHeight) {
            (0 until measurables.size).forEach { i ->
                val measurable = measurables[i]
                val childData = measurable.stackChildData
                val placeable = placeables[i]!!

                val position = childData.alignment.align(
                    IntSize(
                        stackWidth - placeable.width,
                        stackHeight - placeable.height
                    ),
                    layoutDirection
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}

/**
 * A StackScope provides a scope for the children of a [Stack].
 */
@LayoutScopeMarker
@Immutable
class StackScope {
    /**
     * Pull the content element to a specific [Alignment] within the [Stack].
     */
    @Stable
    fun Modifier.gravity(align: Alignment) = this.then(StackGravityModifier(align))

    /**
     * Size the element to match the size of the [Stack] after all other content elements have
     * been measured.
     *
     * The element using this modifier does not take part in defining the size of the [Stack].
     * Instead, it matches the size of the [Stack] after all other children (not using
     * matchParentSize() modifier) have been measured to obtain the [Stack]'s size.
     * In contrast, a general-purpose [Modifier.fillMaxSize] modifier, which makes an element
     * occupy all available space, will take part in defining the size of the [Stack]. Consequently,
     * using it for an element inside a [Stack] will make the [Stack] itself always fill the
     * available space.
     */
    @Stable
    fun Modifier.matchParentSize() = this.then(StretchGravityModifier)

    internal companion object {
        @Stable
        val StretchGravityModifier: ParentDataModifier =
            StackGravityModifier(Alignment.Center, true)
    }
}

private data class StackChildData(
    val alignment: Alignment,
    val stretch: Boolean = false
)

private val Measurable.stackChildData: StackChildData
    get() = (parentData as? StackChildData) ?: StackChildData(Alignment.TopStart)
private val Measurable.stretch: Boolean
    get() = stackChildData.stretch

private data class StackGravityModifier(
    val alignment: Alignment,
    val stretch: Boolean = false
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): StackChildData {
        return ((parentData as? StackChildData) ?: StackChildData(alignment, stretch))
    }
}
