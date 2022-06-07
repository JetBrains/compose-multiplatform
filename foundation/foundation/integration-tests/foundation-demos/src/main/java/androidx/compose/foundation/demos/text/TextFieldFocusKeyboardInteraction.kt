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

import android.widget.Button as AndroidButton
import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Preview
@Composable
fun TextFieldFocusKeyboardInteraction() {
    val focusManager = LocalFocusManager.current
    Column(
        verticalArrangement = spacedBy(4.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Button(onClick = { focusManager.clearFocus() }) {
            Text("Clear focus")
        }

        Column(
            Modifier
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Text("Outer Compose Fields")
            FocusableFieldRow()
        }

        Column(
            Modifier
                .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Text("Android EditTexts")
            AndroidView(::AndroidFieldRow)
        }

        Text(
            "Click around in the different text fields above to watch how the soft keyboard " +
                "changes, and is shown and hidden. Click the buttons to move focus to something " +
                "that doesn't request the keyboard itself. The bottom Compose text fields are in " +
                "a different ComposeView than the top ones."
        )
    }
}

@Composable
private fun FocusableFieldRow() {
    Row(horizontalArrangement = spacedBy(4.dp)) {
        var text1 by remember { mutableStateOf("") }
        var text2 by remember { mutableStateOf("") }
        var text3 by remember { mutableStateOf("") }
        TextField(
            text1,
            onValueChange = { text1 = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        TextField(
            text2,
            onValueChange = { text2 = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        TextField(
            text3,
            onValueChange = { text3 = it },
            modifier = Modifier.weight(1f)
        )

        val interactionSource = remember { MutableInteractionSource() }
        val isButtonFocused by interactionSource.collectIsFocusedAsState()
        val buttonFocusRequester = remember { FocusRequester() }
        Button(
            onClick = { buttonFocusRequester.requestFocus() },
            Modifier
                .weight(1f)
                .focusRequester(buttonFocusRequester)
                .focusable(interactionSource = interactionSource)
        ) {
            Text(if (isButtonFocused) "Focused" else "Click to focus")
        }
    }
}

private class AndroidFieldRow(context: Context) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        addView(row, LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val text1 = EditText(context)
        text1.inputType = EditorInfo.TYPE_CLASS_NUMBER
        row.addView(text1, LayoutParams(0, WRAP_CONTENT, 1f))
        val text2 = EditText(context)
        row.addView(text2, LayoutParams(0, WRAP_CONTENT, 1f))
        val button = AndroidButton(context)
        fun updateButtonText() {
            button.text = if (button.isFocused) "Focused" else "Click to focus"
        }
        updateButtonText()
        button.isFocusableInTouchMode = true
        button.setOnClickListener { button.requestFocus() }
        button.setOnFocusChangeListener { _, _ -> updateButtonText() }
        row.addView(button, LayoutParams(0, WRAP_CONTENT, 1f))

        val innerCompose = ComposeView(context)
        innerCompose.setContent {
            Column(
                Modifier
                    .border(1.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Text("Inner Compose Fields")
                FocusableFieldRow()
            }
        }
        addView(innerCompose, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }
}