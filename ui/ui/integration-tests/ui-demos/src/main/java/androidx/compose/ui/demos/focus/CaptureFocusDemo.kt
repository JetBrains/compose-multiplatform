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

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.unit.dp

@Composable
fun CaptureFocusDemo() {
    Column {
        Text(
            "This demo demonstrates how a component can capture focus when it is in an " +
                "invalidated state."
        )

        Spacer(Modifier.height(30.dp))

        Text("Enter a word that is 5 characters or shorter")
        val shortWord = remember { FocusRequester() }
        var shortString by remember { mutableStateOf("apple") }
        var shortStringBorder by remember { mutableStateOf(Transparent) }
        TextField(
            value = shortString,
            onValueChange = {
                shortString = it
                if (shortString.length > 5) shortWord.captureFocus() else shortWord.freeFocus()
            },
            modifier = Modifier
                .border(2.dp, shortStringBorder)
                .focusRequester(shortWord)
                .onFocusChanged { shortStringBorder = if (it.isCaptured) Red else Transparent }
        )

        Spacer(Modifier.height(30.dp))

        Text("Enter a word that is longer than 5 characters")
        val longWord = remember { FocusRequester() }
        var longString by remember { mutableStateOf("pineapple") }
        var longStringBorder by remember { mutableStateOf(Transparent) }

        TextField(
            value = longString,
            onValueChange = {
                longString = it
                if (longString.length < 5) longWord.captureFocus() else longWord.freeFocus()
            },
            modifier = Modifier
                .border(2.dp, longStringBorder)
                .focusRequester(longWord)
                .onFocusChanged { longStringBorder = if (it.isCaptured) Red else Transparent }
        )
    }
}