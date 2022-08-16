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

package androidx.compose.foundation.benchmark

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.benchmark.lazy.MotionEventHelper
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class OverscrollBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val overscrollTestCase = { OverscrollTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(overscrollTestCase)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(overscrollTestCase)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(overscrollTestCase)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(overscrollTestCase)
    }

    @Test
    fun overscroll_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(overscrollTestCase, false)
    }

    @Test
    fun overscroll_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(overscrollTestCase, false)
    }

    @Test
    fun overscroll_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(overscrollTestCase, false)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(overscrollTestCase)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(overscrollTestCase)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class OverscrollTestCase : LayeredComposeTestCase(), ToggleableTestCase {

    private lateinit var view: View
    private lateinit var motionEventHelper: MotionEventHelper

    private var showingOverscroll = false

    @Composable
    override fun MeasuredContent() {
        view = LocalView.current
        if (!::motionEventHelper.isInitialized) motionEventHelper = MotionEventHelper(view)
        val scrollState = rememberScrollState()
        val overscrollEffect = ScrollableDefaults.overscrollEffect().apply { isEnabled = true }
        Box(
            Modifier
                .scrollable(
                    scrollState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = overscrollEffect
                )
                .overscroll(overscrollEffect)
                .fillMaxSize()
        ) {
            Box(
                Modifier
                    .offset { IntOffset(0, scrollState.value) }
                    .background(color = Color.Red)
                    .fillMaxWidth()
                    .height(100.dp))
        }
    }

    override fun toggleState() {
        if (!showingOverscroll) {
            val height = view.measuredHeight
            motionEventHelper.sendEvent(MotionEvent.ACTION_DOWN, Offset(x = 0f, y = height / 4f))
            motionEventHelper.sendEvent(MotionEvent.ACTION_MOVE, Offset(x = 0f, y = height / 8f))
            showingOverscroll = true
        } else {
            motionEventHelper.sendEvent(MotionEvent.ACTION_UP, Offset.Zero)
            showingOverscroll = false
        }
    }
}
