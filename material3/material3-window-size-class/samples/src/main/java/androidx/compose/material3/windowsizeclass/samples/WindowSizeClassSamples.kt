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

package androidx.compose.material3.windowsizeclass.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Sampled
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Sampled
fun AndroidWindowSizeClassSample() {
    class MyActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                // Calculate the window size class for the activity's current window. If the window
                // size changes, for example when the device is rotated, the value returned by
                // calculateSizeClass will also change.
                val windowSizeClass = calculateWindowSizeClass(this)
                // Perform logic on the window size class to decide whether to use a nav rail.
                val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact

                // MyScreen knows nothing about window size classes, and performs logic based on a
                // Boolean flag.
                MyScreen(useNavRail = useNavRail)
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun MyScreen(useNavRail: Boolean) {}
