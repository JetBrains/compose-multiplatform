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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.samples.CreditCardSample
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview

/**
 * The offset translator which works for all offset keep remains the same.
 */
private val identityTranslator = object : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int = offset
    override fun transformedToOriginal(offset: Int): Int = offset
}

/**
 * The visual filter for capitalization.
 *
 * This filer converts ASCII characters to capital form.
 */
private class CapitalizeTransformation(
    val locale: LocaleList = LocaleList("en-US")
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Note: identityTranslator doesn't work for some locale, e.g. Turkish
        return TransformedText(AnnotatedString(text.text).toUpperCase(locale), identityTranslator)
    }
}

/**
 * The offset translator for phone number
 *
 * @see phoneNumberFilter
 */
private fun phoneNumberOffsetTranslator(text: String) = object : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return when (offset) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 6
            4 -> 7
            5 -> 8
            6 -> 10
            7 -> 11
            8 -> 12
            9 -> 13
            else -> 14
        }
    }

    override fun transformedToOriginal(offset: Int): Int {
        return when (offset) {
            0 -> 0
            1 -> 0
            2 -> 1
            3 -> 2
            4 -> 3
            5 -> 3
            6 -> 3
            7 -> 4
            8 -> 5
            9 -> 6
            10 -> 6
            11 -> 7
            12 -> 8
            13 -> 9
            else -> 10
        }.coerceAtMost(text.length)
    }
}

/**
 * The visual filter for phone number.
 *
 * This filter converts up to 10 digits to phone number form.
 * For example, "1234567890" will be shown as "(123) 456-7890".
 */
private val phoneNumberFilter = VisualTransformation { text ->
    val trimmed = if (text.text.length >= 10) text.text.substring(0..9) else text.text
    val filled = trimmed + "_".repeat(10 - trimmed.length)
    val res = "(" + filled.substring(0..2) + ") " + filled.substring(3..5) + "-" +
        filled.substring(6..9)
    TransformedText(AnnotatedString(text = res), phoneNumberOffsetTranslator(text.text))
}

private val emailFilter = VisualTransformation { text ->
    if (text.text.indexOf("@") == -1) {
        TransformedText(
            AnnotatedString(text = text.text + "@gmail.com"),
            emailOffsetTranslator(text.text)
        )
    } else {
        TransformedText(text, identityTranslator)
    }
}

private fun emailOffsetTranslator(text: String) = object : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        return (offset).coerceAtMost(text.length + 10)
    }

    override fun transformedToOriginal(offset: Int): Int {
        return if (offset <= text.length) {
            offset
        } else {
            (offset - 10).coerceAtMost(text.length).coerceAtLeast(0)
        }
    }
}

@Preview
@Composable
fun VariousInputFieldDemo() {
    LazyColumn {
        item {
            TagLine(tag = "Capitalization")
            VariousEditLine(
                keyboardType = KeyboardType.Ascii,
                onValueChange = { old, new ->
                    if (new.any { !it.isLetterOrDigit() }) old else new
                },
                visualTransformation = CapitalizeTransformation()
            )
        }
        item {
            TagLine(tag = "Capitalization (Turkish)")
            VariousEditLine(
                keyboardType = KeyboardType.Ascii,
                onValueChange = { old, new ->
                    if (new.any { !it.isLetterOrDigit() }) old else new
                },
                visualTransformation = CapitalizeTransformation(LocaleList("tr"))
            )
        }
        item {
            TagLine(tag = "Password")
            VariousEditLine(
                keyboardType = KeyboardType.Password,
                onValueChange = { old, new ->
                    if (new.any { !it.isLetterOrDigit() }) old else new
                },
                visualTransformation = PasswordVisualTransformation()
            )
        }
        item {
            TagLine(tag = "Phone Number")
            VariousEditLine(
                keyboardType = KeyboardType.Number,
                onValueChange = { old, new ->
                    if (new.length > 10 || new.any { !it.isDigit() }) old else new
                },
                visualTransformation = phoneNumberFilter
            )
        }
        item {
            TagLine(tag = "Credit Card")
            CreditCardSample()
        }
        item {
            TagLine(tag = "Email Suggestion")
            VariousEditLine(
                keyboardType = KeyboardType.Email,
                visualTransformation = emailFilter
            )
        }
        item {
            TagLine(tag = "Editfield with Hint Text")
            HintEditText {
                Text(
                    text = "Hint Text",
                    color = Color(0xFF888888),
                    style = TextStyle(fontSize = fontSize8)
                )
            }
        }
        item {
            TagLine(tag = "TextField MutableInteractionSource")
            InteractionSourceTextField()
        }
    }
}

@Composable
private fun VariousEditLine(
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onValueChange: (String, String) -> String = { _, new -> new },
    visualTransformation: VisualTransformation
) {
    val state = rememberSaveable { mutableStateOf("") }
    BasicTextField(
        modifier = demoTextFieldModifiers,
        value = state.value,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        visualTransformation = visualTransformation,
        onValueChange = {
            val value = onValueChange(state.value, it)
            state.value = value
        },
        textStyle = TextStyle(fontSize = fontSize8)
    )
}

@Composable
private fun HintEditText(content: @Composable () -> Unit) {
    val state = rememberSaveable { mutableStateOf("") }

    Box(demoTextFieldModifiers) {
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.value,
            onValueChange = { state.value = it },
            textStyle = TextStyle(fontSize = fontSize8)
        )
        if (state.value.isEmpty()) {
            content()
        }
    }
}

@Composable
private fun InteractionSourceTextField() {
    val state = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val interactionSource = remember { MutableInteractionSource() }

    Column(demoTextFieldModifiers) {
        Text(
            "Pressed?: ${interactionSource.collectIsPressedAsState().value}",
            fontSize = fontSize4
        )
        Text(
            "Focused?: ${interactionSource.collectIsFocusedAsState().value}",
            fontSize = fontSize4
        )
        Text(
            "Dragged?: ${interactionSource.collectIsDraggedAsState().value}",
            fontSize = fontSize4
        )
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.value,
            singleLine = true,
            interactionSource = interactionSource,
            onValueChange = { state.value = it },
            textStyle = TextStyle(fontSize = fontSize8)
        )
    }
}