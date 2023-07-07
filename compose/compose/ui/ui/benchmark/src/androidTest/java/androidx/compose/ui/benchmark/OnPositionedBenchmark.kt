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

package androidx.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class OnPositionedBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun deepHierarchyOnPositioned_layout() {
        benchmarkRule.toggleStateBenchmarkLayout({
            DeepHierarchyOnPositionedTestCase()
        })
    }
}

private class DeepHierarchyOnPositionedTestCase :
    ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<Dp>

    @Composable
    override fun Content() {
        val size = remember { mutableStateOf(200.dp) }
        this.state = size
        Box {
            Box(Modifier.size(size.value), contentAlignment = Alignment.Center) {
                StaticChildren(100)
            }
        }
    }

    @Composable
    private fun StaticChildren(count: Int) {
        if (count > 0) {
            val modifier = if (count == 1) {
                Modifier.onGloballyPositioned { it.size }
            } else {
                Modifier
            }
            Box(
                Modifier.size(100.dp).then(modifier),
                contentAlignment = Alignment.Center
            ) {
                StaticChildren(count - 1)
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}
