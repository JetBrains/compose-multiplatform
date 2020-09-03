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

import androidx.compose.animation.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.ripple.RippleIndication
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Component to represent two states, selected and not selected.
 *
 * @sample androidx.compose.material.samples.RadioButtonSample
 *
 * [RadioButton]s can be combined together with [Text] in the desired layout (e.g. [Column] or
 * [Row]) to achieve radio group-like behaviour, where the entire layout is selectable:
 *
 * @sample androidx.compose.material.samples.RadioGroupSample
 *
 * @param selected boolean state for this button: either it is selected or not
 * @param onClick callback to be invoked when the RadioButton is being clicked
 * @param modifier Modifier to be applied to the radio button
 * @param enabled Controls the enabled state of the [RadioButton]. When `false`, this button will
 * not be selectable and appears in the disabled ui state
 * @param color color of the RadioButton. See [RadioButtonConstants.animateDefaultColor] for
 * customizing the color of the RadioButton in one / multiple states, such as when [selected] or
 * not [enabled].
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = RadioButtonConstants.animateDefaultColor(selected, enabled)
) {
    val dotRadius = animate(
        target = if (selected) RadioButtonDotSize / 2 else 0.dp,
        animSpec = tween(durationMillis = RadioAnimationDuration)
    )
    Canvas(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                indication = RippleIndication(bounded = false, radius = RadioButtonRippleRadius)
            )
            .wrapContentSize(Alignment.Center)
            .padding(RadioButtonPadding)
            .size(RadioButtonSize)
    ) {
        drawRadio(color, dotRadius)
    }
}

/**
 * Constants used in [RadioButton].
 */
object RadioButtonConstants {
    /**
     * Represents the default color used for a [RadioButton] as it animates between states.
     *
     * @param selected whether the RadioButton is selected
     * @param enabled whether the RadioButton is enabled
     * @param selectedColor the color to use for the RadioButton when selected and enabled.
     * @param unselectedColor the color to use for the RadioButton when unselected and enabled.
     * @param disabledColor the color to use for the RadioButton when disabled.
     * @return the resulting [Color] used for the RadioButton
     */
    @Composable
    fun animateDefaultColor(
        selected: Boolean,
        enabled: Boolean,
        selectedColor: Color = MaterialTheme.colors.secondary,
        unselectedColor: Color = defaultUnselectedColor,
        disabledColor: Color = defaultDisabledColor
    ): Color {
        val target = when {
            !enabled -> disabledColor
            !selected -> unselectedColor
            else -> selectedColor
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            animate(target, tween(durationMillis = RadioAnimationDuration))
        } else {
            target
        }
    }

    /**
     * Default color that will be used for a RadioButton when unselected
     */
    @Composable
    val defaultUnselectedColor: Color
        get() = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)

    /**
     * Default color that will be used for a RadioButton when disabled
     */
    @Composable
    val defaultDisabledColor: Color
        get() = EmphasisAmbient.current.disabled.applyEmphasis(MaterialTheme.colors.onSurface)
}

private fun DrawScope.drawRadio(color: Color, dotRadius: Dp) {
    val strokeWidth = RadioStrokeWidth.toPx()
    drawCircle(color, RadioRadius.toPx() - strokeWidth / 2, style = Stroke(strokeWidth))
    if (dotRadius > 0.dp) {
        drawCircle(color, dotRadius.toPx() - strokeWidth / 2, style = Fill)
    }
}

private const val RadioAnimationDuration = 100

private val RadioButtonRippleRadius = 24.dp
private val RadioButtonPadding = 2.dp
private val RadioButtonSize = 20.dp
private val RadioRadius = RadioButtonSize / 2
private val RadioButtonDotSize = 12.dp
private val RadioStrokeWidth = 2.dp
