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

package androidx.compose.foundation.text

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.selection.DisableSelection
import androidx.compose.ui.selection.SelectionContainer
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Implements disabled and readonly text field using Text.
 */
@Composable
internal fun InactiveTextField(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionState: InteractionState? = null
) {
    val transformedText = remember(value, visualTransformation) {
        visualTransformation.filter(value.annotatedString)
    }.text

    val text: @Composable (Modifier) -> Unit = @Composable { textModifier ->
        BasicText(
            text = transformedText,
            modifier = textModifier.semantics {
                if (!enabled) disabled()
            },
            softWrap = !singleLine,
            maxLines = if (singleLine) 1 else maxLines,
            style = textStyle,
            onTextLayout = onTextLayout
        )
    }
    val textModifier = modifier.focusable(enabled, interactionState)
    if (enabled) {
        SelectionContainer(textModifier) {
            text(Modifier)
        }
    } else {
        DisableSelection {
            text(textModifier)
        }
    }
}