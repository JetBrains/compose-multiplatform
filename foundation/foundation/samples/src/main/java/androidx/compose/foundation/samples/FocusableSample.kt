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
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focusRequester

@Sampled
@Composable
@OptIn(ExperimentalFocus::class)
fun FocusableSample() {
    // initialize focus requester to be able to request focus programmatically
    val requester = FocusRequester()
    // interaction state to track changes of the component's interactions (like "focused")
    val interactionState = remember { InteractionState() }

    // text below will change when we focus it via button click
    val isFocused = interactionState.contains(Interaction.Focused)
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
                .focusRequester(requester)
                .focusable(interactionState = interactionState)
        )
        Button(onClick = { requester.requestFocus() }) {
            Text("Bring focus to the text above")
        }
    }
}