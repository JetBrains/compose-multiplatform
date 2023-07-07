// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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

package androidx.compose.integration.docs.preview

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/preview
 *
 * No action required if it's modified.
 */

private object PreviewSnippet1 {
    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello $name!")
    }
}

private object PreviewSnippet2 {
    @Preview
    @Composable
    fun PreviewGreeting() {
        Greeting("Android")
    }
}

private object PreviewSnippet3 {
    @Preview(name = "Android greeting")
    @Composable
    fun PreviewGreeting() {
        Greeting("Android")
    }
}

private object PreviewSnippet4 {
    @Preview(name = "Long greeting")
    @Composable
    fun PreviewLongGreeting() {
        Greeting("my valued friend, whom I am incapable of " +
            "greeting without using a great many words")
    }
    @Preview(name = "Newline greeting")
    @Composable
    fun PreviewNewlineGreeting() {
        Greeting("world\nwith a line break")
    }
}

private object PreviewSnippet5 {
    @Composable
    fun Counter(count: Int, updateCount: (Int) -> Unit) {
        Button(
            onClick = { updateCount(count + 1) },
            colors = buttonColors(
                backgroundColor = if (count > 5) Color.Green else Color.White
            )
        ) {
            Text("I've been clicked $count times")
        }
    }
    @Preview
    @Composable
    fun PreviewCounter() {
        val counterState = remember { mutableStateOf(0) }

        Counter(
            count = counterState.value,
            updateCount = { newCount ->
                counterState.value = newCount
            }
        )
    }
}

/*
Fakes needed for snippets to build:
 */

@Composable private fun Greeting(name: String) {}
