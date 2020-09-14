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
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.TransitionSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.ToggleableState
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material.ripple.RippleIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Radius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * A component that represents two states (checked / unchecked).
 *
 * @sample androidx.compose.material.samples.CheckboxSample
 *
 * @see [TriStateCheckbox] if you require support for an indeterminate state, or more advanced
 * color customization between states.
 *
 * @param checked whether Checkbox is checked or unchecked
 * @param onCheckedChange callback to be invoked when checkbox is being clicked,
 * therefore the change of checked state in requested.
 * @param modifier Modifier to be applied to the layout of the checkbox
 * @param enabled enabled whether or not this [Checkbox] will handle input events and appear
 * enabled for semantics purposes
 * @param checkedColor color of the box of the Checkbox when [checked]. See
 * [TriStateCheckbox] to fully customize the color of the checkmark / box / border in different
 * states.
 * @param uncheckedColor color of the border of the Checkbox when not [checked]. See
 * [TriStateCheckbox] to fully customize the color of the checkmark / box / border in different
 * states.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = MaterialTheme.colors.secondary,
    uncheckedColor: Color = CheckboxConstants.defaultUncheckedColor
) {
    TriStateCheckbox(
        state = ToggleableState(checked),
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        boxColor = CheckboxConstants.animateDefaultBoxColor(
            state = ToggleableState(checked),
            enabled = enabled,
            checkedColor = checkedColor
        ),
        borderColor = CheckboxConstants.animateDefaultBorderColor(
            state = ToggleableState(checked),
            enabled = enabled,
            checkedColor = checkedColor,
            uncheckedColor = uncheckedColor
        ),
        modifier = modifier
    )
}

/**
 * A TriStateCheckbox is a toggleable component that provides
 * checked / unchecked / indeterminate options.
 * <p>
 * A TriStateCheckbox should be used when there are
 * dependent checkboxes associated to this component and those can have different values.
 *
 * @sample androidx.compose.material.samples.TriStateCheckboxSample
 *
 * @see [Checkbox] if you want a simple component that represents Boolean state
 *
 * @param state whether TriStateCheckbox is checked, unchecked or in indeterminate state
 * @param onClick callback to be invoked when checkbox is being clicked,
 * therefore the change of ToggleableState state is requested.
 * @param modifier Modifier to be applied to the layout of the checkbox
 * @param enabled whether or not this [TriStateCheckbox] will handle input events and
 * appear enabled for semantics purposes
 * @param checkMarkColor color of the check mark of the [TriStateCheckbox]. See
 * [CheckboxConstants.animateDefaultCheckmarkColor] for customizing the check mark color in
 * different [state]s.
 * @param boxColor background color of the box containing the checkmark. See
 * [CheckboxConstants.animateDefaultBoxColor] for customizing the box color in different [state]s,
 * such as when [ToggleableState.On] or not [enabled].
 * @param borderColor color of the border of the box containing the checkmark. See
 * [CheckboxConstants.animateDefaultBorderColor] for customizing the border color in different
 * [state]s, such as when [ToggleableState.On] or not [enabled].
 */
@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkMarkColor: Color = CheckboxConstants.animateDefaultCheckmarkColor(state),
    boxColor: Color = CheckboxConstants.animateDefaultBoxColor(state, enabled),
    borderColor: Color = CheckboxConstants.animateDefaultBorderColor(state, enabled)
) {
    CheckboxImpl(
        value = state,
        modifier = modifier
            .triStateToggleable(
                state = state,
                onClick = onClick,
                enabled = enabled,
                indication = RippleIndication(bounded = false, radius = CheckboxRippleRadius)
            )
            .padding(CheckboxDefaultPadding),
        checkColor = checkMarkColor,
        boxColor = boxColor,
        borderColor = borderColor
    )
}

/**
 * Constants used in [Checkbox] and [TriStateCheckbox].
 */
object CheckboxConstants {

    /**
     * Represents the default color used for the checkmark in a [Checkbox] or [TriStateCheckbox]
     * as it animates between states.
     *
     * @param state the [ToggleableState] of the checkbox
     * @param checkedColor the color to use for the checkmark when the Checkbox is
     * [ToggleableState.On].
     * @param uncheckedColor the color to use for the checkmark when the Checkbox is
     * [ToggleableState.Off] - this is typically transparent, as no checkmark should be displayed
     * in this state.
     * @return the [Color] representing the checkmark color
     */
    @Composable
    fun animateDefaultCheckmarkColor(
        state: ToggleableState,
        checkedColor: Color = MaterialTheme.colors.surface,
        uncheckedColor: Color = checkedColor.copy(alpha = 0f)
    ): Color {
        val target = if (state == ToggleableState.Off) uncheckedColor else checkedColor

        val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
        return animate(target, tween(durationMillis = duration))
    }

    /**
     * Represents the default color used for the background of the box in a [Checkbox] or
     * [TriStateCheckbox] as it animates between states.
     *
     * @param state the [ToggleableState] of the checkbox
     * @param enabled whether the checkbox is enabled
     * @param checkedColor the color to use for the background of the box when the Checkbox is
     * [ToggleableState.On].
     * @param uncheckedColor the color to use for the background of the box when the Checkbox is
     * [ToggleableState.Off] - this is typically transparent.
     * @param disabledCheckedColor the color to use for the background of the box when the Checkbox is
     * [ToggleableState.On] and not [enabled].
     * @param disabledUncheckedColor the color to use for the background of the box when the
     * Checkbox is [ToggleableState.Off] and not [enabled].
     * @param disabledIndeterminateColor the color to use for the background of the box when the
     * Checkbox is [ToggleableState.Indeterminate] and not [enabled].
     * @return the [Color] representing the background color of the box
     */
    @Composable
    fun animateDefaultBoxColor(
        state: ToggleableState,
        enabled: Boolean,
        checkedColor: Color = MaterialTheme.colors.secondary,
        uncheckedColor: Color = checkedColor.copy(alpha = 0f),
        disabledCheckedColor: Color = defaultDisabledColor,
        disabledUncheckedColor: Color = Color.Transparent,
        disabledIndeterminateColor: Color = defaultDisabledIndeterminateColor(checkedColor)
    ): Color {
        val target = if (enabled) {
            when (state) {
                ToggleableState.On, ToggleableState.Indeterminate -> checkedColor
                ToggleableState.Off -> uncheckedColor
            }
        } else {
            when (state) {
                ToggleableState.On -> disabledCheckedColor
                ToggleableState.Indeterminate -> disabledIndeterminateColor
                ToggleableState.Off -> disabledUncheckedColor
            }
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
            animate(target, tween(durationMillis = duration))
        } else {
            target
        }
    }

    /**
     * Represents the default color used for the border of the box in a [Checkbox] or
     * [TriStateCheckbox]s as it animates between states.
     *
     * @param state the [ToggleableState] of the checkbox
     * @param enabled whether the checkbox is enabled
     * @param checkedColor the color to use for the border of the box when the Checkbox is
     * [ToggleableState.On].
     * @param uncheckedColor the color to use for the border of the box when the Checkbox is
     * [ToggleableState.Off].
     * @param disabledColor the color to use for the border of the box when the Checkbox is
     * [ToggleableState.On] or [ToggleableState.Off], and not [enabled].
     * @param disabledIndeterminateColor the color to use for the border of the box when the
     * Checkbox is [ToggleableState.Indeterminate] and not [enabled].
     * @return the [Color] representing the border color of the box
     */
    @Composable
    fun animateDefaultBorderColor(
        state: ToggleableState,
        enabled: Boolean,
        checkedColor: Color = MaterialTheme.colors.secondary,
        uncheckedColor: Color = defaultUncheckedColor,
        disabledColor: Color = defaultDisabledColor,
        disabledIndeterminateColor: Color = defaultDisabledIndeterminateColor(checkedColor)
    ): Color {
        val target = if (enabled) {
            when (state) {
                ToggleableState.On, ToggleableState.Indeterminate -> checkedColor
                ToggleableState.Off -> uncheckedColor
            }
        } else {
            when (state) {
                ToggleableState.Indeterminate -> disabledIndeterminateColor
                ToggleableState.On, ToggleableState.Off -> disabledColor
            }
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
            animate(target, tween(durationMillis = duration))
        } else {
            target
        }
    }

    /**
     * Default color that will be used for a Checkbox when unchecked
     */
    @Composable
    val defaultUncheckedColor: Color
        get() = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)

    /**
     * Default color that will be used for a Checkbox when disabled
     */
    @Composable
    val defaultDisabledColor: Color
        get() = EmphasisAmbient.current.disabled.applyEmphasis(MaterialTheme.colors.onSurface)

    /**
     * Default color that will be used for [TriStateCheckbox] when disabled and in a
     * [ToggleableState.Indeterminate] state.
     */
    @Composable
    fun defaultDisabledIndeterminateColor(checkedColor: Color): Color {
        return EmphasisAmbient.current.disabled.applyEmphasis(checkedColor)
    }
}

@Composable
private fun CheckboxImpl(
    value: ToggleableState,
    modifier: Modifier,
    checkColor: Color,
    boxColor: Color,
    borderColor: Color
) {
    val state = transition(definition = TransitionDefinition, toState = value)
    val checkCache = remember { CheckDrawingCache() }
    Canvas(modifier.wrapContentSize(Alignment.Center).size(CheckboxSize)) {
        val strokeWidthPx = StrokeWidth.toPx()
        drawBox(
            boxColor = boxColor,
            borderColor = borderColor,
            radius = RadiusSize.toPx(),
            strokeWidth = strokeWidthPx
        )
        drawCheck(
            checkColor = checkColor,
            checkFraction = state[CheckDrawFraction],
            crossCenterGravitation = state[CheckCenterGravitationShiftFraction],
            strokeWidthPx = strokeWidthPx,
            drawingCache = checkCache
        )
    }
}

private fun DrawScope.drawBox(
    boxColor: Color,
    borderColor: Color,
    radius: Float,
    strokeWidth: Float
) {
    val halfStrokeWidth = strokeWidth / 2.0f
    val stroke = Stroke(strokeWidth)
    val checkboxSize = size.width
    drawRoundRect(
        boxColor,
        topLeft = Offset(strokeWidth, strokeWidth),
        size = Size(checkboxSize - strokeWidth * 2, checkboxSize - strokeWidth * 2),
        radius = Radius(radius / 2),
        style = Fill
    )
    drawRoundRect(
        borderColor,
        topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
        size = Size(checkboxSize - strokeWidth, checkboxSize - strokeWidth),
        radius = Radius(radius),
        style = stroke
    )
}

private fun DrawScope.drawCheck(
    checkColor: Color,
    checkFraction: Float,
    crossCenterGravitation: Float,
    strokeWidthPx: Float,
    drawingCache: CheckDrawingCache
) {
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Square)
    val width = size.width
    val checkCrossX = 0.4f
    val checkCrossY = 0.7f
    val leftX = 0.2f
    val leftY = 0.5f
    val rightX = 0.8f
    val rightY = 0.3f

    val gravitatedCrossX = lerp(checkCrossX, 0.5f, crossCenterGravitation)
    val gravitatedCrossY = lerp(checkCrossY, 0.5f, crossCenterGravitation)
    // gravitate only Y for end to achieve center line
    val gravitatedLeftY = lerp(leftY, 0.5f, crossCenterGravitation)
    val gravitatedRightY = lerp(rightY, 0.5f, crossCenterGravitation)

    with(drawingCache) {
        checkPath.reset()
        checkPath.moveTo(width * leftX, width * gravitatedLeftY)
        checkPath.lineTo(width * gravitatedCrossX, width * gravitatedCrossY)
        checkPath.lineTo(width * rightX, width * gravitatedRightY)
        // TODO: replace with proper declarative non-android alternative when ready (b/158188351)
        pathMeasure.setPath(checkPath, false)
        pathToDraw.reset()
        pathMeasure.getSegment(
            0f, pathMeasure.length * checkFraction, pathToDraw, true
        )
    }
    drawPath(drawingCache.pathToDraw, checkColor, style = stroke)
}

@Immutable
private class CheckDrawingCache(
    val checkPath: Path = Path(),
    val pathMeasure: PathMeasure = PathMeasure(),
    val pathToDraw: Path = Path()
)

// all float props are fraction now [0f .. 1f] as it seems convenient
private val CheckDrawFraction = FloatPropKey()
private val CheckCenterGravitationShiftFraction = FloatPropKey()

private const val BoxInDuration = 50
private const val BoxOutDuration = 100
private const val CheckAnimationDuration = 100

private val TransitionDefinition = transitionDefinition<ToggleableState> {
    state(ToggleableState.On) {
        this[CheckDrawFraction] = 1f
        this[CheckCenterGravitationShiftFraction] = 0f
    }
    state(ToggleableState.Off) {
        this[CheckDrawFraction] = 0f
        this[CheckCenterGravitationShiftFraction] = 0f
    }
    state(ToggleableState.Indeterminate) {
        this[CheckDrawFraction] = 1f
        this[CheckCenterGravitationShiftFraction] = 1f
    }
    transition(
        ToggleableState.Off to ToggleableState.On,
        ToggleableState.Off to ToggleableState.Indeterminate
    ) {
        boxTransitionToChecked()
    }
    transition(
        ToggleableState.On to ToggleableState.Indeterminate,
        ToggleableState.Indeterminate to ToggleableState.On
    ) {
        CheckCenterGravitationShiftFraction using tween(
            durationMillis = CheckAnimationDuration
        )
    }
    transition(
        ToggleableState.Indeterminate to ToggleableState.Off,
        ToggleableState.On to ToggleableState.Off
    ) {
        checkboxTransitionToUnchecked()
    }
}

private fun TransitionSpec<ToggleableState>.boxTransitionToChecked() {
    CheckCenterGravitationShiftFraction using snap()
    CheckDrawFraction using tween(
        durationMillis = CheckAnimationDuration
    )
}

private fun TransitionSpec<ToggleableState>.checkboxTransitionToUnchecked() {
    // TODO: emulate delayed snap and replace when actual API is available b/158189074
    CheckDrawFraction using keyframes {
        durationMillis = BoxOutDuration
        1f at 0
        1f at BoxOutDuration - 1
        0f at BoxOutDuration
    }
    CheckCenterGravitationShiftFraction using tween(
        durationMillis = 1,
        delayMillis = BoxOutDuration - 1
    )
}

private val CheckboxRippleRadius = 24.dp
private val CheckboxDefaultPadding = 2.dp
private val CheckboxSize = 20.dp
private val StrokeWidth = 2.dp
private val RadiusSize = 2.dp
