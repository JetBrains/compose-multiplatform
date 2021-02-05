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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Sampled
@Composable
fun ContentAlphaSample() {
    // Note the alpha values listed below are the values for light theme. The values are slightly
    // different in dark theme to provide proper contrast against the background.
    Column {
        Text(
            "No content alpha applied - uses the default content alpha set by MaterialTheme - " +
                "87% alpha"
        )
        CompositionLocalProvider(LocalContentAlpha provides 1.00f) {
            Text("1.00f alpha applied - 100% alpha")
        }
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
            Text("High content alpha applied - 87% alpha")
        }
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text("Medium content alpha applied - 60% alpha")
        }
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
            Text("Disabled content alpha applied - 38% alpha")
        }
    }
}
