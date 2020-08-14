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
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.StackScope
import androidx.compose.foundation.layout.offsetPx
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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
 * @param color main color of the track and trumb when the Switch is checked
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colors.secondaryVariant
) {
    val minBound = 0f
    val maxBound = with(DensityAmbient.current) { ThumbPathLength.toPx() }
    val swipeableState = rememberSwipeableStateFor(checked, onCheckedChange, AnimationSpec)
    val interactionState = remember { InteractionState() }
    val isRtl = LayoutDirectionAmbient.current == LayoutDirection.Rtl
    Stack(
        modifier
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
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
                resistanceFactorAtMin = 0f,
                resistanceFactorAtMax = 0f
            )
            .padding(DefaultSwitchPadding)
    ) {
        SwitchImpl(
            checked = checked,
            enabled = enabled,
            checkedColor = color,
            thumbValue = swipeableState.offset,
            interactionState = interactionState
        )
    }
}

@Composable
private fun StackScope.SwitchImpl(
    checked: Boolean,
    enabled: Boolean,
    checkedColor: Color,
    thumbValue: State<Float>,
    interactionState: InteractionState
) {
    val hasInteraction =
        Interaction.Pressed in interactionState || Interaction.Dragged in interactionState
    val elevation =
        if (hasInteraction) {
            SwitchDefaults.ThumbPressedElevation
        } else {
            SwitchDefaults.ThumbDefaultElevation
        }
    val trackColor = SwitchDefaults.resolveTrackColor(checked, enabled, checkedColor)
    val thumbColor = SwitchDefaults.resolveThumbColor(checked, enabled, checkedColor)
    Canvas(Modifier.gravity(Alignment.Center).preferredSize(SwitchWidth, SwitchHeight)) {
        drawTrack(trackColor, TrackWidth.toPx(), TrackStrokeWidth.toPx())
    }
    Surface(
        shape = CircleShape,
        color = thumbColor,
        elevation = elevation,
        modifier = Modifier
            .gravity(Alignment.CenterStart)
            .offsetPx(x = thumbValue)
            .indication(
                interactionState = interactionState,
                indication = RippleIndication(radius = ThumbRippleRadius, bounded = false)
            )
            .preferredSize(ThumbDiameter)
    ) {}
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

internal object SwitchDefaults {

    const val CheckedTrackOpacity = 0.54f
    const val UncheckedTrackOpacity = 0.38f

    val ThumbDefaultElevation = 1.dp
    val ThumbPressedElevation = 6.dp

    @Composable
    private val uncheckedTrackColor
        get() = MaterialTheme.colors.onSurface

    @Composable
    private fun makeDisabledCheckedTrackColor(checkedColor: Color) = EmphasisAmbient.current
        .disabled
        .applyEmphasis(checkedColor)
        .compositeOver(MaterialTheme.colors.surface)

    @Composable
    private val disabledUncheckedTrackColor
        get() = EmphasisAmbient.current.disabled.applyEmphasis(MaterialTheme.colors.onSurface)
            .compositeOver(MaterialTheme.colors.surface)

    @Composable
    private val uncheckedThumbColor
        get() = MaterialTheme.colors.surface

    @Composable
    private fun makeDisabledCheckedThumbColor(checkedColor: Color) = EmphasisAmbient.current
        .disabled
        .applyEmphasis(checkedColor)
        .compositeOver(MaterialTheme.colors.surface)

    @Composable
    private val disabledUncheckedThumbColor
        get() = EmphasisAmbient.current.disabled.applyEmphasis(MaterialTheme.colors.surface)
            .compositeOver(MaterialTheme.colors.surface)

    @Composable
    internal fun resolveTrackColor(checked: Boolean, enabled: Boolean, checkedColor: Color): Color {
        return if (checked) {
            val color = if (enabled) checkedColor else makeDisabledCheckedTrackColor(checkedColor)
            color.copy(alpha = CheckedTrackOpacity)
        } else {
            val color = if (enabled) uncheckedTrackColor else disabledUncheckedTrackColor
            color.copy(alpha = UncheckedTrackOpacity)
        }
    }

    @Composable
    internal fun resolveThumbColor(checked: Boolean, enabled: Boolean, checkedColor: Color): Color {
        return if (checked) {
            if (enabled) checkedColor else makeDisabledCheckedThumbColor(checkedColor)
        } else {
            if (enabled) uncheckedThumbColor else disabledUncheckedThumbColor
        }
    }
}