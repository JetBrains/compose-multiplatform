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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
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

@LargeTest
@RunWith(Parameterized::class)
class LazyListScrollingBenchmark(
    private val testCase: LazyListScrollingTestCase
) {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun scrollProgrammatically_noNewItems() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun scrollProgrammatically_newItemComposed() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun scrollViaPointerInput_noNewItems() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
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
            ListRemeasureTestCase(
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
            ListRemeasureTestCase(
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
            ListRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<LazyListScrollingTestCase> =
            arrayOf(
                LazyColumn,
                LazyRow
            )

        // Copied from AndroidComposeTestCaseRunner
        private val supportsRenderNode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        private val supportsMRenderNode = Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}

class LazyListScrollingTestCase(
    private val name: String,
    val isVertical: Boolean,
    val content: @Composable ListRemeasureTestCase.(LazyListState) -> Unit
) {
    override fun toString(): String {
        return name
    }
}

private val LazyColumn = LazyListScrollingTestCase(
    "LazyColumn",
    isVertical = true
) { state ->
    LazyColumn(
        state = state,
        modifier = Modifier.requiredHeight(400.dp).fillMaxWidth(),
        flingBehavior = NoFlingBehavior
    ) {
        item {
            FirstLargeItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

private val LazyRow = LazyListScrollingTestCase(
    "LazyRow",
    isVertical = false
) { state ->
    LazyRow(
        state = state,
        modifier = Modifier.requiredWidth(400.dp).fillMaxHeight(),
        flingBehavior = NoFlingBehavior
    ) {
        item {
            FirstLargeItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

private object NoFlingBehavior : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return 0f
    }
}

// TODO(b/169852102 use existing public constructs instead)
private fun ComposeBenchmarkRule.toggleStateBenchmark(
    caseFactory: () -> ListRemeasureTestCase
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
private fun ComposeBenchmarkRule.toggleStateBenchmarkDraw(
    caseFactory: () -> ListRemeasureTestCase
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

class ListRemeasureTestCase(
    val addNewItemOnToggle: Boolean,
    val content: @Composable ListRemeasureTestCase.(LazyListState) -> Unit,
    val isVertical: Boolean,
    val usePointerInput: Boolean = false
) : ComposeTestCase {

    val items = List(100) { ListItem(it) }

    private lateinit var listState: LazyListState
    private lateinit var view: View
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
        touchSlop = LocalViewConfiguration.current.touchSlop
        listState = rememberLazyListState()
        content(listState)
    }

    @Composable
    fun RegularItem() {
        Box(Modifier.requiredSize(20.dp).background(Color.Red, RoundedCornerShape(8.dp)))
    }

    fun beforeToggle() {
        runBlocking {
            listState.scrollToItem(0, 0)
        }
        if (usePointerInput) {
            val size = if (isVertical) view.measuredHeight else view.measuredWidth
            sendEvent(MotionEvent.ACTION_DOWN, size / 2f)
            sendEvent(MotionEvent.ACTION_MOVE, touchSlop)
        }
        assertEquals(0, listState.firstVisibleItemIndex)
        assertEquals(0, listState.firstVisibleItemScrollOffset)
    }

    fun toggle() {
        if (usePointerInput) {
            sendEvent(MotionEvent.ACTION_MOVE, -scrollBy.toFloat())
        } else {
            runBlocking {
                listState.scrollBy(scrollBy.toFloat())
            }
        }
    }

    fun afterToggle() {
        assertEquals(0, listState.firstVisibleItemIndex)
        assertEquals(scrollBy, listState.firstVisibleItemScrollOffset)
        if (usePointerInput) {
            sendEvent(MotionEvent.ACTION_UP, 0f)
        }
    }

    private var time = 0L
    private var lastCoord: Float? = null

    private fun sendEvent(
        action: Int,
        delta: Float
    ) {
        time += 10L

        val coord = delta + (lastCoord ?: 0f)

        if (action == MotionEvent.ACTION_UP) {
            lastCoord = null
        } else {
            lastCoord = coord
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
                    this.x = locationOnScreen[0] + if (!isVertical) coord else 1f
                    this.y = locationOnScreen[1] + if (isVertical) coord else 1f
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

data class ListItem(val index: Int)
