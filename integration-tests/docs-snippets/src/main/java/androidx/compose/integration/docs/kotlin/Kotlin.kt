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

package androidx.compose.integration.docs.kotlin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ConstraintLayout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/kotlin
 *
 * No action required if it's modified.
 */

@Composable private fun KotlinSnippet1() {
    Text(text = "Hello, Android!")
}

@Composable private fun KotlinSnippet2() {
    Text(text = "Hello, Android!",
        color = Color.Unspecified,
        fontSize = TextUnit.Unspecified,
        letterSpacing = TextUnit.Unspecified,
        overflow = TextOverflow.Clip
    )
}

@Composable private fun KotlinSnippet3() {
    Button( // ...
        onClick = {
            // do something
            // do something else
        }
    ) { /*...*/ }
}

@Composable private fun KotlinSnippet4() {
    Column(
        modifier = Modifier.padding(16.dp), content = {
            Text("Some text")
            Text("Some more text")
            Text("Last text")
        }
    )
}

@Composable private fun KotlinSnippet5() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Some text")
        Text("Some more text")
        Text("Last text")
    }
}

@Composable private fun KotlinSnippet6() {
    Column {
        Text("Some text")
        Text("Some more text")
        Text("Last text")
    }
}

@Composable private fun KotlinSnippet7() {
    Row {
        Text(
            text = "Hello world",
            // This Text is inside a RowScope so it has access to
            // Alignment.CenterVertically but not to
            // Alignment.CenterHorizontally, which would be available
            // in a ColumnScope.
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable private fun KotlinSnippet8() {
    Box(
        modifier = Modifier.drawBehind {
            // This method accepts a lambda of type DrawScope.() -> Unit
            // therefore in this lambda we can access properties and functions
            // available from DrawScope, such as the `drawRectangle` function.
            drawRect( /*...*/)
        }
    )
}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VALUE")
@Composable private fun KotlinSnippet9() {
    var showDialog by remember { mutableStateOf(false) }

    // Updating the var automatically triggers a state change
    showDialog = true
}

@Composable private fun KotlinSnippet10() {
    ConstraintLayout {

        val (image, title, subtitle) = createRefs()

        // The `createRefs` function returns a data object;
        // the first three components are extracted into the
        // image, title, and subtitle variables.

        // ...
    }
}

/*
Fakes needed for snippets to build:
 */

@Suppress("ClassName")
private object R {
    object string {
        const val profile = 0
        const val friends_list = 1
    }
}

private fun drawRect() {}