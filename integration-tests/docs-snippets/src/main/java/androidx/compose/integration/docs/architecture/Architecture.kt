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
@file:Suppress("unused", "ControlFlowWithEmptyBody", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.architecture

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/architecture
 *
 * No action required if it's modified.
 */

@Composable private fun ArchitectureSnippet1() {
    var name by remember { mutableStateOf("") }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Name") }
    )
}

private object ArchitectureSnippet2 {
    @Composable
    fun Header(title: String, subtitle: String) {
        // Recomposes when title or subtitle have changed.
    }

    @Composable
    fun Header(news: News) {
        // Recomposes when a new instance of News is passed in.
    }
}

private object ArchitectureSnippet3 {
    @Composable
    fun MyAppTopAppBar(topAppBarText: String, onBackPressed: () -> Unit) {
        TopAppBar(
            title = {
                Text(
                    text = topAppBarText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = localizedString
                    )
                }
            },
            // ...
        )
    }
}

/*
 * Fakes needed for snippets to build:
 */

// For ArchitectureSnippet2
private class News {
    // null
}

// For ArchitectureSnippet3
private val localizedString = "null"
