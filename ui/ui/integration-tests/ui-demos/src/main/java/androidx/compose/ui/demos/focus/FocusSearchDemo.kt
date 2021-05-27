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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.unit.dp

@Composable
fun FocusSearchDemo() {
    Column {
        Text("Use Tab/Shift+Tab or arrow keys to move focus:")
        Box(Modifier.padding(10.dp).size(330.dp, 280.dp)) {
            FocusableBox(Modifier.offset(10.dp, 10.dp).size(40.dp, 40.dp))
            FocusableBox(Modifier.offset(60.dp, 10.dp).size(40.dp, 40.dp))
            Row(Modifier.offset(110.dp, 10.dp)) {
                FocusableBox(Modifier.size(23.dp, 20.dp))
                FocusableBox(Modifier.padding(horizontal = 10.dp).size(23.dp, 20.dp))
                FocusableBox(Modifier.size(23.dp, 20.dp))
            }
            FocusableBox(Modifier.offset(210.dp, 10.dp).size(40.dp, 120.dp))
            FocusableBox(Modifier.offset(260.dp, 10.dp).size(60.dp, 260.dp)) {
                FocusableBox(Modifier.offset(10.dp, 10.dp).size(40.dp, 40.dp))
                FocusableBox(Modifier.offset(10.dp, 60.dp).size(40.dp, 40.dp))
                FocusableBox(Modifier.offset(10.dp, 110.dp).size(40.dp, 40.dp))
                FocusableBox(Modifier.offset(10.dp, 160.dp).size(40.dp, 40.dp))
                FocusableBox(Modifier.offset(10.dp, 210.dp).size(40.dp, 40.dp))
            }
            FocusableBox(Modifier.offset(60.dp, 60.dp).size(18.dp, 18.dp))
            FocusableBox(Modifier.offset(82.dp, 60.dp).size(18.dp, 18.dp))
            FocusableBox(Modifier.offset(110.dp, 40.dp).size(40.dp, 40.dp))
            FocusableBox(Modifier.offset(160.dp, 40.dp).size(40.dp, 40.dp))
            FocusableBox(Modifier.offset(10.dp, 60.dp).size(40.dp, 210.dp))
            FocusableBox(Modifier.offset(60.dp, 90.dp).size(140.dp, 40.dp))
            FocusableBox(Modifier.offset(60.dp, 140.dp).size(190.dp, 130.dp)) {
                FocusableBox(Modifier.offset(10.dp, 10.dp).size(170.dp, 110.dp)) {
                    FocusableBox(Modifier.offset(10.dp, 10.dp).size(150.dp, 90.dp)) {
                        FocusableBox(Modifier.offset(10.dp, 10.dp).size(130.dp, 70.dp)) {
                            FocusableBox(Modifier.offset(10.dp, 15.dp).size(40.dp, 40.dp))
                            FocusableBox(Modifier.offset(58.dp, 15.dp).size(15.dp, 15.dp))
                            FocusableBox(Modifier.offset(58.dp, 40.dp).size(15.dp, 15.dp))
                            FocusableBox(Modifier.offset(80.dp, 15.dp).size(40.dp, 40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusableBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    var borderColor by remember { mutableStateOf(Black) }
    Box(
        modifier = modifier
            .onFocusChanged { borderColor = if (it.isFocused) Red else Black }
            .border(2.dp, borderColor)
            .focusable(),
        content = content
    )
}
