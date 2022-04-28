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

package androidx.compose.material3

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.RadioButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Material Design radio button.
 *
 * Radio buttons allow users to select one option from a set.
 *
 * ![Radio button image](https://developer.android.com/images/reference/androidx/compose/material3/radio-button.png)
 *
 * @sample androidx.compose.material3.samples.RadioButtonSample
 *
 * [RadioButton]s can be combined together with [Text] in the desired layout (e.g. [Column] or
 * [Row]) to achieve radio group-like behaviour, where the entire layout is selectable:
 * @sample androidx.compose.material3.samples.RadioGroupSample
 *
 * @param selected whether this radio button is selected or not
 * @param onClick called when this radio button is clicked. If `null`, then this radio button will
 * not be interactable, unless something else handles its input events and updates its state.
 * @param modifier the [Modifier] to be applied to this radio button
 * @param enabled controls the enabled state of this radio button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this radio button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this radio button in different states.
 * @param colors [RadioButtonColors] that will be used to resolve the color used for this radio
 * button in different states. See [RadioButtonDefaults.colors].
 */
@ExperimentalMaterial3Api
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: RadioButtonColors = RadioButtonDefaults.colors()
) {
    val dotRadius = animateDpAsState(
        targetValue = if (selected) RadioButtonDotSize / 2 else 0.dp,
        animationSpec = tween(durationMillis = RadioAnimationDuration)
    )
    val radioColor = colors.radioColor(enabled, selected)
    val selectableModifier =
        if (onClick != null) {
            Modifier.selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = false,
                    radius = RadioButtonTokens.StateLayerSize / 2
                )
            )
        } else {
            Modifier
        }
    Canvas(
        modifier
            .then(
                if (onClick != null) {
                    Modifier.minimumTouchTargetSize()
                } else {
                    Modifier
                }
            )
            .then(selectableModifier)
            .wrapContentSize(Alignment.Center)
            .padding(RadioButtonPadding)
            .requiredSize(RadioButtonTokens.IconSize)
    ) {
        // Draw the radio button
        val strokeWidth = RadioStrokeWidth.toPx()
        drawCircle(
            radioColor.value,
            radius = (RadioButtonTokens.IconSize / 2).toPx() - strokeWidth / 2,
            style = Stroke(strokeWidth)
        )
        if (dotRadius.value > 0.dp) {
            drawCircle(radioColor.value, dotRadius.value.toPx() - strokeWidth / 2, style = Fill)
        }
    }
}

/**
 * Represents the color used by a [RadioButton] in different states.
 *
 * See [RadioButtonDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@Stable
interface RadioButtonColors {
    /**
     * Represents the main color used to draw the outer and inner circles, depending on whether
     * the [RadioButton] is [enabled] / [selected].
     *
     * @param enabled whether the [RadioButton] is enabled
     * @param selected whether the [RadioButton] is selected
     */
    @Composable
    fun radioColor(enabled: Boolean, selected: Boolean): State<Color>
}

/**
 * Defaults used in [RadioButton].
 */
object RadioButtonDefaults {
    /**
     * Creates a [RadioButtonColors] that will animate between the provided colors according to
     * the Material specification.
     *
     * @param selectedColor the color to use for the RadioButton when selected and enabled.
     * @param unselectedColor the color to use for the RadioButton when unselected and enabled.
     * @param disabledSelectedColor the color to use for the RadioButton when disabled and selected.
     * @param disabledUnselectedColor the color to use for the RadioButton when disabled and not
     * selected.
     * @return the resulting [RadioButtonColors] used for the RadioButton
     */
    @Composable
    fun colors(
        selectedColor: Color = RadioButtonTokens.SelectedIconColor.toColor(),
        unselectedColor: Color = RadioButtonTokens.UnselectedIconColor.toColor(),
        disabledSelectedColor: Color = RadioButtonTokens.DisabledSelectedIconColor
            .toColor()
            .copy(alpha = RadioButtonTokens.DisabledSelectedIconOpacity),
        disabledUnselectedColor: Color = RadioButtonTokens.DisabledUnselectedIconColor
            .toColor()
            .copy(alpha = RadioButtonTokens.DisabledUnselectedIconOpacity)
    ): RadioButtonColors {
        return remember(
            selectedColor,
            unselectedColor,
            disabledSelectedColor,
            disabledUnselectedColor
        ) {
            DefaultRadioButtonColors(
                selectedColor,
                unselectedColor,
                disabledSelectedColor,
                disabledUnselectedColor
            )
        }
    }
}

/**
 * Default [RadioButtonColors] implementation.
 */
@Immutable
private class DefaultRadioButtonColors(
    private val selectedColor: Color,
    private val unselectedColor: Color,
    private val disabledSelectedColor: Color,
    private val disabledUnselectedColor: Color
) : RadioButtonColors {
    @Composable
    override fun radioColor(enabled: Boolean, selected: Boolean): State<Color> {
        val target = when {
            enabled && selected -> selectedColor
            enabled && !selected -> unselectedColor
            !enabled && selected -> disabledSelectedColor
            else -> disabledUnselectedColor
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            animateColorAsState(target, tween(durationMillis = RadioAnimationDuration))
        } else {
            rememberUpdatedState(target)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultRadioButtonColors

        if (selectedColor != other.selectedColor) return false
        if (unselectedColor != other.unselectedColor) return false
        if (disabledSelectedColor != other.disabledSelectedColor) return false
        if (disabledUnselectedColor != other.disabledUnselectedColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedColor.hashCode()
        result = 31 * result + unselectedColor.hashCode()
        result = 31 * result + disabledSelectedColor.hashCode()
        result = 31 * result + disabledUnselectedColor.hashCode()
        return result
    }
}

private const val RadioAnimationDuration = 100

private val RadioButtonPadding = 2.dp
private val RadioButtonDotSize = 12.dp
private val RadioStrokeWidth = 2.dp
