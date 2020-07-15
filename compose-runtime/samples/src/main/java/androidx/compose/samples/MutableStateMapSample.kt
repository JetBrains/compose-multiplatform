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
import androidx.compose.mutableStateMapOf
import androidx.compose.state
import androidx.ui.foundation.Text
import androidx.ui.input.TextFieldValue
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.material.Button
import androidx.ui.text.CoreTextField

@Composable
@Sampled
fun stateMapSample() {
    @Composable
    fun NamesAndAges() {
        var name by state { TextFieldValue("name") }
        var saying by state { TextFieldValue("saying") }
        val sayings = mutableStateMapOf(
            "Caesar" to "Et tu, Brute?",
            "Hamlet" to "To be or not to be",
            "Richard III" to "My kingdom for a horse"
        )

        Column {
            Row {
                CoreTextField(
                    value = name,
                    onValueChange = { name = it }
                )
                CoreTextField(
                    value = saying,
                    onValueChange = { saying = it }
                )
                Button(onClick = { sayings[name.text] = saying.text }) {
                    Text("Add")
                }
                Button(onClick = { sayings.remove(name.text) }) {
                    Text("Remove")
                }
            }
            Text("Sayings:")
            Column {
                for (entry in sayings) {
                    Text("${entry.key} says '${entry.value}'")
                }
            }
        }
    }
}
