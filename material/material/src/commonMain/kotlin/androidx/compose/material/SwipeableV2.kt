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

package androidx.compose.material

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Enable swipe gestures between a set of predefined states.
 *
 * When a swipe is detected, the offset of the [SwipeableV2State] will be updated with the swipe
 * delta. You should use this offset to move your content accordingly (see [Modifier.offset]).
 * When the swipe ends, the offset will be animated to one of the anchors and when that anchor is
 * reached, the value of the [SwipeableV2State] will also be updated to the state corresponding to
 * the new anchor.
 *
 * Swiping is constrained between the minimum and maximum anchors.
 *
 * @param state The associated [SwipeableV2State].
 * @param orientation The orientation in which the swipeable can be swiped.
 * @param enabled Whether this [swipeable] is enabled and should react to the user's input.
 * @param reverseDirection Whether to reverse the direction of the swipe, so a top to bottom
 * swipe will behave like bottom to top, and a left to right swipe will behave like right to left.
 * @param interactionSource Optional [MutableInteractionSource] that will passed on to
 * the internal [Modifier.draggable].
 */
@ExperimentalMaterialApi
internal fun <T> Modifier.swipeableV2(
    state: SwipeableV2State<T>,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    interactionSource: MutableInteractionSource? = null
) = draggable(
    state = state.draggableState,
    orientation = orientation,
    enabled = enabled,
    interactionSource = interactionSource,
    reverseDirection = reverseDirection,
    startDragImmediately = state.isAnimationRunning,
    onDragStopped = { velocity -> launch { state.settle(velocity) } }
)

/**
 * Define anchor points for a given [SwipeableV2State] based on this node's layout size and update
 * the state with them.
 *
 * @param state The associated [SwipeableV2State]
 * @param possibleStates All possible states the [SwipeableV2State] could be in.
 * @param anchorsChanged A callback to be invoked when the anchors have changed, `null` by default.
 * Components with custom reconciliation logic should implement this callback, i.e. to re-target an
 * in-progress animation.
 * @param calculateAnchor This method will be invoked to calculate the position of all
 * [possibleStates], given this node's layout size. Return the anchor's offset from the initial
 * anchor, or `null` to indicate that a state does not exist.
 */
@ExperimentalMaterialApi
internal fun <T> Modifier.swipeAnchors(
    state: SwipeableV2State<T>,
    possibleStates: Set<T>,
    anchorsChanged: ((oldAnchors: Map<T, Float>, newAnchors: Map<T, Float>) -> Unit)? = null,
    calculateAnchor: (state: T, layoutSize: IntSize) -> Float?,
) = onSizeChanged { layoutSize ->
    val previousAnchors = state.anchors
    val newAnchors = mutableMapOf<T, Float>()
    possibleStates.forEach {
        val anchorValue = calculateAnchor(it, layoutSize)
        if (anchorValue != null) {
            newAnchors[it] = anchorValue
        }
    }
    if (previousAnchors == newAnchors) return@onSizeChanged
    state.updateAnchors(newAnchors)

    if (previousAnchors.isNotEmpty()) {
        anchorsChanged?.invoke(previousAnchors, newAnchors)
    }
}

/**
 * State of the [swipeableV2] modifier.
 *
 * This contains necessary information about any ongoing swipe or animation and provides methods
 * to change the state either immediately or by starting an animation. To create and remember a
 * [SwipeableState] use [rememberSwipeableState].
 *
 * @param initialState The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
@ExperimentalMaterialApi
internal class SwipeableV2State<T>(
    initialState: T,
    val animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    val confirmStateChange: (newValue: T) -> Boolean = { true },
) {

    /**
     * The current state of the [SwipeableState].
     */
    var currentState: T by mutableStateOf(initialState)
        private set

    /**
     * The target state. This is the closest state to the current offset (taking into account
     * positional thresholds). If no animation is in progress, this will be the current state.
     */
    val targetState: T by derivedStateOf {
        if (unsafeOffset.isNaN()) currentState else anchors.closestState(unsafeOffset)
    }

    /**
     * The current offset.
     */
    val offset = derivedStateOf {
        if (unsafeOffset.isNaN()) error(
            "The offset was read before being initialized. Did you access the offset in a phase " +
                "before layout, like effects or composition?"
        )
        unsafeOffset
    }

    /**
     * Whether an animation is currently in progress.
     */
    var isAnimationRunning: Boolean by mutableStateOf(false)
        private set

    /**
     * The fraction of the progress going from currentState to targetState, within [0f..1f] bounds.
     */
    /*@FloatRange(from = 0f, to = 1f)*/
    val progress: Float by derivedStateOf {
        val a = anchors[currentState] ?: 0f
        val b = anchors[targetState] ?: 0f
        val distance = abs(b - a)
        if (distance > 1e-6f) {
            val progress = (this.offset.value - a) / (b - a)
            // If we are very close to 0f or 1f, we round to the closest
            if (progress < 1e-6f) 0f else if (progress > 1 - 1e-6f) 1f else progress
        } else 1f
    }

    /**
     * The velocity of the last known animation. Gets reset to 0f when an animation completes
     * successfully, but does not get reset when an animation gets interrupted.
     * You can use this value to provide smooth reconciliation behavior when re-targeting an
     * animation.
     */
    var lastVelocity: Float by mutableStateOf(0f)
        private set

    private val dragPosition = mutableStateOf(Float.NaN)

    private val unsafeOffset by derivedStateOf {
        dragPosition.value.coerceIn(minBound, maxBound)
    }

    private val minBound by derivedStateOf { anchors.minOrNull() ?: Float.NEGATIVE_INFINITY }
    private val maxBound by derivedStateOf { anchors.maxOrNull() ?: Float.POSITIVE_INFINITY }

    private var positionalThresholds: (lower: T, upper: T) -> Float by mutableStateOf(
        { _, _ -> 0f } // TODO
    )

    private var velocityThreshold by mutableStateOf(0f)

    internal val draggableState = DraggableState { dragPosition.value = dragPosition.value + it }

    internal var anchors by mutableStateOf(emptyMap<T, Float>())

    internal fun updateAnchors(newAnchors: Map<T, Float>) {
        val previousAnchorsEmpty = anchors.isEmpty()
        anchors = newAnchors
        if (previousAnchorsEmpty) {
            dragPosition.value = anchors.requireAnchor(this.currentState)
        }
    }

    /**
     * Whether the [state] has an anchor associated with it.
     */
    fun hasAnchorForState(state: T): Boolean = anchors.containsKey(state)

    /**
     * Snap to a [targetState] without any animation.
     *
     * @throws CancellationException if the interaction interrupted by another interaction like a
     * gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
     *
     * @param targetState The target state of the animation
     */
    suspend fun snapTo(targetState: T) {
        val targetOffset = anchors.requireAnchor(targetState)
        draggableState.drag {
            dragBy(targetOffset - offset.value)
        }
        this.currentState = targetState
    }

    /**
     * Animate to a [targetState].
     *
     * @throws CancellationException if the interaction interrupted by another interaction like a
     * gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
     *
     * @param targetState The target state of the animation
     * @param velocity The velocity the animation should start with, [lastVelocity] by default
     */
    suspend fun animateTo(
        targetState: T,
        velocity: Float = lastVelocity,
    ) {
        val targetOffset = anchors.requireAnchor(targetState)
        try {
            draggableState.drag {
                isAnimationRunning = true
                var prev = dragPosition.value
                try {
                    animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                        dragBy(value - prev)
                        prev = value
                        lastVelocity = velocity
                    }
                } finally {
                    isAnimationRunning = false
                }
            }
            lastVelocity = 0f
        } finally {
            val endOffset = dragPosition.value
            val endState = anchors
                .entries
                .firstOrNull { (_, anchorOffset) -> abs(anchorOffset - endOffset) < 0.5f }
                ?.key
            this.currentState = endState ?: currentState
        }
    }

    /**
     * Find the closest anchor taking into account the velocity and settle at it with an animation.
     */
    suspend fun settle(velocity: Float) {
        val previousState = this.currentState
        val targetState = computeTarget(
            offset = this.offset.value,
            currentState = previousState,
            thresholds = positionalThresholds,
            velocity = velocity,
            velocityThreshold = velocityThreshold
        )
        if (confirmStateChange(targetState)) {
            animateTo(targetState, velocity)
        } else {
            // If the user vetoed the state change, rollback to the previous state.
            animateTo(previousState, velocity)
        }
    }

    /**
     * Swipe by the [delta], coerce it in the bounds and dispatch it to the [draggableState].
     *
     * @return The delta the [draggableState] will consume
     */
    fun dispatchRawDelta(delta: Float): Float {
        val potentiallyConsumed = dragPosition.value + delta
        val clamped = potentiallyConsumed.coerceIn(minBound, maxBound)
        val deltaToConsume = clamped - dragPosition.value
        if (abs(deltaToConsume) > 0) {
            draggableState.dispatchRawDelta(deltaToConsume)
        }
        return deltaToConsume
    }

    private fun computeTarget(
        offset: Float,
        currentState: T,
        thresholds: (lower: T, upper: T) -> Float,
        velocity: Float,
        velocityThreshold: Float
    ): T {
        val currentAnchors = anchors
        return if (currentAnchors.requireAnchor(currentState) <= offset) {
            // Swiping from lower to upper (positive).
            if (velocity >= velocityThreshold) {
                currentAnchors.closestState(offset, true)
            } else {
                val upper = currentAnchors.closestState(offset, true)
                val threshold = thresholds(currentState, upper)
                if (offset < threshold) currentState else upper
            }
        } else {
            // Swiping from upper to lower (negative).
            if (velocity <= -velocityThreshold) {
                currentAnchors.closestState(offset, false)
            } else {
                val lower = currentAnchors.closestState(offset, false)
                val threshold = thresholds(currentState, lower)
                if (offset > threshold) currentState else lower
            }
        }
    }

    companion object {
        /**
         * The default [Saver] implementation for [SwipeableV2State].
         */
        fun <T : Any> Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (T) -> Boolean,
        ) = Saver<SwipeableV2State<T>, T>(
            save = { it.currentState },
            restore = {
                SwipeableV2State(
                    initialState = it,
                    animationSpec = animationSpec,
                    confirmStateChange = confirmStateChange,
                )
            }
        )
    }
}

/**
 * Create and remember a [SwipeableV2State].
 *
 * @param initialState The initial state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterialApi
internal fun <T : Any> rememberSwipeableV2State(
    initialState: T,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (newValue: T) -> Boolean = { true },
): SwipeableV2State<T> {
    return rememberSaveable(
        saver = SwipeableV2State.Saver(
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    ) {
        SwipeableV2State(
            initialState = initialState,
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    }
}

private fun <T> Map<T, Float>.closestState(offset: Float = 0f): T {
    require(isNotEmpty()) { "The anchors were empty when trying to find the closest state" }
    return minBy { (_, anchor) ->
        abs(anchor - offset)
    }.key
}

private fun <T> Map<T, Float>.closestState(
    offset: Float = 0f,
    searchUpwards: Boolean = false
): T {
    require(isNotEmpty()) { "The anchors were empty when trying to find the closest state" }
    return minBy { (_, anchor) ->
        val delta = if (searchUpwards) anchor - offset else offset - anchor
        if (delta < 0) Float.POSITIVE_INFINITY else delta
    }.key
}

private fun <T> Map<T, Float>.minOrNull() = minOfOrNull { (_, offset) -> offset }
private fun <T> Map<T, Float>.maxOrNull() = maxOfOrNull { (_, offset) -> offset }
private fun <T> Map<T, Float>.requireAnchor(state: T) = requireNotNull(this[state]) {
    "Required anchor $state was not found in anchors. Current anchors: ${this.toMap()}"
}
