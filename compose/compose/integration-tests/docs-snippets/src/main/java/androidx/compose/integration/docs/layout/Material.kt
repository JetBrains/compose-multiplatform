// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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

// Ignore lint warnings in documentation snippets
@file:Suppress(
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "RemoveEmptyParenthesesFromLambdaCall"
)

package androidx.compose.integration.docs.layout

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.BackdropValue
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomDrawer
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.material.rememberBottomDrawerState
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/layouts/material
 *
 * No action required if it's modified.
 */

private object MaterialSnippet1 {
    @Composable
    fun MyApp() {
        MaterialTheme {
            // Material Components like Button, Card, Switch, etc.
        }
    }
}

private object MaterialSnippet2 {
    @Composable
    fun MyButton() {
        Button(
            onClick = { /* ... */ },
            // Uses ButtonDefaults.ContentPadding by default
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 12.dp,
                end = 20.dp,
                bottom = 12.dp
            )
        ) {
            // Inner content including an icon and a text label
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Like")
        }
    }
}

private object MaterialSnippet3 {
    @Composable
    fun MyExtendedFloatingActionButton() {
        ExtendedFloatingActionButton(
            onClick = { /* ... */ },
            icon = {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Favorite"
                )
            },
            text = { Text("Like") }
        )
    }
}

private object MaterialSnippet4 {
    @Composable
    fun MyScaffold() {
        Scaffold(/* ... */) { contentPadding ->
            // Screen content
            Box(modifier = Modifier.padding(contentPadding)) { /* ... */ }
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet5 {
    @Composable
    fun MyTopAppBar() {
        Scaffold(
            topBar = {
                TopAppBar { /* Top app bar content */ }
            }
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet6 {
    @Composable
    fun MyBottomAppBar() {
        Scaffold(
            bottomBar = {
                BottomAppBar { /* Bottom app bar content */ }
            }
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet7 {
    @Composable
    fun MyFAB() {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { /* ... */ }) {
                    /* FAB content */
                }
            }
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet8 {
    @Composable
    fun MyFAB() {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { /* ... */ }) {
                    /* FAB content */
                }
            },
            // Defaults to FabPosition.End
            floatingActionButtonPosition = FabPosition.Center
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet9 {
    @Composable
    fun MyFAB() {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { /* ... */ }) {
                    /* FAB content */
                }
            },
            // Defaults to false
            isFloatingActionButtonDocked = true,
            bottomBar = {
                BottomAppBar { /* Bottom app bar content */ }
            }
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet10 {
    @Composable
    fun MyFAB() {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { /* ... */ }) {
                    /* FAB content */
                }
            },
            isFloatingActionButtonDocked = true,
            bottomBar = {
                BottomAppBar(
                    // Defaults to null, that is, no cutout
                    cutoutShape = MaterialTheme.shapes.small.copy(
                        CornerSize(percent = 50)
                    )
                ) {
                    /* Bottom app bar content */
                }
            }
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet11 {
    @Composable
    fun MySnackbar() {
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()
        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Show snackbar") },
                    onClick = {
                        scope.launch {
                            scaffoldState.snackbarHostState
                                .showSnackbar("Snackbar")
                        }
                    }
                )
            }
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet12 {
    @Composable
    fun MySnackbar() {
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()
        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Show snackbar") },
                    onClick = {
                        scope.launch {
                            val result = scaffoldState.snackbarHostState
                                .showSnackbar(
                                    message = "Snackbar",
                                    actionLabel = "Action",
                                    // Defaults to SnackbarDuration.Short
                                    duration = SnackbarDuration.Indefinite
                                )
                            when (result) {
                                SnackbarResult.ActionPerformed -> {
                                    /* Handle snackbar action performed */
                                }
                                SnackbarResult.Dismissed -> {
                                    /* Handle snackbar dismissed */
                                }
                            }
                        }
                    }
                )
            }
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet13 {
    @Composable
    fun MyDrawer() {
        Scaffold(
            drawerContent = {
                Text("Drawer title", modifier = Modifier.padding(16.dp))
                Divider()
                // Drawer items
            }
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet14 {
    @Composable
    fun MyDrawer() {
        Scaffold(
            drawerContent = {
                // Drawer content
            },
            // Defaults to true
            drawerGesturesEnabled = false
        ) {
            // Screen content
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
private object MaterialSnippet15 {
    @Composable
    fun MyDrawer() {
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()
        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                // Drawer content
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Open or close drawer") },
                    onClick = {
                        scope.launch {
                            scaffoldState.drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }
                )
            }
        ) {
            // Screen content
        }
    }
}

private object MaterialSnippet16 {
    @Composable
    fun MyModalDrawer() {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        ModalDrawer(
            drawerState = drawerState,
            drawerContent = {
                // Drawer content
            }
        ) {
            // Screen content
        }
    }
}

private object MaterialSnippet17 {
    @ExperimentalMaterialApi
    @Composable
    fun MyModalDrawer() {
        val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
        BottomDrawer(
            drawerState = drawerState,
            drawerContent = {
                // Drawer content
            }
        ) {
            // Screen content
        }
    }
}

private object MaterialSnippet18 {
    @ExperimentalMaterialApi
    @Composable
    fun MyBottomSheet() {
        BottomSheetScaffold(
            sheetContent = {
                // Sheet content
            }
        ) {
            // Screen content
        }
    }
}

private object MaterialSnippet19 {
    @ExperimentalMaterialApi
    @Composable
    fun MyBottomSheet() {
        BottomSheetScaffold(
            sheetContent = {
                // Sheet content
            },
            // Defaults to BottomSheetScaffoldDefaults.SheetPeekHeight
            sheetPeekHeight = 128.dp,
            // Defaults to true
            sheetGesturesEnabled = false

        ) {
            // Screen content
        }
    }
}

private object MaterialSnippet20 {
    @ExperimentalMaterialApi
    @Composable
    fun MyBottomSheet() {
        val scaffoldState = rememberBottomSheetScaffoldState()
        val scope = rememberCoroutineScope()
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                // Sheet content
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Expand or collapse sheet") },
                    onClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.apply {
                                if (isCollapsed) expand() else collapse()
                            }
                        }
                    }
                )
            }
        ) {
            // Screen content
        }
    }
}

private object MaterialSnippet21 {
    @ExperimentalMaterialApi
    @Composable
    fun MyBottomSheet() {
        val sheetState = rememberModalBottomSheetState(
            ModalBottomSheetValue.Hidden
        )
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetContent = {
                // Sheet content
            }
        ) {
            // Screen content
        }
    }
}

private object MaterialSnippet22 {
    @ExperimentalMaterialApi
    @Composable
    fun MyBackdrop() {
        BackdropScaffold(
            appBar = {
                // Top app bar
            },
            backLayerContent = {
                // Back layer content
            },
            frontLayerContent = {
                // Front layer content
            }
        )
    }
}

private object MaterialSnippet23 {
    @ExperimentalMaterialApi
    @Composable
    fun MyBackdrop() {
        BackdropScaffold(
            appBar = {
                // Top app bar
            },
            backLayerContent = {
                // Back layer content
            },
            frontLayerContent = {
                // Front layer content
            },
            // Defaults to BackdropScaffoldDefaults.PeekHeight
            peekHeight = 40.dp,
            // Defaults to BackdropScaffoldDefaults.HeaderHeight
            headerHeight = 60.dp,
            // Defaults to true
            gesturesEnabled = false
        )
    }
}

private object MaterialSnippet24 {
    @ExperimentalMaterialApi
    @Composable
    fun MyBackdrop() {
        val scaffoldState = rememberBackdropScaffoldState(
            BackdropValue.Concealed
        )
        val scope = rememberCoroutineScope()
        BackdropScaffold(
            scaffoldState = scaffoldState,
            appBar = {
                TopAppBar(
                    title = { Text("Backdrop") },
                    navigationIcon = {
                        if (scaffoldState.isConcealed) {
                            IconButton(
                                onClick = {
                                    scope.launch { scaffoldState.reveal() }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    scope.launch { scaffoldState.conceal() }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                    },
                    elevation = 0.dp,
                    backgroundColor = Color.Transparent
                )
            },
            backLayerContent = {
                // Back layer content
            },
            frontLayerContent = {
                // Front layer content
            }
        )
    }
}
