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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.lazy.LazyColumnForIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyRowFor
import androidx.compose.foundation.lazy.LazyRowForIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
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

    @Test
    fun draw_notAddingNewItemsAsResult() {
        benchmarkRule.toggleStateBenchmarkDraw {
            ListRemeasureTestCase(false, testCase.content)
        }
    }

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
                LazyColumnFor,
                LazyColumnForIndexed,
                LazyRowWithItemAndItems,
                LazyRowWithItems,
                LazyRowWithItemsIndexed,
                LazyRowFor,
                LazyRowForIndexed
            )
    }
}

class LazyListScrollingTestCase(
    private val name: String,
    val content: @Composable ListRemeasureTestCase.() -> Unit
) {
    override fun toString(): String {
        return name
    }
}

private val LazyColumnWithItemAndItems = LazyListScrollingTestCase("LazyColumnWithItemAndItems") {
    LazyColumn(modifier = Modifier.height(400.dp).fillMaxWidth()) {
        item {
            RemeasurableItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

private val LazyColumnWithItems = LazyListScrollingTestCase("LazyColumnWithItems") {
    LazyColumn(modifier = Modifier.height(400.dp).fillMaxWidth()) {
        items(items) {
            if (it.index == 0) {
                RemeasurableItem()
            } else {
                RegularItem()
            }
        }
    }
}

private val LazyColumnWithItemsIndexed = LazyListScrollingTestCase("LazyColumnWithItemsIndexed") {
    LazyColumn(modifier = Modifier.height(400.dp).fillMaxWidth()) {
        itemsIndexed(items) { index, _ ->
            if (index == 0) {
                RemeasurableItem()
            } else {
                RegularItem()
            }
        }
    }
}

private val LazyColumnFor = LazyListScrollingTestCase("LazyColumnFor") {
    LazyColumnFor(items, modifier = Modifier.height(400.dp).fillMaxWidth()) {
        if (it.index == 0) {
            RemeasurableItem()
        } else {
            RegularItem()
        }
    }
}

private val LazyColumnForIndexed = LazyListScrollingTestCase("LazyColumnForIndexed") {
    LazyColumnForIndexed(items, modifier = Modifier.height(400.dp).fillMaxWidth()) { index, _ ->
        if (index == 0) {
            RemeasurableItem()
        } else {
            RegularItem()
        }
    }
}

private val LazyRowWithItemAndItems = LazyListScrollingTestCase("LazyRowWithItemAndItems") {
    LazyRow(modifier = Modifier.width(400.dp).fillMaxHeight()) {
        item {
            RemeasurableItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

private val LazyRowWithItems = LazyListScrollingTestCase("LazyRowWithItems") {
    LazyRow(modifier = Modifier.width(400.dp).fillMaxHeight()) {
        items(items) {
            if (it.index == 0) {
                RemeasurableItem()
            } else {
                RegularItem()
            }
        }
    }
}

private val LazyRowWithItemsIndexed = LazyListScrollingTestCase("LazyRowWithItemsIndexed") {
    LazyRow(modifier = Modifier.width(400.dp).fillMaxHeight()) {
        itemsIndexed(items) { index, _ ->
            if (index == 0) {
                RemeasurableItem()
            } else {
                RegularItem()
            }
        }
    }
}

private val LazyRowFor = LazyListScrollingTestCase("LazyRowFor") {
    LazyRowFor(items, modifier = Modifier.width(400.dp).fillMaxHeight()) {
        if (it.index == 0) {
            RemeasurableItem()
        } else {
            RegularItem()
        }
    }
}

private val LazyRowForIndexed = LazyListScrollingTestCase("LazyRowForIndexed") {
    LazyRowForIndexed(items, modifier = Modifier.width(400.dp).fillMaxHeight()) { index, _ ->
        if (index == 0) {
            RemeasurableItem()
        } else {
            RegularItem()
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
            assertNoPendingChanges()
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
    val content: @Composable ListRemeasureTestCase.() -> Unit
) : ComposeTestCase {

    private var size = 25.dp
    val items = List(100) { ListItem(it) }

    private var remeasurement: Remeasurement? = null

    @Composable
    fun RemeasurableItem() {
        Layout(
            emptyContent(),
            modifier = object : RemeasurementModifier {
                override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                    this@ListRemeasureTestCase.remeasurement = remeasurement
                }
            }
        ) { _, _ ->
            val size = size.toIntPx()
            layout(size, size) {}
        }
    }

    @Composable
    override fun Content() {
        content()
    }

    @Composable
    fun RegularItem() {
        Box(Modifier.graphicsLayer().size(20.dp).background(Color.Red, RoundedCornerShape(8.dp)))
    }

    fun prepareForToggle() {
        if (addNewItemOnToggle && size != 25.dp) {
            size = 25.dp
            remeasurement!!.forceRemeasure()
        }
    }

    fun toggle() {
        if (addNewItemOnToggle) {
            size = 15.dp
        } else {
            if (size == 25.dp) {
                size = 24.dp
            } else {
                size = 25.dp
            }
        }
        remeasurement!!.forceRemeasure()
    }
}

data class ListItem(val index: Int)
