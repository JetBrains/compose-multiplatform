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

package androidx.compose.runtime.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

val local = compositionLocalOf { 0 }

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CompositionLocalBenchmark : ComposeBenchmarkBase() {

    @UiThreadTest
    @Test
    @Ignore // Only used for overhead comparison, not to be tracked.
    fun compositionLocal_compose_depth_1_1() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(1) {
                    local.current
                }
            }
        }
    }

    @UiThreadTest
    @Test
    @Ignore // Only used for overhead comparison, not to be tracked.
    fun compositionLocal_compose_depth_1_10() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(1) {
                    repeat(10) { local.current }
                }
            }
        }
    }

    @UiThreadTest
    @Test
    @Ignore // Only used for overhead comparison, not to be tracked.
    fun compositionLocal_compose_depth_1_100() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(1) {
                    repeat(100) { local.current }
                }
            }
        }
    }

    @UiThreadTest
    @Test
    @Ignore // Only used for overhead comparison, not to be tracked.
    fun compositionLocal_compose_depth_100_1() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(100) {
                    local.current
                }
            }
        }
    }

    @UiThreadTest
    @Test
    @Ignore // Only used for overhead comparison, not to be tracked.
    fun compositionLocal_compose_depth_100_10() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(100) {
                    repeat(10) { local.current }
                }
            }
        }
    }

    @UiThreadTest
    @Test
    @Ignore // Only used for overhead comparison, not to be tracked.
    fun compositionLocal_compose_depth_100_100() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(100) {
                    repeat(100) { local.current }
                }
            }
        }
    }

    @UiThreadTest
    @Test
    fun compositionLocal_compose_depth_10000_1() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(10000) {
                    local.current
                }
            }
        }
    }
    @UiThreadTest
    @Test
    @Ignore // Only used for overhead comparison, not to be tracked.
    fun compositionLocal_compose_depth_10000_10() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(10000) {
                    repeat(10) { local.current }
                }
            }
        }
    }

    // This is the only one of the "compose" benchmarks that should be tracked.
    @UiThreadTest
    @Test
    fun compositionLocal_compose_depth_10000_100() = runBlockingTestWithFrameClock {
        measureCompose {
            CompositionLocalProvider(local provides 100) {
                DepthOf(10000) {
                    repeat(100) { local.current }
                }
            }
        }
    }

    @UiThreadTest
    @Test
    fun compositionLocal_recompose_depth_10000_1() = runBlockingTestWithFrameClock {
        var data by mutableStateOf(0)
        var sync: Int = 0

        measureRecomposeSuspending {
            compose {
                DepthOf(10000) {
                    // Force the read to occur in a way that is difficult for the compiler to figure
                    // out that it is not used.
                    sync = data
                    repeat(1) { local.current }
                }
            }
            update {
                data++
            }
        }
        if (sync > Int.MAX_VALUE / 2) {
            println("This is just to fool the compiler into thinking sync is used")
        }
    }

    @UiThreadTest
    @Test
    fun compositionLocal_recompose_depth_10000_100() = runBlockingTestWithFrameClock {
        var data by mutableStateOf(0)
        var sync: Int = 0

        measureRecomposeSuspending {
            compose {
                DepthOf(10000) {
                    // Force the read to occur in a way that is difficult for the compiler to figure
                    // out that it is not used.
                    sync = data
                    repeat(100) { local.current }
                }
            }
            update {
                data++
            }
        }
        if (sync > Int.MAX_VALUE / 2) {
            println("This is just to fool the compiler into thinking sync is used")
        }
    }
}

@Composable
fun DepthOf(count: Int, content: @Composable () -> Unit) {
    if (count > 0) DepthOf(count - 1, content)
    else content()
}