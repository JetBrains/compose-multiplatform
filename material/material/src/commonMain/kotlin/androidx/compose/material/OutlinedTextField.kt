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

package androidx.compose.material

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Material Design implementation of an
 * [Outlined TextField](https://material.io/components/text-fields/#outlined-text-field)
 *
 * See example usage:
 * @sample androidx.compose.material.samples.SimpleOutlinedTextFieldSample
 *
 * If apart from input text change you also want to observe the cursor location, selection range,
 * or IME composition use the OutlinedTextField overload with the [TextFieldValue] parameter
 * instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [AmbientTextStyle] defined by the theme
 * @param label the optional label to be displayed inside the text field container. The default
 * text style for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * @param isErrorValue indicates if the text field's current value is in error. If set to true, the
 * label, bottom indicator and trailing icon will be displayed in [errorColor] color
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.compose.ui.text.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines. The keyboard will be informed to not show
 * the return key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the
 * maxLines attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. Should be
 * equal or greater than 1. Note that this parameter will be ignored and instead maxLines will be
 * set to 1 if [singleLine] is set to true.
 * @param onImeActionPerformed is triggered when the input service performs an [ImeAction].
 * Note that the emitted IME action may be different from what you specified through the
 * [KeyboardOptions.imeAction] field. The callback also exposes a [SoftwareKeyboardController]
 * instance as a parameter that can be used to request to hide the software keyboard
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this OutlinedTextField. You can create and pass in your own remembered
 * [InteractionState] if you want to read the [InteractionState] and customize the appearance /
 * behavior of this OutlinedTextField in different [Interaction]s.
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of either the input text or placeholder when the text field is in
 * focus, and the color of the label and bottom indicator when the text field is not in focus
 * @param errorColor the alternative color of the label, bottom indicator, cursor and trailing icon
 * used when [isErrorValue] is set to true
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = AmbientTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isErrorValue: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    onImeActionPerformed: (ImeAction, SoftwareKeyboardController?) -> Unit = { _, _ -> },
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    interactionState: InteractionState = remember { InteractionState() },
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    errorColor: Color = MaterialTheme.colors.error
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)

    TextFieldImpl(
        type = TextFieldType.Outlined,
        value = textFieldValue,
        onValueChange = {
            textFieldValueState = it
            if (value != it.text) {
                onValueChange(it.text)
            }
        },
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leading = leadingIcon,
        trailing = trailingIcon,
        isErrorValue = isErrorValue,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        maxLines = maxLines,
        onImeActionPerformed = onImeActionPerformed,
        onTextInputStarted = onTextInputStarted,
        interactionState = interactionState,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        errorColor = errorColor,
        backgroundColor = Color.Unspecified,
        shape = RectangleShape
    )
}

/**
 * Material Design implementation of an
 * [Outlined TextField](https://material.io/components/text-fields/#outlined-text-field)
 *
 * See example usage:
 * @sample androidx.compose.material.samples.OutlinedTextFieldSample
 *
 * This overload provides access to the input text, cursor position and selection range and
 * IME composition. If you only want to observe an input text change, use the OutlinedTextField
 * overload with the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 * [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [AmbientTextStyle] defined by the theme
 * @param label the optional label to be displayed inside the text field container. The default
 * text style for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * @param isErrorValue indicates if the text field's current value is in error state. If set to
 * true, the label, bottom indicator and trailing icon will be displayed in [errorColor] color
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.compose.ui.text.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines. The keyboard will be informed to not show
 * the return key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the
 * maxLines attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. Should be
 * equal or greater than 1. Note that this parameter will be ignored and instead maxLines will be
 * set to 1 if [singleLine] is set to true.
 * @param onImeActionPerformed is triggered when the input service performs an [ImeAction].
 * Note that the emitted IME action may be different from what you specified through the
 * [KeyboardOptions.imeAction] field. The callback also exposes a [SoftwareKeyboardController]
 * instance as a parameter that can be used to request to hide the software keyboard.
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this OutlinedTextField. You can create and pass in your own remembered
 * [InteractionState] if you want to read the [InteractionState] and customize the appearance /
 * behavior of this OutlinedTextField in different [Interaction]s.
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of either the input text or placeholder when the text field is in
 * focus, and the color of the label and bottom indicator when the text field is not in focus
 * @param errorColor the alternative color of the label, bottom indicator, cursor and trailing icon
 * used when [isErrorValue] is set to true
 */
@Composable
fun OutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = AmbientTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isErrorValue: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    onImeActionPerformed: (ImeAction, SoftwareKeyboardController?) -> Unit = { _, _ -> },
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    interactionState: InteractionState = remember { InteractionState() },
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    errorColor: Color = MaterialTheme.colors.error
) {
    TextFieldImpl(
        type = TextFieldType.Outlined,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leading = leadingIcon,
        trailing = trailingIcon,
        isErrorValue = isErrorValue,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        maxLines = maxLines,
        onImeActionPerformed = onImeActionPerformed,
        onTextInputStarted = onTextInputStarted,
        interactionState = interactionState,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        errorColor = errorColor,
        backgroundColor = Color.Unspecified,
        shape = RectangleShape
    )
}

@Composable
internal fun OutlinedTextFieldLayout(
    modifier: Modifier = Modifier,
    decoratedTextField: @Composable (Modifier) -> Unit,
    decoratedPlaceholder: @Composable ((Modifier) -> Unit)?,
    decoratedLabel: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    singleLine: Boolean,
    leadingColor: Color,
    trailingColor: Color,
    labelProgress: Float,
    indicatorWidth: Dp,
    indicatorColor: Color
) {
    val outlinedBorderParams = remember {
        OutlinedBorderParams(
            indicatorWidth,
            indicatorColor
        )
    }
    if (indicatorColor != outlinedBorderParams.color.value ||
        indicatorWidth != outlinedBorderParams.borderWidth.value
    ) {
        outlinedBorderParams.color.value = indicatorColor
        outlinedBorderParams.borderWidth.value = indicatorWidth
    }

    // places leading icon, input field, label, placeholder, trailing icon
    IconsWithTextFieldLayout(
        modifier = modifier.drawOutlinedBorder(outlinedBorderParams),
        textField = decoratedTextField,
        leading = leading,
        trailing = trailing,
        singleLine = singleLine,
        leadingColor = leadingColor,
        trailingColor = trailingColor,
        onLabelMeasured = {
            val labelWidth = it * labelProgress
            if (outlinedBorderParams.labelWidth.value != labelWidth) {
                outlinedBorderParams.labelWidth.value = labelWidth
            }
        },
        animationProgress = labelProgress,
        placeholder = decoratedPlaceholder,
        label = decoratedLabel
    )
}

/**
 * Layout of the leading and trailing icons and the text field, label and placeholder in
 * [OutlinedTextField].
 * It doesn't use Row to position the icons and middle part because label should not be
 * positioned in the middle part.
\ */
@Composable
private fun IconsWithTextFieldLayout(
    modifier: Modifier = Modifier,
    textField: @Composable (Modifier) -> Unit,
    placeholder: @Composable ((Modifier) -> Unit)?,
    label: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    singleLine: Boolean,
    leadingColor: Color,
    trailingColor: Color,
    animationProgress: Float,
    onLabelMeasured: (Int) -> Unit
) {
    Layout(
        content = {
            if (leading != null) {
                Box(Modifier.layoutId("leading").iconPadding(start = HorizontalIconPadding)) {
                    Decoration(
                        contentColor = leadingColor,
                        content = leading
                    )
                }
            }
            if (trailing != null) {
                Box(Modifier.layoutId("trailing").iconPadding(end = HorizontalIconPadding)) {
                    Decoration(
                        contentColor = trailingColor,
                        content = trailing
                    )
                }
            }
            if (placeholder != null) {
                placeholder(Modifier.layoutId(PlaceholderId).padding(horizontal = TextFieldPadding))
            }

            textField(
                Modifier
                    .layoutId(TextFieldId)
                    .padding(horizontal = TextFieldPadding)
            )

            if (label != null) {
                Box(modifier = Modifier.layoutId(LabelId)) { label() }
            }
        },
        modifier = modifier
    ) { measurables, incomingConstraints ->
        // used to calculate the constraints for measuring elements that will be placed in a row
        var occupiedSpaceHorizontally = 0
        val bottomPadding = TextFieldPadding.toIntPx()

        // measure leading icon
        val constraints =
            incomingConstraints.copy(minWidth = 0, minHeight = 0)
        val leadingPlaceable = measurables.find { it.layoutId == "leading" }?.measure(constraints)
        occupiedSpaceHorizontally += widthOrZero(
            leadingPlaceable
        )

        // measure trailing icon
        val trailingPlaceable = measurables.find { it.layoutId == "trailing" }
            ?.measure(constraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(
            trailingPlaceable
        )

        // measure label
        val labelConstraints = constraints.offset(
            horizontal = -occupiedSpaceHorizontally,
            vertical = -bottomPadding
        )
        val labelPlaceable =
            measurables.find { it.layoutId == LabelId }?.measure(labelConstraints)
        labelPlaceable?.let {
            onLabelMeasured(it.width)
        }

        // measure text field
        // on top we offset either by default padding or by label's half height if its too big
        // minWidth must not be set to 0 due to how foundation TextField treats zero minWidth
        val topPadding = max(heightOrZero(labelPlaceable) / 2, bottomPadding)
        val textContraints = incomingConstraints.offset(
            horizontal = -occupiedSpaceHorizontally,
            vertical = -bottomPadding - topPadding
        ).copy(minHeight = 0)
        val textFieldPlaceable =
            measurables.first { it.layoutId == TextFieldId }.measure(textContraints)

        // measure placeholder
        val placeholderConstraints = textContraints.copy(minWidth = 0)
        val placeholderPlaceable =
            measurables.find { it.layoutId == PlaceholderId }?.measure(placeholderConstraints)

        val width =
            calculateWidth(
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                incomingConstraints
            )
        val height =
            calculateHeight(
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                incomingConstraints,
                density
            )
        layout(width, height) {
            place(
                height,
                width,
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                animationProgress,
                singleLine,
                density
            )
        }
    }
}

/**
 * Calculate the width of the [OutlinedTextField] given all elements that should be
 * placed inside
 */
private fun calculateWidth(
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    textFieldPlaceable: Placeable,
    labelPlaceable: Placeable?,
    placeholderPlaceable: Placeable?,
    constraints: Constraints
): Int {
    val middleSection = maxOf(
        textFieldPlaceable.width,
        widthOrZero(labelPlaceable),
        widthOrZero(placeholderPlaceable)
    )
    val wrappedWidth =
        widthOrZero(leadingPlaceable) + middleSection + widthOrZero(
            trailingPlaceable
        )
    return max(wrappedWidth, constraints.minWidth)
}

/**
 * Calculate the height of the [OutlinedTextField] given all elements that should be
 * placed inside
 */
private fun calculateHeight(
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    textFieldPlaceable: Placeable,
    labelPlaceable: Placeable?,
    placeholderPlaceable: Placeable?,
    constraints: Constraints,
    density: Float
): Int {
    // middle section is defined as a height of the text field or placeholder ( whichever is
    // taller) plus 16.dp or half height of the label if it is taller, given that the label
    // is vertically centered to the top edge of the resulting text field's container
    val inputFieldHeight = max(
        textFieldPlaceable.height,
        heightOrZero(placeholderPlaceable)
    )
    val topBottomPadding = TextFieldPadding.value * density
    val middleSectionHeight = inputFieldHeight + topBottomPadding + max(
        topBottomPadding,
        heightOrZero(labelPlaceable) / 2f
    )
    return max(
        constraints.minHeight,
        maxOf(
            heightOrZero(leadingPlaceable),
            heightOrZero(trailingPlaceable),
            middleSectionHeight.roundToInt()
        )
    )
}

/**
 * Places the provided text field, placeholder, label, optional leading and trailing icons inside
 * the [OutlinedTextField]
 */
private fun Placeable.PlacementScope.place(
    height: Int,
    width: Int,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    textFieldPlaceable: Placeable,
    labelPlaceable: Placeable?,
    placeholderPlaceable: Placeable?,
    animationProgress: Float,
    singleLine: Boolean,
    density: Float
) {
    val topBottomPadding = (TextFieldPadding.value * density).roundToInt()

    // placed center vertically and to the start edge horizontally
    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )

    // placed center vertically and to the end edge horizontally
    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )

    // label position is animated
    // in single line text field label is centered vertically before animation starts
    labelPlaceable?.let {
        val startPositionY = if (singleLine) {
            Alignment.CenterVertically.align(it.height, height)
        } else {
            topBottomPadding
        }
        val positionY =
            startPositionY * (1 - animationProgress) - (it.height / 2) * animationProgress
        val positionX = (TextFieldPadding.value * density) +
            widthOrZero(leadingPlaceable) * (1 - animationProgress)
        it.placeRelative(positionX.roundToInt(), positionY.roundToInt())
    }

    // placed center vertically and after the leading icon horizontally if single line text field
    // placed to the top with padding for multi line text field
    val textVerticalPosition = if (singleLine) {
        Alignment.CenterVertically.align(textFieldPlaceable.height, height)
    } else {
        topBottomPadding
    }
    textFieldPlaceable.placeRelative(widthOrZero(leadingPlaceable), textVerticalPosition)

    // placed similar to the input text above
    placeholderPlaceable?.let {
        val placeholderVerticalPosition = if (singleLine) {
            Alignment.CenterVertically.align(it.height, height)
        } else {
            topBottomPadding
        }
        it.placeRelative(widthOrZero(leadingPlaceable), placeholderVerticalPosition)
    }
}

/**
 * A draw modifier to draw a border line in [OutlinedTextField]
 */
private fun Modifier.drawOutlinedBorder(
    borderParams: OutlinedBorderParams
): Modifier = drawBehind {
    val padding = TextFieldPadding.value * density
    val innerPadding = OutlinedTextFieldInnerPadding.value * density

    val lineWidth = borderParams.borderWidth.value.value * density
    val width: Float = size.width
    val height: Float = size.height

    val radius = borderParams.cornerRadius.value * density
    val dx = if (radius > width / 2) width / 2 else radius
    val dy = if (radius > height / 2) height / 2 else radius

    val path = Path().apply {
        // width and height minus corners and line width
        val effectiveWidth: Float = width - 2 * dx - lineWidth
        val effectiveHeight: Float = height - 2 * dy - lineWidth

        // top-right corner
        moveTo(width - lineWidth / 2, dy + lineWidth / 2)
        relativeQuadraticBezierTo(0f, -dy, -dx, -dy)

        // top line with gap
        val diff = borderParams.labelWidth.value
        if (diff == 0f) {
            relativeLineTo(-effectiveWidth, 0f)
        } else {
            val effectivePadding = padding - innerPadding - dx - lineWidth / 2
            val gap = diff + 2 * innerPadding
            if (layoutDirection == LayoutDirection.Ltr) {
                relativeLineTo(-effectiveWidth + effectivePadding + gap, 0f)
                relativeMoveTo(-gap, 0f)
                relativeLineTo(-effectivePadding, 0f)
            } else {
                relativeLineTo(-effectivePadding, 0f)
                relativeMoveTo(-gap, 0f)
                relativeLineTo(-effectiveWidth + gap + effectivePadding, 0f)
            }
        }

        // top-left corner and left line
        relativeQuadraticBezierTo(-dx, 0f, -dx, dy)
        relativeLineTo(0f, effectiveHeight)

        // bottom-left corner and bottom line
        relativeQuadraticBezierTo(0f, dy, dx, dy)
        relativeLineTo(effectiveWidth, 0f)

        // bottom-right corner and right line
        relativeQuadraticBezierTo(dx, 0f, dx, -dy)
        relativeLineTo(0f, -effectiveHeight)
    }

    drawPath(
        path = path,
        color = borderParams.color.value,
        style = Stroke(width = lineWidth)
    )
}

/**
 * A data class that stores parameters needed for [drawOutlinedBorder] modifier
 */
@Stable
private class OutlinedBorderParams(
    initialBorderWidth: Dp,
    initialColor: Color
) {
    val borderWidth = mutableStateOf(initialBorderWidth)
    val color = mutableStateOf(initialColor)
    val cornerRadius = OutlinedTextFieldCornerRadius
    val labelWidth = mutableStateOf(0f)
}

// TODO(b/158077409) support shape in OutlinedTextField
private val OutlinedTextFieldCornerRadius = 4.dp
private val OutlinedTextFieldInnerPadding = 4.dp