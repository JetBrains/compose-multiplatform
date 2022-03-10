/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.SwitchTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Material Design switch toggles the state of a single item on or off.
 *
 * @sample androidx.compose.material3.samples.SwitchSample
 *
 * @param checked whether or not this component is checked
 * @param onCheckedChange callback to be invoked when Switch is being clicked,
 * therefore the change of checked state is requested.  If null, then this is passive
 * and relies entirely on a higher-level component to control the "checked" state.
 * @param modifier Modifier to be applied to the switch layout
 * @param enabled whether the component is enabled or grayed out
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Switch. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Switch in different [Interaction]s.
 * @param colors [SwitchColors] that will be used to determine the color of the thumb and track
 * in different states. See [SwitchDefaults.colors].
 */
// TODO: b/223858692 add m.io documentation
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = SwitchDefaults.colors()
) {
    val minBound = 0f
    val maxBound = with(LocalDensity.current) { ThumbPathLength.toPx() }
    val valueToOffset = remember<(Boolean) -> Float>(minBound, maxBound) {
        { value -> if (value) maxBound else minBound }
    }

    val offset = remember { Animatable(valueToOffset(checked)) }
    val scope = rememberCoroutineScope()
    // TODO: Add Swipeable modifier b/223797571
    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                onValueChange = { value: Boolean ->
                    scope.launch {
                        offset.animateTo(valueToOffset(value), AnimationSpec)
                    }
                    onCheckedChange(value)
                },
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null
            )
        } else {
            Modifier
        }

    Box(
        modifier
            .then(
                if (onCheckedChange != null) Modifier.minimumTouchTargetSize() else Modifier
            )
            .then(toggleableModifier)
            .wrapContentSize(Alignment.Center)
            .padding(DefaultSwitchPadding)
            .requiredSize(SwitchWidth, SwitchHeight)
    ) {
        SwitchImpl(
            checked = checked,
            enabled = enabled,
            colors = colors,
            thumbValue = offset.asState(),
            interactionSource = interactionSource,
            handleShape = SwitchTokens.HandleShape.toShape()
        )
    }
}

/**
 * Represents the colors used by a [Switch] in different states
 *
 * See [SwitchDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@Stable
interface SwitchColors {

    /**
     * Represents the color used for the switch's thumb, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Composable
    fun thumbColor(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the color used for the switch's track, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    @Composable
    fun trackColor(enabled: Boolean, checked: Boolean): State<Color>
}

@Composable
private fun BoxScope.SwitchImpl(
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    thumbValue: State<Float>,
    interactionSource: InteractionSource,
    handleShape: Shape,
) {
    val elevation = if (enabled) ThumbDefaultElevation else ThumbDisabledElevation
    val trackColor by colors.trackColor(enabled, checked)
    Canvas(Modifier.align(Alignment.Center).fillMaxSize()) {
        drawTrack(trackColor, TrackWidth.toPx(), TrackStrokeWidth.toPx())
    }
    val thumbColor by colors.thumbColor(enabled, checked)
    val resolvedThumbColor = thumbColor
    Spacer(
        Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset(thumbValue.value.roundToInt(), 0) }
            .indication(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false, radius = ThumbRippleRadius)
            )
            .requiredSize(ThumbDiameter)
            .shadow(elevation, handleShape, clip = false)
            .background(resolvedThumbColor, handleShape)
    )
}

private fun DrawScope.drawTrack(trackColor: Color, trackWidth: Float, strokeWidth: Float) {
    val strokeRadius = strokeWidth / 2
    drawLine(
        trackColor,
        Offset(strokeRadius, center.y),
        Offset(trackWidth - strokeRadius, center.y),
        strokeWidth,
        StrokeCap.Round
    )
}

internal val TrackWidth = SwitchTokens.TrackWidth
internal val TrackStrokeWidth = SwitchTokens.TrackHeight
internal val ThumbDiameter = SwitchTokens.HandleWidth

private val ThumbRippleRadius = SwitchTokens.StateLayerSize / 2

private val DefaultSwitchPadding = 2.dp
private val SwitchWidth = TrackWidth
private val SwitchHeight = ThumbDiameter
private val ThumbPathLength = TrackWidth - ThumbDiameter

private val AnimationSpec = TweenSpec<Float>(durationMillis = 100)
private val ThumbDefaultElevation = SwitchTokens.HandleElevation
private val ThumbDisabledElevation = SwitchTokens.DisabledHandleElevation

/**
 * Contains the default values used by [Switch]
 */
object SwitchDefaults {
    /**
     * Creates a [SwitchColors] that represents the different colors used in a [Switch] in
     * different states.
     *
     * @param checkedThumbColor the color used for the thumb when enabled and checked
     * @param checkedTrackColor the color used for the track when enabled and checked
     * [disabledCheckedTrackColor]
     * @param uncheckedThumbColor the color used for the thumb when enabled and unchecked
     * @param uncheckedTrackColor the color used for the track when enabled and unchecked
     * [disabledUncheckedTrackColor]
     * @param disabledCheckedThumbColor the color used for the thumb when disabled and checked
     * @param disabledCheckedTrackColor the color used for the track when disabled and checked
     * @param disabledUncheckedThumbColor the color used for the thumb when disabled and unchecked
     * @param disabledUncheckedTrackColor the color used for the track when disabled and unchecked
     */
    @Composable
    fun colors(
        checkedThumbColor: Color = SwitchTokens.SelectedHandleColor.toColor(),
        checkedTrackColor: Color = SwitchTokens.SelectedTrackColor.toColor(),
        uncheckedThumbColor: Color = SwitchTokens.UnselectedHandleColor.toColor(),
        uncheckedTrackColor: Color = SwitchTokens.UnselectedTrackColor.toColor(),
        disabledCheckedThumbColor: Color = checkedThumbColor
            .copy(alpha = SwitchTokens.DisabledHandleOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledCheckedTrackColor: Color = checkedTrackColor
            .copy(alpha = SwitchTokens.DisabledTrackOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedThumbColor: Color = uncheckedThumbColor
            .copy(alpha = SwitchTokens.DisabledHandleOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface),
        disabledUncheckedTrackColor: Color = uncheckedTrackColor
            .copy(alpha = SwitchTokens.DisabledTrackOpacity)
            .compositeOver(MaterialTheme.colorScheme.surface)
    ): SwitchColors = DefaultSwitchColors(
        checkedThumbColor = checkedThumbColor,
        checkedTrackColor = checkedTrackColor,
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedTrackColor = uncheckedTrackColor,
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor,
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor
    )
}

/**
 * Default [SwitchColors] implementation.
 */
@Immutable
private class DefaultSwitchColors(
    private val checkedThumbColor: Color,
    private val checkedTrackColor: Color,
    private val uncheckedThumbColor: Color,
    private val uncheckedTrackColor: Color,
    private val disabledCheckedThumbColor: Color,
    private val disabledCheckedTrackColor: Color,
    private val disabledUncheckedThumbColor: Color,
    private val disabledUncheckedTrackColor: Color
) : SwitchColors {
    @Composable
    override fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedThumbColor else uncheckedThumbColor
            } else {
                if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor
            }
        )
    }

    @Composable
    override fun trackColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedTrackColor else uncheckedTrackColor
            } else {
                if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSwitchColors

        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackColor != other.disabledUncheckedTrackColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedThumbColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackColor.hashCode()
        return result
    }
}
