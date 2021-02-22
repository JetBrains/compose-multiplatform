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

import android.graphics.Color
import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewbinding.samples.databinding.SampleLayoutBinding
import androidx.compose.ui.viewinterop.AndroidViewBinding

@Sampled
@Composable
fun AndroidViewBindingSample() {
    // Inflates and composes sample_layout.xml and changes the color of the `second` View.
    // The `second` View is part of sample_layout.xml.
    AndroidViewBinding(SampleLayoutBinding::inflate) {
        second.setBackgroundColor(Color.GRAY)
    }
}