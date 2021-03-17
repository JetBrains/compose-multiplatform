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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.roundToInt

@MediumTest
@RunWith(Parameterized::class)
class LazyScrollTest(private val orientation: Orientation) {
    @get:Rule
    val rule = createComposeRule()

    private val vertical: Boolean
        get() = orientation == Orientation.Vertical

    private val items = (1..20).toList()
    private lateinit var state: LazyListState

    @Before
    fun setup() {
        rule.setContent {
            state = rememberLazyListState()
            TestContent()
        }
    }

    @Test
    fun testSetupWorks() {
        assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun snapToItemTest() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.scrollToItem(3)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @ExperimentalFoundationApi
    @Test
    fun smoothScrollByTest() = runBlocking {
        fun Int.dpToPx(): Int = with(rule.density) { dp.toPx().roundToInt() }
        val scrollDistance = 320.dpToPx()
        val itemSize = 101.dpToPx()

        val expectedIndex = scrollDistance / itemSize // resolves to 3
        val expectedOffset = scrollDistance % itemSize // resolves to ~17.dp.toIntPx()

        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollBy(scrollDistance.toFloat())
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(expectedIndex)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(expectedOffset)
    }

    @Test
    fun smoothScrollToItemTest() = runBlocking {
        withContext(Dispatchers.Main + AutoTestFrameClock()) {
            state.animateScrollToItem(5, 10)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(5)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(10)
    }

    @Composable
    private fun TestContent() {
        if (vertical) {
            LazyColumn(Modifier.height(300.dp), state) {
                items(items) {
                    ItemContent()
                }
            }
        } else {
            LazyRow(Modifier.width(300.dp), state) {
                items(items) {
                    ItemContent()
                }
            }
        }
    }

    @Composable
    private fun ItemContent() {
        val modifier = if (vertical) {
            Modifier.height(101.dp)
        } else {
            Modifier.width(101.dp)
        }
        Spacer(modifier)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
