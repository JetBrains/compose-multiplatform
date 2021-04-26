/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * <a href="https://material.io/components/radio-buttons" class="external" target="_blank">Material Design radio button</a>.
 *
 * Radio buttons allow users to select one option from a set.
 *
 * ![Radio buttons image](https://developer.android.com/images/reference/androidx/compose/material/radio-buttons.png)
 *
 * @sample androidx.compose.material.samples.RadioButtonSample
 *
 * [RadioButton]s can be combined together with [Text] in the desired layout (e.g. [Column] or
 * [Row]) to achieve radio group-like behaviour, where the entire layout is selectable:
 *
 * @sample androidx.compose.material.samples.RadioGroupSample
 *
 * @param selected boolean state for this button: either it is selected or not
 * @param onClick callback to be invoked when the RadioButton is being clicked.  If null,
 * then this is passive and relies entirely on a higher-level component to control the state.
 * @param modifier Modifier to be applied to the radio button
 * @param enabled Controls the enabled state of the [RadioButton]. When `false`, this button will
 * not be selectable and appears in the disabled ui state
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this RadioButton. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this RadioButton in different [Interaction]s.
 * @param colors [RadioButtonColors] that will be used to resolve the color used for this
 * RadioButton in different states. See [RadioButtonDefaults.colors].
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: RadioButtonColors = RadioButtonDefaults.colors()
) {
    val dotRadius by animateDpAsState(
        targetValue = if (selected) RadioButtonDotSize / 2 else 0.dp,
        animationSpec = tween(durationMillis = RadioAnimationDuration)
    )
    val radioColor by colors.radioColor(enabled, selected)
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
                    radius = RadioButtonRippleRadius
                )
            )
        } else {
            Modifier
        }
    Canvas(
        modifier
            .then(selectableModifier)
            .wrapContentSize(Alignment.Center)
            .padding(RadioButtonPadding)
            .requiredSize(RadioButtonSize)
    ) {
        drawRadio(radioColor, dotRadius)
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
     * @param disabledColor the color to use for the RadioButton when disabled.
     * @return the resulting [Color] used for the RadioButton
     */
    @Composable
    fun colors(
        selectedColor: Color = MaterialTheme.colors.secondary,
        unselectedColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        disabledColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
    ): RadioButtonColors {
        return remember(
            selectedColor,
            unselectedColor,
            disabledColor
        ) {
            DefaultRadioButtonColors(selectedColor, unselectedColor, disabledColor)
        }
    }
}

private fun DrawScope.drawRadio(color: Color, dotRadius: Dp) {
    val strokeWidth = RadioStrokeWidth.toPx()
    drawCircle(color, RadioRadius.toPx() - strokeWidth / 2, style = Stroke(strokeWidth))
    if (dotRadius > 0.dp) {
        drawCircle(color, dotRadius.toPx() - strokeWidth / 2, style = Fill)
    }
}

/**
 * Default [RadioButtonColors] implementation.
 */
@Stable
private class DefaultRadioButtonColors(
    private val selectedColor: Color,
    private val unselectedColor: Color,
    private val disabledColor: Color
) : RadioButtonColors {
    @Composable
    override fun radioColor(enabled: Boolean, selected: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledColor
            !selected -> unselectedColor
            else -> selectedColor
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            animateColorAsState(target, tween(durationMillis = RadioAnimationDuration))
        } else {
            rememberUpdatedState(target)
        }
    }
}

private const val RadioAnimationDuration = 100

private val RadioButtonRippleRadius = 24.dp
private val RadioButtonPadding = 2.dp
private val RadioButtonSize = 20.dp
private val RadioRadius = RadioButtonSize / 2
private val RadioButtonDotSize = 12.dp
private val RadioStrokeWidth = 2.dp
