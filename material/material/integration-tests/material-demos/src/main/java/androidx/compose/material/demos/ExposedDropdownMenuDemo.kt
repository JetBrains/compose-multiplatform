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

package androidx.compose.material.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.samples.EditableExposedDropdownMenuSample
import androidx.compose.material.samples.ExposedDropdownMenuSample
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun ExposedDropdownMenuDemo() {
    Column(
        verticalArrangement = Arrangement.spacedBy(DefaultSpace)
    ) {
        ExposedDropdownMenuSample()

        EditableExposedDropdownMenuSample()

        OutlinedExposedDropdownMenu(isReadOnly = true)

        OutlinedExposedDropdownMenu()
    }
}

@Composable
fun OutlinedExposedDropdownMenu(isReadOnly: Boolean = false) {
    ExposedDropdownMenuImpl(true, isReadOnly)
}

@OptIn(ExperimentalMaterialApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ExposedDropdownMenuImpl(isOutlined: Boolean, isReadOnly: Boolean) {
    val options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5")
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(if (isReadOnly) options[0] else "") }
    // We want to react on tap/press on TextField to show menu
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        if (isOutlined) {
            OutlinedTextField(
                readOnly = isReadOnly,
                value = selectedOptionText,
                onValueChange = { selectedOptionText = it },
                label = { Text("Label") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
        } else {
            TextField(
                readOnly = isReadOnly,
                value = selectedOptionText,
                onValueChange = { selectedOptionText = it },
                label = { Text("Label") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
        }
        if (
            options.any {
                it.contains(
                        selectedOptionText,
                        ignoreCase = true
                    )
            } || isReadOnly
        ) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                options.forEach { selectionOption ->
                    if (selectionOption.contains(
                            selectedOptionText,
                            ignoreCase = true
                        ) || isReadOnly
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                selectedOptionText = selectionOption
                                expanded = false
                            }
                        ) {
                            Text(text = selectionOption)
                        }
                    }
                }
            }
        }
    }
}

private val DefaultSpace = 20.dp