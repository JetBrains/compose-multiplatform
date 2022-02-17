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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class LazyLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lazyListShowsCombinedItems() {
        val counter = mutableStateOf(0)
        var remeasureCount = 0
        val policy = LazyMeasurePolicy { _, _ ->
            remeasureCount++
            object : MeasureResult {
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()
                override val height: Int = 10
                override val width: Int = 10
                override fun placeChildren() {}
            }
        }
        val itemsProvider = {
            object : LazyLayoutItemsProvider {
                override fun getContent(index: Int): @Composable () -> Unit = {}
                override val itemsCount: Int = 0
                override fun getKey(index: Int) = Unit
                override val keyToIndexMap: Map<Any, Int> = emptyMap()
                override fun getContentType(index: Int): Any? = null
            }
        }

        rule.setContent {
            counter.value // just to trigger recomposition
            LazyLayout(
                itemsProvider = itemsProvider,
                measurePolicy = policy,
                // this will return a new object everytime causing LazyLayout recomposition
                // without causing remeasure
                modifier = Modifier.composed { Modifier }
            )
        }

        rule.runOnIdle {
            assertThat(remeasureCount).isEqualTo(1)
            counter.value++
        }

        rule.runOnIdle {
            assertThat(remeasureCount).isEqualTo(1)
        }
    }
}
