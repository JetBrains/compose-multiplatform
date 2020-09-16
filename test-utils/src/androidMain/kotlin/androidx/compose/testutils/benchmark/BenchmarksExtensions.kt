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
import androidx.compose.testutils.setupContent

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
 * Measures the time of the first composition of the given compose test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstCompose(caseFactory: () -> ComposeTestCase) {
    runBenchmarkFor(caseFactory) {
        measureRepeated {
            runWithTimingDisabled {
                createTestCase()
            }

            emitContent()

            runWithTimingDisabled {
                assertNoPendingChanges()
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first set content of the given Android test case.
 */
fun AndroidBenchmarkRule.benchmarkFirstSetContent(caseFactory: () -> AndroidTestCase) {
    runBenchmarkFor(caseFactory) {
        measureRepeated {
            setupContent()
            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first measure of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstMeasure(caseFactory: () -> ComposeTestCase) {
    runBenchmarkFor(caseFactory) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
            }

            measure()

            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first measure of the given test case.
 */
fun AndroidBenchmarkRule.benchmarkFirstMeasure(caseFactory: () -> AndroidTestCase) {
    runBenchmarkFor(caseFactory) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
            }

            measure()

            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first layout of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstLayout(caseFactory: () -> ComposeTestCase) {
    runBenchmarkFor(caseFactory) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
                measure()
            }

            layout()

            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first layout of the given test case.
 */
fun AndroidBenchmarkRule.benchmarkFirstLayout(caseFactory: () -> AndroidTestCase) {
    runBenchmarkFor(caseFactory) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
                measure()
            }

            layout()

            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first draw of the given test case.
 */
fun ComposeBenchmarkRule.benchmarkFirstDraw(caseFactory: () -> ComposeTestCase) {
    runBenchmarkFor(caseFactory) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }

            draw()

            runWithTimingDisabled {
                drawFinish()
                disposeContent()
            }
        }
    }
}

/**
 * Measures the time of the first draw of the given test case.
 */
fun AndroidBenchmarkRule.benchmarkFirstDraw(caseFactory: () -> AndroidTestCase) {
    runBenchmarkFor(caseFactory) {
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                requestLayout()
                measure()
                layout()
                drawPrepare()
            }

            draw()

            runWithTimingDisabled {
                drawFinish()
                disposeContent()
            }
        }
    }
}

/**
 *  Measures recomposition time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkRecompose(
    caseFactory: () -> T
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
            }
            recomposeAssertHadChanges()
            assertNoPendingChanges()
        }
    }
}

/**
 *  Measures measure time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkMeasure(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true
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
                assertNoPendingChanges()
            }
            measure()
            assertNoPendingChanges()
        }
    }
}

/**
 *  Measures layout time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkLayout(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true
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
                assertNoPendingChanges()
            }
            layout()
            assertNoPendingChanges()
        }
    }
}

/**
 *  Measures draw time of the hierarchy after changing a state.
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> T,
    toggleCausesRecompose: Boolean = true
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
                if (toggleCausesRecompose) {
                    recomposeAssertHadChanges()
                }
                assertNoPendingChanges()
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
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
    caseFactory: () -> T
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            getTestCase().toggleState()
            recomposeAssertHadChanges()
            assertNoPendingChanges()
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
 */
fun <T> ComposeBenchmarkRule.toggleStateBenchmarkMeasureLayout(
    caseFactory: () -> T
) where T : ComposeTestCase, T : ToggleableTestCase {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
                assertNoPendingChanges()
            }
            measure()
            assertNoPendingChanges()
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
