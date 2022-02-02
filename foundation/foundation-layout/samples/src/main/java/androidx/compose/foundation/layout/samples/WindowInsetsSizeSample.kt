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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

@Sampled
fun insetsStartWidthSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.fillMaxSize()) {
                    // Background for navigation bar at the start
                    Box(Modifier.windowInsetsStartWidth(WindowInsets.navigationBars)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .background(Color.Red)
                    )
                    // app content
                }
            }
        }
    }
}

@Sampled
fun insetsTopHeightSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.fillMaxSize()) {
                    // Background for status bar at the top
                    Box(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Red)
                    )
                    // app content
                }
            }
        }
    }
}

@Sampled
fun insetsEndWidthSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.fillMaxSize()) {
                    // Background for navigation bar at the end
                    Box(Modifier.windowInsetsEndWidth(WindowInsets.navigationBars)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(Color.Red)
                    )
                    // app content
                }
            }
        }
    }
}

@Sampled
fun insetsBottomHeightSample() {
    class SampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            super.onCreate(savedInstanceState)
            setContent {
                Box(Modifier.fillMaxSize()) {
                    // Background for navigation bar at the bottom
                    Box(Modifier.windowInsetsTopHeight(WindowInsets.navigationBars)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Red)
                    )
                    // app content
                }
            }
        }
    }
}
