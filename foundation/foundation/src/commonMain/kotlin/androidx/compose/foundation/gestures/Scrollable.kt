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

package androidx.compose.foundation.gestures

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimatedFloat
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.defaultFlingConfig
import androidx.compose.foundation.animation.fling
import androidx.compose.runtime.AtomicReference
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.Direction
import androidx.compose.ui.gesture.ScrollCallback
import androidx.compose.ui.gesture.nestedscroll.NestedScrollConnection
import androidx.compose.ui.gesture.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.gesture.nestedscroll.NestedScrollSource
import androidx.compose.ui.gesture.nestedscroll.nestedScroll
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.minus
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Create and remember [ScrollableController] for [scrollable] with default [FlingConfig] and
 * [AnimationClockObservable]
 *
 * @param interactionState [InteractionState] that will be updated when this scrollable is
 * being scrolled by dragging, using [Interaction.Dragged]. If you want to know whether the fling
 * (or smooth scroll) is in progress, use [ScrollableController.isAnimationRunning].
 * @param consumeScrollDelta callback invoked when scrollable drag/fling/smooth scrolling occurs.
 * The callback receives the delta in pixels. Callers should update their state in this lambda
 * and return amount of delta consumed
 */
@Composable
fun rememberScrollableController(
    interactionState: InteractionState? = null,
    consumeScrollDelta: (Float) -> Float
): ScrollableController {
    val clocks = AmbientAnimationClock.current.asDisposableClock()
    val flingConfig = defaultFlingConfig()
    return remember(clocks, flingConfig, interactionState) {
        ScrollableController(consumeScrollDelta, flingConfig, clocks, interactionState)
    }
}

/**
 * Controller to control the [scrollable] modifier with. Contains necessary information about the
 * ongoing fling and provides smooth scrolling capabilities.
 *
 * @param consumeScrollDelta callback invoked when drag/fling/smooth scrolling occurs. The
 * callback receives the delta in pixels. Callers should update their state in this lambda and
 * return the amount of delta consumed
 * @param flingConfig fling configuration to use for flinging
 * @param animationClock animation clock to run flinging and smooth scrolling on
 * @param interactionState [InteractionState] that will be updated when this scrollable is
 * being scrolled by dragging, using [Interaction.Dragged]. If you want to know whether the fling
 * (or smooth scroll) is in progress, use [ScrollableController.isAnimationRunning].
 */
class ScrollableController(
    internal val consumeScrollDelta: (Float) -> Float,
    internal val flingConfig: FlingConfig,
    animationClock: AnimationClockObservable,
    internal val interactionState: InteractionState? = null
) : Scrollable {
    /**
     * Smooth scroll by [value] amount of pixels
     *
     * @param value delta to scroll by
     * @param spec [AnimationSpec] to be used for this smooth scrolling
     * @param onEnd lambda to be called when smooth scrolling has ended
     */
    fun smoothScrollBy(
        value: Float,
        spec: AnimationSpec<Float> = SpringSpec(),
        onEnd: (endReason: AnimationEndReason, finishValue: Float) -> Unit = { _, _ -> }
    ) {
        val to = animatedFloat.value + value
        animatedFloat.animateTo(to, anim = spec, onEnd = onEnd)
    }

    private val scrollControlJob = AtomicReference<Job?>(null)
    private val scrollControlMutex = Mutex()

    private val scrollScope: ScrollScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float = consumeScrollDelta(pixels)
    }

    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this
     * object) in order to guarantee that mutual exclusion is enforced.
     *
     * Cancels the currently running scroll, if any, and suspends until the cancellation is
     * complete.
     *
     * If [scroll] is called from elsewhere, this will be canceled.
     */
    override suspend fun scroll(
        block: suspend ScrollScope.() -> Unit
    ): Unit = coroutineScope {
        stopFlingAnimation()
        val currentJob = coroutineContext[Job]
        scrollControlJob.getAndSet(currentJob)?.cancel()
        scrollControlMutex.withLock(currentJob) {
            // TODO: this is a workaround to make isAnimationRunning work for now by considering all
            //  suspend scrolls to be animations
            isAnimationRunningState.value = true
            scrollScope.block()
            isAnimationRunningState.value = false
        }
    }

    private val isAnimationRunningState = mutableStateOf(false)

    private val clocksProxy: AnimationClockObservable = object : AnimationClockObservable {
        override fun subscribe(observer: AnimationClockObserver) {
            isAnimationRunningState.value = true
            animationClock.subscribe(observer)
        }

        override fun unsubscribe(observer: AnimationClockObserver) {
            isAnimationRunningState.value = false
            animationClock.unsubscribe(observer)
        }
    }

    /**
     * whether this [ScrollableController] is currently scrolling via [scroll].
     *
     * Note: **all** scrolls initiated via [scroll] are considered to be animations, regardless of
     * whether they are actually performing an animation.  For instance, gestures that perform
     * scrolls via `scroll
     */
    val isAnimationRunning
        get() = isAnimationRunningState.value

    /**
     * The current velocity of the fling animation.
     *
     * Useful for handoff between animations
     */
    internal val velocity: Float
        get() = animatedFloat.velocity

    /**
     * Stop any ongoing animation, smooth scrolling or fling
     *
     * Call this to stop receiving scrollable deltas in [consumeScrollDelta]
     */
    internal fun stopFlingAnimation() {
        animatedFloat.stop()
    }

    /**
     * Stop any ongoing animation, smooth scrolling, fling, or any other scroll occurring via
     * [scroll].
     *
     * Call this to stop receiving scrollable deltas in [consumeScrollDelta]
     */
    fun stopAnimation() {
        stopFlingAnimation()
        scrollControlJob.getAndSet(null)?.cancel()
    }

    private val animatedFloat =
        DeltaAnimatedFloat(0f, clocksProxy) {
            dispatchScroll(it.reverseIfNeeded(), NestedScrollSource.Fling)
        }

    private var orientation by mutableStateOf(Orientation.Vertical)
    private var reverseDirection by mutableStateOf(false)

    // this is not good, should be gone when we have sync (suspend) animation and scroll
    internal fun update(orientation: Orientation, reverseDirection: Boolean) {
        this.orientation = orientation
        this.reverseDirection = reverseDirection
    }

    internal val nestedScrollDispatcher = NestedScrollDispatcher()

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset = performDeltaConsumption(available)

        override fun onPostFling(
            consumed: Velocity,
            available: Velocity,
            onFinished: (Velocity) -> Unit
        ) {
            performFlingInternal(Offset(available.x, available.y)) { leftAfterUs ->
                onFinished.invoke(available - Velocity(leftAfterUs.x, leftAfterUs.y))
            }
        }
    }

    internal fun dispatchScroll(scrollDelta: Float, source: NestedScrollSource) {
        val scrollOffset = scrollDelta.toOffset()
        val preConsumedByParent = nestedScrollDispatcher.dispatchPreScroll(scrollOffset, source)

        val scrollAvailable = scrollOffset - preConsumedByParent
        val consumed = performDeltaConsumption(scrollAvailable)
        val leftForParent = scrollAvailable - consumed
        nestedScrollDispatcher.dispatchPostScroll(consumed, leftForParent, source)
    }

    private fun performDeltaConsumption(delta: Offset): Offset {
        // reverse once for users if needed and then back to the original axis system
        return consumeScrollDelta(delta.toFloat().reverseIfNeeded()).reverseIfNeeded().toOffset()
    }

    internal fun dispatchFling(velocity: Float, onScrollEnd: (Float) -> Unit) {
        val consumedByParent =
            nestedScrollDispatcher.dispatchPreFling(velocity.toVelocity())
        val available = velocity.toVelocity() - consumedByParent
        performFlingInternal(Offset(available.x, available.y)) { velocityLeft ->
            // when notifying users code -- reverse if needed to obey their setting
            val velocityLeftAsVelocity = Velocity(velocityLeft.x, velocityLeft.y)
            onScrollEnd(velocityLeft.toFloat().reverseIfNeeded())
            nestedScrollDispatcher.dispatchPostFling(
                available - velocityLeftAsVelocity,
                velocityLeftAsVelocity
            )
        }
    }

    private fun performFlingInternal(velocity: Offset, onScrollEnd: (Offset) -> Unit) {
        animatedFloat.fling(
            config = flingConfig,
            startVelocity = velocity.toFloat().reverseIfNeeded(),
            onAnimationEnd = { _, _, velocityLeft ->
                onScrollEnd(velocityLeft.reverseIfNeeded().toOffset())
            }
        )
    }

    private fun Float.toOffset(): Offset =
        if (orientation == Orientation.Horizontal) Offset(this, 0f) else Offset(0f, this)

    private fun Float.toVelocity(): Velocity =
        if (orientation == Orientation.Horizontal) Velocity(this, 0f) else Velocity(0f, this)

    private fun Offset.toFloat(): Float =
        if (orientation == Orientation.Horizontal) this.x else this.y

    private fun Velocity.toFloat(): Float =
        if (orientation == Orientation.Horizontal) this.x else this.y

    private fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this
}

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state via [ScrollableController.consumeScrollDelta] and reflect
 * their own state in UI when using this component.
 *
 * [ScrollableController] is required for this modifier to work correctly. When constructing
 * [ScrollableController], you must provide a [ScrollableController.consumeScrollDelta] lambda,
 * which will be invoked whenever scroll happens (by gesture input, by smooth scrolling or
 * flinging) with the delta in pixels. The amount of scrolling delta consumed must be returned
 * from this lambda to ensure proper nested scrolling.
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 *
 * @param orientation orientation of the scrolling
 * @param controller [ScrollableController] object that is responsible for redirecting scroll
 * deltas to [ScrollableController.consumeScrollDelta] callback and provides smooth scrolling
 * capabilities
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param canScroll callback to indicate whether or not scroll is allowed for given [Direction]
 * @param onScrollStarted callback to be invoked when scroll has started from the certain
 * position on the screen
 * @param onScrollStopped callback to be invoked when scroll stops with amount of velocity
 * unconsumed provided
 */
fun Modifier.scrollable(
    orientation: Orientation,
    controller: ScrollableController,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    canScroll: (Direction) -> Boolean = { enabled },
    onScrollStarted: (startedPosition: Offset) -> Unit = {},
    onScrollStopped: (velocity: Float) -> Unit = {}
): Modifier = composed(
    factory = {
        controller.update(orientation, reverseDirection)
        onDispose {
            controller.stopAnimation()
            controller.interactionState?.removeInteraction(Interaction.Dragged)
        }

        val scrollCallback = object : ScrollCallback {

            override fun onStart(downPosition: Offset) {
                if (enabled) {
                    controller.stopFlingAnimation()
                    controller.interactionState?.addInteraction(Interaction.Dragged)
                    onScrollStarted(downPosition)
                }
            }

            override fun onScroll(scrollDistance: Float): Float {
                if (!enabled) return 0f
                controller.stopFlingAnimation()
                controller.dispatchScroll(scrollDistance, NestedScrollSource.Drag)
                // consume everything since we handle nested scrolling separately
                return scrollDistance
            }

            override fun onCancel() {
                controller.interactionState?.removeInteraction(Interaction.Dragged)
                if (enabled) {
                    onScrollStopped(0f)
                }
            }

            override fun onStop(velocity: Float) {
                controller.interactionState?.removeInteraction(Interaction.Dragged)
                if (enabled) {
                    controller.dispatchFling(
                        velocity = velocity,
                        onScrollEnd = onScrollStopped
                    )
                }
            }
        }

        touchScrollable(
            scrollCallback = scrollCallback,
            orientation = orientation,
            canScroll = canScroll,
            startScrollImmediately = controller.isAnimationRunning
        ).mouseScrollable(
            scrollCallback,
            orientation
        ).nestedScroll(controller.nestedScrollConnection, controller.nestedScrollDispatcher)
    },
    inspectorInfo = debugInspectorInfo {
        name = "scrollable"
        properties["orientation"] = orientation
        properties["controller"] = controller
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["canScroll"] = canScroll
        properties["onScrollStarted"] = onScrollStarted
        properties["onScrollStopped"] = onScrollStopped
    }
)

internal expect fun Modifier.touchScrollable(
    scrollCallback: ScrollCallback,
    orientation: Orientation,
    canScroll: ((Direction) -> Boolean)?,
    startScrollImmediately: Boolean
): Modifier

// TODO(demin): think how we can move touchScrollable/mouseScrollable into commonMain,
//  so Android can support mouse wheel scrolling, and desktop can support touch scrolling.
//  For this we need first to implement different types of PointerInputEvent
//  (to differentiate mouse and touch)
internal expect fun Modifier.mouseScrollable(
    scrollCallback: ScrollCallback,
    orientation: Orientation
): Modifier

private class DeltaAnimatedFloat(
    initial: Float,
    clock: AnimationClockObservable,
    private val onDelta: (Float) -> Unit
) : AnimatedFloat(clock, Spring.DefaultDisplacementThreshold) {

    override var value = initial
        set(value) {
            if (isRunning) {
                val delta = value - field
                onDelta(delta)
            }
            field = value
        }
}