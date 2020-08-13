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

import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.TransitionSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.transition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.selection.ToggleableState
import androidx.compose.foundation.selection.triStateToggleable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.RippleIndication
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * A component that represents two states (checked / unchecked).
 *
 * @sample androidx.compose.material.samples.CheckboxSample
 *
 * @see [TriStateCheckbox] if you require support for an indeterminate state.
 *
 * @param checked whether Checkbox is checked or unchecked
 * @param onCheckedChange callback to be invoked when checkbox is being clicked,
 * therefore the change of checked state in requested.
 * @param modifier Modifier to be applied to the layout of the checkbox
 * @param enabled enabled whether or not this [Checkbox] will handle input events and appear
 * enabled for semantics purposes
 * @param checkedColor color of the box when it is checked
 * @param uncheckedColor color of the box border when it is unchecked
 * @param disabledColor color for the checkbox to appear when disabled
 * @param checkMarkColor color of the check mark of the [Checkbox]
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = MaterialTheme.colors.secondary,
    uncheckedColor: Color = MaterialTheme.colors.onSurface,
    disabledColor: Color = MaterialTheme.colors.onSurface,
    checkMarkColor: Color = MaterialTheme.colors.surface
) {
    TriStateCheckbox(
        state = ToggleableState(checked),
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        checkedColor = checkedColor,
        uncheckedColor = uncheckedColor,
        checkMarkColor = checkMarkColor,
        disabledColor = disabledColor,
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
 * @param enabled enabled whether or not this [TriStateCheckbox] will handle input events and
 * appear enabled for semantics purposes
 * @param checkedColor color of the box when it is in [ToggleableState.On] or [ToggleableState
 * .Indeterminate] states
 * @param uncheckedColor color of the box border when it is in [ToggleableState.Off] state
 * @param disabledColor color for the checkbox to appear when disabled
 * @param checkMarkColor color of the check mark of the [TriStateCheckbox]
 */
@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedColor: Color = MaterialTheme.colors.secondary,
    uncheckedColor: Color = MaterialTheme.colors.onSurface,
    disabledColor: Color = MaterialTheme.colors.onSurface,
    checkMarkColor: Color = MaterialTheme.colors.surface
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
        enabled = enabled,
        activeColor = checkedColor,
        inactiveColor = uncheckedColor,
        checkColor = checkMarkColor,
        disabledColor = disabledColor
    )
}

@Composable
private fun CheckboxImpl(
    value: ToggleableState,
    modifier: Modifier,
    enabled: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    checkColor: Color,
    disabledColor: Color
) {
    val unselectedColor = inactiveColor.copy(alpha = UncheckedBoxOpacity)
    val definition = remember(activeColor, unselectedColor) {
        generateTransitionDefinition(activeColor, unselectedColor)
    }
    val disabledEmphasis = EmphasisAmbient.current.disabled
    val indeterminateDisabledColor = disabledEmphasis.applyEmphasis(activeColor)
    val disabledEmphasisedColor = disabledEmphasis.applyEmphasis(disabledColor)
    val state = transition(definition = definition, toState = value)
    val checkCache = remember { CheckDrawingCache() }
    Canvas(modifier.wrapContentSize(Alignment.Center).size(CheckboxSize)) {
        val boxColor =
            if (enabled) {
                activeColor.copy(alpha = state[BoxOpacityFraction])
            } else if (value == ToggleableState.Indeterminate) {
                indeterminateDisabledColor
            } else if (value == ToggleableState.Off) {
                Color.Transparent
            } else {
                disabledEmphasisedColor
            }
        val borderColor =
            if (enabled) {
                state[BoxBorderColor]
            } else if (value == ToggleableState.Indeterminate) {
                indeterminateDisabledColor
            } else {
                disabledEmphasisedColor
            }
        val strokeWidthPx = StrokeWidth.toPx()
        drawBox(
            boxColor = boxColor,
            borderColor = borderColor,
            radius = RadiusSize.toPx(),
            strokeWidth = strokeWidthPx
        )
        drawCheck(
            checkColor = checkColor.copy(alpha = state[CheckOpacityFraction]),
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
private val BoxOpacityFraction = FloatPropKey()
private val CheckOpacityFraction = FloatPropKey()
private val CheckCenterGravitationShiftFraction = FloatPropKey()
private val BoxBorderColor = ColorPropKey()

private val BoxInDuration = 50
private val BoxOutDuration = 100
private val CheckAnimationDuration = 100

private fun generateTransitionDefinition(color: Color, unselectedColor: Color) =
    transitionDefinition<ToggleableState> {
        state(ToggleableState.On) {
            this[CheckDrawFraction] = 1f
            this[BoxOpacityFraction] = 1f
            this[CheckOpacityFraction] = 1f
            this[CheckCenterGravitationShiftFraction] = 0f
            this[BoxBorderColor] = color
        }
        state(ToggleableState.Off) {
            this[CheckDrawFraction] = 0f
            this[BoxOpacityFraction] = 0f
            this[CheckOpacityFraction] = 0f
            this[CheckCenterGravitationShiftFraction] = 0f
            this[BoxBorderColor] = unselectedColor
        }
        state(ToggleableState.Indeterminate) {
            this[CheckDrawFraction] = 1f
            this[BoxOpacityFraction] = 1f
            this[CheckOpacityFraction] = 1f
            this[CheckCenterGravitationShiftFraction] = 1f
            this[BoxBorderColor] = color
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
    BoxBorderColor using tween(
        durationMillis = BoxInDuration
    )
    BoxOpacityFraction using tween(
        durationMillis = BoxInDuration
    )
    CheckOpacityFraction using tween(
        durationMillis = BoxInDuration
    )
    CheckDrawFraction using tween(
        durationMillis = CheckAnimationDuration
    )
}

private fun TransitionSpec<ToggleableState>.checkboxTransitionToUnchecked() {
    BoxBorderColor using tween(
        durationMillis = BoxOutDuration
    )
    BoxOpacityFraction using tween(
        durationMillis = BoxOutDuration
    )
    CheckOpacityFraction using tween(
        durationMillis = BoxOutDuration
    )
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

private val UncheckedBoxOpacity = 0.6f
