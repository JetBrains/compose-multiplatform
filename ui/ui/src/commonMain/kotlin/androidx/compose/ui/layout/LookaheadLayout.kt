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

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.composed
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeWrapper
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

/**
 * [LookaheadLayout] is a Layout that runs a lookahead measure and placement pass to determine the
 * layout. Immediately afterwards, another measure and placement pass will begin, in which the
 * measurement and placement of any layout can be adjusted
 * based on the lookahead results via [LookaheadLayoutScope.intermediateLayout].
 *
 * During the lookahead pass, the layout adjustment logic defined in
 * [LookaheadLayoutScope.intermediateLayout] will be skipped, so that any transient morphing of
 * the layout is not taken into account when predetermining the target layout.
 *
 * Once the lookahead is finished, another measure & layout pass will begin.
 * [LookaheadLayoutScope.intermediateLayout] can be used to create an intermediate layout based on
 * incoming constraints and the lookahead results. This can result in layouts
 * that gradually change their sizes & positions towards the target layout calculated by the
 * lookahead.
 *
 * *Caveat:* [SubcomposeLayout] is not yet supported in [LookaheadLayout]. It will be supported in
 * an upcoming release.
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutSample
 *
 * @param content The children composable to be laid out.
 * @param modifier Modifiers to be applied to the layout.
 * @param measurePolicy The policy defining the measurement and positioning of the layout.
 */
@Suppress("ComposableLambdaParameterPosition")
@ExperimentalComposeUiApi
@UiComposable
@Composable
fun LookaheadLayout(
    content: @Composable @UiComposable LookaheadLayoutScope.() -> Unit,
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
) {
    val materialized = currentComposer.materialize(modifier)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current

    val scope = remember { LookaheadLayoutScopeImpl() }
    ReusableComposeNode<LayoutNode, Applier<Any>>(
        factory = LayoutNode.Constructor,
        update = {
            set(materialized, ComposeUiNode.SetModifier)
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
            set(viewConfiguration, ComposeUiNode.SetViewConfiguration)
            set(scope) { scope ->
                scope.root = innerLayoutNodeWrapper
            }
            init {
                isLookaheadRoot = true
            }
        },
        content = {
            scope.content()
        }
    )
}

/**
 * [LookaheadLayoutScope] provides a receiver scope for all (direct and indirect) child layouts in
 * [LookaheadLayout]. In [LookaheadLayoutScope], the measurement and placement of any layout
 * calculated in the lookahead pass can be observed via [Modifier.intermediateLayout] and
 * [Modifier.onPlaced] respectively.
 *
 * @sample androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
 */
@ExperimentalComposeUiApi
interface LookaheadLayoutScope {

    /**
     * [onPlaced] gets invoked after the parent [LayoutModifier] has been placed
     * and before child [LayoutModifier] is placed. This allows child [LayoutModifier] to adjust
     * its own placement based on its parent.
     *
     * [onPlaced] callback will be invoked with the [LookaheadLayoutCoordinates] of the LayoutNode
     * emitted by [LookaheadLayout] as the first parameter, and the [LookaheadLayoutCoordinates] of
     * this modifier as the second parameter. Given the [LookaheadLayoutCoordinates]s, both
     * lookahead position and current position of this modifier in the [LookaheadLayout]'s
     * coordinates system can be calculated using
     * [LookaheadLayoutCoordinates.localLookaheadPositionOf] and
     * [LookaheadLayoutCoordinates.localPositionOf], respectively.
     */
    fun Modifier.onPlaced(
        onPlaced: (
            lookaheadScopeCoordinates: LookaheadLayoutCoordinates,
            layoutCoordinates: LookaheadLayoutCoordinates
        ) -> Unit
    ): Modifier

    /**
     * Creates an intermediate layout based on target size of the child layout calculated
     * in the lookahead. This allows the intermediate layout to morph the child layout
     * after lookahead through [measure], in which the size of the child layout calculated from the
     * lookahead is provided. [intermediateLayout] does _not_ participate in the lookahead. It is
     * only invoked for retroactively changing the layout based on the lookahead before the layout
     * is drawn.
     *
     * @sample androidx.compose.ui.samples.LookaheadLayoutSample
     */
    fun Modifier.intermediateLayout(
        measure: MeasureScope.(
            measurable: Measurable,
            constraints: Constraints,
            lookaheadSize: IntSize
        ) -> MeasureResult,
    ): Modifier
}

@OptIn(ExperimentalComposeUiApi::class)
internal class LookaheadOnPlacedModifier(
    val callback: (
        lookaheadScopeRootCoordinates: LookaheadLayoutCoordinates,
        coordinates: LookaheadLayoutCoordinates
    ) -> Unit,
    val rootCoordinates: () -> LookaheadLayoutCoordinates,
) : Modifier.Element {

    fun onPlaced(coordinates: LookaheadLayoutCoordinates) {
        callback(rootCoordinates(), coordinates)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class LookaheadLayoutScopeImpl : LookaheadLayoutScope {
    var root: LayoutNodeWrapper? = null
    override fun Modifier.onPlaced(
        onPlaced: (
            lookaheadScopeCoordinates: LookaheadLayoutCoordinates,
            layoutCoordinates: LookaheadLayoutCoordinates
        ) -> Unit
    ): Modifier = composed(
        debugInspectorInfo {
            name = "onPlaced"
            properties["onPlaced"] = onPlaced
        }
    ) {
        this.then(remember {
            LookaheadOnPlacedModifier(onPlaced) {
                root?.run { lookaheadDelegate!!.lookaheadLayoutCoordinates }
                    ?: error("Lookahead root has not been set up yet")
            }
        })
    }

    override fun Modifier.intermediateLayout(
        measure: MeasureScope.(
            measurable: Measurable,
            constraints: Constraints,
            lookaheadSize: IntSize
        ) -> MeasureResult
    ): Modifier = this.then(
        LookaheadIntermediateLayoutModifierImpl(measure,
            debugInspectorInfo {
                name = "intermediateLayout"
                properties["measure"] = measure
            }
        )
    )
}