/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.material

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimatedFloat
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.TargetAnimation
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.fling
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.defaultFlingConfig
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeightIn
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.preferredWidthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.SliderDefaults.InactiveTrackColorAlpha
import androidx.compose.material.SliderDefaults.TickColorAlpha
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.abs

/**
 * Sliders allow users to make selections from a range of values.
 *
 * Sliders reflect a range of values along a bar, from which users may select a single value.
 * They are ideal for adjusting settings such as volume, brightness, or applying image filters.
 *
 * Use continuous sliders allow users to make meaningful selections that don’t
 * require a specific value:
 *
 * @sample androidx.compose.material.samples.SliderSample
 *
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * @sample androidx.compose.material.samples.StepsSliderSample
 *
 * @param value current value of the Slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange lambda in which value should be updated
 * @param modifier modifiers for the Slider layout
 * @param valueRange range of values that Slider value can take. Passed [value] will be coerced to
 * this range
 * @param steps if greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, slider will behave as a continuous slider and allow
 * to choose any value from the range specified. Must not be negative.
 * @param onValueChangeEnd lambda to be invoked when value change has ended. This callback
 * shouldn't be used to update the slider value (use [onValueChange] for that), but rather to
 * know when the user has completed selecting a new value by ending a drag or a click.
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this Slider. You can create and pass in your own remembered
 * [InteractionState] if you want to read the [InteractionState] and customize the appearance /
 * behavior of this Slider in different [Interaction]s.
 * @param thumbColor color of thumb of the slider
 * @param activeTrackColor color of the track in the part that is "active", meaning that the
 * thumb is ahead of it
 * @param inactiveTrackColor color of the track in the part that is "inactive", meaning that the
 * thumb is before it
 * @param activeTickColor colors to be used to draw tick marks on the active track, if [steps]
 * is specified
 * @param inactiveTickColor colors to be used to draw tick marks on the inactive track, if
 * [steps] is specified
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeEnd: () -> Unit = {},
    interactionState: InteractionState = remember { InteractionState() },
    thumbColor: Color = MaterialTheme.colors.primary,
    activeTrackColor: Color = MaterialTheme.colors.primary,
    inactiveTrackColor: Color = activeTrackColor.copy(alpha = InactiveTrackColorAlpha),
    activeTickColor: Color = MaterialTheme.colors.onPrimary.copy(alpha = TickColorAlpha),
    inactiveTickColor: Color = activeTrackColor.copy(alpha = TickColorAlpha)
) {
    val clock = AmbientAnimationClock.current.asDisposableClock()
    val position = remember(valueRange, steps) {
        SliderPosition(value, valueRange, steps, clock, onValueChange)
    }
    position.onValueChange = onValueChange
    position.scaledValue = value
    BoxWithConstraints(
        modifier.sliderSemantics(value, position, onValueChange, valueRange, steps)
    ) {
        val isRtl = AmbientLayoutDirection.current == LayoutDirection.Rtl
        val maxPx = constraints.maxWidth.toFloat()
        val minPx = 0f
        position.setBounds(minPx, maxPx)

        val flingConfig = sliderFlingConfig(position, position.anchorsPx)
        val gestureEndAction = { velocity: Float ->
            if (flingConfig != null) {
                position.holder.fling(
                    velocity,
                    flingConfig.decayAnimation,
                    flingConfig.adjustTarget
                ) { reason, endValue, _ ->
                    if (reason != AnimationEndReason.Interrupted) {
                        position.holder.snapTo(endValue)
                        onValueChangeEnd()
                    }
                }
            } else {
                onValueChangeEnd()
            }
        }

        val press = Modifier.pressIndicatorGestureFilter(
            onStart = { pos ->
                position.holder.snapTo(if (isRtl) maxPx - pos.x else pos.x)
                interactionState.addInteraction(Interaction.Pressed, pos)
            },
            onStop = {
                gestureEndAction(0f)
                interactionState.removeInteraction(Interaction.Pressed)
            },
            onCancel = {
                interactionState.removeInteraction(Interaction.Pressed)
            }
        )

        val drag = Modifier.draggable(
            orientation = Orientation.Horizontal,
            reverseDirection = isRtl,
            interactionState = interactionState,
            onDragStopped = gestureEndAction,
            startDragImmediately = position.holder.isRunning,
            onDrag = { position.holder.snapTo(position.holder.value + it) }
        )
        val coerced = value.coerceIn(position.startValue, position.endValue)
        val fraction = calcFraction(position.startValue, position.endValue, coerced)
        SliderImpl(
            fraction,
            position.tickFractions,
            thumbColor,
            activeTrackColor,
            inactiveTrackColor,
            activeTickColor,
            inactiveTickColor,
            maxPx,
            interactionState,
            modifier = press.then(drag)
        )
    }
}

/**
 * Object to hold defaults used by [Slider]
 */
object SliderDefaults {
    /**
     * Default alpha of the inactive part of the track
     */
    const val InactiveTrackColorAlpha = 0.24f

    /**
     * Default alpha of the ticks that are drawn on top of the track
     */
    const val TickColorAlpha = 0.54f
}

@Composable
private fun SliderImpl(
    positionFraction: Float,
    tickFractions: List<Float>,
    thumbColor: Color,
    trackColor: Color,
    inactiveTrackColor: Color,
    activeTickColor: Color,
    inactiveTickColor: Color,
    width: Float,
    interactionState: InteractionState,
    modifier: Modifier
) {
    val widthDp = with(AmbientDensity.current) {
        width.toDp()
    }
    Box(modifier.then(DefaultSliderConstraints)) {
        val thumbSize = ThumbRadius * 2
        val offset = (widthDp - thumbSize) * positionFraction
        val center = Modifier.align(Alignment.CenterStart)

        val trackStrokeWidth: Float
        val thumbPx: Float
        with(AmbientDensity.current) {
            trackStrokeWidth = TrackHeight.toPx()
            thumbPx = ThumbRadius.toPx()
        }
        Track(
            center.fillMaxSize(),
            trackColor,
            inactiveTrackColor,
            activeTickColor,
            inactiveTickColor,
            positionFraction,
            tickFractions,
            thumbPx,
            trackStrokeWidth
        )
        Box(center.padding(start = offset)) {
            val elevation = if (
                Interaction.Pressed in interactionState || Interaction.Dragged in interactionState
            ) {
                ThumbPressedElevation
            } else {
                ThumbDefaultElevation
            }
            Surface(
                shape = CircleShape,
                color = thumbColor,
                elevation = elevation,
                modifier = Modifier
                    .focusable(interactionState = interactionState)
                    .indication(
                        interactionState = interactionState,
                        indication = rememberRipple(
                            bounded = false,
                            radius = ThumbRippleRadius
                        )
                    )
            ) {
                Spacer(Modifier.preferredSize(thumbSize, thumbSize))
            }
        }
    }
}

@Composable
private fun Track(
    modifier: Modifier,
    color: Color,
    inactiveColor: Color,
    activeTickColor: Color,
    inactiveTickColor: Color,
    positionFraction: Float,
    tickFractions: List<Float>,
    thumbPx: Float,
    trackStrokeWidth: Float
) {
    Canvas(modifier) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(thumbPx, center.y)
        val sliderRight = Offset(size.width - thumbPx, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        drawLine(
            inactiveColor,
            sliderStart,
            sliderEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        val sliderValue = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * positionFraction,
            center.y
        )

        drawLine(color, sliderStart, sliderValue, trackStrokeWidth, StrokeCap.Round)
        tickFractions.groupBy { it > positionFraction }.forEach { (afterFraction, list) ->
            drawPoints(
                list.map {
                    Offset(lerp(sliderStart, sliderEnd, it).x, center.y)
                },
                PointMode.Points,
                if (afterFraction) inactiveTickColor else activeTickColor,
                trackStrokeWidth,
                StrokeCap.Round
            )
        }
    }
}

// Scale x1 from a1..b1 range to a2..b2 range
private fun scale(a1: Float, b1: Float, x1: Float, a2: Float, b2: Float) =
    lerp(a2, b2, calcFraction(a1, b1, x1))

// Calculate the 0..1 fraction that `pos` value represents between `a` and `b`
private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun sliderFlingConfig(
    value: SliderPosition,
    anchors: List<Float>
): FlingConfig? {
    return if (anchors.isEmpty()) {
        null
    } else {
        val adjustTarget: (Float) -> TargetAnimation? = { _ ->
            val now = value.holder.value
            val point = anchors.minByOrNull { abs(it - now) }
            val adjusted = point ?: now
            TargetAnimation(adjusted, SliderToTickAnimation)
        }
        defaultFlingConfig(adjustTarget = adjustTarget)
    }
}

private fun Modifier.sliderSemantics(
    value: Float,
    position: SliderPosition,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
): Modifier {
    val coerced = value.coerceIn(position.startValue, position.endValue)
    return semantics(mergeDescendants = true) {
        setProgress(
            action = { targetValue ->
                val newValue = targetValue.coerceIn(position.startValue, position.endValue)
                val resolvedValue = if (steps > 0) {
                    position.tickFractions
                        .map { lerp(position.startValue, position.endValue, it) }
                        .minByOrNull { abs(it - newValue) } ?: newValue
                } else {
                    newValue
                }
                // This is to keep it consistent with AbsSeekbar.java: return false if no
                // change from current.
                if (resolvedValue == coerced) {
                    false
                } else {
                    onValueChange(resolvedValue)
                    true
                }
            }
        )
    }.progressSemantics(value, valueRange, steps)
}

/**
 * Internal state for [Slider] that represents the Slider value, its bounds and optional amount of
 * steps evenly distributed across the Slider range.
 *
 * @param initial initial value for the Slider when created. If outside of range provided,
 * initial position will be coerced to this range
 * @param valueRange range of values that Slider value can take
 * @param steps if greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, slider will behave as a continuous slider and allow
 * to choose any value from the range specified. Must not be negative.
 */
private class SliderPosition(
    initial: Float = 0f,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    animatedClock: AnimationClockObservable,
    var onValueChange: (Float) -> Unit
) {

    internal val startValue: Float = valueRange.start
    internal val endValue: Float = valueRange.endInclusive

    init {
        require(steps >= 0) {
            "steps should be >= 0"
        }
    }

    internal var scaledValue: Float = initial
        set(value) {
            val scaled = scale(startValue, endValue, value, startPx, endPx)
            // floating point error due to rescaling
            if ((scaled - holder.value) > floatPointMistakeCorrection) {
                holder.snapTo(scaled)
            }
        }

    private val floatPointMistakeCorrection = (valueRange.endInclusive - valueRange.start) / 100

    private var endPx = Float.MAX_VALUE
    private var startPx = Float.MIN_VALUE

    internal fun setBounds(min: Float, max: Float) {
        if (startPx == min && endPx == max) return
        val newValue = scale(startPx, endPx, holder.value, min, max)
        startPx = min
        endPx = max
        holder.setBounds(min, max)
        anchorsPx = tickFractions.map {
            lerp(startPx, endPx, it)
        }
        holder.snapTo(newValue)
    }

    internal val tickFractions: List<Float> =
        if (steps == 0) emptyList() else List(steps + 2) { it.toFloat() / (steps + 1) }

    internal var anchorsPx: List<Float> = emptyList()
        private set

    @Suppress("UnnecessaryLambdaCreation")
    internal val holder =
        CallbackBasedAnimatedFloat(
            scale(startValue, endValue, initial, startPx, endPx),
            animatedClock
        ) { onValueChange(scale(startPx, endPx, it, startValue, endValue)) }
}

private class CallbackBasedAnimatedFloat(
    initial: Float,
    clock: AnimationClockObservable,
    var onValue: (Float) -> Unit
) : AnimatedFloat(clock) {

    override var value = initial
        set(value) {
            onValue(value)
            field = value
        }
}

// Internal to be referred to in tests
internal val ThumbRadius = 10.dp
private val ThumbRippleRadius = 24.dp
private val ThumbDefaultElevation = 1.dp
private val ThumbPressedElevation = 6.dp

// Internal to be referred to in tests
internal val TrackHeight = 4.dp
private val SliderHeight = 48.dp
private val SliderMinWidth = 144.dp // TODO: clarify min width
private val DefaultSliderConstraints =
    Modifier.preferredWidthIn(min = SliderMinWidth)
        .preferredHeightIn(max = SliderHeight)

private val SliderToTickAnimation = TweenSpec<Float>(durationMillis = 100)
