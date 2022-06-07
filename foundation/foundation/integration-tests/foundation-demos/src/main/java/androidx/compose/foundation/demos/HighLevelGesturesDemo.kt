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

package androidx.compose.foundation.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.samples.DraggableSample
import androidx.compose.foundation.samples.FocusableSample
import androidx.compose.foundation.samples.HoverableSample
import androidx.compose.foundation.samples.OverscrollSample
import androidx.compose.foundation.samples.ScrollableSample
import androidx.compose.foundation.samples.TransformableSample
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun HighLevelGesturesDemo() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        DraggableSample()
        Spacer(Modifier.height(50.dp))
        Row {
            ScrollableSample()
            Spacer(Modifier.width(30.dp))
            OverscrollSample()
        }
        Spacer(Modifier.height(50.dp))
        TransformableSample()
        Spacer(Modifier.height(50.dp))
        FocusableSample()
        Spacer(Modifier.height(50.dp))
        HoverableSample()
    }
}