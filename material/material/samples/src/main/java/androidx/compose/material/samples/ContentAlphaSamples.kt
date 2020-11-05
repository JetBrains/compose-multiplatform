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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AmbientContentAlpha
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers

@Sampled
@Composable
fun ContentAlphaSample() {
    Column {
        Text("No alpha applied - 100% opacity")
        Providers(AmbientContentAlpha provides ContentAlpha.high) {
            Text("High content alpha applied - 87% opacity")
        }
        Providers(AmbientContentAlpha provides ContentAlpha.medium) {
            Text("Medium content alpha applied - 60% opacity")
        }
        Providers(AmbientContentAlpha provides ContentAlpha.disabled) {
            Text("Disabled content alpha applied - 38% opacity")
        }
    }
}
