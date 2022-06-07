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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun FocusableSample() {
    // initialize focus reference to be able to request focus programmatically
    val focusRequester = remember { FocusRequester() }
    // MutableInteractionSource to track changes of the component's interactions (like "focused")
    val interactionSource = remember { MutableInteractionSource() }

    // text below will change when we focus it via button click
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val text = if (isFocused) {
        "Focused! tap anywhere to free the focus"
    } else {
        "Bring focus to me by tapping the button below!"
    }
    Column {
        // this Text will change it's text parameter depending on the presence of a focus
        Text(
            text = text,
            modifier = Modifier
                // add focusRequester modifier before the focusable (or even in the parent)
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
        )
        Button(onClick = { focusRequester.requestFocus() }) {
            Text("Bring focus to the text above")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun FocusGroupSample() {
    Row {
        Column(Modifier.focusGroup()) {
            Button({}) { Text("Row1 Col1") }
            Button({}) { Text("Row2 Col1") }
            Button({}) { Text("Row3 Col1") }
        }
        Column(Modifier.focusGroup()) {
            Button({}) { Text("Row1 Col2") }
            Button({}) { Text("Row2 Col2") }
            Button({}) { Text("Row3 Col2") }
        }
    }
}

@Sampled
@Composable
fun FocusableFocusGroupSample() {
    val interactionSource = remember { MutableInteractionSource() }
    LazyRow(
        Modifier
            .focusable(interactionSource = interactionSource)
            .border(1.dp, if (interactionSource.collectIsFocusedAsState().value) Red else Black)
    ) {
        repeat(10) {
            item {
                Button({}) { Text("Button$it") }
            }
        }
    }
}
