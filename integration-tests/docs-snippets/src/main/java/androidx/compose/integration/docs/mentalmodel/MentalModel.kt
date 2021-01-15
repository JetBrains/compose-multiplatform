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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.mentalmodel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/mental-model
 *
 * No action required if it's modified.
 */

private object MentalModelSnippet1 {
    @Composable
    fun Greeting(names: List<String>) {
        for (name in names) {
            Text("Hello $name")
        }
    }
}

private object MentalModelSnippet2 {
    @Composable
    fun ClickCounter(clicks: Int, onClick: () -> Unit) {
        Button(onClick = onClick) {
            Text("I've been clicked $clicks times")
        }
    }
}
private object MentalModelSnippet3 {
    @Composable
    fun SharedPrefsToggle(
        text: String,
        value: Boolean,
        onValueChanged: (Boolean) -> Unit
    ) {
        Row {
            Text(text)
            Checkbox(checked = value, onCheckedChange = onValueChanged)
        }
    }
}

private object MentalModelSnippet4 {
    @Composable
    fun ListComposable(myList: List<String>) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                for (item in myList) {
                    Text("Item: $item")
                }
            }
            Text("Count: ${myList.size}")
        }
    }
}

private object MentalModelSnippet5 {
    @Composable
    @Deprecated("Example with bug")
    fun ListWithBug(myList: List<String>) {
        var items = 0

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                for (item in myList) {
                    Text("Item: $item")
                    items++ // Avoid! Side-effect of the column recomposing.
                }
            }
            Text("Count: $items")
        }
    }
}

private object MentalModelSnippet6 {
    /**
     * Display a list of names the user can click with a header
     */
    @Composable
    fun NamePicker(
        header: String,
        names: List<String>,
        onNameClicked: (String) -> Unit
    ) {
        Column {
            // this will recompose when [header] changes, but not when [names] changes
            Text(header, style = MaterialTheme.typography.h5)
            Divider()

            // LazyColumn is the Compose version of a RecyclerView.
            // The lambda passed to items() is similar to a RecyclerView.ViewHolder.
            LazyColumn {
                items(names) { name ->
                    // When an item's [name] updates, the adapter for that item
                    // will recompose. This will not recompose when [header] changes
                    NamePickerItem(name, onNameClicked)
                }
            }
        }
    }
}

private object MentalModelSnippet7 {
    /**
     * Display a single name the user can click.
     */
    @Composable
    private fun NamePickerItem(name: String, onClicked: (String) -> Unit) {
        Text(name, Modifier.clickable(onClick = { onClicked(name) }))
    }
}

/*
Fakes needed for snippets to build:
 */

private fun NamePickerItem(name: String, onNameClicked: (String) -> Unit) { }
