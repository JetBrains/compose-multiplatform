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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign

@Sampled
@Composable
fun SurfaceSample() {
    Surface(
        color = MaterialTheme.colors.background
    ) {
        Text("Text color is `onBackground`")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun ClickableSurfaceSample() {
    var count by remember { mutableStateOf(0) }
    Surface(
        onClick = { count++ },
        color = MaterialTheme.colors.background
    ) {
        Text("Clickable surface Text with `onBackground` color and count: $count")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun SelectableSurfaceSample() {
    var selected by remember { mutableStateOf(false) }
    Surface(
        selected = selected,
        onClick = { selected = !selected },
        color = MaterialTheme.colors.background
    ) {
        Text(
            text = if (selected) "Selected" else "Not Selected",
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Sampled
@Composable
fun ToggleableSurfaceSample() {
    var checked by remember { mutableStateOf(false) }
    Surface(
        checked = checked,
        onCheckedChange = { checked = !checked },
        color = if (checked) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.background
        }
    ) {
        Text(
            text = if (checked) "ON" else "OFF",
            textAlign = TextAlign.Center
        )
    }
}
