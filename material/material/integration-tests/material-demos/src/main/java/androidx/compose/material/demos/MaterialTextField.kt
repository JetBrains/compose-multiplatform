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

package androidx.compose.material.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.samples.PasswordTextField
import androidx.compose.material.samples.SimpleOutlinedTextFieldSample
import androidx.compose.material.samples.TextArea
import androidx.compose.material.samples.TextFieldSample
import androidx.compose.material.samples.TextFieldWithErrorState
import androidx.compose.material.samples.TextFieldWithHelperMessage
import androidx.compose.material.samples.TextFieldWithHideKeyboardOnImeAction
import androidx.compose.material.samples.TextFieldWithIcons
import androidx.compose.material.samples.TextFieldWithPlaceholder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TextFieldsDemo() {
    LazyColumn(
        modifier = Modifier.wrapContentSize(Alignment.Center).width(280.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text("Password text field")
            PasswordTextField()
        }
        item {
            Text("Text field with leading and trailing icons")
            TextFieldWithIcons()
        }
        item {
            Text("Outlined text field")
            SimpleOutlinedTextFieldSample()
        }
        item {
            Text("Text field with placeholder")
            TextFieldWithPlaceholder()
        }
        item {
            Text("Text field with error state handling")
            TextFieldWithErrorState()
        }
        item {
            Text("Text field with helper message")
            TextFieldWithHelperMessage()
        }
        item {
            Text("Hide keyboard on IME action")
            TextFieldWithHideKeyboardOnImeAction()
        }
        item {
            Text("TextFieldValue overload")
            TextFieldSample()
        }
        item {
            Text("Outlined text field with custom shape")
            CustomShapeOutlinedTextFieldSample()
        }
        item {
            Text("Text area")
            TextArea()
        }
    }
}

@Composable
fun VerticalAlignmentsInTextField() {
    Column {
        val singleLine = remember { mutableStateOf(false) }
        val label = remember { mutableStateOf(false) }
        val text = remember { mutableStateOf("") }

        Spacer(Modifier.requiredHeight(10.dp))
        OptionRow(
            title = "Single line",
            checked = singleLine.value,
            onCheckedChange = { singleLine.value = it }
        )
        OptionRow(
            title = "Label",
            checked = label.value,
            onCheckedChange = { label.value = it }
        )

        Spacer(Modifier.requiredHeight(10.dp))
        val textFieldModifier = Modifier
            .align(Alignment.CenterHorizontally)
            .requiredWidth(300.dp)
            .requiredHeightIn(max = 200.dp)
            .then(if (singleLine.value) Modifier else Modifier.requiredHeightIn(min = 100.dp))
        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            label = if (label.value) {
                @Composable { Text("Label") }
            } else null,
            singleLine = singleLine.value,
            modifier = textFieldModifier
        )
        Spacer(Modifier.requiredHeight(10.dp))
        OutlinedTextField(
            value = text.value,
            onValueChange = { text.value = it },
            label = if (label.value) {
                @Composable { Text("Label") }
            } else null,
            singleLine = singleLine.value,
            modifier = textFieldModifier
        )
    }
}

@Composable
fun MaterialTextFieldDemo() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(10.dp))) {
        var text by rememberSaveable { mutableStateOf("") }
        var leadingChecked by rememberSaveable { mutableStateOf(false) }
        var trailingChecked by rememberSaveable { mutableStateOf(false) }
        val characterCounterChecked by rememberSaveable { mutableStateOf(false) }
        var singleLineChecked by rememberSaveable { mutableStateOf(true) }
        var selectedOption by rememberSaveable { mutableStateOf(Option.None) }
        var selectedTextField by rememberSaveable { mutableStateOf(TextFieldType.Filled) }
        var disabled by rememberSaveable { mutableStateOf(false) }
        var readOnly by rememberSaveable { mutableStateOf(false) }

        val textField: @Composable () -> Unit = @Composable {
            when (selectedTextField) {
                TextFieldType.Filled ->
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        enabled = !disabled,
                        readOnly = readOnly,
                        singleLine = singleLineChecked,
                        label = {
                            val label =
                                "Label" + if (selectedOption == Option.Error) "*" else ""
                            Text(text = label)
                        },
                        leadingIcon = if (leadingChecked) {
                            @Composable { Icon(Icons.Filled.Favorite, "Favorite") }
                        } else {
                            null
                        },
                        trailingIcon = if (trailingChecked) {
                            @Composable { Icon(Icons.Filled.Info, "Info") }
                        } else {
                            null
                        },
                        isError = selectedOption == Option.Error,
                        modifier = Modifier.requiredWidth(300.dp)
                    )
                TextFieldType.Outlined ->
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        enabled = !disabled,
                        readOnly = readOnly,
                        singleLine = singleLineChecked,
                        label = {
                            val label =
                                "Label" + if (selectedOption == Option.Error) "*" else ""
                            Text(text = label)
                        },
                        leadingIcon = if (leadingChecked) {
                            @Composable { Icon(Icons.Filled.Favorite, "Favorite") }
                        } else {
                            null
                        },
                        trailingIcon = if (trailingChecked) {
                            @Composable { Icon(Icons.Filled.Info, "Info") }
                        } else {
                            null
                        },
                        isError = selectedOption == Option.Error,
                        modifier = Modifier.requiredWidth(300.dp)
                    )
            }
        }

        Box(Modifier.height(150.dp).align(Alignment.CenterHorizontally)) {
            if (selectedOption == Option.None) {
                textField()
            } else {
                TextFieldWithMessage(selectedOption, textField)
            }
        }

        Column {
            Title("Text field type")
            TextFieldType.values().map { it.name }.forEach { textType ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (textType == selectedTextField.name),
                            onClick = {
                                selectedTextField = TextFieldType.valueOf(textType)
                            }
                        )
                        .padding(horizontal = 16.dp)
                ) {
                    RadioButton(
                        selected = (textType == selectedTextField.name),
                        onClick = null
                    )
                    Text(
                        text = textType,
                        style = MaterialTheme.typography.body1.merge(),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            Title("Options")
            OptionRow(
                title = "Leading icon",
                checked = leadingChecked,
                onCheckedChange = { leadingChecked = it }
            )
            OptionRow(
                title = "Trailing icon",
                checked = trailingChecked,
                onCheckedChange = { trailingChecked = it }
            )
            OptionRow(
                title = "Single line",
                checked = singleLineChecked,
                onCheckedChange = { singleLineChecked = it }
            )
            OptionRow(
                title = "Character counter (TODO)",
                checked = characterCounterChecked,
                enabled = false,
                onCheckedChange = { /* TODO */ }
            )

            Spacer(Modifier.height(20.dp))

            Title("Assistive text")
            Option.values().map { it.name }.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (text == selectedOption.name),
                            onClick = { selectedOption = Option.valueOf(text) }
                        )
                        .padding(horizontal = 16.dp)
                ) {
                    RadioButton(
                        selected = (text == selectedOption.name),
                        onClick = null
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.body1.merge(),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            Title("Other settings")
            OptionRow(
                title = "Read-only",
                checked = readOnly,
                onCheckedChange = { readOnly = it }
            )
            OptionRow(
                title = "Disabled",
                checked = disabled,
                onCheckedChange = { disabled = it }
            )
        }
    }
}

@Composable
fun CustomShapeOutlinedTextFieldSample() {
    var text by rememberSaveable { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label") },
        shape = CutCornerShape(5.dp)
    )
}

/**
 * Text field with helper or error message below.
 */
@Composable
private fun TextFieldWithMessage(
    helperMessageOption: Option,
    content: @Composable () -> Unit
) {
    val typography = MaterialTheme.typography.caption
    val color = when (helperMessageOption) {
        Option.Helper -> {
            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        }
        Option.Error -> MaterialTheme.colors.error
        else -> Color.Unspecified
    }

    Column {
        Box(modifier = Modifier.weight(1f, fill = false)) { content() }
        Text(
            text = "Helper message",
            color = color,
            style = typography,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun ColumnScope.Title(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.body1,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun OptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        Modifier
            .padding(start = 10.dp, top = 10.dp)
            .fillMaxWidth()
            .toggleable(
                value = checked, onValueChange = onCheckedChange, enabled = enabled
            )
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(20.dp))
        Text(text = title, style = MaterialTheme.typography.body1)
    }
}

/**
 * Helper message option
 */
private enum class Option { None, Helper, Error }

private enum class TextFieldType { Filled, Outlined }
