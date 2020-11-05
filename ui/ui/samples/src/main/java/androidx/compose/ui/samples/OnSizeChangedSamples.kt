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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged

@Sampled
@Composable
fun OnSizeChangedSample(name: String) {
    // Use onSizeChanged() for diagnostics. Use Layout or SubcomposeLayout if you want
    // to use the size of one component to affect the size of another component.
    Text(
        "Hello $name",
        Modifier.onSizeChanged { size ->
            println("The size of the Text in pixels is $size")
        }
    )
}
