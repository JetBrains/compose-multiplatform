/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.demos.input.nestedscroll

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.compose.ui.layout.RelocationRequester
import androidx.compose.ui.layout.relocationRequester
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BringIntoViewDemo() {
    val greenRequester = remember { RelocationRequester() }
    val redRequester = remember { RelocationRequester() }
    Column {
        Row(Modifier.width(300.dp).horizontalScroll(rememberScrollState())) {
            Box(Modifier.background(Blue).size(100.dp))
            Box(Modifier.background(Green).size(100.dp).relocationRequester(greenRequester))
            Box(Modifier.background(Yellow).size(100.dp))
            Box(Modifier.background(Magenta).size(100.dp))
            Box(Modifier.background(Red).size(100.dp).relocationRequester(redRequester))
            Box(Modifier.background(LightGray).size(100.dp))
        }
        Button(onClick = { greenRequester.bringIntoView() }) {
            Text("Bring Green box into view")
        }
        Button(onClick = { redRequester.bringIntoView() }) {
            Text("Bring Red box into view")
        }
    }
}
