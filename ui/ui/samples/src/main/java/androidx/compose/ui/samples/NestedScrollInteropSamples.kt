/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun NestedScrollInteropSample(composeView: ComposeView) {
    with(composeView) {
        val nestedSrollInterop = rememberNestedScrollInteropConnection(this)
        // Add the nested scroll connection to your top level @Composable element
        // using the nestedScroll modifier.
        LazyColumn(modifier = Modifier.nestedScroll(nestedSrollInterop)) {
            items(20) { item ->
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .height(56.dp)
                        .fillMaxWidth()
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.toString())
                }
            }
        }
    }
}