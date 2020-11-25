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
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.defaultFlingConfig
import androidx.compose.foundation.animation.fling
import androidx.compose.runtime.AtomicReference
import androidx.compose.runtime.Composable
import androidx.compose.runtime.dispatch.withFrameMillis
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.Direction
import androidx.compose.ui.gesture.ScrollCallback
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.platform.debugInspectorInfo
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
 * Scope used for suspending scroll blocks
 */
interface ScrollScope {
    /**
     * Attempts to scroll forward by [pixels] px.
     *
     * @return the amount of the requested scroll that was consumed (that is, how far it scrolled)
     */
    fun scrollBy(pixels: Float): Float
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
) {
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

    /**
     * Smooth scroll by [value] pixels.
     *
     * Cancels the currently running scroll, if any, and suspends until the cancellation is
     * complete.
     *
     * @param value delta to scroll by
     * @param spec [AnimationSpec] to be used for this smooth scrolling
     *
     * @return the amount of scroll consumed
     */
    @OptIn(ExperimentalFoundationApi::class)
    suspend fun smoothScrollBy(
        value: Float,
        spec: AnimationSpec<Float> = spring()
    ): Float {
        val animSpec = spec.vectorize(Float.VectorConverter)
        val conv = Float.VectorConverter
        val zeroVector = conv.convertToVector(0f)
        val targetVector = conv.convertToVector(value)
        var previousValue = 0f

        scroll {
            val startTimeMillis = withFrameMillis { it }
            do {
                val finished = withFrameMillis { frameTimeMillis ->
                    val newValue = conv.convertFromVector(
                        animSpec.getValue(
                            playTime = frameTimeMillis - startTimeMillis,
                            start = zeroVector,
                            end = targetVector,
                            // TODO: figure out if/how we should incorporate existing velocity
                            startVelocity = zeroVector
                        )
                    )
                    val delta = newValue - previousValue
                    val consumed = scrollBy(delta)

                    if (consumed != delta) {
                        previousValue += consumed
                        true
                    } else {
                        previousValue = newValue
                        previousValue == value
                    }
                }
            } while (!finished)
        }
        return previousValue
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
    suspend fun scroll(
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
        DeltaAnimatedFloat(0f, clocksProxy, consumeScrollDelta)

    /**
     * current position for scrollable
     */
    internal var value: Float
        get() = animatedFloat.value
        set(value) = animatedFloat.snapTo(value)

    internal fun fling(velocity: Float, onScrollEnd: (Float) -> Unit) {
        animatedFloat.fling(
            config = flingConfig,
            startVelocity = velocity,
            onAnimationEnd = { _, _, velocityLeft ->
                onScrollEnd(velocityLeft)
            }
        )
    }
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
                val toConsume = if (reverseDirection) scrollDistance * -1 else scrollDistance
                val consumed = controller.consumeScrollDelta(toConsume)
                controller.value = controller.value + consumed
                return if (reverseDirection) consumed * -1 else consumed
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
                    controller.fling(
                        velocity = if (reverseDirection) velocity * -1 else velocity,
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
        )
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
    private val onDelta: (Float) -> Float
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