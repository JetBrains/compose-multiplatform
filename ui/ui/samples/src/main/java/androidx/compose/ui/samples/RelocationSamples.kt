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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.RelocationRequester
import androidx.compose.ui.layout.relocationRequester
import androidx.compose.ui.unit.dp

@ExperimentalComposeUiApi
@Sampled
@Composable
fun BringIntoViewSample() {
    val relocationRequester = remember { RelocationRequester() }
    Column {
        Box(Modifier.width(100.dp).horizontalScroll(rememberScrollState())) {
            Box(Modifier.size(100.dp))
            Box(Modifier.size(100.dp).relocationRequester(relocationRequester))
        }
        Button(onClick = { relocationRequester.bringIntoView() }) {
            Text("Bring box into view")
        }
    }
}
