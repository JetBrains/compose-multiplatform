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

@file:Suppress("DEPRECATION_ERROR")

package androidx.compose.ui.benchmark

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ModifiersBenchmark(
    val name: String,
    val count: Int,
    val modifierFn: (Boolean) -> Modifier
) {
    companion object {
        /**
         * INSTRUCTIONS FOR ADDING A MODIFIER TO THIS LIST
         * ===============================================
         *
         * To add a modifier, add a `*modifier(...)` line which includes:
         *      (1) the name of the modifier
         *      (2) a lambda which accepts a boolean value and returns the modifier. You should use
         *          the passed in boolean value to toggle between two different sets of parameters
         *          to the modifier chain. If the modifier takes no parameters, just ignore the
         *          boolean.
         *
         * Many modifiers require parameters that are objects that need to be allocated. If this is
         * an object which the developer is expected to remember or hold on to somewhere in their
         * composition, you should allocate one below on this companion object and then just
         * reference it in the modifier lambda so that allocating these isn't included in the
         * "recomposition" time.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}_{1}x")
        fun data(): Collection<Array<Any>> = listOf(
            *modifier("Modifier") { Modifier },
            *modifier("emptyElement", true) { Modifier.emptyElement() },
            *modifier("clickable", true) { Modifier.clickable { capture(it) } },
            *modifier("semantics", true) { Modifier.semantics { capture(it) } },
            *modifier("pointerInput") { Modifier.pointerInput(it) { capture(it) } },
            *modifier("focusable") { Modifier.focusable() },
            *modifier("drawWithCache") {
                Modifier.drawWithCache {
                    val rectSize = if (it) size / 2f else size
                    onDrawBehind {
                        drawRect(Color.Black, size = rectSize)
                    }
                }
            },
            *modifier("testTag") { Modifier.testTag("$it") },
            *modifier("selectableGroup") { Modifier.selectableGroup() },
            *modifier("indication") {
                Modifier.indication(interactionSource, if (it) indication else null)
            },
            *modifier("draggable") {
                Modifier.draggable(
                    draggableState,
                    if (it) Orientation.Vertical else Orientation.Horizontal
                )
            },
            *modifier("hoverable") {
                Modifier.hoverable(interactionSource)
            },
            *modifier("scrollable") {
                Modifier.scrollable(
                    scrollableState,
                    if (it) Orientation.Vertical else Orientation.Horizontal
                )
            },
            *modifier("toggleable") { Modifier.toggleable(it) { capture(it) } },
            *modifier("onFocusEvent") { Modifier.onFocusEvent { capture(it) } },
            *modifier("selectable") { Modifier.selectable(it) { capture(it) } },
            *modifier("focusTarget", true) { Modifier.focusTarget() },
            *modifier("focusRequester") { Modifier.focusRequester(focusRequester) },
            *modifier("border") {
                Modifier.border(
                    if (it) 4.dp else 2.dp,
                    if (it) Color.Black else Color.Blue,
                    CircleShape
                )
            },
        )

        private val focusRequester = FocusRequester()
        private val interactionSource = MutableInteractionSource()
        private val indication = object : Indication {
            @Composable
            override fun rememberUpdatedInstance(
                interactionSource: InteractionSource
            ): IndicationInstance {
                return object : IndicationInstance {
                    override fun ContentDrawScope.drawIndication() {
                        drawContent()
                    }
                }
            }
        }
        private val draggableState = object : DraggableState {
            override suspend fun drag(
                dragPriority: MutatePriority,
                block: suspend DragScope.() -> Unit
            ) {}

            override fun dispatchRawDelta(delta: Float) {}
        }
        private val scrollableState = ScrollableState { it }

        fun modifier(
            name: String,
            allCounts: Boolean = false,
            modifierFn: (Boolean) -> Modifier
        ): Array<Array<Any>> {
            return if (allCounts) {
                arrayOf(
                    arrayOf(name, 1, modifierFn),
                    arrayOf(name, 10, modifierFn),
                    arrayOf(name, 100, modifierFn)
                )
            } else {
                arrayOf(arrayOf(name, 10, modifierFn))
            }
        }
    }

    @get:Rule
    val rule = ComposeBenchmarkRule()

    /**
     * DEFINITIONS
     * ===========
     *
     * "base"       - means that we are only including cost of composition and cost of setting the
     *                modifier on the layoutnode itself, but excluding Layout/Draw.
     *
     * "full"       - means that we includ all costs from "base", but also including Layout/Draw.
     *
     * "hoisted"    - means that the modifier chain's creation is not included in the benchmark.
     *                The hoisted measurement is representative of a developer "hoisting" the
     *                allocation of the benchmark up into a higher scope than the composable it is
     *                used in so that recomposition doesn't create a new one. The non-hoisted
     *                variants are more representative of developers making modifier chains inline
     *                in the composable body (most common).
     *
     * "reuse"      - means that we change up the parameters of the modifier factory, so we are
     *                effectively measuring how well we are able to "reuse" the state from the
     *                "same" modifier, but with different parameters
     */

    // base cost, including calling the modifier factory
    @Test
    fun base() = rule.measureModifier(
        count = count,
        reuse = false,
        hoistCreation = false,
        includeComposition = true,
        includeLayout = false,
        includeDraw = false,
        modifierFn = modifierFn,
    )

    // base cost. without calling the modifier factory
    @Test
    fun baseHoisted() = rule.measureModifier(
        count = count,
        reuse = false,
        hoistCreation = true,
        includeComposition = true,
        includeLayout = false,
        includeDraw = false,
        modifierFn = modifierFn,
    )

    // base cost. with different parameters (potential for reuse). includes calling the modifier factory.
    @Test
    fun baseReuse() = rule.measureModifier(
        count = count,
        reuse = true,
        hoistCreation = true,
        includeComposition = true,
        includeLayout = false,
        includeDraw = false,
        modifierFn = modifierFn,
    )

    // base cost. with different parameters (potential for reuse). without calling the modifier factory
    @Test
    fun baseReuseHoisted() = rule.measureModifier(
        count = count,
        reuse = true,
        hoistCreation = true,
        includeComposition = true,
        includeLayout = false,
        includeDraw = false,
        modifierFn = modifierFn,
    )

    // base cost + layout/draw, including calling the modifier factory
    @Test
    fun full() = rule.measureModifier(
        count = count,
        reuse = false,
        hoistCreation = false,
        includeComposition = true,
        includeLayout = true,
        includeDraw = true,
        modifierFn = modifierFn,
    )

    // base cost + layout/draw. without calling the modifier factory
    @Test
    fun fullHoisted() = rule.measureModifier(
        count = count,
        reuse = false,
        hoistCreation = true,
        includeComposition = true,
        includeLayout = true,
        includeDraw = true,
        modifierFn = modifierFn,
    )

    // base cost + layout/draw. with different parameters (potential for reuse). includes calling the modifier factory.
    @Test
    fun fullReuse() = rule.measureModifier(
        count = count,
        reuse = true,
        hoistCreation = false,
        includeComposition = true,
        includeLayout = true,
        includeDraw = true,
        modifierFn = modifierFn,
    )

    // base cost + layout/draw. with different parameters (potential for reuse). without calling the modifier factory
    @Test
    fun fullReuseHoisted() = rule.measureModifier(
        count = count,
        reuse = true,
        hoistCreation = true,
        includeComposition = true,
        includeLayout = true,
        includeDraw = true,
        modifierFn = modifierFn,
    )
}

fun Modifier.emptyElement(): Modifier = this then object : Modifier.Element {}
@Suppress("UNUSED_PARAMETER")
fun capture(value: Any?) {}