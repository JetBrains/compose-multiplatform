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

package androidx.compose.foundation.layout.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.consumedWindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.mandatorySystemGesturesPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.waterfallPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Sampled
fun captionBarPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.captionBarPadding()) {
                    // app content
                }
            }
        }
    }
}

@Sampled
fun systemBarsPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.systemBarsPadding()) {
                    // app content
                }
            }
        }
    }
}

@Sampled
fun displayCutoutPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Blue).statusBarsPadding()) {
                    Box(Modifier.background(Color.Yellow).displayCutoutPadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun statusBarsAndNavigationBarsPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Blue).statusBarsPadding()) {
                    Box(Modifier.background(Color.Green).navigationBarsPadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun imePaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Blue).systemBarsPadding()) {
                    Box(Modifier.imePadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun waterfallPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Blue).systemBarsPadding()) {
                    // The app content shouldn't spill over the edges. They will be green.
                    Box(Modifier.background(Color.Green).waterfallPadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun systemGesturesPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Blue).systemBarsPadding()) {
                    // The app content won't interfere with the system gestures area.
                    // It will just be white.
                    Box(Modifier.background(Color.White).systemGesturesPadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun mandatorySystemGesturesPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Blue).systemBarsPadding()) {
                    // The app content won't interfere with the mandatory system gestures area.
                    // It will just be white.
                    Box(Modifier.background(Color.White).mandatorySystemGesturesPadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun safeDrawingPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Black).systemBarsPadding()) {
                    // The app content won't have anything drawing over it, but all the
                    // background not in the status bars will be white.
                    Box(Modifier.background(Color.White).safeDrawingPadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun safeGesturesPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Black).systemBarsPadding()) {
                    // The app content will only be drawn where there is no possible
                    // gesture confusion. The rest will be plain white
                    Box(Modifier.background(Color.White).safeGesturesPadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun safeContentPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.background(Color.Black).systemBarsPadding()) {
                    // The app content will only be drawn where there is no possible
                    // gesture confusion and content will not be drawn over.
                    // The rest will be plain white
                    Box(Modifier.background(Color.White).safeContentPadding()) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun insetsPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                val insets = WindowInsets.systemBars.union(WindowInsets.ime)
                Box(Modifier.background(Color.White).fillMaxSize().windowInsetsPadding(insets)) {
                    // app content
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
fun consumedInsetsPaddingSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                with(LocalDensity.current) {
                    val paddingValues = PaddingValues(horizontal = 20.dp)
                    Box(
                        Modifier
                            .padding(paddingValues)
                            .consumedWindowInsets(paddingValues)
                    ) {
                        // app content
                    }
                }
            }
        }
    }
}

@Sampled
fun insetsInt() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                // Make sure we are at least 10 pixels away from the top.
                val insets = WindowInsets.statusBars.union(WindowInsets(top = 10))
                Box(Modifier.windowInsetsPadding(insets)) {
                    // app content
                }
            }
        }
    }
}

@Sampled
fun insetsDp() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                // Make sure we are at least 10 DP away from the top.
                val insets = WindowInsets.statusBars.union(WindowInsets(top = 10.dp))
                Box(Modifier.windowInsetsPadding(insets)) {
                    // app content
                }
            }
        }
    }
}

@Sampled
fun paddingValuesSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                LazyColumn(
                    contentPadding = WindowInsets.navigationBars.asPaddingValues()
                ) {
                    // items
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
fun consumedInsetsSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.padding(WindowInsets.navigationBars.asPaddingValues())) {
                    Box(Modifier.consumedWindowInsets(WindowInsets.navigationBars)) {
                        // app content
                    }
                }
            }
        }
    }
}
