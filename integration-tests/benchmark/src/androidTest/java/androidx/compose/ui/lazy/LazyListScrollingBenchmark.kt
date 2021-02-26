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

package androidx.compose.ui.lazy

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.runBlocking
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
    fun measure_notAddingNewItemsAsResult() {
        benchmarkRule.toggleStateBenchmarkMeasure {
            ListRemeasureTestCase(false, testCase.content)
        }
    }

    @Test
    fun measure_addingNewItemAsResult() {
        benchmarkRule.toggleStateBenchmarkMeasure {
            ListRemeasureTestCase(true, testCase.content)
        }
    }

    // this test makes sense only when run on the Android version which supports RenderNodes
    // as this tests how efficiently we move RenderNodes.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun draw_notAddingNewItemsAsResult() {
        benchmarkRule.toggleStateBenchmarkDraw {
            ListRemeasureTestCase(false, testCase.content)
        }
    }

    // this test makes sense only when run on the Android version which supports RenderNodes
    // as this tests how efficiently we move RenderNodes.
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun draw_addingNewItemAsResult() {
        benchmarkRule.toggleStateBenchmarkDraw {
            ListRemeasureTestCase(true, testCase.content)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<LazyListScrollingTestCase> =
            arrayOf(
                LazyColumnWithItemAndItems,
                LazyColumnWithItems,
                LazyColumnWithItemsIndexed,
                LazyRowWithItemAndItems,
                LazyRowWithItems,
                LazyRowWithItemsIndexed
            )
    }
}

class LazyListScrollingTestCase(
    private val name: String,
    val content: @Composable ListRemeasureTestCase.(LazyListState) -> Unit
) {
    override fun toString(): String {
        return name
    }
}

private val LazyColumnWithItemAndItems = LazyListScrollingTestCase(
    "LazyColumnWithItemAndItems"
) { state ->
    LazyColumn(state = state, modifier = Modifier.requiredHeight(400.dp).fillMaxWidth()) {
        item {
            FirstLargeItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

private val LazyColumnWithItems = LazyListScrollingTestCase(
    "LazyColumnWithItems"
) { state ->
    LazyColumn(state = state, modifier = Modifier.requiredHeight(400.dp).fillMaxWidth()) {
        items(items) {
            if (it.index == 0) {
                FirstLargeItem()
            } else {
                RegularItem()
            }
        }
    }
}

private val LazyColumnWithItemsIndexed = LazyListScrollingTestCase(
    "LazyColumnWithItemsIndexed"
) { state ->
    LazyColumn(state = state, modifier = Modifier.requiredHeight(400.dp).fillMaxWidth()) {
        itemsIndexed(items) { index, _ ->
            if (index == 0) {
                FirstLargeItem()
            } else {
                RegularItem()
            }
        }
    }
}

private val LazyRowWithItemAndItems = LazyListScrollingTestCase(
    "LazyRowWithItemAndItems"
) { state ->
    LazyRow(state = state, modifier = Modifier.requiredWidth(400.dp).fillMaxHeight()) {
        item {
            FirstLargeItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

private val LazyRowWithItems = LazyListScrollingTestCase(
    "LazyRowWithItems"
) { state ->
    LazyRow(state = state, modifier = Modifier.requiredWidth(400.dp).fillMaxHeight()) {
        items(items) {
            if (it.index == 0) {
                FirstLargeItem()
            } else {
                RegularItem()
            }
        }
    }
}

private val LazyRowWithItemsIndexed = LazyListScrollingTestCase(
    "LazyRowWithItemsIndexed"
) { state ->
    LazyRow(state = state, modifier = Modifier.requiredWidth(400.dp).fillMaxHeight()) {
        itemsIndexed(items) { index, _ ->
            if (index == 0) {
                FirstLargeItem()
            } else {
                RegularItem()
            }
        }
    }
}

// TODO(b/169852102 use existing public constructs instead)
private fun ComposeBenchmarkRule.toggleStateBenchmarkMeasure(
    caseFactory: () -> ListRemeasureTestCase
) {
    runBenchmarkFor(caseFactory) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().prepareForToggle()
                assertNoPendingChanges()
            }
            getTestCase().toggle()
            runWithTimingDisabled {
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
                getTestCase().prepareForToggle()
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
                drawFinish()
            }
        }
    }
}

class ListRemeasureTestCase(
    val addNewItemOnToggle: Boolean,
    val content: @Composable ListRemeasureTestCase.(LazyListState) -> Unit
) : ComposeTestCase {

    val items = List(100) { ListItem(it) }

    private lateinit var listState: LazyListState
    private lateinit var density: Density

    @Composable
    fun FirstLargeItem() {
        Box(Modifier.requiredSize(30.dp))
    }

    @Composable
    override fun Content() {
        density = LocalDensity.current
        listState = rememberLazyListState()
        content(listState)
    }

    @Composable
    fun RegularItem() {
        Box(Modifier.requiredSize(20.dp).background(Color.Red, RoundedCornerShape(8.dp)))
    }

    fun prepareForToggle() {
        if (addNewItemOnToggle && listState.firstVisibleItemScrollOffset != 0) {
            runBlocking {
                listState.scrollToItem(0, 0)
            }
        }
    }

    fun toggle() {
        val scrollTo = if (addNewItemOnToggle) {
            with(density) { 15.dp.roundToPx() }
        } else {
            if (listState.firstVisibleItemScrollOffset == 5) {
                0
            } else {
                5
            }
        }
        runBlocking {
            listState.scrollToItem(0, scrollTo)
        }
    }
}

data class ListItem(val index: Int)
