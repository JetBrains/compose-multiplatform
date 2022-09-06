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

package androidx.compose.ui.layout

import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.getChildrenOfVirtualChildren
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastMap

/**
 * Defines the measure and layout behavior of a [Layout] overload which accepts a list of multiple
 * composable content lambdas.
 *
 * This interface is identical to [MeasurePolicy], but provides you with a list of lists of
 * [Measurable]s which allows to threat children put into different content lambdas differently.
 * Such list has the same size as the list of contents passed into [Layout] and contains the list
 * of [Measurable]s of the corresponding content lambda in the same order.
 *
 * Intrinsic measurement methods define the intrinsic size of the layout. These can be queried
 * by the layout's parent in order to obtain, in specific cases, more information about
 * the size of the layout in the absence of specific constraints:
 * - [minIntrinsicWidth] defines the minimum width this layout can take, given
 *   a specific height, such that the content of the layout will be painted correctly
 * - [minIntrinsicHeight] defines the minimum height this layout can take, given
 *   a specific width, such that the content of the layout will be painted correctly
 * - [maxIntrinsicWidth] defines the minimum width such that increasing it further
 *   will not decrease the minimum intrinsic height
 * - [maxIntrinsicHeight] defines the minimum height such that increasing it further
 *   will not decrease the minimum intrinsic width
 * Most layout scenarios do not require querying intrinsic measurements. Therefore, when writing
 * a custom layout, it is common to only define the actual measurement, as most of the times
 * the intrinsic measurements of the layout will not be queried. Moreover, intrinsic measurement
 * methods have default implementations that make a best effort attempt to calculate the intrinsic
 * measurements by reusing the [measure] method. Note this will not be correct for all layouts,
 * but can be a convenient approximation.
 * Intrinsic measurements can be useful when the layout system enforcement of no more than one
 * measurement per child is limiting. Layouts that use them are the `preferredWidth(IntrinsicSize)`
 * and `preferredHeight(IntrinsicSize)` modifiers. See their samples for when they can be useful.
 *
 * @see Layout
 * @see MeasurePolicy
 */
@Stable
@ExperimentalComposeUiApi
fun interface MultiContentMeasurePolicy {
    /**
     * The function that defines the measurement and layout. Each [Measurable] in the [measurables]
     * lists corresponds to a layout child of the layout, and children can be measured using the
     * [Measurable.measure] method. This method takes the [Constraints] which the child should
     * respect; different children can be measured with different constraints.
     * Measuring a child returns a [Placeable], which reveals the size chosen by the child as a
     * result of its own measurement. According to the children sizes, the parent defines the
     * position of the children, by [placing][Placeable.PlacementScope.place] the [Placeable]s in
     * the [MeasureResult.placeChildren] of the returned [MeasureResult]. Therefore the parent needs
     * to measure its children with appropriate [Constraints], such that whatever valid sizes
     * children choose, they can be laid out correctly according to the parent's layout algorithm.
     * This is because there is no measurement negotiation between the parent and children:
     * once a child chooses its size, the parent needs to handle it correctly.
     *
     * It is identical to [MeasurePolicy.measure], but provides you with a list of lists of
     * [Measurable]s which allows to threat children put into different content lambdas
     * differently. Such list has the same size as the list of contents passed into [Layout] and
     * contains the list of [Measurable]s of the corresponding content lambda in the same order.
     *
     * Note that a child is allowed to choose a size that does not satisfy its constraints. However,
     * when this happens, the placeable's [width][Placeable.width] and [height][Placeable.height]
     * will not represent the real size of the child, but rather the size coerced in the
     * child's constraints. Therefore, it is common for parents to assume in their layout
     * algorithm that its children will always respect the constraints. When this
     * does not happen in reality, the position assigned to the child will be
     * automatically offset to be centered on the space assigned by the parent under
     * the assumption that constraints were respected. Rarely, when a parent really needs to know
     * the true size of the child, they can read this from the placeable's
     * [Placeable.measuredWidth] and [Placeable.measuredHeight].
     *
     * [MeasureResult] objects are usually created using the [MeasureScope.layout]
     * factory, which takes the calculated size of this layout, its alignment lines, and a block
     * defining the positioning of the children layouts.
     */
    fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicWidth]. It represents
     * the minimum width this layout can take, given a specific height, such that the content
     * of the layout can be painted correctly.
     *
     * It is identical to [MeasurePolicy.minIntrinsicWidth], but provides you with a list of
     * lists of [Measurable]s which allows to threat children put into different content lambdas
     * differently. Such list has the same size as the list of contents passed into [Layout] and
     * contains the list of [Measurable]s of the corresponding content lambda in the same order.
     */
    fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int
    ): Int {
        val mapped = measurables.fastMap { list ->
            list.fastMap {
                DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Width)
            }
        }
        val constraints = Constraints(maxHeight = height)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.width
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.minIntrinsicHeight]. It represents
     * the minimum height this layout can take, given a specific width, such that the content
     * of the layout will be painted correctly.
     *
     * It is identical to [MeasurePolicy.minIntrinsicHeight], but provides you with a list of
     * lists of [Measurable]s which allows to threat children put into different content lambdas
     * differently. Such list has the same size as the list of contents passed into [Layout] and
     * contains the list of [Measurable]s of the corresponding content lambda in the same order.
     */
    fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int
    ): Int {
        val mapped = measurables.fastMap { list ->
            list.fastMap {
                DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Min, IntrinsicWidthHeight.Height)
            }
        }
        val constraints = Constraints(maxWidth = width)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.height
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicWidth]. It represents the
     * minimum width such that increasing it further will not decrease the minimum intrinsic height.
     *
     * It is identical to [MeasurePolicy.maxIntrinsicWidth], but provides you with a list of
     * lists of [Measurable]s which allows to threat children put into different content lambdas
     * differently. Such list has the same size as the list of contents passed into [Layout] and
     * contains the list of [Measurable]s of the corresponding content lambda in the same order.
     */
    fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int
    ): Int {
        val mapped = measurables.fastMap { list ->
            list.fastMap {
                DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Width)
            }
        }
        val constraints = Constraints(maxHeight = height)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.width
    }

    /**
     * The function used to calculate [IntrinsicMeasurable.maxIntrinsicHeight]. It represents the
     * minimum height such that increasing it further will not decrease the minimum intrinsic width.
     *
     * It is identical to [MeasurePolicy.maxIntrinsicHeight], but provides you with a list of
     * lists of [Measurable]s which allows to threat children put into different content lambdas
     * differently. Such list has the same size as the list of contents passed into [Layout] and
     * contains the list of [Measurable]s of the corresponding content lambda in the same order.
     */
    fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int
    ): Int {
        val mapped = measurables.fastMap { list ->
            list.fastMap {
                DefaultIntrinsicMeasurable(it, IntrinsicMinMax.Max, IntrinsicWidthHeight.Height)
            }
        }
        val constraints = Constraints(maxWidth = width)
        val layoutReceiver = IntrinsicsMeasureScope(this, layoutDirection)
        val layoutResult = layoutReceiver.measure(mapped, constraints)
        return layoutResult.height
    }
}

@ExperimentalComposeUiApi
@PublishedApi
internal fun createMeasurePolicy(
    // metalava thinks experimental MultiContentMeasurePolicy is hidden b/244423074
    @Suppress("HiddenTypeParameter") measurePolicy: MultiContentMeasurePolicy
): MeasurePolicy =
    with(measurePolicy) {
        object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ) = measure(getChildrenOfVirtualChildren(this), constraints)

            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ) = minIntrinsicWidth(getChildrenOfVirtualChildren(this), height)

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ) = minIntrinsicHeight(getChildrenOfVirtualChildren(this), width)

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ) = maxIntrinsicWidth(getChildrenOfVirtualChildren(this), height)

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ) = maxIntrinsicHeight(getChildrenOfVirtualChildren(this), width)
        }
    }
