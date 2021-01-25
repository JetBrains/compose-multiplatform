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

@file:Suppress("DEPRECATION")

package androidx.compose.foundation

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Composable that enables users to edit text via hardware or software keyboard.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains
 * the text entered by user, as well as selection, cursor and text composition information.
 * Please check [TextFieldValue] for the description of its contents.
 *
 * It is crucial that the value provided in the [onValueChange] is fed back into [BaseTextField] in
 * order to have the final state of the text being displayed. Example usage:
 * @sample androidx.compose.foundation.samples.TextFieldSample
 *
 * Please keep in mind that [onValueChange] is useful to be informed about the latest state of the
 * text input by users, however it is generally not recommended to modify the values in the
 * [TextFieldValue] that you get via [onValueChange] callback. Any change to the values in
 * [TextFieldValue] may result in a context reset and end up with input session restart. Such
 * a scenario would cause glitches in the UI or text input experience for users.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material Design Filled text field is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * For example, if you need to include a hint in your TextField you can write a composable as below:
 * @sample androidx.compose.foundation.samples.PlaceholderTextFieldSample
 *
 * @param value The [TextFieldValue] to be shown in the [BaseTextField].
 * @param onValueChange Called when the input service updates values in
 * [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param textColor [Color] to apply to the text. If [Color.Unspecified], and [textStyle] has no
 * color set, this will be [AmbientContentColor].
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * The default [textStyle] uses the [AmbientTextStyle] defined by the theme
 * @param keyboardType The keyboard type to be used in this text field. Note that this input type
 * is honored by IME and shows corresponding keyboard but this is not guaranteed. For example,
 * some IME may send non-ASCII character even if you set [KeyboardType.Ascii].
 * @param imeAction The IME action. This IME action is honored by IME and may show specific icons
 * on the keyboard. For example, search icon may be shown if [ImeAction.Search] is specified.
 * Then, when user tap that key, the [onImeActionPerformed] callback is called with specified
 * ImeAction.
 *  * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param visualTransformation The visual transformation filter for changing the visual
 * representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 * @param onTextInputStarted Callback that is executed when the initialization has done for
 * communicating with platform text input service, e.g. software keyboard on Android. Called with
 * [SoftwareKeyboardController] instance which can be used for requesting input show/hide software
 * keyboard.
 * @param cursorColor Color of the cursor. If [Color.Unspecified], there will be no cursor drawn
 *
 * @see TextFieldValue
 * @see ImeAction
 * @see KeyboardType
 * @see VisualTransformation
 */
@Deprecated(
    "Use BasicTextField instead.",
    replaceWith = ReplaceWith(
        "BasicTextField(value, onValueChange, modifier, textStyle.merge(TextStyle(color = " +
            "textColor)), keyboardType, imeAction, onImeActionPerformed, visualTransformation, " +
            "onTextLayout, onTextInputStarted, cursorColor)",
        "androidx.compose.foundation.text.BasicTextField",
        "androidx.compose.foundation.AmbientContentColor",
        "androidx.compose.foundation.AmbientTextStyle",
        "androidx.compose.ui.text.TextStyle",
    )
)
@Composable
@ExperimentalFoundationApi
fun BaseTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    textStyle: TextStyle = TextStyle(),
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    cursorColor: Color = AmbientContentColor.current
) {
    val color = textColor.takeOrElse { textStyle.color.takeOrElse { AmbientContentColor.current } }
    val mergedStyle = textStyle.merge(TextStyle(color = color))

    BasicTextField(
        value = value,
        modifier = modifier,
        onValueChange = onValueChange,
        textStyle = mergedStyle,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        onImeActionPerformed = onImeActionPerformed,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        onTextInputStarted = onTextInputStarted,
        cursorColor = cursorColor
    )
}
