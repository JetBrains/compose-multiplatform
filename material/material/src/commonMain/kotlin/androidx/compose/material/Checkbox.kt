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
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.TransitionSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
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
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this Checkbox. You can create and pass in your own remembered
 * [InteractionState] if you want to read the [InteractionState] and customize the appearance /
 * behavior of this Checkbox in different [Interaction]s.
 * @param colors [CheckboxColors] that will be used to determine the color of the checkmark / box
 * / border in different states. See [CheckboxDefaults.colors].
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() },
    colors: CheckboxColors = CheckboxDefaults.colors()
) {
    TriStateCheckbox(
        state = ToggleableState(checked),
        onClick = { onCheckedChange(!checked) },
        interactionState = interactionState,
        enabled = enabled,
        colors = colors,
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
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this TriStateCheckbox. You can create and pass in your own remembered
 * [InteractionState] if you want to read the [InteractionState] and customize the appearance /
 * behavior of this TriStateCheckbox in different [Interaction]s.
 * @param colors [CheckboxColors] that will be used to determine the color of the checkmark / box
 * / border in different states. See [CheckboxDefaults.colors].
 */
@Composable
fun TriStateCheckbox(
    state: ToggleableState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() },
    colors: CheckboxColors = CheckboxDefaults.colors()
) {
    CheckboxImpl(
        enabled = enabled,
        value = state,
        modifier = modifier
            .triStateToggleable(
                state = state,
                onClick = onClick,
                enabled = enabled,
                role = Role.Checkbox,
                interactionState = interactionState,
                indication = rememberRipple(
                    bounded = false,
                    radius = CheckboxRippleRadius
                )
            )
            .padding(CheckboxDefaultPadding),
        colors = colors
    )
}

/**
 * Represents the colors used by the three different sections (checkmark, box, and border) of a
 * [Checkbox] or [TriStateCheckbox] in different states.
 *
 * See [CheckboxDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@Stable
interface CheckboxColors {

    /**
     * Represents the color used for the checkmark inside the checkbox, depending on [state].
     *
     * @param state the [ToggleableState] of the checkbox
     */
    @Composable
    fun checkmarkColor(state: ToggleableState): State<Color>

    /**
     * Represents the color used for the box (background) of the checkbox, depending on [enabled]
     * and [state].
     *
     * @param enabled whether the checkbox is enabled or not
     * @param state the [ToggleableState] of the checkbox
     */
    @Composable
    fun boxColor(enabled: Boolean, state: ToggleableState): State<Color>

    /**
     * Represents the color used for the border of the checkbox, depending on [enabled] and [state].
     *
     * @param enabled whether the checkbox is enabled or not
     * @param state the [ToggleableState] of the checkbox
     */
    @Composable
    fun borderColor(enabled: Boolean, state: ToggleableState): State<Color>
}

/**
 * Defaults used in [Checkbox] and [TriStateCheckbox].
 */
object CheckboxDefaults {
    /**
     * Creates a [CheckboxColors] that will animate between the provided colors according to the
     * Material specification.
     *
     * @param checkedColor the color that will be used for the border and box when checked
     * @param uncheckedColor color that will be used for the border when unchecked
     * @param checkmarkColor color that will be used for the checkmark when checked
     * @param disabledColor color that will be used for the box and border when disabled
     * @param disabledIndeterminateColor color that will be used for the box and
     * border in a [TriStateCheckbox] when disabled AND in an [ToggleableState.Indeterminate] state.
     */
    @Composable
    fun colors(
        checkedColor: Color = MaterialTheme.colors.secondary,
        uncheckedColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        checkmarkColor: Color = MaterialTheme.colors.surface,
        disabledColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledIndeterminateColor: Color = checkedColor.copy(alpha = ContentAlpha.disabled)
    ): CheckboxColors {
        return remember(
            checkedColor,
            uncheckedColor,
            checkmarkColor,
            disabledColor,
            disabledIndeterminateColor,
        ) {
            DefaultCheckboxColors(
                checkedBorderColor = checkedColor,
                checkedBoxColor = checkedColor,
                checkedCheckmarkColor = checkmarkColor,
                uncheckedCheckmarkColor = checkmarkColor.copy(alpha = 0f),
                uncheckedBoxColor = checkedColor.copy(alpha = 0f),
                disabledCheckedBoxColor = disabledColor,
                disabledUncheckedBoxColor = disabledColor.copy(alpha = 0f),
                disabledIndeterminateBoxColor = disabledIndeterminateColor,
                uncheckedBorderColor = uncheckedColor,
                disabledBorderColor = disabledColor,
                disabledIndeterminateBorderColor = disabledIndeterminateColor,
            )
        }
    }
}

@Composable
private fun CheckboxImpl(
    enabled: Boolean,
    value: ToggleableState,
    modifier: Modifier,
    colors: CheckboxColors
) {
    val state = transition(definition = TransitionDefinition, toState = value)
    val checkCache = remember { CheckDrawingCache() }
    val checkColor by colors.checkmarkColor(value)
    val boxColor by colors.boxColor(enabled, value)
    val borderColor by colors.borderColor(enabled, value)
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
        cornerRadius = CornerRadius(radius / 2),
        style = Fill
    )
    drawRoundRect(
        borderColor,
        topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
        size = Size(checkboxSize - strokeWidth, checkboxSize - strokeWidth),
        cornerRadius = CornerRadius(radius),
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

/**
 * Default [CheckboxColors] implementation.
 */
@Stable
private class DefaultCheckboxColors(
    private val checkedCheckmarkColor: Color,
    private val uncheckedCheckmarkColor: Color,
    private val checkedBoxColor: Color,
    private val uncheckedBoxColor: Color,
    private val disabledCheckedBoxColor: Color,
    private val disabledUncheckedBoxColor: Color,
    private val disabledIndeterminateBoxColor: Color,
    private val checkedBorderColor: Color,
    private val uncheckedBorderColor: Color,
    private val disabledBorderColor: Color,
    private val disabledIndeterminateBorderColor: Color
) : CheckboxColors {
    @Composable
    override fun checkmarkColor(state: ToggleableState): State<Color> {
        val target = if (state == ToggleableState.Off) {
            uncheckedCheckmarkColor
        } else {
            checkedCheckmarkColor
        }

        val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
        return animateColorAsState(target, tween(durationMillis = duration))
    }

    @Composable
    override fun boxColor(enabled: Boolean, state: ToggleableState): State<Color> {
        val target = if (enabled) {
            when (state) {
                ToggleableState.On, ToggleableState.Indeterminate -> checkedBoxColor
                ToggleableState.Off -> uncheckedBoxColor
            }
        } else {
            when (state) {
                ToggleableState.On -> disabledCheckedBoxColor
                ToggleableState.Indeterminate -> disabledIndeterminateBoxColor
                ToggleableState.Off -> disabledUncheckedBoxColor
            }
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
            animateColorAsState(target, tween(durationMillis = duration))
        } else {
            rememberUpdatedState(target)
        }
    }

    @Composable
    override fun borderColor(enabled: Boolean, state: ToggleableState): State<Color> {
        val target = if (enabled) {
            when (state) {
                ToggleableState.On, ToggleableState.Indeterminate -> checkedBorderColor
                ToggleableState.Off -> uncheckedBorderColor
            }
        } else {
            when (state) {
                ToggleableState.Indeterminate -> disabledIndeterminateBorderColor
                ToggleableState.On, ToggleableState.Off -> disabledBorderColor
            }
        }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            val duration = if (state == ToggleableState.Off) BoxOutDuration else BoxInDuration
            animateColorAsState(target, tween(durationMillis = duration))
        } else {
            rememberUpdatedState(target)
        }
    }
}

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
