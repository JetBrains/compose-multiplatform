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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A composable that defines its own content according to the available space, based on the incoming
 * constraints or the current [LayoutDirection].
 * Example usage:
 * @sample androidx.compose.foundation.layout.samples.BoxWithConstraintsSample
 *
 * The composable will compose the given children, and will position the resulting layout
 * elements in a parent layout which behaves similar to a [Box].
 * The layout will size itself to fit the content, subject to the incoming constraints.
 * When children are smaller than the parent, by default they will be positioned inside
 * the layout according to the [contentAlignment]. For individually specifying the alignments
 * of the children layouts, use the [BoxScope.align] modifier.
 * By default, the content will be measured without the [Box]'s incoming min constraints,
 * unless [propagateMinConstraints] is `true`. As an example, setting [propagateMinConstraints] to
 * `true` can be useful when the [BoxWithConstraints] has content on which modifiers cannot be
 * specified directly and setting a min size on the content of the [BoxWithConstraints] is needed.
 * If [propagateMinConstraints] is set to `true`, the min size set on the [BoxWithConstraints] will
 * also be applied to the content, whereas otherwise the min size will only apply to the
 * [BoxWithConstraints].
 * When the content has more than one layout child the layout children will be stacked one
 * on top of the other (positioned as explained above) in the composition order.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param contentAlignment The default alignment inside the [BoxWithConstraints].
 * @param propagateMinConstraints Whether the incoming min constraints should be passed to content.
 * @param content The content of the [BoxWithConstraints].
 */
@Composable
@UiComposable
fun BoxWithConstraints(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content:
        @Composable @UiComposable BoxWithConstraintsScope.() -> Unit
) {
    val measurePolicy = rememberBoxMeasurePolicy(contentAlignment, propagateMinConstraints)
    SubcomposeLayout(modifier) { constraints ->
        val scope = BoxWithConstraintsScopeImpl(this, constraints)
        val measurables = subcompose(Unit) { scope.content() }
        with(measurePolicy) { measure(measurables, constraints) }
    }
}

/**
 * Receiver scope being used by the children parameter of [BoxWithConstraints]
 */
@Stable
interface BoxWithConstraintsScope : BoxScope {
    /**
     * The constraints given by the parent layout in pixels.
     *
     * Use [minWidth], [maxWidth], [minHeight] or [maxHeight] if you need value in [Dp].
     */
    val constraints: Constraints
    /**
     * The minimum width in [Dp].
     *
     * @see constraints for the values in pixels.
     */
    val minWidth: Dp
    /**
     * The maximum width in [Dp].
     *
     * @see constraints for the values in pixels.
     */
    val maxWidth: Dp
    /**
     * The minimum height in [Dp].
     *
     * @see constraints for the values in pixels.
     */
    val minHeight: Dp
    /**
     * The maximum height in [Dp].
     *
     * @see constraints for the values in pixels.
     */
    val maxHeight: Dp
}

private data class BoxWithConstraintsScopeImpl(
    private val density: Density,
    override val constraints: Constraints
) : BoxWithConstraintsScope, BoxScope by BoxScopeInstance {
    override val minWidth: Dp get() = with(density) { constraints.minWidth.toDp() }
    override val maxWidth: Dp get() = with(density) {
        if (constraints.hasBoundedWidth) constraints.maxWidth.toDp() else Dp.Infinity
    }
    override val minHeight: Dp get() = with(density) { constraints.minHeight.toDp() }
    override val maxHeight: Dp get() = with(density) {
        if (constraints.hasBoundedHeight) constraints.maxHeight.toDp() else Dp.Infinity
    }
}
