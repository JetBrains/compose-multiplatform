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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TextFieldsDemo() {
    LazyColumn(
        modifier = Modifier.fillMaxHeight()
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
            Text("Text field with helper/error message")
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
    }
}

@Composable
fun VerticalAlignmentsInTextField() {
    Column {
        val singleLine = remember { mutableStateOf(false) }
        val label = remember { mutableStateOf(false) }
        val text = remember { mutableStateOf("") }

        Spacer(Modifier.height(10.dp))
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

        Spacer(Modifier.height(10.dp))
        val textFieldModifier = Modifier
            .align(Alignment.CenterHorizontally)
            .width(300.dp)
            .heightIn(max = 200.dp)
            .then(if (singleLine.value) Modifier else Modifier.heightIn(min = 100.dp))
        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            label = { if (label.value) Text("Label") },
            singleLine = singleLine.value,
            modifier = textFieldModifier
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = text.value,
            onValueChange = { text.value = it },
            label = { if (label.value) Text("Label") },
            singleLine = singleLine.value,
            modifier = textFieldModifier
        )
    }
}

@Composable
fun MaterialTextFieldDemo() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(PaddingValues(10.dp))) {
        var text by savedInstanceState { "" }
        var leadingChecked by savedInstanceState { false }
        var trailingChecked by savedInstanceState { false }
        val characterCounterChecked by savedInstanceState { false }
        var singleLineChecked by savedInstanceState { true }
        var selectedOption by savedInstanceState { Option.None }
        var selectedTextField by savedInstanceState { TextFieldType.Filled }
        var disabled by savedInstanceState { false }
        var readOnly by savedInstanceState { false }

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
                        leadingIcon = { if (leadingChecked) Icon(Icons.Filled.Favorite) },
                        trailingIcon = { if (trailingChecked) Icon(Icons.Filled.Info) },
                        isErrorValue = selectedOption == Option.Error,
                        modifier = Modifier.width(300.dp)
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
                        leadingIcon = { if (leadingChecked) Icon(Icons.Filled.Favorite) },
                        trailingIcon = { if (trailingChecked) Icon(Icons.Filled.Info) },
                        isErrorValue = selectedOption == Option.Error,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
            }
        }

        Box(Modifier.preferredHeight(150.dp).align(Alignment.CenterHorizontally)) {
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
                        onClick = { selectedTextField = TextFieldType.valueOf(textType) }
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

            Spacer(Modifier.preferredHeight(20.dp))

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
                        onClick = { selectedOption = Option.valueOf(text) }
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
    Spacer(Modifier.preferredHeight(10.dp))
}

@Composable
private fun OptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(Modifier.padding(start = 10.dp, top = 10.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Spacer(Modifier.preferredWidth(20.dp))
        Text(text = title, style = MaterialTheme.typography.body1)
    }
}

/**
 * Helper message option
 */
private enum class Option { None, Helper, Error }

private enum class TextFieldType { Filled, Outlined }
