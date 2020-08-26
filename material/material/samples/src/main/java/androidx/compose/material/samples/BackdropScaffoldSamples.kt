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
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.BackdropValue.Concealed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberBackdropState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Sampled
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun BackdropSample() {
    val selection = remember { mutableStateOf(1) }
    val backdropScaffoldState = rememberBackdropState(Concealed)
    BackdropScaffold(
        backdropScaffoldState = backdropScaffoldState,
        appBar = {
            TopAppBar(
                title = { Text("Backdrop") },
                navigationIcon = {
                    if (backdropScaffoldState.isConcealed) {
                        IconButton(onClick = { backdropScaffoldState.reveal() }) {
                            Icon(Icons.Default.Menu)
                        }
                    } else {
                        IconButton(onClick = { backdropScaffoldState.conceal() }) {
                            Icon(Icons.Default.Close)
                        }
                    }
                },
                elevation = 0.dp,
                backgroundColor = Color.Transparent
            )
        },
        backLayerContent = {
            LazyColumnFor((1..5).toList()) {
                ListItem(
                    Modifier.clickable {
                        selection.value = it
                        backdropScaffoldState.conceal()
                    },
                    text = { Text("Select $it") }
                )
            }
        },
        frontLayerContent = {
            Box(
                Modifier.fillMaxSize(),
                gravity = ContentGravity.Center
            ) {
                Text("Selection: ${selection.value}")
            }
        }
    )
}

@Sampled
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun BackdropWithSnackbarSample() {
    val scope = rememberCoroutineScope()
    val selection = remember { mutableStateOf(1) }
    val backdropScaffoldState = rememberBackdropState(Concealed)
    BackdropScaffold(
        backdropScaffoldState = backdropScaffoldState,
        appBar = {
            TopAppBar(
                title = { Text("Backdrop") },
                navigationIcon = {
                    if (backdropScaffoldState.isConcealed) {
                        IconButton(onClick = { backdropScaffoldState.reveal() }) {
                            Icon(Icons.Default.Menu)
                        }
                    } else {
                        IconButton(onClick = { backdropScaffoldState.conceal() }) {
                            Icon(Icons.Default.Close)
                        }
                    }
                },
                actions = {
                    var clickCount by remember { mutableStateOf(0) }
                    IconButton(onClick = {
                        // show snackbar as a suspend function
                        scope.launch {
                            backdropScaffoldState.snackbarHostState
                                .showSnackbar("Snackbar #${++clickCount}")
                        }
                    }) {
                        Icon(Icons.Default.Favorite)
                    }
                },
                elevation = 0.dp,
                backgroundColor = Color.Transparent
            )
        },
        backLayerContent = {
            LazyColumnFor((1..5).toList()) {
                ListItem(
                    Modifier.clickable {
                        selection.value = it
                        backdropScaffoldState.conceal()
                    },
                    text = { Text("Select $it") }
                )
            }
        },
        frontLayerContent = {
            Box(
                Modifier.fillMaxSize(),
                gravity = ContentGravity.Center
            ) {
                Text("Selection: ${selection.value}")
            }
        }
    )
}
