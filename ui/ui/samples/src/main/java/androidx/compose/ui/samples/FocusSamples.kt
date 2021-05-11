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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager

@Sampled
@Composable
fun MoveFocusSample() {
    val focusManager = LocalFocusManager.current
    Column {
        Row {
            Box(Modifier.focusable())
            Box(Modifier.focusable())
        }
        Row {
            Box(Modifier.focusable())
            Box(Modifier.focusable())
        }
        Button(onClick = { focusManager.moveFocus(FocusDirection.Right) }) { Text("Right") }
        Button(onClick = { focusManager.moveFocus(FocusDirection.Left) }) { Text("Left") }
        Button(onClick = { focusManager.moveFocus(FocusDirection.Up) }) { Text("Up") }
        Button(onClick = { focusManager.moveFocus(FocusDirection.Down) }) { Text("Down") }
    }
}