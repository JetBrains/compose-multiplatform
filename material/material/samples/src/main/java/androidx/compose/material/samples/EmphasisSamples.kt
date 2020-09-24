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
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AmbientEmphasisLevels
import androidx.compose.material.ProvideEmphasis
import androidx.compose.runtime.Composable

@Sampled
@Composable
fun EmphasisSample() {
    Column {
        Text("No emphasis applied - 100% opacity")
        val emphasisLevels = AmbientEmphasisLevels.current
        ProvideEmphasis(emphasisLevels.high) {
            Text("High emphasis applied - 87% opacity")
        }
        ProvideEmphasis(emphasisLevels.medium) {
            Text("Medium emphasis applied - 60% opacity")
        }
        ProvideEmphasis(emphasisLevels.disabled) {
            Text("Disabled emphasis applied - 38% opacity")
        }
    }
}
