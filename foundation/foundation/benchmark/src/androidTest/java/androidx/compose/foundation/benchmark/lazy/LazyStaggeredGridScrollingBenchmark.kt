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

package androidx.compose.foundation.benchmark.lazy

import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyStaggeredGridScrollingBenchmark(
    private val testCase: LazyStaggeredGridScrollingTestCase
) {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun scrollProgrammatically_noNewItems() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun scrollProgrammatically_newItemComposed() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun scrollViaPointerInput_noNewItems() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true
            )
        }
    }

    @Test
    fun scrollViaPointerInput_newItemComposed() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true
            )
        }
    }

    @Test
    fun drawAfterScroll_noNewItems() {
        // this test makes sense only when run on the Android version which supports RenderNodes
        // as this tests how efficiently we move RenderNodes.
        Assume.assumeTrue(supportsRenderNode || supportsMRenderNode)
        benchmarkRule.toggleStateBenchmarkDraw {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun drawAfterScroll_newItemComposed() {
        // this test makes sense only when run on the Android version which supports RenderNodes
        // as this tests how efficiently we move RenderNodes.
        Assume.assumeTrue(supportsRenderNode || supportsMRenderNode)
        benchmarkRule.toggleStateBenchmarkDraw {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<LazyStaggeredGridScrollingTestCase> =
            arrayOf(
                Vertical,
                Horizontal
            )

        // Copied from AndroidComposeTestCaseRunner
        private val supportsRenderNode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        private val supportsMRenderNode = Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}

@OptIn(ExperimentalFoundationApi::class)
class LazyStaggeredGridScrollingTestCase(
    private val name: String,
    val isVertical: Boolean,
    val content: @Composable StaggeredGridRemeasureTestCase.(LazyStaggeredGridState) -> Unit
) {
    override fun toString(): String {
        return name
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val Vertical = LazyStaggeredGridScrollingTestCase(
    "Vertical",
    isVertical = true
) { state ->
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = state,
        modifier = Modifier.requiredHeight(400.dp).fillMaxWidth(),
        flingBehavior = NoFlingBehavior
    ) {
        items(2) {
            FirstLargeItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val Horizontal = LazyStaggeredGridScrollingTestCase(
    "Horizontal",
    isVertical = false
) { state ->
    LazyHorizontalStaggeredGrid(
        rows = StaggeredGridCells.Fixed(2),
        state = state,
        modifier = Modifier.requiredHeight(400.dp).fillMaxWidth(),
        flingBehavior = NoFlingBehavior
    ) {
        items(2) {
            FirstLargeItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
class StaggeredGridRemeasureTestCase(
    val addNewItemOnToggle: Boolean,
    val content: @Composable StaggeredGridRemeasureTestCase.(LazyStaggeredGridState) -> Unit,
    val isVertical: Boolean,
    val usePointerInput: Boolean = false
) : LazyBenchmarkTestCase {

    val items = List(300) { LazyItem(it) }

    private lateinit var state: LazyStaggeredGridState
    private lateinit var view: View
    private lateinit var motionEventHelper: MotionEventHelper
    private var touchSlop: Float = 0f
    private var scrollBy: Int = 0

    @Composable
    fun FirstLargeItem() {
        Box(Modifier.requiredSize(30.dp))
    }

    @Composable
    override fun Content() {
        scrollBy = if (addNewItemOnToggle) {
            with(LocalDensity.current) { 15.dp.roundToPx() }
        } else {
            5
        }
        view = LocalView.current
        if (!::motionEventHelper.isInitialized) motionEventHelper = MotionEventHelper(view)
        touchSlop = LocalViewConfiguration.current.touchSlop
        state = rememberLazyStaggeredGridState()
        content(state)
    }

    @Composable
    fun RegularItem() {
        Box(Modifier.requiredSize(20.dp).background(Color.Red, RoundedCornerShape(8.dp)))
    }

    override fun beforeToggle() {
        runBlocking {
            state.scrollToItem(0, 0)
        }
        if (usePointerInput) {
            val size = if (isVertical) view.measuredHeight else view.measuredWidth
            motionEventHelper.sendEvent(MotionEvent.ACTION_DOWN, (size / 2f).toSingleAxisOffset())
            motionEventHelper.sendEvent(MotionEvent.ACTION_MOVE, touchSlop.toSingleAxisOffset())
        }
        assertEquals(0, state.firstVisibleItemIndex)
        assertEquals(0, state.firstVisibleItemScrollOffset)
    }

    override fun toggle() {
        if (usePointerInput) {
            motionEventHelper
                .sendEvent(MotionEvent.ACTION_MOVE, -scrollBy.toFloat().toSingleAxisOffset())
        } else {
            runBlocking {
                state.scrollBy(scrollBy.toFloat())
            }
        }
    }

    override fun afterToggle() {
        assertEquals(0, state.firstVisibleItemIndex)
        assertEquals(scrollBy, state.firstVisibleItemScrollOffset)
        if (usePointerInput) {
            motionEventHelper.sendEvent(MotionEvent.ACTION_UP, Offset.Zero)
        }
    }

    private fun Float.toSingleAxisOffset(): Offset =
        Offset(x = if (isVertical) 0f else this, y = if (isVertical) this else 0f)
}
