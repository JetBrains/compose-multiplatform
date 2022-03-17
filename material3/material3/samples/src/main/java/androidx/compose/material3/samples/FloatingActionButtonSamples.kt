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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun FloatingActionButtonSample() {
    FloatingActionButton(
        onClick = { /* do something */ },
    ) {
        Icon(Icons.Filled.Add, "Localized description")
    }
}

@Sampled
@Composable
fun SmallFloatingActionButtonSample() {
    SmallFloatingActionButton(
        onClick = { /* do something */ },
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Localized description")
    }
}

@Sampled
@Composable
fun LargeFloatingActionButtonSample() {
    LargeFloatingActionButton(
        onClick = { /* do something */ },
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = "Localized description",
            modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
        )
    }
}

@Sampled
@Composable
fun ExtendedFloatingActionButtonTextSample() {
    ExtendedFloatingActionButton(onClick = { /* do something */ }) {
        Text(text = "Extended FAB")
    }
}

@Sampled
@Composable
fun ExtendedFloatingActionButtonSample() {
    ExtendedFloatingActionButton(
        onClick = { /* do something */ },
        icon = { Icon(Icons.Filled.Add, "Localized description") },
        text = { Text(text = "Extended FAB") },
    )
}

@Sampled
@Composable
fun AnimatedExtendedFloatingActionButtonSample() {
    val listState = rememberLazyListState()
    // The FAB is initially expanded. Once the first visible item is past the first item we
    // collapse the FAB. We use a remembered derived state to minimize unnecessary compositions.
    val expandedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* do something */ },
                expanded = expandedFab,
                icon = { Icon(Icons.Filled.Add, "Add") },
                text = { Text(text = "Extended FAB") },
            )
        },
        isFloatingActionButtonDocked = false,
        floatingActionButtonPosition = FabPosition.End,
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            for (index in 0 until 100) {
                item {
                    Text(
                        text = "List item - $index",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}