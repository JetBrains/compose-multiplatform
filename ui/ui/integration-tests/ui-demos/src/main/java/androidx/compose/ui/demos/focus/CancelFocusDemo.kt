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

package androidx.compose.ui.demos.focus

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement.SpaceEvenly
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CancelFocusDemo() {
    Column {
        Text("Use the arrow keys to move focus left/right/up/down.")
    }
    Column(Modifier.fillMaxSize(), SpaceEvenly) {
        var blockFocusMove by remember { mutableStateOf(false) }
        Row {
            Text("Cancel focus moves between 3 and 4")
            Switch(checked = blockFocusMove, onCheckedChange = { blockFocusMove = !blockFocusMove })
        }
        Row(Modifier.fillMaxWidth(), SpaceEvenly) {
            Text("1", Modifier.focusableWithBorder())
            Text("2", Modifier.focusableWithBorder())
        }
        Row(Modifier.fillMaxWidth(), SpaceEvenly) {
            Text(
                text = "3",
                modifier = Modifier
                    .focusProperties { if (blockFocusMove) { right = Cancel } }
                    .focusableWithBorder()
            )
            Text(
                text = "4",
                modifier = Modifier
                    .focusProperties { left = if (blockFocusMove) Cancel else Default }
                    .focusableWithBorder()
            )
        }
    }
}

@SuppressLint("ModifierInspectorInfo")
private fun Modifier.focusableWithBorder() = composed {
    var color by remember { mutableStateOf(Black) }
    Modifier
        .size(50.dp)
        .border(1.dp, color)
        .onFocusChanged { color = if (it.isFocused) Red else Black }
        .focusable()
}
