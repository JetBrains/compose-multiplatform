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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun BasicTextFieldSample() {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    BasicTextField(
        value = value,
        onValueChange = {
            // it is crucial that the update is fed back into BasicTextField in order to
            // see updates on the text
            value = it
        }
    )
}

@Sampled
@Composable
fun BasicTextFieldWithStringSample() {
    var value by rememberSaveable { mutableStateOf("initial value") }
    BasicTextField(
        value = value,
        onValueChange = {
            // it is crucial that the update is fed back into BasicTextField in order to
            // see updates on the text
            value = it
        }
    )
}

@Sampled
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PlaceholderBasicTextFieldSample() {
    var value by rememberSaveable { mutableStateOf("initial value") }
    Box {
        BasicTextField(
            value = value,
            onValueChange = { value = it }
        )
        if (value.isEmpty()) {
            Text(text = "Placeholder")
        }
    }
}

@Sampled
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TextFieldWithIconSample() {
    var value by rememberSaveable { mutableStateOf("initial value") }
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        decorationBox = { innerTextField ->
            // Because the decorationBox is used, the whole Row gets the same behaviour as the
            // internal input field would have otherwise. For example, there is no need to add a
            // Modifier.clickable to the Row anymore to bring the text field into focus when user
            // taps on a larger text field area which includes paddings and the icon areas.
            Row(
                Modifier
                    .background(Color.LightGray, RoundedCornerShape(percent = 30))
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.MailOutline, contentDescription = null)
                Spacer(Modifier.width(16.dp))
                innerTextField()
            }
        }
    )
}

@Sampled
@Composable
fun CreditCardSample() {
    /** The offset translator used for credit card input field */
    val creditCardOffsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return when {
                offset < 4 -> offset
                offset < 8 -> offset + 1
                offset < 12 -> offset + 2
                offset <= 16 -> offset + 3
                else -> 19
            }
        }

        override fun transformedToOriginal(offset: Int): Int {
            return when {
                offset <= 4 -> offset
                offset <= 9 -> offset - 1
                offset <= 14 -> offset - 2
                offset <= 19 -> offset - 3
                else -> 16
            }
        }
    }

    /**
     * Converts up to 16 digits to hyphen connected 4 digits string. For example,
     * "1234567890123456" will be shown as "1234-5678-9012-3456"
     */
    val creditCardTransformation = VisualTransformation { text ->
        val trimmedText = if (text.text.length > 16) text.text.substring(0..15) else text.text
        var transformedText = ""
        trimmedText.forEachIndexed { index, char ->
            transformedText += char
            if ((index + 1) % 4 == 0 && index != 15) transformedText += "-"
        }
        TransformedText(AnnotatedString(transformedText), creditCardOffsetTranslator)
    }

    var text by rememberSaveable { mutableStateOf("") }
    BasicTextField(
        value = text,
        onValueChange = { input ->
            if (input.length <= 16 && input.none { !it.isDigit() }) {
                text = input
            }
        },
        modifier = Modifier.size(170.dp, 30.dp).background(Color.LightGray).wrapContentSize(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = creditCardTransformation
    )
}