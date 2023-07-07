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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun OneDimensionalFocusSearchDemo() {
    Column {
        Text("Use Tab/Shift+Tab to move focus:")
        Row(Modifier.focusableWithBorder()) {
            Box(Modifier.size(100.dp, 100.dp).focusableWithBorder())
            Box(Modifier.size(100.dp, 100.dp).focusableWithBorder())
            Box(Modifier.size(100.dp, 100.dp).focusableWithBorder())
        }
        Box(Modifier.size(300.dp, 100.dp).focusableWithBorder()) {
            Box(Modifier.offset(0.dp, 0.dp).size(100.dp, 100.dp).focusableWithBorder())
            Box(Modifier.offset(200.dp, 0.dp).size(100.dp, 100.dp).focusableWithBorder())
            Box(Modifier.offset(100.dp, 0.dp).size(100.dp, 100.dp).focusableWithBorder())
        }
    }
}

private fun Modifier.focusableWithBorder() = composed {
    var borderColor by remember { mutableStateOf(Color.Black) }
    Modifier.onFocusChanged { borderColor = if (it.isFocused) Color.Red else Color.Black }
        .border(5.dp, borderColor)
        .focusable()
}
