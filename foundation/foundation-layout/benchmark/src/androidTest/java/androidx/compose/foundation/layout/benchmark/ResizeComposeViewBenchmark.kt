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

package androidx.compose.foundation.layout.benchmark

import android.widget.LinearLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark that runs [RectsInColumnTestCase].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ResizeComposeViewBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val rectsInColumnCaseFactory = { ComposeViewTestCase() }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(rectsInColumnCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(rectsInColumnCaseFactory)
    }

    @Test
    fun toggleSize_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(rectsInColumnCaseFactory)
    }

    @Test
    fun toggleSize_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(rectsInColumnCaseFactory)
    }
}

class ComposeViewTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private var childSize by mutableStateOf(IntSize(300, 400))

    @Composable
    override fun MeasuredContent() {
        with(LocalDensity.current) {
            Box(Modifier.size(childSize.width.toDp(), childSize.height.toDp())) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        val column = LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                        }
                        repeat(10) {
                            val row = ComposeView(context).apply {
                                setContent {
                                    Layout(content = {
                                        with(LocalDensity.current) {
                                            repeat(20) {
                                                Row(Modifier.size(10.toDp())) {
                                                    repeat(10) {
                                                        Box(
                                                            Modifier
                                                                .width(1.toDp())
                                                                .fillMaxHeight()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }) { measurables, constraints ->
                                        val width = constraints.constrainWidth(400)
                                        val height = constraints.constrainWidth(400)
                                        layout(width, height) {
                                            measurables.forEachIndexed { i, m ->
                                                val p = m.measure(Constraints.fixed(10, 10))
                                                p.place(i * 10, 0)
                                            }
                                        }
                                    }
                                }
                            }
                            val layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                0,
                                1f
                            )
                            column.addView(row, layoutParams)
                        }
                        column
                    }
                )
            }
        }
    }

    override fun toggleState() {
        if (childSize.width == 300) {
            childSize = IntSize(400, 300)
        } else {
            childSize = IntSize(300, 400)
        }
    }
}