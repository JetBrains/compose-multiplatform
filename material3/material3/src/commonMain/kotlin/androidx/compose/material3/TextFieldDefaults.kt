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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.TextFieldDefaults.OutlinedTextFieldDecorationBox
import androidx.compose.material3.tokens.FilledTextFieldTokens
import androidx.compose.material3.tokens.OutlinedTextFieldTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Contains the default values used by [TextField] and [OutlinedTextField].
 */
@ExperimentalMaterial3Api
@Immutable
object TextFieldDefaults {
    /** Default shape for an outlined text field. */
    val outlinedShape: Shape @Composable get() = OutlinedTextFieldTokens.ContainerShape.toShape()

    /** Default shape for a filled text field. */
    val filledShape: Shape @Composable get() = FilledTextFieldTokens.ContainerShape.toShape()

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
     * Composable that draws a default container for the content of [TextField], with an indicator
     * line at the bottom. You can use it to draw a container for your custom text field based on
     * [TextFieldDecorationBox]. [TextField] applies it automatically.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param shape shape of the container
     */
    @ExperimentalMaterial3Api
    @Composable
    fun FilledContainerBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        shape: Shape = filledShape,
    ) {
        Box(
            Modifier
                .background(colors.containerColor(isError).value, shape)
                .indicatorLine(enabled, isError, interactionSource, colors))
    }

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
     * Composable that draws a default container for [OutlinedTextField] with a border stroke. You
     * can use it to draw a border stroke in your custom text field based on
     * [OutlinedTextFieldDecorationBox]. The [OutlinedTextField] applies it automatically.
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
    fun OutlinedBorderContainerBox(
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
        Box(
            Modifier
                .border(borderStroke.value, shape)
                .background(colors.containerColor(isError).value, shape))
    }

    /**
     * Default content padding applied to [TextField] when there is a label.
     *
     * Note that when the label is present, the "top" padding is a distance between the top edge of
     * the [TextField] and the top of the label, not to the top of the input field. The input field
     * is placed directly beneath the label.
     *
     * See [PaddingValues] for more details.
     */
    @ExperimentalMaterial3Api
    fun textFieldWithLabelPadding(
        start: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        top: Dp = TextFieldWithLabelVerticalPadding,
        bottom: Dp = TextFieldWithLabelVerticalPadding
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
     * Default padding applied to supporting text for both [TextField] and [OutlinedTextField].
     * See [PaddingValues] for more details.
     */
    // TODO(246775477): consider making this public
    @ExperimentalMaterial3Api
    internal fun supportingTextPadding(
        start: Dp = TextFieldPadding,
        top: Dp = SupportingTopPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = 0.dp,
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in a [TextField].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     * focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     * state
     * @param containerColor the container color for this text field
     * @param errorContainerColor the container color for this text field when in error state
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param selectionColors the colors used when the input text of this text field is selected
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
     * @param focusedPlaceholderColor the placeholder color for this text field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     * @param errorPlaceholderColor the placeholder color for this text field when in error state
     * @param focusedSupportingTextColor the supporting text color for this text field when focused
     * @param unfocusedSupportingTextColor the supporting text color for this text field when not
     * focused
     * @param disabledSupportingTextColor the supporting text color for this text field when
     * disabled
     * @param errorSupportingTextColor the supporting text color for this text field when in error
     * state
     * @param focusedPrefixColor the prefix color for this text field when focused
     * @param unfocusedPrefixColor the prefix color for this text field when not focused
     * @param disabledPrefixColor the prefix color for this text field when disabled
     * @param errorPrefixColor the prefix color for this text field when in error state
     * @param focusedSuffixColor the suffix color for this text field when focused
     * @param unfocusedSuffixColor the suffix color for this text field when not focused
     * @param disabledSuffixColor the suffix color for this text field when disabled
     * @param errorSuffixColor the suffix color for this text field when in error state
     */
    @ExperimentalMaterial3Api
    @Composable
    fun textFieldColors(
        focusedTextColor: Color = FilledTextFieldTokens.FocusInputColor.toColor(),
        unfocusedTextColor: Color = FilledTextFieldTokens.InputColor.toColor(),
        disabledTextColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorTextColor: Color = FilledTextFieldTokens.ErrorInputColor.toColor(),
        containerColor: Color = FilledTextFieldTokens.ContainerColor.toColor(),
        errorContainerColor: Color = FilledTextFieldTokens.ContainerColor.toColor(),
        cursorColor: Color = FilledTextFieldTokens.CaretColor.toColor(),
        errorCursorColor: Color = FilledTextFieldTokens.ErrorFocusCaretColor.toColor(),
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
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
        focusedPlaceholderColor: Color = FilledTextFieldTokens.InputPlaceholderColor.toColor(),
        unfocusedPlaceholderColor: Color = FilledTextFieldTokens.InputPlaceholderColor.toColor(),
        disabledPlaceholderColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorPlaceholderColor: Color = FilledTextFieldTokens.InputPlaceholderColor.toColor(),
        focusedSupportingTextColor: Color = FilledTextFieldTokens.FocusSupportingColor.toColor(),
        unfocusedSupportingTextColor: Color = FilledTextFieldTokens.SupportingColor.toColor(),
        disabledSupportingTextColor: Color = FilledTextFieldTokens.DisabledSupportingColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledSupportingOpacity),
        errorSupportingTextColor: Color = FilledTextFieldTokens.ErrorSupportingColor.toColor(),
        focusedPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.toColor(),
        unfocusedPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.toColor(),
        disabledPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.toColor(),
        focusedSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.toColor(),
        unfocusedSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.toColor(),
        disabledSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.toColor(),
    ): TextFieldColors =
        TextFieldColors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            containerColor = containerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            textSelectionColors = selectionColors,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedSupportingTextColor = focusedSupportingTextColor,
            unfocusedSupportingTextColor = unfocusedSupportingTextColor,
            disabledSupportingTextColor = disabledSupportingTextColor,
            errorSupportingTextColor = errorSupportingTextColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in an [OutlinedTextField].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     * focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     * state
     * @param containerColor the container color for this text field
     * @param errorContainerColor the container color for this text field when in error state
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param selectionColors the colors used when the input text of this text field is selected
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
     * @param focusedPlaceholderColor the placeholder color for this text field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     * @param errorPlaceholderColor the placeholder color for this text field when in error state
     * @param focusedSupportingTextColor the supporting text color for this text field when focused
     * @param unfocusedSupportingTextColor the supporting text color for this text field when not
     * focused
     * @param disabledSupportingTextColor the supporting text color for this text field when
     * disabled
     * @param errorSupportingTextColor the supporting text color for this text field when in error
     * state
     * @param focusedPrefixColor the prefix color for this text field when focused
     * @param unfocusedPrefixColor the prefix color for this text field when not focused
     * @param disabledPrefixColor the prefix color for this text field when disabled
     * @param errorPrefixColor the prefix color for this text field when in error state
     * @param focusedSuffixColor the suffix color for this text field when focused
     * @param unfocusedSuffixColor the suffix color for this text field when not focused
     * @param disabledSuffixColor the suffix color for this text field when disabled
     * @param errorSuffixColor the suffix color for this text field when in error state
     */
    @ExperimentalMaterial3Api
    @Composable
    fun outlinedTextFieldColors(
        focusedTextColor: Color = OutlinedTextFieldTokens.FocusInputColor.toColor(),
        unfocusedTextColor: Color = OutlinedTextFieldTokens.InputColor.toColor(),
        disabledTextColor: Color = OutlinedTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorTextColor: Color = OutlinedTextFieldTokens.ErrorInputColor.toColor(),
        containerColor: Color = Color.Transparent,
        errorContainerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedTextFieldTokens.CaretColor.toColor(),
        errorCursorColor: Color = OutlinedTextFieldTokens.ErrorFocusCaretColor.toColor(),
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
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
        focusedPlaceholderColor: Color = OutlinedTextFieldTokens.InputPlaceholderColor.toColor(),
        unfocusedPlaceholderColor: Color = OutlinedTextFieldTokens.InputPlaceholderColor.toColor(),
        disabledPlaceholderColor: Color = OutlinedTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorPlaceholderColor: Color = OutlinedTextFieldTokens.InputPlaceholderColor.toColor(),
        focusedSupportingTextColor: Color = OutlinedTextFieldTokens.FocusSupportingColor.toColor(),
        unfocusedSupportingTextColor: Color = OutlinedTextFieldTokens.SupportingColor.toColor(),
        disabledSupportingTextColor: Color = OutlinedTextFieldTokens.DisabledSupportingColor
            .toColor().copy(alpha = OutlinedTextFieldTokens.DisabledSupportingOpacity),
        errorSupportingTextColor: Color = OutlinedTextFieldTokens.ErrorSupportingColor.toColor(),
        focusedPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.toColor(),
        unfocusedPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.toColor(),
        disabledPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.toColor(),
        focusedSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.toColor(),
        unfocusedSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.toColor(),
        disabledSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.toColor(),
    ): TextFieldColors =
        TextFieldColors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            containerColor = containerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            textSelectionColors = selectionColors,
            focusedIndicatorColor = focusedBorderColor,
            unfocusedIndicatorColor = unfocusedBorderColor,
            disabledIndicatorColor = disabledBorderColor,
            errorIndicatorColor = errorBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedSupportingTextColor = focusedSupportingTextColor,
            unfocusedSupportingTextColor = unfocusedSupportingTextColor,
            disabledSupportingTextColor = disabledSupportingTextColor,
            errorSupportingTextColor = errorSupportingTextColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
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
     * @param prefix the optional prefix to be displayed before the input text in the text field
     * @param suffix the optional suffix to be displayed after the input text in the text field
     * @param supportingText the optional supporting text to be displayed below the text field
     * @param shape defines the shape of this text field's container
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     * field in different states. See [TextFieldDefaults.textFieldColors].
     * @param contentPadding the spacing values to apply internally between the internals of text
     * field and the decoration box container. You can use it to implement dense text fields or
     * simply to control horizontal padding. See [TextFieldDefaults.textFieldWithLabelPadding] and
     * [TextFieldDefaults.textFieldWithoutLabelPadding].
     * Note that if there's a label in the text field, the [top][PaddingValues.calculateTopPadding]
     * padding represents the distance from the top edge of the container to the top of the label.
     * Otherwise if label is null, it represents the distance from the top edge of the container to
     * the top of the input field. All other paddings represent the distance from the corresponding
     * edge of the container to the corresponding edge of the closest element.
     * @param container the container to be drawn behind the text field. By default, this includes
     * the bottom indicator line. Default colors for the container come from the [colors].
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
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        shape: Shape = filledShape,
        colors: TextFieldColors = textFieldColors(),
        contentPadding: PaddingValues =
            if (label == null) {
                textFieldWithoutLabelPadding()
            } else {
                textFieldWithLabelPadding()
            },
        container: @Composable () -> Unit = {
            FilledContainerBox(enabled, isError, interactionSource, colors, shape)
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
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = contentPadding,
            container = container
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
     * thickness of the border, use [container] parameter to achieve that.
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
     * @param prefix the optional prefix to be displayed before the input text in the text field
     * @param suffix the optional suffix to be displayed after the input text in the text field
     * @param supportingText the optional supporting text to be displayed below the text field
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     * field in different states. See [TextFieldDefaults.outlinedTextFieldColors].
     * @param contentPadding the spacing values to apply internally between the internals of text
     * field and the decoration box container. You can use it to implement dense text fields or
     * simply to control horizontal padding. See [TextFieldDefaults.outlinedTextFieldPadding].
     * @param container the container to be drawn behind the text field. By default, this is
     * transparent and only includes a border. The cutout in the border to fit the [label] will be
     * automatically added by the framework. Note that by default the color of the border comes from
     * the [colors].
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
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        colors: TextFieldColors = outlinedTextFieldColors(),
        contentPadding: PaddingValues = outlinedTextFieldPadding(),
        container: @Composable () -> Unit = {
            OutlinedBorderContainerBox(enabled, isError, interactionSource, colors)
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
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            singleLine = singleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = contentPadding,
            container = container
        )
    }

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @ExperimentalMaterial3Api
    @Composable
    fun textFieldColors(
        textColor: Color = FilledTextFieldTokens.InputColor.toColor(),
        disabledTextColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        containerColor: Color = FilledTextFieldTokens.ContainerColor.toColor(),
        cursorColor: Color = FilledTextFieldTokens.CaretColor.toColor(),
        errorCursorColor: Color = FilledTextFieldTokens.ErrorFocusCaretColor.toColor(),
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
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
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        focusedSupportingTextColor: Color = FilledTextFieldTokens.FocusSupportingColor.toColor(),
        unfocusedSupportingTextColor: Color = FilledTextFieldTokens.SupportingColor.toColor(),
        disabledSupportingTextColor: Color = FilledTextFieldTokens.DisabledSupportingColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledSupportingOpacity),
        errorSupportingTextColor: Color = FilledTextFieldTokens.ErrorSupportingColor.toColor(),
        focusedPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.toColor(),
        unfocusedPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.toColor(),
        disabledPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.toColor(),
        focusedSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.toColor(),
        unfocusedSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.toColor(),
        disabledSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.toColor(),
    ): TextFieldColors = textFieldColors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = disabledTextColor,
        errorTextColor = textColor,
        containerColor = containerColor,
        errorContainerColor = containerColor,
        cursorColor = cursorColor,
        errorCursorColor = errorCursorColor,
        selectionColors = selectionColors,
        focusedIndicatorColor = focusedIndicatorColor,
        unfocusedIndicatorColor = unfocusedIndicatorColor,
        disabledIndicatorColor = disabledIndicatorColor,
        errorIndicatorColor = errorIndicatorColor,
        focusedLeadingIconColor = focusedLeadingIconColor,
        unfocusedLeadingIconColor = unfocusedLeadingIconColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        errorLeadingIconColor = errorLeadingIconColor,
        focusedTrailingIconColor = focusedTrailingIconColor,
        unfocusedTrailingIconColor = unfocusedTrailingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        errorTrailingIconColor = errorTrailingIconColor,
        focusedLabelColor = focusedLabelColor,
        unfocusedLabelColor = unfocusedLabelColor,
        disabledLabelColor = disabledLabelColor,
        errorLabelColor = errorLabelColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
        errorPlaceholderColor = placeholderColor,
        focusedSupportingTextColor = focusedSupportingTextColor,
        unfocusedSupportingTextColor = unfocusedSupportingTextColor,
        disabledSupportingTextColor = disabledSupportingTextColor,
        errorSupportingTextColor = errorSupportingTextColor,
        focusedPrefixColor = focusedPrefixColor,
        unfocusedPrefixColor = unfocusedPrefixColor,
        disabledPrefixColor = disabledPrefixColor,
        errorPrefixColor = errorPrefixColor,
        focusedSuffixColor = focusedSuffixColor,
        unfocusedSuffixColor = unfocusedSuffixColor,
        disabledSuffixColor = disabledSuffixColor,
        errorSuffixColor = errorSuffixColor,
    )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @ExperimentalMaterial3Api
    @Composable
    fun outlinedTextFieldColors(
        textColor: Color = OutlinedTextFieldTokens.InputColor.toColor(),
        disabledTextColor: Color = OutlinedTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        containerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedTextFieldTokens.CaretColor.toColor(),
        errorCursorColor: Color = OutlinedTextFieldTokens.ErrorFocusCaretColor.toColor(),
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
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
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        focusedSupportingTextColor: Color = OutlinedTextFieldTokens.FocusSupportingColor.toColor(),
        unfocusedSupportingTextColor: Color = OutlinedTextFieldTokens.SupportingColor.toColor(),
        disabledSupportingTextColor: Color = OutlinedTextFieldTokens.DisabledSupportingColor
            .toColor().copy(alpha = OutlinedTextFieldTokens.DisabledSupportingOpacity),
        errorSupportingTextColor: Color = OutlinedTextFieldTokens.ErrorSupportingColor.toColor(),
        focusedPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.toColor(),
        unfocusedPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.toColor(),
        disabledPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.toColor(),
        focusedSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.toColor(),
        unfocusedSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.toColor(),
        disabledSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.toColor()
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.toColor(),
    ): TextFieldColors = outlinedTextFieldColors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = disabledTextColor,
        errorTextColor = textColor,
        containerColor = containerColor,
        errorContainerColor = containerColor,
        cursorColor = cursorColor,
        errorCursorColor = errorCursorColor,
        selectionColors = selectionColors,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = disabledBorderColor,
        errorBorderColor = errorBorderColor,
        focusedLeadingIconColor = focusedLeadingIconColor,
        unfocusedLeadingIconColor = unfocusedLeadingIconColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        errorLeadingIconColor = errorLeadingIconColor,
        focusedTrailingIconColor = focusedTrailingIconColor,
        unfocusedTrailingIconColor = unfocusedTrailingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        errorTrailingIconColor = errorTrailingIconColor,
        focusedLabelColor = focusedLabelColor,
        unfocusedLabelColor = unfocusedLabelColor,
        disabledLabelColor = disabledLabelColor,
        errorLabelColor = errorLabelColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
        errorPlaceholderColor = placeholderColor,
        focusedSupportingTextColor = focusedSupportingTextColor,
        unfocusedSupportingTextColor = unfocusedSupportingTextColor,
        disabledSupportingTextColor = disabledSupportingTextColor,
        errorSupportingTextColor = errorSupportingTextColor,
        focusedPrefixColor = focusedPrefixColor,
        unfocusedPrefixColor = unfocusedPrefixColor,
        disabledPrefixColor = disabledPrefixColor,
        errorPrefixColor = errorPrefixColor,
        focusedSuffixColor = focusedSuffixColor,
        unfocusedSuffixColor = unfocusedSuffixColor,
        disabledSuffixColor = disabledSuffixColor,
        errorSuffixColor = errorSuffixColor,
    )

    @Deprecated("Use overload with prefix and suffix parameters", level = DeprecationLevel.HIDDEN)
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
        supportingText: @Composable (() -> Unit)? = null,
        shape: Shape = filledShape,
        colors: TextFieldColors = textFieldColors(),
        contentPadding: PaddingValues =
            if (label == null) {
                textFieldWithoutLabelPadding()
            } else {
                textFieldWithLabelPadding()
            },
        container: @Composable () -> Unit = {
            FilledContainerBox(enabled, isError, interactionSource, colors, shape)
        }
    ) {
        TextFieldDecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            isError = isError,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = null,
            suffix = null,
            supportingText = supportingText,
            shape = shape,
            colors = colors,
            contentPadding = contentPadding,
            container = container,
        )
    }

    @Deprecated("Use overload with prefix and suffix parameters", level = DeprecationLevel.HIDDEN)
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
        supportingText: @Composable (() -> Unit)? = null,
        colors: TextFieldColors = outlinedTextFieldColors(),
        contentPadding: PaddingValues = outlinedTextFieldPadding(),
        container: @Composable () -> Unit = {
            OutlinedBorderContainerBox(enabled, isError, interactionSource, colors)
        }
    ) {
        OutlinedTextFieldDecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            isError = isError,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = null,
            suffix = null,
            supportingText = supportingText,
            colors = colors,
            contentPadding = contentPadding,
            container = container
        )
    }
}

/**
 * Represents the colors of the input text, container, and content (including label, placeholder,
 * leading and trailing icons) used in a text field in different states.
 *
 * See [TextFieldDefaults.textFieldColors] for the default colors used in [TextField].
 * See [TextFieldDefaults.outlinedTextFieldColors] for the default colors used in
 * [OutlinedTextField].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Immutable
class TextFieldColors internal constructor(
    private val focusedTextColor: Color,
    private val unfocusedTextColor: Color,
    private val disabledTextColor: Color,
    private val errorTextColor: Color,
    private val containerColor: Color,
    private val errorContainerColor: Color,
    private val cursorColor: Color,
    private val errorCursorColor: Color,
    private val textSelectionColors: TextSelectionColors,
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
    private val focusedLabelColor: Color,
    private val unfocusedLabelColor: Color,
    private val disabledLabelColor: Color,
    private val errorLabelColor: Color,
    private val focusedPlaceholderColor: Color,
    private val unfocusedPlaceholderColor: Color,
    private val disabledPlaceholderColor: Color,
    private val errorPlaceholderColor: Color,
    private val focusedSupportingTextColor: Color,
    private val unfocusedSupportingTextColor: Color,
    private val disabledSupportingTextColor: Color,
    private val errorSupportingTextColor: Color,
    private val focusedPrefixColor: Color,
    private val unfocusedPrefixColor: Color,
    private val disabledPrefixColor: Color,
    private val errorPrefixColor: Color,
    private val focusedSuffixColor: Color,
    private val unfocusedSuffixColor: Color,
    private val disabledSuffixColor: Color,
    private val errorSuffixColor: Color,
) {
    /**
     * Represents the color used for the leading icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun leadingIconColor(
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

    /**
     * Represents the color used for the trailing icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun trailingIconColor(
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

    /**
     * Represents the color used for the border indicator of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun indicatorColor(
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

    /**
     * Represents the container color for this text field.
     *
     * @param isError whether the text field's current value is in error
     */
    @Composable
    internal fun containerColor(isError: Boolean): State<Color> {
        return rememberUpdatedState(if (isError) errorContainerColor else containerColor)
    }

    /**
     * Represents the color used for the placeholder of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun placeholderColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledPlaceholderColor
            isError -> errorPlaceholderColor
            focused -> focusedPlaceholderColor
            else -> unfocusedPlaceholderColor
        }
        return rememberUpdatedState(targetValue)
    }

    /**
     * Represents the color used for the label of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun labelColor(
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

    /**
     * Represents the color used for the input field of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun textColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledTextColor
            isError -> errorTextColor
            focused -> focusedTextColor
            else -> unfocusedTextColor
        }
        return rememberUpdatedState(targetValue)
    }

    @Composable
    internal fun supportingTextColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        return rememberUpdatedState(
            when {
                !enabled -> disabledSupportingTextColor
                isError -> errorSupportingTextColor
                focused -> focusedSupportingTextColor
                else -> unfocusedSupportingTextColor
            }
        )
    }

    /**
     * Represents the color used for the prefix of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun prefixColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledPrefixColor
            isError -> errorPrefixColor
            focused -> focusedPrefixColor
            else -> unfocusedPrefixColor
        }
        return rememberUpdatedState(targetValue)
    }

    /**
     * Represents the color used for the suffix of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun suffixColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledSuffixColor
            isError -> errorSuffixColor
            focused -> focusedSuffixColor
            else -> unfocusedSuffixColor
        }
        return rememberUpdatedState(targetValue)
    }

    /**
     * Represents the color used for the cursor of this text field.
     *
     * @param isError whether the text field's current value is in error
     */
    @Composable
    internal fun cursorColor(isError: Boolean): State<Color> {
        return rememberUpdatedState(if (isError) errorCursorColor else cursorColor)
    }

    /**
     * Represents the colors used for text selection in this text field.
     */
    internal val selectionColors: TextSelectionColors
        @Composable get() = textSelectionColors

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextFieldColors) return false

        if (focusedTextColor != other.focusedTextColor) return false
        if (unfocusedTextColor != other.unfocusedTextColor) return false
        if (disabledTextColor != other.disabledTextColor) return false
        if (errorTextColor != other.errorTextColor) return false
        if (containerColor != other.containerColor) return false
        if (errorContainerColor != other.errorContainerColor) return false
        if (cursorColor != other.cursorColor) return false
        if (errorCursorColor != other.errorCursorColor) return false
        if (textSelectionColors != other.textSelectionColors) return false
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
        if (focusedLabelColor != other.focusedLabelColor) return false
        if (unfocusedLabelColor != other.unfocusedLabelColor) return false
        if (disabledLabelColor != other.disabledLabelColor) return false
        if (errorLabelColor != other.errorLabelColor) return false
        if (focusedPlaceholderColor != other.focusedPlaceholderColor) return false
        if (unfocusedPlaceholderColor != other.unfocusedPlaceholderColor) return false
        if (disabledPlaceholderColor != other.disabledPlaceholderColor) return false
        if (errorPlaceholderColor != other.errorPlaceholderColor) return false
        if (focusedSupportingTextColor != other.focusedSupportingTextColor) return false
        if (unfocusedSupportingTextColor != other.unfocusedSupportingTextColor) return false
        if (disabledSupportingTextColor != other.disabledSupportingTextColor) return false
        if (errorSupportingTextColor != other.errorSupportingTextColor) return false
        if (focusedPrefixColor != other.focusedPrefixColor) return false
        if (unfocusedPrefixColor != other.unfocusedPrefixColor) return false
        if (disabledPrefixColor != other.disabledPrefixColor) return false
        if (errorPrefixColor != other.errorPrefixColor) return false
        if (focusedSuffixColor != other.focusedSuffixColor) return false
        if (unfocusedSuffixColor != other.unfocusedSuffixColor) return false
        if (disabledSuffixColor != other.disabledSuffixColor) return false
        if (errorSuffixColor != other.errorSuffixColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = focusedTextColor.hashCode()
        result = 31 * result + unfocusedTextColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + errorTextColor.hashCode()
        result = 31 * result + containerColor.hashCode()
        result = 31 * result + errorContainerColor.hashCode()
        result = 31 * result + cursorColor.hashCode()
        result = 31 * result + errorCursorColor.hashCode()
        result = 31 * result + textSelectionColors.hashCode()
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
        result = 31 * result + focusedLabelColor.hashCode()
        result = 31 * result + unfocusedLabelColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + errorLabelColor.hashCode()
        result = 31 * result + focusedPlaceholderColor.hashCode()
        result = 31 * result + unfocusedPlaceholderColor.hashCode()
        result = 31 * result + disabledPlaceholderColor.hashCode()
        result = 31 * result + errorPlaceholderColor.hashCode()
        result = 31 * result + focusedSupportingTextColor.hashCode()
        result = 31 * result + unfocusedSupportingTextColor.hashCode()
        result = 31 * result + disabledSupportingTextColor.hashCode()
        result = 31 * result + errorSupportingTextColor.hashCode()
        result = 31 * result + focusedPrefixColor.hashCode()
        result = 31 * result + unfocusedPrefixColor.hashCode()
        result = 31 * result + disabledPrefixColor.hashCode()
        result = 31 * result + errorPrefixColor.hashCode()
        result = 31 * result + focusedSuffixColor.hashCode()
        result = 31 * result + unfocusedSuffixColor.hashCode()
        result = 31 * result + disabledSuffixColor.hashCode()
        result = 31 * result + errorSuffixColor.hashCode()
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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