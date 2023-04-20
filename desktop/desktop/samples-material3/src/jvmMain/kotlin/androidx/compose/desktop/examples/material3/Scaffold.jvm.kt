/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.desktop.examples.material3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

private const val title = "Material3 Desktop Compose Elements"

fun main() = singleWindowApplication(
    title = title,
    state = WindowState(width = 1024.dp, height = 850.dp),
) {
    App()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrameWindowScope.App() {
    MaterialTheme {
        Scaffold(
            topBar = {
                WindowDraggableArea {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                            }
                        },
                        navigationIcon = {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = null,
                                modifier = Modifier.clickable {

                                })
                        }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Star, contentDescription = null)
                }
            },
            bottomBar = {
                BottomAppBar {
                    Text("BottomAppBar")
                }
            },
            content = { innerPadding: PaddingValues ->
                Column(Modifier.padding(innerPadding)) {
                    repeat(5) {
                        Text("$it")
                    }
                    SwitchRippleAnimationBug()
                }
            }
        )
    }
}
