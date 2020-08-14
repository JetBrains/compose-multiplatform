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

package androidx.ui.benchmark.test

import androidx.compose.foundation.layout.InnerPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.benchmarkDrawPerf
import androidx.ui.benchmark.benchmarkFirstCompose
import androidx.ui.benchmark.benchmarkFirstDraw
import androidx.ui.benchmark.benchmarkFirstLayout
import androidx.ui.benchmark.benchmarkFirstMeasure
import androidx.ui.benchmark.benchmarkLayoutPerf
import androidx.ui.benchmark.toggleStateBenchmarkDraw
import androidx.ui.benchmark.toggleStateBenchmarkLayout
import androidx.ui.benchmark.toggleStateBenchmarkMeasure
import androidx.ui.benchmark.toggleStateBenchmarkRecompose
import androidx.ui.test.ToggleableTestCase
import androidx.ui.test.ComposeTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class PaddingBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val noModifierCaseFactory = { NoModifierTestCase() }
    private val modifierCaseFactory = { ModifierTestCase() }

    @Test
    fun noModifier_first_compose() {
        benchmarkRule.benchmarkFirstCompose(noModifierCaseFactory)
    }

    @Test
    fun noModifier_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(noModifierCaseFactory)
    }

    @Test
    fun noModifier_first_layout() {
        benchmarkRule.benchmarkFirstLayout(noModifierCaseFactory)
    }

    @Test
    fun noModifier_first_draw() {
        benchmarkRule.benchmarkFirstDraw(noModifierCaseFactory)
    }

    @Test
    fun noModifier_togglePadding_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(noModifierCaseFactory)
    }

    @Test
    fun noModifier_togglePadding_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(noModifierCaseFactory)
    }

    @Test
    fun noModifier_togglePadding_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(noModifierCaseFactory)
    }

    @Test
    fun noModifier_togglePadding_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(noModifierCaseFactory)
    }

    @Test
    fun noModifier_layout() {
        benchmarkRule.benchmarkLayoutPerf(noModifierCaseFactory)
    }

    @Test
    fun noModifier_draw() {
        benchmarkRule.benchmarkDrawPerf(noModifierCaseFactory)
    }

    @Test
    fun modifier_first_compose() {
        benchmarkRule.benchmarkFirstCompose(modifierCaseFactory)
    }

    @Test
    fun modifier_first_measure() {
        benchmarkRule.benchmarkFirstMeasure(modifierCaseFactory)
    }

    @Test
    fun modifier_first_layout() {
        benchmarkRule.benchmarkFirstLayout(modifierCaseFactory)
    }

    @Test
    fun modifier_first_draw() {
        benchmarkRule.benchmarkFirstDraw(modifierCaseFactory)
    }

    @Test
    fun modifier_togglePadding_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(modifierCaseFactory)
    }

    @Test
    fun modifier_togglePadding_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(modifierCaseFactory)
    }

    @Test
    fun modifier_togglePadding_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(modifierCaseFactory)
    }

    @Test
    fun modifier_togglePadding_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(modifierCaseFactory)
    }

    @Test
    fun modifier_layout() {
        benchmarkRule.benchmarkLayoutPerf(modifierCaseFactory)
    }

    @Test
    fun modifier_draw() {
        benchmarkRule.benchmarkDrawPerf(modifierCaseFactory)
    }
}

private sealed class PaddingTestCase : ComposeTestCase, ToggleableTestCase {

    var paddingState: MutableState<Dp>? = null

    override fun toggleState() {
        with(paddingState!!) {
            value = if (value == 5.dp) 10.dp else 5.dp
        }
    }

    @Composable
    override fun emitContent() {
        val padding = remember { mutableStateOf(5.dp) }
        paddingState = padding

        FillerContainer {
            emitPaddedContainer(padding.value) {
                emitPaddedContainer(padding.value) {
                    emitPaddedContainer(padding.value) {
                        emitPaddedContainer(padding.value) {
                            emitPaddedContainer(padding.value) {}
                        }
                    }
                }
            }
        }
    }

    @Composable
    abstract fun emitPaddedContainer(padding: Dp, child: @Composable () -> Unit)
}

@Composable
fun FillerContainer(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(children, modifier) { measurable, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeable = measurable.firstOrNull()?.measure(childConstraints)
        val width =
            if (constraints.hasBoundedWidth) constraints.maxWidth else placeable?.width ?: 0
        val height =
            if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                placeable?.height ?: 0
            }
        layout(width, height) {
            placeable?.placeRelative(0, 0)
        }
    }
}

private class ModifierTestCase : PaddingTestCase() {

    @Composable
    override fun emitPaddedContainer(padding: Dp, child: @Composable () -> Unit) {
        FillerContainer(Modifier.padding(padding), child)
    }
}

private class NoModifierTestCase : PaddingTestCase() {

    @Composable
    override fun emitPaddedContainer(padding: Dp, child: @Composable () -> Unit) {
        FillerContainer {
            Padding(all = padding, children = child)
        }
    }
}

// The Padding composable function has been removed in favour of modifier. Keeping this private
// implementation to benchmark it against a modifier.
@Composable
private fun Padding(
    all: Dp,
    children: @Composable () -> Unit
) {
    val padding = InnerPadding(all)
    Layout(children) { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) { }
        } else {
            val paddingLeft = padding.start.toIntPx()
            val paddingTop = padding.top.toIntPx()
            val paddingRight = padding.end.toIntPx()
            val paddingBottom = padding.bottom.toIntPx()
            val horizontalPadding = (paddingLeft + paddingRight)
            val verticalPadding = (paddingTop + paddingBottom)

            val newConstraints = constraints.offset(-horizontalPadding, -verticalPadding)
            val placeable = measurable.measure(newConstraints)
            val width = (placeable.width + horizontalPadding).coerceAtMost(constraints.maxWidth)
            val height = (placeable.height + verticalPadding).coerceAtMost(constraints.maxHeight)

            layout(width, height) {
                placeable.placeRelative(paddingLeft, paddingTop)
            }
        }
    }
}
