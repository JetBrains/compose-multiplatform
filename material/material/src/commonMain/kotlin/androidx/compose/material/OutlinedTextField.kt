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

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextFieldDefaults.MinHeight
import androidx.compose.material.TextFieldDefaults.MinWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * <a href="https://material.io/components/text-fields#outlined-text-field" class="external" target="_blank">Material Design outlined text field</a>.
 *
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field image](https://developer.android.com/images/reference/androidx/compose/material/outlined-text-field.png)
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
 * @param enabled controls the enabled state of the [OutlinedTextField]. When `false`, the text field will
 * be neither editable nor focusable, the input of the text field will not be selectable,
 * visually text field will appear in the disabled UI state
 * @param readOnly controls the editable state of the [OutlinedTextField]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [LocalTextStyle] defined by the theme
 * @param label the optional label to be displayed inside the text field container. The default
 * text style for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * @param isError indicates if the text field's current value is in error. If set to true, the
 * label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value]
 * For example, you can use [androidx.compose.ui.text.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction]
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction]
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines. The keyboard will be informed to not show
 * the return key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the
 * maxLines attribute will be automatically set to 1
 * @param maxLines the maximum height in terms of maximum number of visible lines. Should be
 * equal or greater than 1. Note that this parameter will be ignored and instead maxLines will be
 * set to 1 if [singleLine] is set to true
 * [KeyboardOptions.imeAction] field.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this OutlinedTextField. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this OutlinedTextField in different [Interaction]s.
 * @param shape the shape of the text field's border
 * @param colors [TextFieldColors] that will be used to resolve color of the text and content
 * (including label, placeholder, leading and trailing icons, border) for this text field in
 * different states. See [TextFieldDefaults.outlinedTextFieldColors]
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)

    OutlinedTextField(
        enabled = enabled,
        readOnly = readOnly,
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
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        maxLines = maxLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

/**
 * <a href="https://material.io/components/text-fields#outlined-text-field" class="external" target="_blank">Material Design outlined text field</a>.
 *
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field image](https://developer.android.com/images/reference/androidx/compose/material/outlined-text-field.png)
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
 * @param enabled controls the enabled state of the [OutlinedTextField]. When `false`, the text field will
 * be neither editable nor focusable, the input of the text field will not be selectable,
 * visually text field will appear in the disabled UI state
 * @param readOnly controls the editable state of the [OutlinedTextField]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [LocalTextStyle] defined by the theme
 * @param label the optional label to be displayed inside the text field container. The default
 * text style for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * @param isError indicates if the text field's current value is in error state. If set to
 * true, the label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value]
 * For example, you can use [androidx.compose.ui.text.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction]
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction]
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines. The keyboard will be informed to not show
 * the return key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the
 * maxLines attribute will be automatically set to 1
 * @param maxLines the maximum height in terms of maximum number of visible lines. Should be
 * equal or greater than 1. Note that this parameter will be ignored and instead maxLines will be
 * set to 1 if [singleLine] is set to true
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this OutlinedTextField. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this OutlinedTextField in different [Interaction]s.
 * @param shape the shape of the text field's border
 * @param colors [TextFieldColors] that will be used to resolve color of the text and content
 * (including label, placeholder, leading and trailing icons, border) for this text field in
 * different states. See [TextFieldDefaults.outlinedTextFieldColors]
 */
@Composable
fun OutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    TextFieldImpl(
        type = TextFieldType.Outlined,
        enabled = enabled,
        readOnly = readOnly,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leading = leadingIcon,
        trailing = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        maxLines = maxLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

@Composable
internal fun OutlinedTextFieldLayout(
    modifier: Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    readOnly: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    textStyle: TextStyle,
    singleLine: Boolean,
    maxLines: Int = Int.MAX_VALUE,
    visualTransformation: VisualTransformation,
    interactionSource: MutableInteractionSource,
    decoratedPlaceholder: @Composable ((Modifier) -> Unit)?,
    decoratedLabel: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    leadingColor: Color,
    trailingColor: Color,
    labelProgress: Float,
    indicatorWidth: Dp,
    indicatorColor: Color,
    cursorColor: Color,
    shape: Shape
) {
    val labelSize = remember { mutableStateOf(Size.Zero) }
    BasicTextField(
        value = value,
        modifier = modifier
            .defaultMinSize(
                minWidth = MinWidth,
                minHeight = MinHeight + OutlinedTextFieldTopPadding,
            )
            .padding(top = OutlinedTextFieldTopPadding),
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        cursorBrush = SolidColor(cursorColor),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        decorationBox = @Composable { coreTextField ->
            // places leading icon, input field, label, placeholder, trailing icon
            IconsWithTextFieldLayout(
                textField = coreTextField,
                leading = leading,
                trailing = trailing,
                singleLine = singleLine,
                leadingColor = leadingColor,
                trailingColor = trailingColor,
                onLabelMeasured = {
                    val labelWidth = it.width * labelProgress
                    val labelHeight = it.height * labelProgress
                    if (labelSize.value.width != labelWidth ||
                        labelSize.value.height != labelHeight
                    ) {
                        labelSize.value = Size(labelWidth, labelHeight)
                    }
                },
                animationProgress = labelProgress,
                placeholder = decoratedPlaceholder,
                label = decoratedLabel,
                shape = shape,
                borderWidth = indicatorWidth,
                borderColor = indicatorColor,
                labelSize = labelSize.value
            )
        }
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
    textField: @Composable () -> Unit,
    placeholder: @Composable ((Modifier) -> Unit)?,
    label: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    singleLine: Boolean,
    leadingColor: Color,
    trailingColor: Color,
    animationProgress: Float,
    onLabelMeasured: (Size) -> Unit,
    shape: Shape,
    borderWidth: Dp,
    borderColor: Color,
    labelSize: Size
) {
    Layout(
        content = {
            // We use additional box here to place an outlined cutout border as a sibling after the
            // rest of UI. This allows us to use Modifier.border to draw an outline on top of the
            // text field. We can't use the border modifier directly on the IconsWithTextFieldLayout
            // as we also need to do the clipping (to form the cutout) which should not affect
            // the rest of text field UI
            Box(
                Modifier
                    .layoutId("border")
                    .outlinedBorder(shape, borderWidth, borderColor, labelSize)
            )

            if (leading != null) {
                Box(Modifier.layoutId(LeadingId).iconPadding(start = HorizontalIconPadding)) {
                    Decoration(
                        contentColor = leadingColor,
                        content = leading
                    )
                }
            }
            if (trailing != null) {
                Box(Modifier.layoutId(TrailingId).iconPadding(end = HorizontalIconPadding)) {
                    Decoration(
                        contentColor = trailingColor,
                        content = trailing
                    )
                }
            }
            if (placeholder != null) {
                placeholder(Modifier.layoutId(PlaceholderId).padding(horizontal = TextFieldPadding))
            }

            Box(
                modifier = Modifier.layoutId(TextFieldId).padding(horizontal = TextFieldPadding),
                propagateMinConstraints = true
            ) {
                textField()
            }

            if (label != null) {
                Box(modifier = Modifier.layoutId(LabelId)) { label() }
            }
        }
    ) { measurables, incomingConstraints ->
        // used to calculate the constraints for measuring elements that will be placed in a row
        var occupiedSpaceHorizontally = 0
        val bottomPadding = TextFieldPadding.roundToPx()

        // measure leading icon
        val constraints =
            incomingConstraints.copy(minWidth = 0, minHeight = 0)
        val leadingPlaceable = measurables.find { it.layoutId == LeadingId }?.measure(constraints)
        occupiedSpaceHorizontally += widthOrZero(
            leadingPlaceable
        )

        // measure trailing icon
        val trailingPlaceable = measurables.find { it.layoutId == TrailingId }
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
            onLabelMeasured(Size(it.width.toFloat(), it.height.toFloat()))
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

        val borderPlaceable = measurables.first { it.layoutId == "border" }.measure(
            Constraints(
                minWidth = if (width != Constraints.Infinity) width else 0,
                maxWidth = width,
                minHeight = if (height != Constraints.Infinity) height else 0,
                maxHeight = height
            )
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
                borderPlaceable,
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
    borderPlaceable: Placeable,
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

    // place border
    borderPlaceable.place(IntOffset.Zero)
}

/**
 * Draws an outlined border with label cutout
 */
private fun Modifier.outlinedBorder(
    shape: Shape,
    borderWidth: Dp,
    borderColor: Color,
    labelSize: Size
) = this
    .outlineCutout(labelSize)
    .border(
        width = borderWidth,
        color = borderColor,
        shape = shape
    )

private fun Modifier.outlineCutout(labelSize: Size) =
    this.drawWithContent {
        val labelWidth = labelSize.width
        if (labelWidth > 0f) {
            val innerPadding = OutlinedTextFieldInnerPadding.toPx()
            val leftLtr = TextFieldPadding.toPx() - innerPadding
            val rightLtr = leftLtr + labelWidth + 2 * innerPadding
            val left = when (layoutDirection) {
                LayoutDirection.Ltr -> leftLtr
                LayoutDirection.Rtl -> size.width - rightLtr
            }
            val right = when (layoutDirection) {
                LayoutDirection.Ltr -> rightLtr
                LayoutDirection.Rtl -> size.width - leftLtr
            }
            val labelHeight = labelSize.height
            // using label height as a cutout area to make sure that no hairline artifacts are
            // left when we clip the border
            clipRect(left, -labelHeight / 2, right, labelHeight / 2, ClipOp.Difference) {
                this@drawWithContent.drawContent()
            }
        } else {
            this@drawWithContent.drawContent()
        }
    }

private val OutlinedTextFieldInnerPadding = 4.dp

/*
This padding is used to allow label not overlap with the content above it. This 8.dp will work
for default cases when developers do not override the label's font size. If they do, they will
need to add additional padding themselves
*/
private val OutlinedTextFieldTopPadding = 8.dp
