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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Strings.Companion.DefaultErrorMessage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class TextFieldType {
    Filled, Outlined
}

/**
 * Implementation of the [TextField] and [OutlinedTextField]
 */
@Composable
internal fun TextFieldImpl(
    type: TextFieldType,
    enabled: Boolean,
    readOnly: Boolean,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    singleLine: Boolean,
    textStyle: TextStyle,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    isError: Boolean,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource,
    shape: Shape,
    colors: TextFieldColors
) {
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    val isFocused = interactionSource.collectIsFocusedAsState().value
    val transformedText = remember(value.annotatedString, visualTransformation) {
        visualTransformation.filter(value.annotatedString)
    }.text
    val inputState = when {
        isFocused -> InputPhase.Focused
        transformedText.isEmpty() -> InputPhase.UnfocusedEmpty
        else -> InputPhase.UnfocusedNotEmpty
    }

    TextFieldTransitionScope.Transition(
        inputState = inputState, showLabel = label != null
    ) { labelProgress, indicatorWidth, placeholderAlphaProgress ->

        val decoratedLabel: @Composable (() -> Unit)? =
            if (label != null) {
                @Composable {
                    val labelAnimatedStyle = lerp(
                        MaterialTheme.typography.subtitle1,
                        MaterialTheme.typography.caption,
                        labelProgress
                    )
                    Decoration(
                        contentColor = colors.labelColor(enabled, isError, interactionSource).value,
                        typography = labelAnimatedStyle,
                        content = label
                    )
                }
            } else null

        val decoratedPlaceholder: @Composable ((Modifier) -> Unit)? =
            if (placeholder != null && transformedText.isEmpty()) {
                @Composable { modifier ->
                    Box(modifier.alpha(placeholderAlphaProgress)) {
                        Decoration(
                            contentColor = colors.placeholderColor(enabled).value,
                            typography = MaterialTheme.typography.subtitle1,
                            content = placeholder
                        )
                    }
                }
            } else null

        // Developers need to handle invalid input manually. But since we don't provide error
        // message slot API, we can set the default error message in case developers forget about
        // it.
        val defaultErrorMessage = getString(DefaultErrorMessage)
        val textFieldModifier = modifier.semantics { if (isError) error(defaultErrorMessage) }
        when (type) {
            TextFieldType.Filled -> {
                TextFieldLayout(
                    modifier = textFieldModifier,
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    textStyle = mergedTextStyle,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    decoratedPlaceholder = decoratedPlaceholder,
                    decoratedLabel = decoratedLabel,
                    leading = leading,
                    trailing = trailing,
                    leadingColor = colors.leadingIconColor(enabled, isError).value,
                    trailingColor = colors.trailingIconColor(enabled, isError).value,
                    labelProgress = labelProgress,
                    indicatorWidth = indicatorWidth,
                    indicatorColor =
                        colors.indicatorColor(enabled, isError, interactionSource).value,
                    backgroundColor = colors.backgroundColor(enabled).value,
                    cursorColor = colors.cursorColor(isError).value,
                    shape = shape
                )
            }
            TextFieldType.Outlined -> {
                OutlinedTextFieldLayout(
                    modifier = textFieldModifier,
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    textStyle = mergedTextStyle,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    decoratedPlaceholder = decoratedPlaceholder,
                    decoratedLabel = decoratedLabel,
                    leading = leading,
                    trailing = trailing,
                    leadingColor = colors.leadingIconColor(enabled, isError).value,
                    trailingColor = colors.trailingIconColor(enabled, isError).value,
                    labelProgress = labelProgress,
                    indicatorWidth = indicatorWidth,
                    indicatorColor =
                        colors.indicatorColor(enabled, isError, interactionSource).value,
                    shape = shape,
                    backgroundColor = colors.backgroundColor(enabled).value,
                    cursorColor = colors.cursorColor(isError).value
                )
            }
        }
    }
}

/**
 * Set content color, typography and emphasis for [content] composable
 */
@Composable
internal fun Decoration(
    contentColor: Color,
    typography: TextStyle? = null,
    contentAlpha: Float? = null,
    content: @Composable () -> Unit
) {
    val colorAndEmphasis: @Composable () -> Unit = @Composable {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (contentAlpha != null) {
                CompositionLocalProvider(
                    LocalContentAlpha provides contentAlpha,
                    content = content
                )
            } else {
                CompositionLocalProvider(
                    LocalContentAlpha provides contentColor.alpha,
                    content = content
                )
            }
        }
    }
    if (typography != null) ProvideTextStyle(typography, colorAndEmphasis) else colorAndEmphasis()
}

private val Placeable.nonZero: Boolean get() = this.width != 0 || this.height != 0
internal fun widthOrZero(placeable: Placeable?) = placeable?.width ?: 0
internal fun heightOrZero(placeable: Placeable?) = placeable?.height ?: 0

private object TextFieldTransitionScope {
    @Composable
    fun Transition(
        inputState: InputPhase,
        showLabel: Boolean,
        content: @Composable (
            labelProgress: Float,
            indicatorWidth: Dp,
            placeholderOpacity: Float
        ) -> Unit
    ) {
        // Transitions from/to InputPhase.Focused are the most critical in the transition below.
        // UnfocusedEmpty <-> UnfocusedNotEmpty are needed when a single state is used to control
        // multiple text fields.
        val transition = updateTransition(inputState)

        val labelProgress by transition.animateFloat(
            transitionSpec = { tween(durationMillis = AnimationDuration) }
        ) {
            when (it) {
                InputPhase.Focused -> 1f
                InputPhase.UnfocusedEmpty -> 0f
                InputPhase.UnfocusedNotEmpty -> 1f
            }
        }

        val indicatorWidth by transition.animateDp(
            transitionSpec = { tween(durationMillis = AnimationDuration) }
        ) {
            when (it) {
                InputPhase.Focused -> IndicatorFocusedWidth
                InputPhase.UnfocusedEmpty -> IndicatorUnfocusedWidth
                InputPhase.UnfocusedNotEmpty -> IndicatorUnfocusedWidth
            }
        }

        val placeholderOpacity by transition.animateFloat(
            transitionSpec = {
                if (InputPhase.Focused isTransitioningTo InputPhase.UnfocusedEmpty) {
                    tween(
                        durationMillis = PlaceholderAnimationDelayOrDuration,
                        easing = LinearEasing
                    )
                } else if (InputPhase.UnfocusedEmpty isTransitioningTo InputPhase.Focused ||
                    InputPhase.UnfocusedNotEmpty isTransitioningTo InputPhase.UnfocusedEmpty
                ) {
                    tween(
                        durationMillis = PlaceholderAnimationDuration,
                        delayMillis = PlaceholderAnimationDelayOrDuration,
                        easing = LinearEasing
                    )
                } else {
                    spring()
                }
            }
        ) {
            when (it) {
                InputPhase.Focused -> 1f
                InputPhase.UnfocusedEmpty -> if (showLabel) 0f else 1f
                InputPhase.UnfocusedNotEmpty -> 0f
            }
        }

        content(
            labelProgress,
            indicatorWidth,
            placeholderOpacity
        )
    }
}

/**
 * An internal state used to animate a label and an indicator.
 */
private enum class InputPhase {
    // Text field is focused
    Focused,

    // Text field is not focused and input text is empty
    UnfocusedEmpty,

    // Text field is not focused but input text is not empty
    UnfocusedNotEmpty
}

internal const val TextFieldId = "TextField"
internal const val PlaceholderId = "Hint"
internal const val LabelId = "Label"
internal const val LeadingId = "Leading"
internal const val TrailingId = "Trailing"

internal const val AnimationDuration = 150
private const val PlaceholderAnimationDuration = 83
private const val PlaceholderAnimationDelayOrDuration = 67

private val IndicatorUnfocusedWidth = 1.dp
private val IndicatorFocusedWidth = 2.dp
internal val TextFieldPadding = 16.dp
internal val HorizontalIconPadding = 12.dp

internal val IconDefaultSizeModifier = Modifier.defaultMinSize(48.dp, 48.dp)