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

package androidx.compose.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.mutableStateListOf
import androidx.compose.state
import androidx.compose.foundation.Text
import androidx.ui.input.TextFieldValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.ui.material.Button
import androidx.ui.text.CoreTextField

@Composable
@Sampled
fun stateListSample() {
    @Composable
    fun Names() {
        var name by state { TextFieldValue("user") }
        val names = mutableStateListOf<String>()

        Column {
            Row {
                CoreTextField(
                    value = name,
                    onValueChange = { name = it }
                )
                Button(onClick = { names.add(name.text) }) {
                    Text("Add")
                }
            }
            Text("Added names:")
            Column {
                for (addedName in names) {
                    Text(addedName)
                }
            }
        }
    }
}
