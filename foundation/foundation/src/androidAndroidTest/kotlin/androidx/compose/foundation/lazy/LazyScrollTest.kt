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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.TestUiDispatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.roundToInt

@MediumTest
@OptIn(ExperimentalTesting::class)
@RunWith(Parameterized::class)
class LazyScrollTest(private val orientation: Orientation) {
    private val LazyListTag = "LazyListTag"

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
        withContext(TestUiDispatcher.Main) {
            state.snapToItemIndex(3)
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @ExperimentalFoundationApi
    @Test
    fun smoothScrollByTest() = runBlocking {
        withContext(TestUiDispatcher.Main) {
            state.smoothScrollBy(with(rule.density) { 320.dp.toPx() })
        }
        assertThat(state.firstVisibleItemIndex).isEqualTo(3)
        assertThat(state.firstVisibleItemScrollOffset)
            .isEqualTo(with(rule.density) { 17.dp.toPx().roundToInt() })
    }

    @Composable
    private fun TestContent() {
        if (vertical) {
            LazyColumnFor(items, Modifier.preferredHeight(300.dp), state) {
                ItemContent()
            }
        } else {
            LazyRowFor(items, Modifier.preferredWidth(300.dp), state) {
                ItemContent()
            }
        }
    }

    @Composable
    private fun LazyItemScope.ItemContent() {
        val modifier = if (vertical) {
            Modifier.preferredHeight(101.dp)
        } else {
            Modifier.preferredWidth(101.dp)
        }
        Spacer(modifier)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}