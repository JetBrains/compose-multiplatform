/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material.ripple

import androidx.compose.animation.AnimatedFloatModel
import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEach

/**
 * Creates and [remember]s a Ripple using values provided by [RippleTheme].
 *
 * A Ripple is a Material implementation of [Indication] that expresses different [Interaction]s
 * by drawing ripple animations and state layers.
 *
 * A Ripple responds to [Interaction.Pressed] by starting a new [RippleAnimation], and
 * responds to other [Interaction]s by showing a fixed [StateLayer] with varying alpha values
 * depending on the [Interaction].
 *
 * If you are using MaterialTheme in your hierarchy, a Ripple will be used as the default
 * [Indication] inside components such as [androidx.compose.foundation.clickable] and
 * [androidx.compose.foundation.indication]. You can also manually provide Ripples through
 * [androidx.compose.foundation.AmbientIndication] for the same effect if you are not using
 * MaterialTheme.
 *
 * You can also explicitly create a Ripple and provide it to components in order to change the
 * parameters from the default, such as to create an unbounded ripple with a fixed size.
 *
 * @param bounded If true, ripples are clipped by the bounds of the target layout. Unbounded
 * ripples always animate from the target layout center, bounded ripples animate from the touch
 * position.
 * @param radius the radius for the ripple. If [Dp.Unspecified] is provided then the size will be
 * calculated based on the target layout size.
 * @param color the color of the ripple. This color is usually the same color used by the text or
 * iconography in the component. This color will then have [RippleTheme.rippleAlpha] applied to
 * calculate the final color used to draw the ripple. If [Color.Unspecified] is provided the color
 * used will be [RippleTheme.defaultColor] instead.
 */
@Composable
@OptIn(ExperimentalRippleApi::class)
public fun rememberRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
    color: Color = Color.Unspecified
): Indication {
    val theme = AmbientRippleTheme.current
    val clock = AmbientAnimationClock.current.asDisposableClock()
    val resolvedColor = color.takeOrElse { theme.defaultColor() }
    val colorState = remember { mutableStateOf(resolvedColor, structuralEqualityPolicy()) }
    colorState.value = resolvedColor
    val rippleAlpha = theme.rippleAlpha()
    return remember(bounded, radius, theme, clock) {
        Ripple(bounded, radius, colorState, rippleAlpha, clock)
    }
}

/**
 * A Ripple is a Material implementation of [Indication] that expresses different [Interaction]s
 * by drawing ripple animations and state layers.
 *
 * A Ripple responds to [Interaction.Pressed] by starting a new [RippleAnimation], and
 * responds to other [Interaction]s by showing a fixed [StateLayer] with varying alpha values
 * depending on the [Interaction].
 *
 * If you are using MaterialTheme in your hierarchy, a Ripple will be used as the default
 * [Indication] inside components such as [androidx.compose.foundation.clickable] and
 * [androidx.compose.foundation.indication]. You can also manually provide Ripples through
 * [androidx.compose.foundation.AmbientIndication] for the same effect if you are not using
 * MaterialTheme.
 *
 * You can also explicitly create a Ripple and provide it to components in order to change the
 * parameters from the default, such as to create an unbounded ripple with a fixed size.
 */
@Stable
@ExperimentalRippleApi
private class Ripple(
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: State<Color>,
    private val rippleAlpha: RippleAlpha,
    private val clock: AnimationClockObservable
) : Indication {
    override fun createInstance(): IndicationInstance {
        return RippleIndicationInstance(bounded, radius, color, rippleAlpha, clock)
    }

    // to force stability on this indication we need equals and hashcode, there's no value in
    // making this class to be "data class"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ripple) return false

        if (bounded != other.bounded) return false
        if (radius != other.radius) return false
        if (color != other.color) return false
        if (rippleAlpha != other.rippleAlpha) return false
        if (clock != other.clock) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bounded.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + rippleAlpha.hashCode()
        result = 31 * result + clock.hashCode()
        return result
    }
}

@ExperimentalRippleApi
private class RippleIndicationInstance constructor(
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: State<Color>,
    private val rippleAlpha: RippleAlpha,
    private val clock: AnimationClockObservable
) : IndicationInstance {

    private val stateLayer = StateLayer(clock, bounded, rippleAlpha)

    private val ripples = mutableStateListOf<RippleAnimation>()
    private var currentPressPosition: Offset? = null
    private var currentRipple: RippleAnimation? = null

    override fun ContentDrawScope.drawIndication(interactionState: InteractionState) {
        val color = color.value
        // TODO: b/174310811 use library function instead of manual logic here
        val targetRadius = if (radius == Dp.Unspecified) {
            getRippleEndRadius(bounded, size)
        } else {
            radius.toPx()
        }
        drawContent()
        with(stateLayer) {
            drawStateLayer(interactionState, targetRadius, color)
        }
        val pressPosition = interactionState.interactionPositionFor(Interaction.Pressed)
        if (pressPosition != null) {
            if (currentPressPosition != pressPosition) {
                addRipple(targetRadius, pressPosition)
            }
        } else {
            removeRipple()
        }
        drawRipples(color)
    }

    private fun ContentDrawScope.addRipple(targetRadius: Float, pressPosition: Offset) {
        currentRipple?.finish()
        val pxSize = Size(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val position = if (bounded) pressPosition else center
        val ripple = RippleAnimation(pxSize, position, targetRadius, bounded, clock) { ripple ->
            ripples.remove(ripple)
            if (currentRipple == ripple) {
                currentRipple = null
            }
        }
        ripples.add(ripple)
        currentPressPosition = pressPosition
        currentRipple = ripple
    }

    private fun removeRipple() {
        currentRipple?.finish()
        currentRipple = null
        currentPressPosition = null
    }

    private fun DrawScope.drawRipples(color: Color) {
        ripples.fastForEach {
            with(it) {
                val alpha = rippleAlpha.alphaForInteraction(Interaction.Pressed)
                if (alpha != 0f) {
                    draw(color.copy(alpha = alpha))
                }
            }
        }
    }

    override fun onDispose() {
        ripples.clear()
        currentRipple = null
    }
}

/**
 * Represents the layer underneath the press ripple, that displays an overlay for states such as
 * [Interaction.Dragged].
 *
 * Typically, there should be both an 'incoming' and an 'outgoing' layer, so that when
 * transitioning between two states, the incoming of the new state, and the outgoing of the old
 * state can be displayed. However, because:
 *
 * a) the duration of these outgoing transitions are so short (mostly 15ms, which is less than 1
 * frame at 60fps), and hence are barely noticeable if they happen at the same time as an
 * incoming transition
 * b) two layers cause a lot of extra work, and related performance concerns
 *
 * We skip managing two layers, and instead only show one layer. The details for the
 * [AnimationSpec]s used are as follows:
 *
 * No state -> a state = incoming transition for the new state
 * A state -> a different state = incoming transition for the new state
 * A state -> no state = outgoing transition for the old state
 *
 * @see IncomingStateLayerAnimationSpecs
 * @see OutgoingStateLayerAnimationSpecs
 */
@ExperimentalRippleApi
private class StateLayer(
    clock: AnimationClockObservable,
    private val bounded: Boolean,
    private val rippleAlpha: RippleAlpha
) {
    private val animatedAlpha = AnimatedFloatModel(0f, clock)
    private var previousInteractions: Set<Interaction> = emptySet()
    private var lastDrawnInteraction: Interaction? = null

    fun ContentDrawScope.drawStateLayer(
        interactionState: InteractionState,
        targetRadius: Float,
        color: Color
    ) {
        val currentInteractions = interactionState.value
        var handled = false

        // Handle a new interaction, starting from the end as we care about the most recent
        // interaction, not the oldest interaction.
        for (interaction in currentInteractions.reversed()) {
            // Stop looping if we have already moved to a new state
            if (handled) break

            // Pressed state is explicitly handled with a ripple animation, and not a state layer
            if (interaction is Interaction.Pressed) continue

            // Move to the next interaction if this interaction is not a new interaction
            if (interaction in previousInteractions) continue

            // Move to the next interaction if this is not an interaction we show a state layer for
            val targetAlpha = rippleAlpha.alphaForInteraction(interaction)
            if (targetAlpha == 0f) continue

            // TODO: consider defaults - these will be used for a custom Interaction that we are
            // not aware of, but has an alpha that should be shown because of a custom RippleTheme.
            val incomingAnimationSpec = IncomingStateLayerAnimationSpecs[interaction]
                ?: TweenSpec(durationMillis = 15, easing = LinearEasing)

            animatedAlpha.animateTo(targetAlpha, incomingAnimationSpec)

            lastDrawnInteraction = interaction
            handled = true
        }

        // Clean up any stale interactions if we have not moved to a new interaction
        if (!handled) {
            val previousInteraction = lastDrawnInteraction
            if (previousInteraction != null && previousInteraction !in currentInteractions) {
                // TODO: consider defaults - these will be used for a custom Interaction that we are
                // not aware of, but has an alpha that should be shown because of a custom
                // RippleTheme.
                val outgoingAnimationSpec = OutgoingStateLayerAnimationSpecs[previousInteraction]
                    ?: TweenSpec(durationMillis = 15, easing = LinearEasing)

                animatedAlpha.animateTo(0f, outgoingAnimationSpec)

                lastDrawnInteraction = null
            }
        }

        previousInteractions = currentInteractions

        val alpha = animatedAlpha.value

        if (alpha > 0f) {
            val modulatedColor = color.copy(alpha = alpha)

            if (bounded) {
                clipRect {
                    drawCircle(modulatedColor, targetRadius)
                }
            } else {
                drawCircle(modulatedColor, targetRadius)
            }
        }
    }
}

/**
 * [AnimationSpec]s used when transitioning to a new state, either from a previous state, or no
 * state.
 *
 * TODO: handle hover / focus states
 */
private val IncomingStateLayerAnimationSpecs: Map<Interaction, AnimationSpec<Float>> = mapOf(
    Interaction.Dragged to TweenSpec(
        durationMillis = 45,
        easing = LinearEasing
    )
)

/**
 * [AnimationSpec]s used when transitioning away from a state, to no state.
 *
 * TODO: handle hover / focus states
 */
private val OutgoingStateLayerAnimationSpecs: Map<Interaction, AnimationSpec<Float>> = mapOf(
    Interaction.Dragged to TweenSpec(
        durationMillis = 150,
        easing = LinearEasing
    )
)
