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

package androidx.compose.foundation.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.samples.BorderSample
import androidx.compose.foundation.samples.BorderSampleWithBrush
import androidx.compose.foundation.samples.BorderSampleWithDataClass
import androidx.compose.foundation.samples.DrawBackgroundColor
import androidx.compose.foundation.samples.DrawBackgroundShapedBrush
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun DrawModifiersDemo() {
    Row {
        Column(Modifier.weight(1f, true).padding(10.dp)) {
            BorderSample()
            Spacer(Modifier.height(30.dp))
            BorderSampleWithBrush()
            Spacer(Modifier.height(30.dp))
            BorderSampleWithDataClass()
        }
        Column(Modifier.weight(1f).padding(10.dp)) {
            DrawBackgroundColor()
            Spacer(Modifier.height(30.dp))
            DrawBackgroundShapedBrush()
        }
    }
}
