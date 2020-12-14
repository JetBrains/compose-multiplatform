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

package androidx.compose.material.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.samples.RadioButtonSample
import androidx.compose.material.samples.RadioGroupSample
import androidx.compose.material.samples.SwitchSample
import androidx.compose.material.samples.TriStateCheckboxSample
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun SelectionControlsDemo() {
    val headerStyle = MaterialTheme.typography.h6
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        item {
            Text(text = "Checkbox", style = headerStyle)
            TriStateCheckboxSample()
        }
        item {
            Text(text = "Switch", style = headerStyle)
            SwitchSample()
        }
        item {
            Text(text = "RadioButtons with custom colors", style = headerStyle)
            RadioButtonSample()
        }
        item {
            Text(text = "Radio group", style = headerStyle)
            RadioGroupSample()
        }
    }
}
