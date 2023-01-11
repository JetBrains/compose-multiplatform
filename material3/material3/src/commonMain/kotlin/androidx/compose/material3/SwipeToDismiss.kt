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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DismissDirection.EndToStart
import androidx.compose.material3.DismissDirection.StartToEnd
import androidx.compose.material3.DismissValue.Default
import androidx.compose.material3.DismissValue.DismissedToEnd
import androidx.compose.material3.DismissValue.DismissedToStart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

/**
 * The directions in which a [SwipeToDismiss] can be dismissed.
 */
@ExperimentalMaterial3Api
enum class DismissDirection {
    /**
     * Can be dismissed by swiping in the reading direction.
     */
    StartToEnd,

    /**
     * Can be dismissed by swiping in the reverse of the reading direction.
     */
    EndToStart
}

/**
 * Possible values of [DismissState].
 */
@ExperimentalMaterial3Api
enum class DismissValue {
    /**
     * Indicates the component has not been dismissed yet.
     */
    Default,

    /**
     * Indicates the component has been dismissed in the reading direction.
     */
    DismissedToEnd,

    /**
     * Indicates the component has been dismissed in the reverse of the reading direction.
     */
    DismissedToStart
}

/**
 * State of the [SwipeToDismiss] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 * while a swipe is in progress and when settling after the swipe ends. This is the distance from
 * the start of a transition. It will be, depending on the direction of the interaction, added or
 * subtracted from/to the origin offset. It should always be a positive value.
 */
@ExperimentalMaterial3Api
class DismissState(
    initialValue: DismissValue,
    confirmValueChange: (DismissValue) -> Boolean = { true },
    positionalThreshold: Density.(totalDistance: Float) -> Float =
        SwipeToDismissDefaults.FixedPositionalThreshold,
) {
    internal val swipeableState = SwipeableV2State(
        initialValue = initialValue,
        confirmValueChange = confirmValueChange,
        positionalThreshold = positionalThreshold,
        velocityThreshold = DismissThreshold
    )

    internal val offset: Float? get() = swipeableState.offset

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float = swipeableState.requireOffset()

    /**
     * The current state value of the [DismissState].
     */
    val currentValue: DismissValue get() = swipeableState.currentValue

    /**
     * The target state. This is the closest state to the current offset (taking into account
     * positional thresholds). If no interactions like animations or drags are in progress, this
     * will be the current state.
     */
    val targetValue: DismissValue get() = swipeableState.targetValue

    /**
     * The fraction of the progress going from currentValue to targetValue, within [0f..1f] bounds.
     */
    val progress: Float get() = swipeableState.progress

    /**
     * The direction (if any) in which the composable has been or is being dismissed.
     *
     * If the composable is settled at the default state, then this will be null. Use this to
     * change the background of the [SwipeToDismiss] if you want different actions on each side.
     */
    val dismissDirection: DismissDirection? get() =
        if (offset == 0f || offset == null) null else if (offset!! > 0f) StartToEnd else EndToStart

    /**
     * Whether the component has been dismissed in the given [direction].
     *
     * @param direction The dismiss direction.
     */
    fun isDismissed(direction: DismissDirection): Boolean {
        return currentValue == if (direction == StartToEnd) DismissedToEnd else DismissedToStart
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: DismissValue) {
        swipeableState.snapTo(targetValue)
    }

    /**
     * Reset the component to the default position with animation and suspend until it if fully
     * reset or animation has been cancelled. This method will throw [CancellationException] if
     * the animation is interrupted
     *
     * @return the reason the reset animation ended
     */
    suspend fun reset() = swipeableState.animateTo(targetValue = Default)

    /**
     * Dismiss the component in the given [direction], with an animation and suspend. This method
     * will throw [CancellationException] if the animation is interrupted
     *
     * @param direction The dismiss direction.
     */
    suspend fun dismiss(direction: DismissDirection) {
        val targetValue = if (direction == StartToEnd) DismissedToEnd else DismissedToStart
        swipeableState.animateTo(targetValue = targetValue)
    }

    companion object {
        /**
         * The default [Saver] implementation for [DismissState].
         */
        fun Saver(
            confirmValueChange: (DismissValue) -> Boolean,
            positionalThreshold: Density.(totalDistance: Float) -> Float,
        ) =
            Saver<DismissState, DismissValue>(
                save = { it.currentValue },
                restore = {
                    DismissState(
                        it, confirmValueChange, positionalThreshold)
                }
            )
    }
}

/**
 * Create and [remember] a [DismissState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 * while a swipe is in progress and when settling after the swipe ends. This is the distance from
 * the start of a transition. It will be, depending on the direction of the interaction, added or
 * subtracted from/to the origin offset. It should always be a positive value.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberDismissState(
    initialValue: DismissValue = Default,
    confirmValueChange: (DismissValue) -> Boolean = { true },
    positionalThreshold: Density.(totalDistance: Float) -> Float =
        SwipeToDismissDefaults.FixedPositionalThreshold,
): DismissState {
    return rememberSaveable(
        saver = DismissState.Saver(confirmValueChange, positionalThreshold)) {
        DismissState(initialValue, confirmValueChange, positionalThreshold)
    }
}

/**
 * A composable that can be dismissed by swiping left or right.
 *
 * @sample androidx.compose.material3.samples.SwipeToDismissListItems
 *
 * @param state The state of this component.
 * @param background A composable that is stacked behind the content and is exposed when the
 * content is swiped. You can/should use the [state] to have different backgrounds on each side.
 * @param dismissContent The content that can be dismissed.
 * @param modifier Optional [Modifier] for this component.
 * @param directions The set of directions in which the component can be dismissed.
 */
@Composable
@ExperimentalMaterial3Api
fun SwipeToDismiss(
    state: DismissState,
    background: @Composable RowScope.() -> Unit,
    dismissContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    directions: Set<DismissDirection> = setOf(EndToStart, StartToEnd),
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier
            .swipeableV2(
                state = state.swipeableState,
                orientation = Orientation.Horizontal,
                enabled = state.currentValue == Default,
                reverseDirection = isRtl,
            )
            .swipeAnchors(
                state = state.swipeableState,
                possibleValues = setOf(Default, DismissedToEnd, DismissedToStart)
            ) { value, layoutSize ->
                val width = layoutSize.width.toFloat()
                when (value) {
                    DismissedToEnd -> if (StartToEnd in directions) width else null
                    DismissedToStart -> if (EndToStart in directions) -width else null
                    Default -> 0f
                }
            }
        ) {
            Row(
                content = background,
                modifier = Modifier.matchParentSize()
            )
            Row(
                content = dismissContent,
                modifier = Modifier.offset { IntOffset(state.requireOffset().roundToInt(), 0) }
            )
        }
}

/** Contains default values for [SwipeToDismiss] and [DismissState]. */
@ExperimentalMaterial3Api
object SwipeToDismissDefaults {
    /** Default positional threshold of 56.dp for [DismissState]. */
    val FixedPositionalThreshold: Density.(totalDistance: Float) -> Float = { _ -> 56.dp.toPx() }
}

private val DismissThreshold = 125.dp