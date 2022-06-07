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

package androidx.compose.material

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
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.TextFieldDefaults.OutlinedTextFieldDecorationBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
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
 * Represents the colors of the input text, background and content (including label, placeholder,
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
     * Represents the background color for this text field.
     *
     * @param enabled whether the text field is enabled
     */
    @Composable
    fun backgroundColor(enabled: Boolean): State<Color>

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
     * @param error whether the text field should show error color according to the Material
     * specifications. If the label is being used as a placeholder, this will be false even if
     * the input is invalid, as the placeholder should not use the error color
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    fun labelColor(
        enabled: Boolean,
        error: Boolean,
        interactionSource: InteractionSource
    ): State<Color>

    /**
     * Represents the color used for the leading icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     */
    @Composable
    fun leadingIconColor(enabled: Boolean, isError: Boolean): State<Color>

    /**
     * Represents the color used for the trailing icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     */
    @Composable
    fun trailingIconColor(enabled: Boolean, isError: Boolean): State<Color>

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
 * Temporary experimental interface, to expose interactionSource to
 * leadingIconColor and trailingIconColor.
 * TODO: Should be removed when b/198571248 is fixed.
 */
@ExperimentalMaterialApi
interface TextFieldColorsWithIcons : TextFieldColors {
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
    ): State<Color> {
        return leadingIconColor(enabled, isError)
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
    fun trailingIconColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        return trailingIconColor(enabled, isError)
    }
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
     * The default opacity used for a [TextField]'s and [OutlinedTextField]'s leading and
     * trailing icons color.
     */
    const val IconOpacity = 0.54f

    /**
     * The default shape used for a [TextField]'s background
     */
    val TextFieldShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.small
            .copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize)

    /**
     * The default shape used for a [OutlinedTextField]'s background and border
     */
    val OutlinedTextFieldShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.small

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
     * The default opacity used for a [TextField]'s background color.
     */
    const val BackgroundOpacity = 0.12f

    // Filled text field uses 42% opacity to meet the contrast requirements for accessibility reasons
    /**
     * The default opacity used for a [TextField]'s indicator line color when text field is
     * not focused.
     */
    const val UnfocusedIndicatorLineOpacity = 0.42f

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
     * @param focusedIndicatorLineThickness thickness of the indicator line when text field is focused
     * @param unfocusedIndicatorLineThickness thickness of the indicator line when text field is
     * not focused
     */
    @ExperimentalMaterialApi
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
    @ExperimentalMaterialApi
    @Composable
    fun BorderBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        shape: Shape = OutlinedTextFieldShape,
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
    @ExperimentalMaterialApi
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
    @ExperimentalMaterialApi
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
    @ExperimentalMaterialApi
    fun outlinedTextFieldPadding(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Creates a [TextFieldColors] that represents the default input text, background and content
     * (including label, placeholder, leading and trailing icons) colors used in a [TextField].
     */
    @Composable
    fun textFieldColors(
        textColor: Color = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledTextColor: Color = textColor.copy(ContentAlpha.disabled),
        backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = BackgroundOpacity),
        cursorColor: Color = MaterialTheme.colors.primary,
        errorCursorColor: Color = MaterialTheme.colors.error,
        focusedIndicatorColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        unfocusedIndicatorColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = UnfocusedIndicatorLineOpacity),
        disabledIndicatorColor: Color = unfocusedIndicatorColor.copy(alpha = ContentAlpha.disabled),
        errorIndicatorColor: Color = MaterialTheme.colors.error,
        leadingIconColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = IconOpacity),
        disabledLeadingIconColor: Color = leadingIconColor.copy(alpha = ContentAlpha.disabled),
        errorLeadingIconColor: Color = leadingIconColor,
        trailingIconColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = IconOpacity),
        disabledTrailingIconColor: Color = trailingIconColor.copy(alpha = ContentAlpha.disabled),
        errorTrailingIconColor: Color = MaterialTheme.colors.error,
        focusedLabelColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        unfocusedLabelColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        disabledLabelColor: Color = unfocusedLabelColor.copy(ContentAlpha.disabled),
        errorLabelColor: Color = MaterialTheme.colors.error,
        placeholderColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        disabledPlaceholderColor: Color = placeholderColor.copy(ContentAlpha.disabled)
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
            leadingIconColor = leadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            trailingIconColor = trailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            backgroundColor = backgroundColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            placeholderColor = placeholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor
        )

    /**
     * Creates a [TextFieldColors] that represents the default input text, background and content
     * (including label, placeholder, leading and trailing icons) colors used in an
     * [OutlinedTextField].
     */
    @Composable
    fun outlinedTextFieldColors(
        textColor: Color = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledTextColor: Color = textColor.copy(ContentAlpha.disabled),
        backgroundColor: Color = Color.Transparent,
        cursorColor: Color = MaterialTheme.colors.primary,
        errorCursorColor: Color = MaterialTheme.colors.error,
        focusedBorderColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        unfocusedBorderColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledBorderColor: Color = unfocusedBorderColor.copy(alpha = ContentAlpha.disabled),
        errorBorderColor: Color = MaterialTheme.colors.error,
        leadingIconColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = IconOpacity),
        disabledLeadingIconColor: Color = leadingIconColor.copy(alpha = ContentAlpha.disabled),
        errorLeadingIconColor: Color = leadingIconColor,
        trailingIconColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = IconOpacity),
        disabledTrailingIconColor: Color = trailingIconColor.copy(alpha = ContentAlpha.disabled),
        errorTrailingIconColor: Color = MaterialTheme.colors.error,
        focusedLabelColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        unfocusedLabelColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        disabledLabelColor: Color = unfocusedLabelColor.copy(ContentAlpha.disabled),
        errorLabelColor: Color = MaterialTheme.colors.error,
        placeholderColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        disabledPlaceholderColor: Color = placeholderColor.copy(ContentAlpha.disabled)
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
            leadingIconColor = leadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            trailingIconColor = trailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            backgroundColor = backgroundColor,
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
     * @sample androidx.compose.material.samples.CustomTextFieldBasedOnDecorationBox
     *C
     * @param value the input [String] shown by the text field
     * @param innerTextField input text field that this decoration box wraps. You will pass here a
     * framework-controlled composable parameter "innerTextField" from the decorationBox lambda of
     * the [BasicTextField]
     * @param enabled controls the enabled state of the [TextField]. When `false`, visually
     * text field will appear in the disabled UI state. You must also pass the same value to the
     * [BasicTextField] for it to adjust the behavior accordingly making the text field non-editable,
     * non-focusable and non-selectable
     * @param singleLine indicates if this is a single line or multi line text field. You must pass
     * the same value as to [BasicTextField]
     * @param visualTransformation transforms the visual representation of the input [value]. You must
     * pass the same value as to [BasicTextField]
     * @param interactionSource this is a read-only [InteractionSource] representing the stream of
     * [Interaction]s for this text field. You first create and pass in your own remembered
     * [MutableInteractionSource] to the [BasicTextField] for it to dispatch events. And then pass the
     * same instance to this decoration box for it to observe [Interaction]s and customize the
     * appearance / behavior in different [Interaction]s
     * @param isError indicates if the text field's current value is in error state. If set to
     * true, the label, bottom indicator and trailing icon by default will be displayed in error color
     * @param label the optional label to be displayed inside the text field container. The default
     * text style for internal [Text] is [Typography.caption] when the text field is in focus and
     * [Typography.subtitle1] when the text field is not in focus
     * @param placeholder the optional placeholder to be displayed when the text field is in focus and
     * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
     * container
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     * container
     * @param colors [TextFieldColors] that will be used to resolve color of the text and content
     * (including label, placeholder, leading and trailing icons, bottom indicator) for this text field in
     * different states. See [TextFieldDefaults.textFieldColors]
     * @param contentPadding the spacing values to apply internally between the internals of text field
     * and the decoration box container. You can use it to implement dense text fields or simply to
     * control horizontal padding. See [TextFieldDefaults.textFieldWithLabelPadding] and
     * [TextFieldDefaults.textFieldWithoutLabelPadding]
     * Note that if there's a label in the text field, the [top][PaddingValues.calculateTopPadding]
     * padding will mean the distance from label's [last baseline][LastBaseline] to the top edge of the
     * container. All other paddings mean the distance from the corresponding edge of the container to
     * the corresponding edge of the closest to it element
     */
    @Composable
    @ExperimentalMaterialApi
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
     * For example, if you need to create a dense outlined text field, use [contentPadding] parameter to
     * decrease the paddings around the input field. If you need to change the thickness of the border,
     * use [border] parameter to achieve that.
     *
     * Example of custom text field based on [OutlinedTextFieldDecorationBox]:
     * @sample androidx.compose.material.samples.CustomOutlinedTextFieldBasedOnDecorationBox
     *
     * @param value the input [String] shown by the text field
     * @param innerTextField input text field that this decoration box wraps. You will pass here a
     * framework-controlled composable parameter "innerTextField" from the decorationBox lambda of the
     * [BasicTextField]
     * @param enabled controls the enabled state of the [OutlinedTextField]. When `false`, visually
     * text field will appear in the disabled UI state. You must also pass the same value to the
     * [BasicTextField] for it to adjust the behavior accordingly making the text field non-editable,
     * non-focusable and non-selectable
     * @param singleLine indicates if this is a single line or multi line text field. You must pass
     * the same value as to [BasicTextField]
     * @param visualTransformation transforms the visual representation of the input [value]. You must
     * pass the same value as to [BasicTextField]
     * @param interactionSource this is a read-only [InteractionSource] representing the stream of
     * [Interaction]s for this text field. You first create and pass in your own remembered
     * [MutableInteractionSource] to the [BasicTextField] for it to dispatch events. And then pass the
     * same instance to this decoration box for it to observe [Interaction]s and customize the
     * appearance / behavior in different [Interaction]s.
     * @param isError indicates if the text field's current value is in error state. If set to
     * true, the label, bottom indicator and trailing icon by default will be displayed in error color
     * @param label the optional label to be displayed inside the text field container. The default
     * text style for internal [Text] is [Typography.caption] when the text field is in focus and
     * [Typography.subtitle1] when the text field is not in focus
     * @param placeholder the optional placeholder to be displayed when the text field is in focus and
     * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
     * container
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     * container
     * @param colors [TextFieldColors] that will be used to resolve color of the text and content
     * (including label, placeholder, leading and trailing icons, border) for this text field in
     * different states. See [TextFieldDefaults.outlinedTextFieldColors]
     * @param border the border to be drawn around the text field. The cutout to fit the [label] will
     * be automatically added by the framework. Note that by default the color of the border comes from
     * the [colors].
     * @param contentPadding the spacing values to apply internally between the internals of text field
     * and the decoration box container. You can use it to implement dense text fields or simply to
     * control horizontal padding. See [TextFieldDefaults.outlinedTextFieldPadding]
     */
    @Composable
    @ExperimentalMaterialApi
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
    private val leadingIconColor: Color,
    private val disabledLeadingIconColor: Color,
    private val errorLeadingIconColor: Color,
    private val trailingIconColor: Color,
    private val disabledTrailingIconColor: Color,
    private val errorTrailingIconColor: Color,
    private val backgroundColor: Color,
    private val focusedLabelColor: Color,
    private val unfocusedLabelColor: Color,
    private val disabledLabelColor: Color,
    private val errorLabelColor: Color,
    private val placeholderColor: Color,
    private val disabledPlaceholderColor: Color
) : TextFieldColors {

    @Composable
    override fun leadingIconColor(enabled: Boolean, isError: Boolean): State<Color> {
        return rememberUpdatedState(
            when {
                !enabled -> disabledLeadingIconColor
                isError -> errorLeadingIconColor
                else -> leadingIconColor
            }
        )
    }

    @Composable
    override fun trailingIconColor(enabled: Boolean, isError: Boolean): State<Color> {
        return rememberUpdatedState(
            when {
                !enabled -> disabledTrailingIconColor
                isError -> errorTrailingIconColor
                else -> trailingIconColor
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
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(backgroundColor)
    }

    @Composable
    override fun placeholderColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) placeholderColor else disabledPlaceholderColor)
    }

    @Composable
    override fun labelColor(
        enabled: Boolean,
        error: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledLabelColor
            error -> errorLabelColor
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
        if (leadingIconColor != other.leadingIconColor) return false
        if (disabledLeadingIconColor != other.disabledLeadingIconColor) return false
        if (errorLeadingIconColor != other.errorLeadingIconColor) return false
        if (trailingIconColor != other.trailingIconColor) return false
        if (disabledTrailingIconColor != other.disabledTrailingIconColor) return false
        if (errorTrailingIconColor != other.errorTrailingIconColor) return false
        if (backgroundColor != other.backgroundColor) return false
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
        result = 31 * result + leadingIconColor.hashCode()
        result = 31 * result + disabledLeadingIconColor.hashCode()
        result = 31 * result + errorLeadingIconColor.hashCode()
        result = 31 * result + trailingIconColor.hashCode()
        result = 31 * result + disabledTrailingIconColor.hashCode()
        result = 31 * result + errorTrailingIconColor.hashCode()
        result = 31 * result + backgroundColor.hashCode()
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