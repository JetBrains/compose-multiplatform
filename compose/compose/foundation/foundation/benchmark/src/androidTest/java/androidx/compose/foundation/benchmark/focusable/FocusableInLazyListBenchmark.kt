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

package androidx.compose.foundation.benchmark.focusable

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.recomposeUntilNoChangesPending
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FocusableInLazyListBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val itemCount = 10000

    @Test
    fun lazyListWithNoFocusTarget() {
        benchmarkRule.toggleAndMeasureRepeated { state ->
            LazyRow(state = state) {
                items(itemCount) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                    ) {
                        Text("Hello There")
                    }
                }
            }
        }
    }

    @Test
    fun lazyListWithFocusTarget() {
        benchmarkRule.toggleAndMeasureRepeated { state ->
            LazyRow(state = state) {
                items(itemCount) {
                    Box(
                        modifier = Modifier
                            .focusable()
                            .size(100.dp)
                    ) {
                        Text("Hello There")
                    }
                }
            }
        }
    }

    private fun ComposeBenchmarkRule.toggleAndMeasureRepeated(
        content: @Composable (LazyListState) -> Unit
    ) {
        runBenchmarkFor({
            LazyListTestCase(itemCount) { state -> content(state) }
        }) {
            measureRepeated {
                runWithTimingDisabled {
                    doFramesUntilNoChangesPending()
                    getTestCase().toggleState()
                }

                recomposeUntilNoChangesPending(100)
                requestLayout()
                measure()
                layout()
                runWithTimingDisabled { drawPrepare() }
                draw()
                runWithTimingDisabled {
                    drawFinish()
                    assertNoPendingChanges()
                    disposeContent()
                }
            }
        }
    }

    private class LazyListTestCase(
        val count: Int,
        val content: @Composable (LazyListState) -> Unit
    ) : ToggleableTestCase, ComposeTestCase {
        lateinit var state: LazyListState
        lateinit var scope: CoroutineScope
        var isScrolled = false

        @Composable
        override fun Content() {
            state = rememberLazyListState()
            scope = rememberCoroutineScope()
            content(state)
        }

        override fun toggleState() {
            scope.launch {
                state.scrollToItem(if (isScrolled) 0 else count - 1)
                isScrolled = !isScrolled
            }
        }
    }
}
