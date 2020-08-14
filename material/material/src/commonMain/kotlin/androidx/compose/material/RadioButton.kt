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

import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.DpPropKey
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
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
import androidx.compose.runtime.remember
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
 * @param selectedColor color of the RadioButton when selected
 * @param unselectedColor color of the RadioButton when not selected
 * @param disabledColor color of the RadioButton when disabled
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = MaterialTheme.colors.secondary,
    unselectedColor: Color = RadioButtonConstants.defaultUnselectedColor,
    disabledColor: Color = RadioButtonConstants.defaultDisabledColor
) {
    val definition = remember(selectedColor, unselectedColor) {
        generateTransitionDefinition(selectedColor, unselectedColor)
    }
    val state = transition(definition = definition, toState = selected)
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
        val color = if (enabled) state[ColorProp] else disabledColor
        drawRadio(color, state[DotRadiusProp])
    }
}

/**
 * Constants used in [RadioButton].
 */
object RadioButtonConstants {

    /**
     * Default color that will be used for [RadioButton] when disabled
     */
    @Composable
    val defaultDisabledColor: Color
        get() {
            return EmphasisAmbient.current.disabled.applyEmphasis(
                MaterialTheme.colors.onSurface
            )
        }

    /**
     * Default color that will be used for [RadioButton] when unselected
     */
    @Composable
    val defaultUnselectedColor: Color
        get() {
            return MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        }
}

private fun DrawScope.drawRadio(color: Color, dotRadius: Dp) {
    val strokeWidth = RadioStrokeWidth.toPx()
    drawCircle(color, RadioRadius.toPx() - strokeWidth / 2, style = Stroke(strokeWidth))
    if (dotRadius > 0.dp) {
        drawCircle(color, dotRadius.toPx() - strokeWidth / 2, style = Fill)
    }
}

private val DotRadiusProp = DpPropKey()
private val ColorProp = ColorPropKey()
private const val RadioAnimationDuration = 100

private fun generateTransitionDefinition(selectedColor: Color, unselectedColor: Color) =
    transitionDefinition<Boolean> {
        state(false) {
            this[DotRadiusProp] = 0.dp
            this[ColorProp] = unselectedColor
        }
        state(true) {
            this[DotRadiusProp] = RadioButtonDotSize / 2
            this[ColorProp] = selectedColor
        }
        transition {
            ColorProp using tween(
                durationMillis = RadioAnimationDuration
            )
            DotRadiusProp using tween(
                durationMillis = RadioAnimationDuration
            )
        }
    }

private val RadioButtonRippleRadius = 24.dp
private val RadioButtonPadding = 2.dp
private val RadioButtonSize = 20.dp
private val RadioRadius = RadioButtonSize / 2
private val RadioButtonDotSize = 12.dp
private val RadioStrokeWidth = 2.dp
