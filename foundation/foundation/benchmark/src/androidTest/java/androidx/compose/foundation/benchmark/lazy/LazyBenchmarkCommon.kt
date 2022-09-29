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

package androidx.compose.foundation.benchmark.lazy

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.geometry.Offset

internal object NoFlingBehavior : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return 0f
    }
}

data class LazyItem(val index: Int)

/**
 * Helper for dispatching simple [MotionEvent]s to a [view] for use in scrolling benchmarks.
 */
class MotionEventHelper(private val view: View) {
    private var time = 0L
    private var lastCoord: Offset? = null

    fun sendEvent(
        action: Int,
        delta: Offset
    ) {
        time += 10L

        val coord = delta + (lastCoord ?: Offset.Zero)

        lastCoord = if (action == MotionEvent.ACTION_UP) {
            null
        } else {
            coord
        }

        val locationOnScreen = IntArray(2) { 0 }
        view.getLocationOnScreen(locationOnScreen)

        val motionEvent = MotionEvent.obtain(
            0,
            time,
            action,
            1,
            arrayOf(MotionEvent.PointerProperties()),
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = locationOnScreen[0] + coord.x.coerceAtLeast(1f)
                    y = locationOnScreen[1] + coord.y.coerceAtLeast(1f)
                }
            ),
            0,
            0,
            0f,
            0f,
            0,
            0,
            0,
            0
        ).apply {
            offsetLocation(-locationOnScreen[0].toFloat(), -locationOnScreen[1].toFloat())
        }

        view.dispatchTouchEvent(motionEvent)
    }
}

// TODO(b/169852102 use existing public constructs instead)
internal fun ComposeBenchmarkRule.toggleStateBenchmark(
    caseFactory: () -> LazyBenchmarkTestCase
) {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                assertNoPendingChanges()
                getTestCase().beforeToggle()
                if (hasPendingChanges()) {
                    doFrame()
                }
                assertNoPendingChanges()
            }
            getTestCase().toggle()
            if (hasPendingChanges()) {
                doFrame()
            }
            runWithTimingDisabled {
                assertNoPendingChanges()
                getTestCase().afterToggle()
                assertNoPendingChanges()
            }
        }
    }
}

// TODO(b/169852102 use existing public constructs instead)
internal fun ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> LazyBenchmarkTestCase
) {
    runBenchmarkFor(caseFactory) {
        doFrame()

        measureRepeated {
            runWithTimingDisabled {
                // reset the state and draw
                getTestCase().beforeToggle()
                measure()
                layout()
                drawPrepare()
                draw()
                drawFinish()
                // toggle and prepare measuring draw
                getTestCase().toggle()
                measure()
                layout()
                drawPrepare()
            }
            draw()
            runWithTimingDisabled {
                getTestCase().afterToggle()
                drawFinish()
            }
        }
    }
}

interface LazyBenchmarkTestCase : ComposeTestCase {
    fun beforeToggle()
    fun toggle()
    fun afterToggle()
}
