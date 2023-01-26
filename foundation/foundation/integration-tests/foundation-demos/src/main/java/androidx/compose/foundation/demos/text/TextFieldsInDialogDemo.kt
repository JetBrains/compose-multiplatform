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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val dialogDemos = listOf(
    ComposableDemo("Full screen dialog, multiple fields") { onNavigateUp ->
        Dialog(onDismissRequest = onNavigateUp) {
            InputFieldDemo()
        }
    },
    ComposableDemo(
        "Small dialog, single field (platform default width, decor fits system windows)"
    ) { onNavigateUp ->
        Dialog(
            onDismissRequest = onNavigateUp,
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = true
            )
        ) { SingleTextFieldDialog() }
    },
    ComposableDemo(
        "Small dialog, single field (decor fits system windows)"
    ) { onNavigateUp ->
        Dialog(
            onDismissRequest = onNavigateUp,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) { SingleTextFieldDialog() }
    },
    ComposableDemo(
        "Small dialog, single field (platform default width)"
    ) { onNavigateUp ->
        Dialog(
            onDismissRequest = onNavigateUp,
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = false
            )
        ) { SingleTextFieldDialog() }
    },
    ComposableDemo(
        "Small dialog, single field"
    ) { onNavigateUp ->
        Dialog(
            onDismissRequest = onNavigateUp,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) { SingleTextFieldDialog() }
    },
    ComposableDemo("Show keyboard automatically") { onNavigateUp ->
        Dialog(onDismissRequest = onNavigateUp) {
            AutoFocusTextFieldDialog()
        }
    }
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TextFieldsInDialogDemo() {
    val listState = rememberLazyListState()
    val (currentDemoIndex, setDemoIndex) = rememberSaveable { mutableStateOf(-1) }

    if (currentDemoIndex == -1) {
        LazyColumn(state = listState) {
            itemsIndexed(dialogDemos) { index, demo ->
                ListItem(Modifier.clickable { setDemoIndex(index) }) {
                    Text(demo.title)
                }
            }
        }
    } else {
        val currentDemo = dialogDemos[currentDemoIndex]
        Text(
            currentDemo.title,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(),
            textAlign = TextAlign.Center
        )
        Layout(
            content = { currentDemo.content(onNavigateUp = { setDemoIndex(-1) }) }
        ) { measurables, _ ->
            check(measurables.isEmpty()) { "Dialog demo must only emit a Dialog composable." }
            layout(0, 0) {}
        }
    }
}

@Composable
private fun SingleTextFieldDialog() {
    var text by remember { mutableStateOf("") }
    TextField(text, onValueChange = { text = it })
}

@Composable
private fun AutoFocusTextFieldDialog() {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    TextField(
        text,
        onValueChange = { text = it },
        modifier = Modifier.focusRequester(focusRequester)
    )
}