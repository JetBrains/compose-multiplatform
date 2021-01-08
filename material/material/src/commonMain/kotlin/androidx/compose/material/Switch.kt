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

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A Switch is a two state toggleable component that provides on/off like options
 *
 * @sample androidx.compose.material.samples.SwitchSample
 *
 * @param checked whether or not this components is checked
 * @param onCheckedChange callback to be invoked when Switch is being clicked,
 * therefore the change of checked state is requested.
 * @param modifier Modifier to be applied to the switch layout
 * @param enabled whether or not components is enabled and can be clicked to request state change
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this Switch. You can create and pass in your own remembered
 * [InteractionState] if you want to read the [InteractionState] and customize the appearance /
 * behavior of this Switch in different [Interaction]s.
 * @param colors [SwitchColors] that will be used to determine the color of the thumb and track
 * in different states. See [SwitchDefaults.colors].
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() },
    colors: SwitchColors = SwitchDefaults.colors()
) {
    val minBound = 0f
    val maxBound = with(AmbientDensity.current) { ThumbPathLength.toPx() }
    val swipeableState = rememberSwipeableStateFor(checked, onCheckedChange, AnimationSpec)
    val isRtl = AmbientLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionState = interactionState,
                indication = null
            )
            .swipeable(
                state = swipeableState,
                anchors = mapOf(minBound to false, maxBound to true),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                enabled = enabled,
                reverseDirection = isRtl,
                interactionState = interactionState,
                resistance = null
            )
            .wrapContentSize(Alignment.Center)
            .padding(DefaultSwitchPadding)
            .size(SwitchWidth, SwitchHeight)
    ) {
        SwitchImpl(
            checked = checked,
            enabled = enabled,
            colors = colors,
            thumbValue = swipeableState.offset,
            interactionState = interactionState
        )
    }
}

/**
 * Represents the colors used by a [Switch] in different states
 *
 * See [SwitchDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@ExperimentalMaterialApi
@Stable
interface SwitchColors {

    /**
     * Represents the color used for the switch's thumb, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    fun thumbColor(enabled: Boolean, checked: Boolean): Color

    /**
     * Represents the color used for the switch's track, depending on [enabled] and [checked].
     *
     * @param enabled whether the [Switch] is enabled or not
     * @param checked whether the [Switch] is checked or not
     */
    fun trackColor(enabled: Boolean, checked: Boolean): Color
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BoxScope.SwitchImpl(
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    thumbValue: State<Float>,
    interactionState: InteractionState
) {
    val hasInteraction =
        Interaction.Pressed in interactionState || Interaction.Dragged in interactionState
    val elevation = if (hasInteraction) {
        ThumbPressedElevation
    } else {
        ThumbDefaultElevation
    }
    Canvas(Modifier.align(Alignment.Center).fillMaxSize()) {
        val trackColor = colors.trackColor(enabled, checked)
        drawTrack(trackColor, TrackWidth.toPx(), TrackStrokeWidth.toPx())
    }
    val thumbColor = colors.thumbColor(enabled, checked)
    Surface(
        shape = CircleShape,
        color = thumbColor,
        elevation = elevation,
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset(thumbValue.value.roundToInt(), 0) }
            .indication(
                interactionState = interactionState,
                indication = rememberRipple(bounded = false, radius = ThumbRippleRadius)
            )
            .size(ThumbDiameter),
        content = emptyContent()
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

internal val TrackWidth = 34.dp
internal val TrackStrokeWidth = 14.dp
internal val ThumbDiameter = 20.dp

private val ThumbRippleRadius = 24.dp

private val DefaultSwitchPadding = 2.dp
private val SwitchWidth = TrackWidth
private val SwitchHeight = ThumbDiameter
private val ThumbPathLength = TrackWidth - ThumbDiameter

private val AnimationSpec = TweenSpec<Float>(durationMillis = 100)

private val ThumbDefaultElevation = 1.dp
private val ThumbPressedElevation = 6.dp

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
     * @param checkedTrackAlpha the alpha applied to [checkedTrackColor] and
     * [disabledCheckedTrackColor]
     * @param uncheckedThumbColor the color used for the thumb when enabled and unchecked
     * @param uncheckedTrackColor the color used for the track when enabled and unchecked
     * @param uncheckedTrackAlpha the alpha applied to [uncheckedTrackColor] and
     * [disabledUncheckedTrackColor]
     * @param disabledCheckedThumbColor the color used for the thumb when disabled and checked
     * @param disabledCheckedTrackColor the color used for the track when disabled and checked
     * @param disabledUncheckedThumbColor the color used for the thumb when disabled and unchecked
     * @param disabledUncheckedTrackColor the color used for the track when disabled and unchecked
     */
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun colors(
        checkedThumbColor: Color = MaterialTheme.colors.secondaryVariant,
        checkedTrackColor: Color = checkedThumbColor,
        checkedTrackAlpha: Float = 0.54f,
        uncheckedThumbColor: Color = MaterialTheme.colors.surface,
        uncheckedTrackColor: Color = MaterialTheme.colors.onSurface,
        uncheckedTrackAlpha: Float = 0.38f,
        disabledCheckedThumbColor: Color = checkedThumbColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        disabledCheckedTrackColor: Color = checkedTrackColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        disabledUncheckedThumbColor: Color = uncheckedThumbColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        disabledUncheckedTrackColor: Color = uncheckedTrackColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface)
    ): SwitchColors = DefaultSwitchColors(
        checkedThumbColor = checkedThumbColor,
        checkedTrackColor = checkedTrackColor.copy(alpha = checkedTrackAlpha),
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedTrackColor = uncheckedTrackColor.copy(alpha = uncheckedTrackAlpha),
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor.copy(alpha = checkedTrackAlpha),
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor.copy(alpha = uncheckedTrackAlpha)
    )
}

/**
 * Default [SwitchColors] implementation.
 */
@OptIn(ExperimentalMaterialApi::class)
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
    override fun thumbColor(enabled: Boolean, checked: Boolean): Color {
        return if (enabled) {
            if (checked) checkedThumbColor else uncheckedThumbColor
        } else {
            if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor
        }
    }

    override fun trackColor(enabled: Boolean, checked: Boolean): Color {
        return if (enabled) {
            if (checked) checkedTrackColor else uncheckedTrackColor
        } else {
            if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
        }
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
