/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TextFieldDefaults.OutlinedTextFieldDecorationBox
import androidx.compose.material3.tokens.FilledTextFieldTokens
import androidx.compose.material3.tokens.OutlinedTextFieldTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents the colors of the input text, container, and content (including label, placeholder,
 * leading and trailing icons) used in a text field in different states.
 *
 * See [TextFieldDefaults.textFieldColors] for the default colors used in [TextField].
 * See [TextFieldDefaults.outlinedTextFieldColors] for the default colors used in
 * [OutlinedTextField].
 */
@Stable
interface TextFieldColors {
    /**
     * Represents the color used for the input text of this text field.
     *
     * @param enabled whether the text field is enabled
     */
    @Composable
    fun textColor(enabled: Boolean): State<Color>

    /**
     * Represents the container color for this text field.
     *
     * @param enabled whether the text field is enabled
     */
    @Composable
    fun containerColor(enabled: Boolean): State<Color>

    /**
     * Represents the color used for the placeholder of this text field.
     *
     * @param enabled whether the text field is enabled
     */
    @Composable
    fun placeholderColor(enabled: Boolean): State<Color>

    /**
     * Represents the color used for the label of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    fun labelColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color>

    /**
     * Represents the color used for the leading icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    fun leadingIconColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color>

    /**
     * Represents the color used for the trailing icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    fun trailingIconColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color>

    /**
     * Represents the color used for the border indicator of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    fun indicatorColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color>

    /**
     * Represents the color used for the cursor of this text field.
     *
     * @param isError whether the text field's current value is in error
     */
    @Composable
    fun cursorColor(isError: Boolean): State<Color>
}

/**
 * Contains the default values used by [TextField] and [OutlinedTextField].
 */
@Immutable
object TextFieldDefaults {
    /**
     * The default min width applied for a [TextField] and [OutlinedTextField].
     * Note that you can override it by applying Modifier.heightIn directly on a text field.
     */
    val MinHeight = 56.dp

    /**
     * The default min width applied for a [TextField] and [OutlinedTextField].
     * Note that you can override it by applying Modifier.widthIn directly on a text field.
     */
    val MinWidth = 280.dp

    /**
     * The default thickness of the border in [OutlinedTextField] or indicator line in [TextField]
     * in unfocused state.
     */
    val UnfocusedBorderThickness = 1.dp

    /**
     * The default thickness of the border in [OutlinedTextField] or indicator line in [TextField]
     * in focused state.
     */
    val FocusedBorderThickness = 2.dp

    /**
     * A modifier to draw a default bottom indicator line in [TextField]. You can use this modifier
     * if you build your custom text field using [TextFieldDecorationBox] whilst the [TextField]
     * applies it automatically.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param focusedIndicatorLineThickness thickness of the indicator line when text field is
     * focused
     * @param unfocusedIndicatorLineThickness thickness of the indicator line when text field is
     * not focused
     */
    @ExperimentalMaterial3Api
    fun Modifier.indicatorLine(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        focusedIndicatorLineThickness: Dp = FocusedBorderThickness,
        unfocusedIndicatorLineThickness: Dp = UnfocusedBorderThickness
    ) = composed(inspectorInfo = debugInspectorInfo {
        name = "indicatorLine"
        properties["enabled"] = enabled
        properties["isError"] = isError
        properties["interactionSource"] = interactionSource
        properties["colors"] = colors
        properties["focusedIndicatorLineThickness"] = focusedIndicatorLineThickness
        properties["unfocusedIndicatorLineThickness"] = unfocusedIndicatorLineThickness
    }) {
        val stroke = animateBorderStrokeAsState(
            enabled,
            isError,
            interactionSource,
            colors,
            focusedIndicatorLineThickness,
            unfocusedIndicatorLineThickness
        )
        Modifier.drawIndicatorLine(stroke.value)
    }

    /**
     * Composable that draws a default border stroke in [OutlinedTextField]. You can use it to
     * draw a border stroke in your custom text field based on [OutlinedTextFieldDecorationBox].
     * The [OutlinedTextField] applies it automatically.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param focusedBorderThickness thickness of the [OutlinedTextField]'s border when it is in
     * focused state
     * @param unfocusedBorderThickness thickness of the [OutlinedTextField]'s border when it is not
     * in focused state
     */
    @ExperimentalMaterial3Api
    @Composable
    fun BorderBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        shape: Shape = OutlinedTextFieldTokens.ContainerShape.toShape(),
        focusedBorderThickness: Dp = FocusedBorderThickness,
        unfocusedBorderThickness: Dp = UnfocusedBorderThickness
    ) {
        val borderStroke = animateBorderStrokeAsState(
            enabled,
            isError,
            interactionSource,
            colors,
            focusedBorderThickness,
            unfocusedBorderThickness
        )
        Box(Modifier.border(borderStroke.value, shape))
    }

    /**
     * Default content padding applied to [TextField] when there is a label.
     *
     * Note that when label is present, the "top" padding (unlike rest of the paddings) is a
     * distance between the label's last baseline and the top edge of the [TextField]. If the "top"
     * value is smaller than the last baseline of the label, then there will be no space between
     * the label and top edge of the [TextField].
     *
     * See [PaddingValues]
     */
    @ExperimentalMaterial3Api
    fun textFieldWithLabelPadding(
        start: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        top: Dp = FirstBaselineOffset,
        bottom: Dp = TextFieldBottomPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Default content padding applied to [TextField] when the label is null.
     * See [PaddingValues] for more details.
     */
    @ExperimentalMaterial3Api
    fun textFieldWithoutLabelPadding(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Default content padding applied to [OutlinedTextField].
     * See [PaddingValues] for more details.
     */
    @ExperimentalMaterial3Api
    fun outlinedTextFieldPadding(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * (including label, placeholder, leading and trailing icons) colors used in a [TextField].
     *
     * @param textColor the color used for the input text of this text field
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param containerColor the container color for this text field
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param focusedIndicatorColor the indicator color for this text field when focused
     * @param unfocusedIndicatorColor the indicator color for this text field when not focused
     * @param disabledIndicatorColor the indicator color for this text field when disabled
     * @param errorIndicatorColor the indicator color for this text field when in error state
     * @param focusedLeadingIconColor the leading icon color for this text field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
     * @param disabledLeadingIconColor the leading icon color for this text field when disabled
     * @param errorLeadingIconColor the leading icon color for this text field when in error state
     * @param focusedTrailingIconColor the trailing icon color for this text field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not
     * focused
     * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
     * @param errorTrailingIconColor the trailing icon color for this text field when in error state
     * @param focusedLabelColor the label color for this text field when focused
     * @param unfocusedLabelColor the label color for this text field when not focused
     * @param disabledLabelColor the label color for this text field when disabled
     * @param errorLabelColor the label color for this text field when in error state
     * @param placeholderColor the placeholder color for this text field
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     */
    @Composable
    fun textFieldColors(
        textColor: Color = FilledTextFieldTokens.InputColor.toColor(),
        disabledTextColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        containerColor: Color = FilledTextFieldTokens.ContainerColor.toColor(),
        cursorColor: Color = FilledTextFieldTokens.CaretColor.toColor(),
        errorCursorColor: Color = FilledTextFieldTokens.ErrorFocusCaretColor.toColor(),
        focusedIndicatorColor: Color = FilledTextFieldTokens.FocusActiveIndicatorColor.toColor(),
        unfocusedIndicatorColor: Color = FilledTextFieldTokens.ActiveIndicatorColor.toColor(),
        disabledIndicatorColor: Color = FilledTextFieldTokens.DisabledActiveIndicatorColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledActiveIndicatorOpacity),
        errorIndicatorColor: Color = FilledTextFieldTokens.ErrorActiveIndicatorColor.toColor(),
        focusedLeadingIconColor: Color = FilledTextFieldTokens.FocusLeadingIconColor.toColor(),
        unfocusedLeadingIconColor: Color = FilledTextFieldTokens.LeadingIconColor.toColor(),
        disabledLeadingIconColor: Color = FilledTextFieldTokens.DisabledLeadingIconColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledLeadingIconOpacity),
        errorLeadingIconColor: Color = FilledTextFieldTokens.ErrorLeadingIconColor.toColor(),
        focusedTrailingIconColor: Color = FilledTextFieldTokens.FocusTrailingIconColor.toColor(),
        unfocusedTrailingIconColor: Color = FilledTextFieldTokens.TrailingIconColor.toColor(),
        disabledTrailingIconColor: Color = FilledTextFieldTokens.DisabledTrailingIconColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledTrailingIconOpacity),
        errorTrailingIconColor: Color = FilledTextFieldTokens.ErrorTrailingIconColor.toColor(),
        focusedLabelColor: Color = FilledTextFieldTokens.FocusLabelColor.toColor(),
        unfocusedLabelColor: Color = FilledTextFieldTokens.LabelColor.toColor(),
        disabledLabelColor: Color = FilledTextFieldTokens.DisabledLabelColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledLabelOpacity),
        errorLabelColor: Color = FilledTextFieldTokens.ErrorLabelColor.toColor(),
        placeholderColor: Color = FilledTextFieldTokens.InputPlaceholderColor.toColor(),
        disabledPlaceholderColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity)
    ): TextFieldColors =
        DefaultTextFieldColors(
            textColor = textColor,
            disabledTextColor = disabledTextColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            containerColor = containerColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            placeholderColor = placeholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor
        )

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * (including label, placeholder, leading and trailing icons) colors used in an
     * [OutlinedTextField].
     *
     * @param textColor the color used for the input text of this text field
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param containerColor the container color for this text field
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param focusedBorderColor the border color for this text field when focused
     * @param unfocusedBorderColor the border color for this text field when not focused
     * @param disabledBorderColor the border color for this text field when disabled
     * @param errorBorderColor the border color for this text field when in error state
     * @param focusedLeadingIconColor the leading icon color for this text field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
     * @param disabledLeadingIconColor the leading icon color for this text field when disabled
     * @param errorLeadingIconColor the leading icon color for this text field when in error state
     * @param focusedTrailingIconColor the trailing icon color for this text field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not focused
     * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
     * @param errorTrailingIconColor the trailing icon color for this text field when in error state
     * @param focusedLabelColor the label color for this text field when focused
     * @param unfocusedLabelColor the label color for this text field when not focused
     * @param disabledLabelColor the label color for this text field when disabled
     * @param errorLabelColor the label color for this text field when in error state
     * @param placeholderColor the placeholder color for this text field
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     */
    @Composable
    fun outlinedTextFieldColors(
        textColor: Color = OutlinedTextFieldTokens.InputColor.toColor(),
        disabledTextColor: Color = OutlinedTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        containerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedTextFieldTokens.CaretColor.toColor(),
        errorCursorColor: Color = OutlinedTextFieldTokens.ErrorFocusCaretColor.toColor(),
        focusedBorderColor: Color = OutlinedTextFieldTokens.FocusOutlineColor.toColor(),
        unfocusedBorderColor: Color = OutlinedTextFieldTokens.OutlineColor.toColor(),
        disabledBorderColor: Color = OutlinedTextFieldTokens.DisabledOutlineColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledOutlineOpacity),
        errorBorderColor: Color = OutlinedTextFieldTokens.ErrorOutlineColor.toColor(),
        focusedLeadingIconColor: Color = OutlinedTextFieldTokens.FocusLeadingIconColor.toColor(),
        unfocusedLeadingIconColor: Color = OutlinedTextFieldTokens.LeadingIconColor.toColor(),
        disabledLeadingIconColor: Color = OutlinedTextFieldTokens.DisabledLeadingIconColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledLeadingIconOpacity),
        errorLeadingIconColor: Color = OutlinedTextFieldTokens.ErrorLeadingIconColor.toColor(),
        focusedTrailingIconColor: Color = OutlinedTextFieldTokens.FocusTrailingIconColor.toColor(),
        unfocusedTrailingIconColor: Color = OutlinedTextFieldTokens.TrailingIconColor.toColor(),
        disabledTrailingIconColor: Color = OutlinedTextFieldTokens.DisabledTrailingIconColor
            .toColor().copy(alpha = OutlinedTextFieldTokens.DisabledTrailingIconOpacity),
        errorTrailingIconColor: Color = OutlinedTextFieldTokens.ErrorTrailingIconColor.toColor(),
        focusedLabelColor: Color = OutlinedTextFieldTokens.FocusLabelColor.toColor(),
        unfocusedLabelColor: Color = OutlinedTextFieldTokens.LabelColor.toColor(),
        disabledLabelColor: Color = OutlinedTextFieldTokens.DisabledLabelColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledLabelOpacity),
        errorLabelColor: Color = OutlinedTextFieldTokens.ErrorLabelColor.toColor(),
        placeholderColor: Color = OutlinedTextFieldTokens.InputPlaceholderColor.toColor(),
        disabledPlaceholderColor: Color = OutlinedTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity)
    ): TextFieldColors =
        DefaultTextFieldColors(
            textColor = textColor,
            disabledTextColor = disabledTextColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            focusedIndicatorColor = focusedBorderColor,
            unfocusedIndicatorColor = unfocusedBorderColor,
            errorIndicatorColor = errorBorderColor,
            disabledIndicatorColor = disabledBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            containerColor = containerColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            placeholderColor = placeholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor
        )

    /**
     * A decoration box which helps creating custom text fields based on
     * <a href="https://material.io/components/text-fields#filled-text-field" class="external" target="_blank">Material Design filled text field</a>.
     *
     * If your text field requires customising elements that aren't exposed by [TextField],
     * consider using this decoration box to achieve the desired design.
     *
     * For example, if you need to create a dense text field, use [contentPadding] parameter to
     * decrease the paddings around the input field. If you need to customise the bottom indicator,
     * apply [indicatorLine] modifier to achieve that.
     *
     * See example of using [TextFieldDecorationBox] to build your own custom text field
     * @sample androidx.compose.material3.samples.CustomTextFieldBasedOnDecorationBox
     *
     * @param value the input [String] shown by the text field
     * @param innerTextField input text field that this decoration box wraps. You will pass here a
     * framework-controlled composable parameter "innerTextField" from the decorationBox lambda of
     * the [BasicTextField]
     * @param enabled controls the enabled state of the text field. When `false`, this component
     * will not respond to user input, and it will appear visually disabled and disabled to
     * accessibility services. You must also pass the same value to the [BasicTextField] for it to
     * adjust the behavior accordingly.
     * @param singleLine indicates if this is a single line or multi line text field. You must pass
     * the same value as to [BasicTextField].
     * @param visualTransformation transforms the visual representation of the input [value]. You
     * must pass the same value as to [BasicTextField].
     * @param interactionSource the read-only [InteractionSource] representing the stream of
     * [Interaction]s for this text field. You must first create and pass in your own `remember`ed
     * [MutableInteractionSource] instance to the [BasicTextField] for it to dispatch events. And
     * then pass the same instance to this decoration box to observe [Interaction]s and customize
     * the appearance / behavior of this text field in different states.
     * @param isError indicates if the text field's current value is in error state. If set to
     * true, the label, bottom indicator and trailing icon by default will be displayed in error
     * color.
     * @param label the optional label to be displayed inside the text field container. The default
     * text style for internal [Text] is [Typography.bodySmall] when the text field is in focus and
     * [Typography.bodyLarge] when the text field is not in focus.
     * @param placeholder the optional placeholder to be displayed when the text field is in focus
     * and the input text is empty. The default text style for internal [Text] is
     * [Typography.bodyLarge].
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text
     * field container
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     * container
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     * field in different states. See [TextFieldDefaults.textFieldColors].
     * @param contentPadding the spacing values to apply internally between the internals of text
     * field and the decoration box container. You can use it to implement dense text fields or
     * simply to control horizontal padding. See [TextFieldDefaults.textFieldWithLabelPadding] and
     * [TextFieldDefaults.textFieldWithoutLabelPadding]
     * Note that if there's a label in the text field, the [top][PaddingValues.calculateTopPadding]
     * padding will mean the distance from label's [last baseline][LastBaseline] to the top edge of
     * the container. All other paddings mean the distance from the corresponding edge of the
     * container to the corresponding edge of the closest to it element
     */
    @Composable
    @ExperimentalMaterial3Api
    fun TextFieldDecorationBox(
        value: String,
        innerTextField: @Composable () -> Unit,
        enabled: Boolean,
        singleLine: Boolean,
        visualTransformation: VisualTransformation,
        interactionSource: InteractionSource,
        isError: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        colors: TextFieldColors = textFieldColors(),
        contentPadding: PaddingValues =
            if (label == null) {
                textFieldWithoutLabelPadding()
            } else {
                textFieldWithLabelPadding()
            }
    ) {
        CommonDecorationBox(
            type = TextFieldType.Filled,
            value = value,
            innerTextField = innerTextField,
            visualTransformation = visualTransformation,
            placeholder = placeholder,
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = contentPadding
        )
    }

    /**
     * A decoration box which helps creating custom text fields based on
     * <a href="https://material.io/components/text-fields#outlined-text-field" class="external" target="_blank">Material Design outlined text field</a>.
     *
     * If your text field requires customising elements that aren't exposed by [OutlinedTextField],
     * consider using this decoration box to achieve the desired design.
     *
     * For example, if you need to create a dense outlined text field, use [contentPadding]
     * parameter to decrease the paddings around the input field. If you need to change the
     * thickness of the border, use [border] parameter to achieve that.
     *
     * Example of custom text field based on [OutlinedTextFieldDecorationBox]:
     * @sample androidx.compose.material3.samples.CustomOutlinedTextFieldBasedOnDecorationBox
     *
     * @param value the input [String] shown by the text field
     * @param innerTextField input text field that this decoration box wraps. You will pass here a
     * framework-controlled composable parameter "innerTextField" from the decorationBox lambda of
     * the [BasicTextField]
     * @param enabled controls the enabled state of the text field. When `false`, this component
     * will not respond to user input, and it will appear visually disabled and disabled to
     * accessibility services. You must also pass the same value to the [BasicTextField] for it to
     * adjust the behavior accordingly.
     * @param singleLine indicates if this is a single line or multi line text field. You must pass
     * the same value as to [BasicTextField].
     * @param visualTransformation transforms the visual representation of the input [value]. You
     * must pass the same value as to [BasicTextField].
     * @param interactionSource the read-only [InteractionSource] representing the stream of
     * [Interaction]s for this text field. You must first create and pass in your own `remember`ed
     * [MutableInteractionSource] instance to the [BasicTextField] for it to dispatch events. And
     * then pass the same instance to this decoration box to observe [Interaction]s and customize
     * the appearance / behavior of this text field in different states.
     * @param isError indicates if the text field's current value is in error state. If set to
     * true, the label, bottom indicator and trailing icon by default will be displayed in error
     * color.
     * @param label the optional label to be displayed inside the text field container. The default
     * text style for internal [Text] is [Typography.bodySmall] when the text field is in focus and
     * [Typography.bodyLarge] when the text field is not in focus.
     * @param placeholder the optional placeholder to be displayed when the text field is in focus
     * and the input text is empty. The default text style for internal [Text] is
     * [Typography.bodyLarge].
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text
     * field container
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     * container
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     * field in different states. See [TextFieldDefaults.outlinedTextFieldColors].
     * @param border the border to be drawn around the text field. The cutout to fit the [label]
     * will be automatically added by the framework. Note that by default the color of the border
     * comes from the [colors].
     * @param contentPadding the spacing values to apply internally between the internals of text
     * field and the decoration box container. You can use it to implement dense text fields or
     * simply to control horizontal padding. See [TextFieldDefaults.outlinedTextFieldPadding].
     */
    @Composable
    @ExperimentalMaterial3Api
    fun OutlinedTextFieldDecorationBox(
        value: String,
        innerTextField: @Composable () -> Unit,
        enabled: Boolean,
        singleLine: Boolean,
        visualTransformation: VisualTransformation,
        interactionSource: InteractionSource,
        isError: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        colors: TextFieldColors = outlinedTextFieldColors(),
        contentPadding: PaddingValues = outlinedTextFieldPadding(),
        border: @Composable () -> Unit = {
            BorderBox(enabled, isError, interactionSource, colors)
        }
    ) {
        CommonDecorationBox(
            type = TextFieldType.Outlined,
            value = value,
            visualTransformation = visualTransformation,
            innerTextField = innerTextField,
            placeholder = placeholder,
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = contentPadding,
            border = border
        )
    }
}

@Immutable
private class DefaultTextFieldColors(
    private val textColor: Color,
    private val disabledTextColor: Color,
    private val cursorColor: Color,
    private val errorCursorColor: Color,
    private val focusedIndicatorColor: Color,
    private val unfocusedIndicatorColor: Color,
    private val errorIndicatorColor: Color,
    private val disabledIndicatorColor: Color,
    private val focusedLeadingIconColor: Color,
    private val unfocusedLeadingIconColor: Color,
    private val disabledLeadingIconColor: Color,
    private val errorLeadingIconColor: Color,
    private val focusedTrailingIconColor: Color,
    private val unfocusedTrailingIconColor: Color,
    private val disabledTrailingIconColor: Color,
    private val errorTrailingIconColor: Color,
    private val containerColor: Color,
    private val focusedLabelColor: Color,
    private val unfocusedLabelColor: Color,
    private val disabledLabelColor: Color,
    private val errorLabelColor: Color,
    private val placeholderColor: Color,
    private val disabledPlaceholderColor: Color
) : TextFieldColors {

    @Composable
    override fun leadingIconColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        return rememberUpdatedState(
            when {
                !enabled -> disabledLeadingIconColor
                isError -> errorLeadingIconColor
                focused -> focusedLeadingIconColor
                else -> unfocusedLeadingIconColor
            }
        )
    }

    @Composable
    override fun trailingIconColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        return rememberUpdatedState(
            when {
                !enabled -> disabledTrailingIconColor
                isError -> errorTrailingIconColor
                focused -> focusedTrailingIconColor
                else -> unfocusedTrailingIconColor
            }
        )
    }

    @Composable
    override fun indicatorColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledIndicatorColor
            isError -> errorIndicatorColor
            focused -> focusedIndicatorColor
            else -> unfocusedIndicatorColor
        }
        return if (enabled) {
            animateColorAsState(targetValue, tween(durationMillis = AnimationDuration))
        } else {
            rememberUpdatedState(targetValue)
        }
    }

    @Composable
    override fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(containerColor)
    }

    @Composable
    override fun placeholderColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) placeholderColor else disabledPlaceholderColor)
    }

    @Composable
    override fun labelColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledLabelColor
            isError -> errorLabelColor
            focused -> focusedLabelColor
            else -> unfocusedLabelColor
        }
        return rememberUpdatedState(targetValue)
    }

    @Composable
    override fun textColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) textColor else disabledTextColor)
    }

    @Composable
    override fun cursorColor(isError: Boolean): State<Color> {
        return rememberUpdatedState(if (isError) errorCursorColor else cursorColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultTextFieldColors

        if (textColor != other.textColor) return false
        if (disabledTextColor != other.disabledTextColor) return false
        if (cursorColor != other.cursorColor) return false
        if (errorCursorColor != other.errorCursorColor) return false
        if (focusedIndicatorColor != other.focusedIndicatorColor) return false
        if (unfocusedIndicatorColor != other.unfocusedIndicatorColor) return false
        if (errorIndicatorColor != other.errorIndicatorColor) return false
        if (disabledIndicatorColor != other.disabledIndicatorColor) return false
        if (focusedLeadingIconColor != other.focusedLeadingIconColor) return false
        if (unfocusedLeadingIconColor != other.unfocusedLeadingIconColor) return false
        if (disabledLeadingIconColor != other.disabledLeadingIconColor) return false
        if (errorLeadingIconColor != other.errorLeadingIconColor) return false
        if (focusedTrailingIconColor != other.focusedTrailingIconColor) return false
        if (unfocusedTrailingIconColor != other.unfocusedTrailingIconColor) return false
        if (disabledTrailingIconColor != other.disabledTrailingIconColor) return false
        if (errorTrailingIconColor != other.errorTrailingIconColor) return false
        if (containerColor != other.containerColor) return false
        if (focusedLabelColor != other.focusedLabelColor) return false
        if (unfocusedLabelColor != other.unfocusedLabelColor) return false
        if (disabledLabelColor != other.disabledLabelColor) return false
        if (errorLabelColor != other.errorLabelColor) return false
        if (placeholderColor != other.placeholderColor) return false
        if (disabledPlaceholderColor != other.disabledPlaceholderColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = textColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + cursorColor.hashCode()
        result = 31 * result + errorCursorColor.hashCode()
        result = 31 * result + focusedIndicatorColor.hashCode()
        result = 31 * result + unfocusedIndicatorColor.hashCode()
        result = 31 * result + errorIndicatorColor.hashCode()
        result = 31 * result + disabledIndicatorColor.hashCode()
        result = 31 * result + focusedLeadingIconColor.hashCode()
        result = 31 * result + unfocusedLeadingIconColor.hashCode()
        result = 31 * result + disabledLeadingIconColor.hashCode()
        result = 31 * result + errorLeadingIconColor.hashCode()
        result = 31 * result + focusedTrailingIconColor.hashCode()
        result = 31 * result + unfocusedTrailingIconColor.hashCode()
        result = 31 * result + disabledTrailingIconColor.hashCode()
        result = 31 * result + errorTrailingIconColor.hashCode()
        result = 31 * result + containerColor.hashCode()
        result = 31 * result + focusedLabelColor.hashCode()
        result = 31 * result + unfocusedLabelColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + errorLabelColor.hashCode()
        result = 31 * result + placeholderColor.hashCode()
        result = 31 * result + disabledPlaceholderColor.hashCode()
        return result
    }
}

@Composable
private fun animateBorderStrokeAsState(
    enabled: Boolean,
    isError: Boolean,
    interactionSource: InteractionSource,
    colors: TextFieldColors,
    focusedBorderThickness: Dp,
    unfocusedBorderThickness: Dp
): State<BorderStroke> {
    val focused by interactionSource.collectIsFocusedAsState()
    val indicatorColor = colors.indicatorColor(enabled, isError, interactionSource)
    val targetThickness = if (focused) focusedBorderThickness else unfocusedBorderThickness
    val animatedThickness = if (enabled) {
        animateDpAsState(targetThickness, tween(durationMillis = AnimationDuration))
    } else {
        rememberUpdatedState(unfocusedBorderThickness)
    }
    return rememberUpdatedState(
        BorderStroke(animatedThickness.value, SolidColor(indicatorColor.value))
    )
}