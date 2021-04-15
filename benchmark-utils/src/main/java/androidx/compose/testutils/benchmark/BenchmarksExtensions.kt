/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.testutils.benchmark

import android.view.View
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.ui.graphics.Canvas
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.android.AndroidTestCase
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.recomposeAssertHadChanges

/**
 * Measures measure and layout performance of the given test case by toggling measure constraints.
 */
fun ComposeBenchmarkRule.benchmarkLayoutPerf(caseFactory: () -> ComposeTestCase) {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        val width = measuredWidth
        val height = measuredHeight
        var widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        var heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

        requestLayout()
        measureWithSpec(widthSpec, heightSpec)
        layout()

        var lastWidth = measuredWidth
        var lastHeight: Int
        measureRepeated {
            runWithTimingDisabled {
                if (lastWidth == width) {
                    lastWidth = width - 10
                    lastHeight = height - 10
                } else {

                    lastWidth = width
                    lastHeight = height
                }
                widthSpec =
                    View.MeasureSpec.makeMeasureSpec(lastWidth, View.MeasureSpec.EXACTLY)
                heightSpec =
                    View.MeasureSpec.makeMeasureSpec(lastHeight, View.MeasureSpec.EXACTLY)
                requestLayout()
            }
            measureWithSpec(widthSpec, heightSpec)
            layout()
        }
    }
}

fun AndroidBenchmarkRule.benchmarkLayoutPerf(caseFactory: () -> AndroidTestCase) {
    runBenchmarkFor(caseFactory) {
        doFrame()

        val width = measuredWidth
        val height = measuredHeight
        var widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        var heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

        requestLayout()
        measureWithSpec(widthSpec, heightSpec)
        layout()

        var lastWidth = measuredWidth
        var lastHeight: Int
        measureRepeated {
            runWithTimingDisabled {
                if (lastWidth == width) {
                    lastWidth = width - 10
                    lastHeight = height - 10
                } else {

                    lastWidth = width
                    lastHeight = height
                }
                widthSpec =
                    View.MeasureSpec.makeMeasureSpec(lastWidth, View.MeasureSpec.EXACTLY)
                heightSpec =
                    View.MeasureSpec.makeMeasureSpec(lastHeight, View.MeasureSpec.EXACTLY)
                requestLayout()
            }
            measureWithSpec(widthSpec, heightSpec)
            layout()
        }
    }
}

/**
 * Measures draw performance of the given test case by invalidating the view hierarchy.
 */
fun AndroidBenchmarkRule.benchmarkDrawPerf(caseFactory: () -> AndroidTestCase) {
    runBenchmarkFor(caseFactory) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                invalidateViews()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 * Measures draw performance of the given test case by invalidating the view hierarchy.
 */
fun ComposeBenchmarkRule.benchmarkDrawPerf(caseFactory: () -> ComposeTestCase) {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                invalidateViews()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 *  Measures recomposition time of the hierarchy after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first recomposition without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkRecompose(
    caseFactory: () -> T,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
            }
            recomposeAssertHadChanges()
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
        }
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first remeasure without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkMeasure(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                requestLayout()
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
            }
            measure()
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
        }
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first relayout without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkLayout(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                requestLayout()
                measure()
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
            }
            layout()
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
        }
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first redraw without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 */
fun <T> AndroidBenchmarkRule.toggleStateBenchmarkMeasure(
    caseFactory: () -> T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
            }
            measure()
        }
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 */
fun <T> AndroidBenchmarkRule.toggleStateBenchmarkLayout(
    caseFactory: () -> T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
                measure()
            }
            layout()
        }
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 */
fun <T> AndroidBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> T
) where T : AndroidTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
                measure()
                layout()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                drawFinish()
            }
        }
    }
}

/**
 *  Measures recompose, measure and layout time after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first recompose, remeasure and relayout without any pending
 * animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
    caseFactory: () -> T,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            getTestCase().toggleState()
            recomposeAssertHadChanges()
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
            measure()
            layout()
            runWithTimingDisabled {
                drawPrepare()
                draw()
                drawFinish()
            }
        }
    }
}

/**
 *  Measures measure and layout time after changing a state.
 *
 * @param assertOneRecomposition whether the benchmark will fail if there are pending
 * recompositions after the first recomposition. By default this is true to enforce correctness in
 * the benchmark, but for components that have animations after being recomposed this can
 * be turned off to benchmark just the first remeasure and relayout without any pending animations.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkMeasureLayout(
    caseFactory: () -> T,
    assertOneRecomposition: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (assertOneRecomposition) {
                    assertNoPendingChanges()
                }
            }
            measure()
            if (assertOneRecomposition) {
                assertNoPendingChanges()
            }
        }
    }
}

/**
 *  Benchmark a block of code with paint operations on canvas.
 */
inline fun <T> BenchmarkRule.measureRepeatedRecordingCanvas(
    width: Int,
    height: Int,
    crossinline block: BenchmarkRule.(Canvas) -> T
) {
    val capture = DrawCapture()
    measureRepeated {
        val canvas = runWithTimingDisabled {
            capture.beginRecording(width, height)
        }
        block(canvas)
        runWithTimingDisabled {
            capture.endRecording()
        }
    }
}
