/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime.benchmark

import androidx.compose.runtime.benchmark.deeptree.DeepTree
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * This is definitely a synthetic benchmark that may not map to realistic trees, but it is an effective way of
 * stress-testing some very large and very complex trees that may map pretty well to the more complicated
 * scenarios. I think this is a decent benchmark for testing Compose UI’s layout system in addition to
 * Compose’s composition performance.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DeepTreeBenchmark : ComposeBenchmarkBase() {
    @UiThreadTest
    @Test
    fun benchmark_deep_tree_01_depth1_breadth100_wrap2() = runBlockingTestWithFrameClock {
        measureCompose {
            DeepTree(depth = 1, breadth = 100, wrap = 2)
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_deep_tree_02_depth7_breadth3_wrap2() = runBlockingTestWithFrameClock {
        measureCompose {
            DeepTree(depth = 7, breadth = 3, wrap = 2)
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_deep_tree_03_depth2_breadth10_wrap2() = runBlockingTestWithFrameClock {
        measureCompose {
            DeepTree(depth = 2, breadth = 10, wrap = 2)
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_deep_tree_04_depth2_breadth10_wrap6() = runBlockingTestWithFrameClock {
        measureCompose {
            DeepTree(depth = 2, breadth = 10, wrap = 6)
        }
    }
}