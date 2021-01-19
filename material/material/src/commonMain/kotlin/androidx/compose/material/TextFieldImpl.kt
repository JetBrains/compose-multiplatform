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

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

internal enum class TextFieldType {
    Filled, Outlined
}

/**
 * Implementation of the [TextField] and [OutlinedTextField]
 */
@Composable
@OptIn(ExperimentalFoundationApi::class, InternalTextApi::class)
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
    isErrorValue: Boolean,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxLines: Int = Int.MAX_VALUE,
    onImeActionPerformed: (ImeAction, SoftwareKeyboardController?) -> Unit,
    onTextInputStarted: (SoftwareKeyboardController) -> Unit,
    interactionState: InteractionState,
    activeColor: Color,
    inactiveColor: Color,
    errorColor: Color,
    backgroundColor: Color,
    shape: Shape
) {
    // TODO(soboleva): b/171305338 provide colors object and apply alpha there instead
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        AmbientContentColor.current
    }.copy(alpha = if (enabled) AmbientContentAlpha.current else ContentAlpha.disabled)
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    val keyboardController: Ref<SoftwareKeyboardController> = remember { Ref() }

    val isFocused = interactionState.contains(Interaction.Focused)
    val inputState = when {
        isFocused -> InputPhase.Focused
        value.text.isEmpty() -> InputPhase.UnfocusedEmpty
        else -> InputPhase.UnfocusedNotEmpty
    }

    TextFieldTransitionScope.Transition(
        inputState = inputState,
        showLabel = label != null,
        activeColor = if (isErrorValue) {
            errorColor
        } else {
            activeColor.applyAlpha(alpha = ContentAlpha.high)
        },
        labelInactiveColor = if (isErrorValue) {
            errorColor
        } else {
            inactiveColor.applyAlpha(if (enabled) ContentAlpha.medium else ContentAlpha.disabled)
        },
        indicatorInactiveColor = when {
            isErrorValue -> errorColor
            type == TextFieldType.Filled -> inactiveColor.applyAlpha(
                if (enabled) IndicatorInactiveAlpha else ContentAlpha.disabled
            )
            else -> inactiveColor.applyAlpha(alpha = ContentAlpha.disabled)
        }

    ) { labelProgress, animatedLabelColor, indicatorWidth, indicatorColor, placeholderAlpha ->

        val leadingColor = inactiveColor.applyAlpha(alpha = TrailingLeadingAlpha)
        val trailingColor = if (isErrorValue) errorColor else leadingColor

        val decoratedLabel: @Composable (() -> Unit)? =
            if (label != null) {
                @Composable {
                    val labelAnimatedStyle = lerp(
                        MaterialTheme.typography.subtitle1,
                        MaterialTheme.typography.caption,
                        labelProgress
                    )
                    Decoration(
                        contentColor = animatedLabelColor,
                        typography = labelAnimatedStyle,
                        content = label
                    )
                }
            } else null

        val decoratedPlaceholder: @Composable ((Modifier) -> Unit)? =
            if (placeholder != null && value.text.isEmpty()) {
                @Composable { modifier ->
                    Box(modifier.alpha(placeholderAlpha)) {
                        Decoration(
                            contentColor = inactiveColor,
                            typography = MaterialTheme.typography.subtitle1,
                            contentAlpha =
                                if (enabled) ContentAlpha.medium else ContentAlpha.disabled,
                            content = placeholder
                        )
                    }
                }
            } else null

        val cursorColor = if (isErrorValue) errorColor else activeColor
        val onImeActionPerformedAction: (ImeAction) -> Unit = {
            onImeActionPerformed(it, keyboardController.value)
        }
        val onTextInputStartedAction: (SoftwareKeyboardController) -> Unit = {
            keyboardController.value = it
            onTextInputStarted(it)
        }
        when (type) {
            TextFieldType.Filled -> {
                TextFieldLayout(
                    modifier = modifier,
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    textStyle = mergedTextStyle,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    onImeActionPerformed = onImeActionPerformedAction,
                    visualTransformation = visualTransformation,
                    onTextInputStarted = onTextInputStartedAction,
                    interactionState = interactionState,
                    decoratedPlaceholder = decoratedPlaceholder,
                    decoratedLabel = decoratedLabel,
                    leading = leading,
                    trailing = trailing,
                    leadingColor = leadingColor,
                    trailingColor = trailingColor,
                    labelProgress = labelProgress,
                    indicatorWidth = indicatorWidth,
                    indicatorColor = indicatorColor,
                    backgroundColor = backgroundColor,
                    cursorColor = cursorColor,
                    shape = shape
                )
            }
            TextFieldType.Outlined -> {
                OutlinedTextFieldLayout(
                    modifier = modifier,
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    textStyle = mergedTextStyle,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    onImeActionPerformed = onImeActionPerformedAction,
                    visualTransformation = visualTransformation,
                    onTextInputStarted = onTextInputStartedAction,
                    interactionState = interactionState,
                    decoratedPlaceholder = decoratedPlaceholder,
                    decoratedLabel = decoratedLabel,
                    leading = leading,
                    trailing = trailing,
                    leadingColor = leadingColor,
                    trailingColor = trailingColor,
                    labelProgress = labelProgress,
                    indicatorWidth = indicatorWidth,
                    indicatorColor = indicatorColor,
                    cursorColor = cursorColor
                )
            }
        }
    }
}

/**
 * Set alpha if the color is not translucent
 */
internal fun Color.applyAlpha(alpha: Float): Color {
    return if (this.alpha != 1f) this else this.copy(alpha = alpha)
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
    val colorAndEmphasis = @Composable {
        Providers(AmbientContentColor provides contentColor) {
            if (contentAlpha != null) {
                Providers(
                    AmbientContentAlpha provides contentAlpha,
                    content = content
                )
            } else {
                Providers(
                    AmbientContentAlpha provides contentColor.alpha,
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

/**
 * A modifier that applies padding only if the size of the element is not zero
 */
internal fun Modifier.iconPadding(start: Dp = 0.dp, end: Dp = 0.dp) =
    this.then(
        object : LayoutModifier, InspectorValueInfo(
            debugInspectorInfo {
                name = "iconPadding"
                properties["start"] = start
                properties["end"] = end
            }
        ) {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                val horizontal = start.toIntPx() + end.toIntPx()
                val placeable = measurable.measure(constraints.offset(-horizontal))
                val width = if (placeable.nonZero) {
                    constraints.constrainWidth(placeable.width + horizontal)
                } else {
                    0
                }
                return layout(width, placeable.height) {
                    placeable.placeRelative(start.toIntPx(), 0)
                }
            }
        }
    )

private object TextFieldTransitionScope {
    @Composable
    fun Transition(
        inputState: InputPhase,
        showLabel: Boolean,
        activeColor: Color,
        labelInactiveColor: Color,
        indicatorInactiveColor: Color,
        content: @Composable (
            labelProgress: Float,
            labelColor: Color,
            indicatorWidth: Dp,
            indicatorColor: Color,
            placeholderOpacity: Float
        ) -> Unit
    ) {
        // Transitions from/to InputPhase.Focused are the most critical in the transition below.
        // UnfocusedEmpty <-> UnfocusedNotEmpty are needed when a single state is used to control
        // multiple text fields.
        val transition = updateTransition(inputState)
        val labelColor by transition.animateColor(
            transitionSpec = { tween(durationMillis = AnimationDuration) }
        ) {
            when (it) {
                InputPhase.Focused -> activeColor
                InputPhase.UnfocusedEmpty -> labelInactiveColor
                InputPhase.UnfocusedNotEmpty -> labelInactiveColor
            }
        }
        val indicatorColor by transition.animateColor(
            transitionSpec = { tween(durationMillis = AnimationDuration) }
        ) {
            when (it) {
                InputPhase.Focused -> activeColor
                InputPhase.UnfocusedEmpty -> indicatorInactiveColor
                InputPhase.UnfocusedNotEmpty -> indicatorInactiveColor
            }
        }

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
            labelColor,
            indicatorWidth,
            indicatorColor,
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

private const val AnimationDuration = 150
private const val PlaceholderAnimationDuration = 83
private const val PlaceholderAnimationDelayOrDuration = 67

private val IndicatorUnfocusedWidth = 1.dp
private val IndicatorFocusedWidth = 2.dp
private const val TrailingLeadingAlpha = 0.54f
internal val TextFieldMinHeight = 56.dp
internal val TextFieldMinWidth = 280.dp
internal val TextFieldPadding = 16.dp
internal val HorizontalIconPadding = 12.dp

// Filled text field uses 42% opacity to meet the contrast requirements for accessibility reasons
private const val IndicatorInactiveAlpha = 0.42f
