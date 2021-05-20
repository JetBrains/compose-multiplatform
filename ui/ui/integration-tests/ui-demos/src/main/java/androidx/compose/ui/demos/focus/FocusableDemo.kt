/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.ui.demos.focus

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun FocusableDemo() {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        CenteredRow {
            Text("Click on any focusable to bring it into focus:")
        }
        CenteredRow {
            FocusableText("Focusable 1")
        }
        CenteredRow {
            FocusableText("Focusable 2")
        }
        CenteredRow {
            FocusableText("Focusable 3")
        }
    }
}

@Composable
private fun FocusableText(text: String) {
    var color by remember { mutableStateOf(Black) }
    val focusRequester = FocusRequester()
    Text(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { color = if (it.isFocused) Green else Black }
            .focusTarget()
            .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
        text = text,
        color = color
    )
}

@Composable
private fun CenteredRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}
