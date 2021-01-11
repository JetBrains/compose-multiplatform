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

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume

@Deprecated("Use AwaitPointerEventScope", ReplaceWith("AwaitPointerEventScope"))
typealias HandlePointerInputScope = AwaitPointerEventScope

/**
 * Receiver scope for awaiting pointer events in a call to [PointerInputScope.awaitPointerEventScope].
 *
 * This is a restricted suspension scope. Code in this scope is always called undispatched and
 * may only suspend for calls to [awaitPointerEvent]. These functions
 * resume synchronously and the caller may mutate the result **before** the next await call to
 * affect the next stage of the input processing pipeline.
 */
@RestrictsSuspension
interface AwaitPointerEventScope : Density {
    /**
     * The measured size of the pointer input region. Input events will be reported with
     * a coordinate space of (0, 0) to (size.width, size,height) as the input region, with
     * (0, 0) indicating the upper left corner.
     */
    val size: IntSize

    /**
     * The [PointerEvent] from the most recent touch event.
     */
    val currentEvent: PointerEvent

    /**
     * The [ViewConfiguration] used to tune gesture detectors.
     */
    val viewConfiguration: ViewConfiguration

    /**
     * Suspend until a [PointerEvent] is reported to the specified input [pass].
     * [pass] defaults to [PointerEventPass.Main].
     *
     * [awaitPointerEvent] resumes **synchronously** in the restricted suspension scope. This
     * means that callers can react immediately to input after [awaitPointerEvent] returns
     * and affect both the current frame and the next handler or phase of the input processing
     * pipeline. Callers should mutate the returned [PointerEvent] before awaiting
     * another event to consume aspects of the event before the next stage of input processing runs.
     */
    suspend fun awaitPointerEvent(
        pass: PointerEventPass = PointerEventPass.Main
    ): PointerEvent
}

/**
 * Receiver scope for [Modifier.pointerInput] that permits
 * [handling pointer input][awaitPointerEventScope].
 */
// Design note: this interface does _not_ implement CoroutineScope, even though doing so
// would more easily permit the use of launch {} inside Modifier.pointerInput {} blocks without
// requiring an additional coroutineScope {} layer of nesting. As it is encouraged to define
// gesture detectors as suspending extensions with a PointerInputScope receiver, also making this
// interface implement CoroutineScope would be an invitation to break structured concurrency in
// these extensions, leaving other launched coroutines running in the calling scope.
interface PointerInputScope : Density {
    /**
     * The measured size of the pointer input region. Input events will be reported with
     * a coordinate space of (0, 0) to (size.width, size,height) as the input region, with
     * (0, 0) indicating the upper left corner.
     */
    val size: IntSize

    /**
     * The [ViewConfiguration] used to tune gesture detectors.
     */
    val viewConfiguration: ViewConfiguration

    /**
     * Suspend and install a pointer input [block] that can await input events and respond to
     * them immediately. A call to [awaitPointerEventScope] will resume with [block]'s result after
     * it completes.
     *
     * More than one [awaitPointerEventScope] can run concurrently in the same [PointerInputScope] by
     * using [kotlinx.coroutines.launch]. [block]s are dispatched to in the order in which they
     * were installed.
     */
    suspend fun <R> awaitPointerEventScope(
        block: suspend AwaitPointerEventScope.() -> R
    ): R

    @Deprecated("Use awaitPointerEventScope", ReplaceWith("awaitPointerEventScope(handler)"))
    suspend fun <R> handlePointerInput(
        handler: suspend AwaitPointerEventScope.() -> R
    ): R = awaitPointerEventScope(handler)
}

/**
 * Create a modifier for processing pointer input within the region of the modified element.
 *
 * [pointerInput] [block]s may call [PointerInputScope.awaitPointerEventScope] to install a pointer
 * input handler that can [AwaitPointerEventScope.awaitPointerEvent] to receive and consume
 * pointer input events. Extension functions on [PointerInputScope] or [AwaitPointerEventScope]
 * may be defined to perform higher-level gesture detection.
 */
fun Modifier.pointerInput(
    block: suspend PointerInputScope.() -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "pointerInput"
        this.properties["block"] = block
    }
) {
    val density = AmbientDensity.current
    val viewConfiguration = AmbientViewConfiguration.current
    remember(density) { SuspendingPointerInputFilter(viewConfiguration, density) }.apply {
        LaunchedEffect(this, block) {
            block()
        }
    }
}

private val DownChangeConsumed = ConsumedData(downChange = true)

/**
 * Implementation notes:
 * This class does a lot of lifting. It is both a [PointerInputModifier] and that modifier's
 * own [pointerInputFilter]. It is returned by way of a [Modifier.composed] from
 * the [Modifier.pointerInput] builder and is always 1-1 with an instance of application to
 * a LayoutNode.
 *
 * [SuspendingPointerInputFilter] implements the [PointerInputScope] used to offer the
 * [Modifier.pointerInput] DSL and carries the [Density] from [AmbientDensity] at the point of
 * the modifier's materialization. Even if this value were returned to the [PointerInputFilter]
 * callbacks, we would still need the value at composition time in order for [Modifier.pointerInput]
 * to begin its internal [LaunchedEffect] for the provided code block.
 */
// TODO: Suppressing deprecation for synchronized; need to move to atomicfu wrapper
@Suppress("DEPRECATION_ERROR")
internal class SuspendingPointerInputFilter(
    override val viewConfiguration: ViewConfiguration,
    density: Density = Density(1f)
) : PointerInputFilter(),
    PointerInputModifier,
    PointerInputScope,
    Density by density {

    override val pointerInputFilter: PointerInputFilter
        get() = this

    private var currentEvent: PointerEvent? = null

    override fun onInit(customEventDispatcher: CustomEventDispatcher) {
    }

    /**
     * Actively registered input handlers from currently ongoing calls to [awaitPointerEventScope].
     * Must use `synchronized(pointerHandlers)` to access.
     */
    private val pointerHandlers = mutableVectorOf<PointerEventHandlerCoroutine<*>>()

    /**
     * Scratch list for dispatching to handlers for a particular phase.
     * Used to hold a copy of the contents of [pointerHandlers] during dispatch so that
     * resumed continuations may add/remove handlers without affecting the current dispatch pass.
     * Must only access on the UI thread.
     */
    private val dispatchingPointerHandlers = mutableVectorOf<PointerEventHandlerCoroutine<*>>()

    /**
     * The last pointer event we saw where at least one pointer was currently down; null otherwise.
     * Used to synthesize a fake "all pointers changed to up/all changes to down-state consumed"
     * event for propagating cancellation. This synthetic event corresponds to Android's
     * `MotionEvent.ACTION_CANCEL`.
     */
    private var lastPointerEvent: PointerEvent? = null

    /**
     * The size of the bounds of this input filter. Normally [PointerInputFilter.size] can
     * be used, but for tests, it is better to not rely on something set to an `internal`
     * method.
     */
    private var boundsSize: IntSize = IntSize.Zero

    /**
     * Snapshot the current [pointerHandlers] and run [block] on each one.
     * May not be called reentrant or concurrent with itself.
     *
     * Dispatches from first to last registered for [PointerEventPass.Initial] and
     * [PointerEventPass.Final]; dispatches from last to first for [PointerEventPass.Main].
     * This corresponds to the down/up/down dispatch behavior of each of these passes along
     * the hit test path through the Compose UI layout hierarchy.
     */
    private inline fun forEachCurrentPointerHandler(
        pass: PointerEventPass,
        block: (PointerEventHandlerCoroutine<*>) -> Unit
    ) {
        // Copy handlers to avoid mutating the collection during dispatch
        synchronized(pointerHandlers) {
            dispatchingPointerHandlers.addAll(pointerHandlers)
        }
        try {
            when (pass) {
                PointerEventPass.Initial, PointerEventPass.Final ->
                    dispatchingPointerHandlers.forEach(block)
                PointerEventPass.Main ->
                    dispatchingPointerHandlers.forEachReversed(block)
            }
        } finally {
            dispatchingPointerHandlers.clear()
        }
    }

    /**
     * Dispatch [pointerEvent] for [pass] to all [pointerHandlers] currently registered when
     * the call begins.
     */
    private fun dispatchPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass
    ) {
        forEachCurrentPointerHandler(pass) {
            it.offerPointerEvent(pointerEvent, pass)
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        boundsSize = bounds
        if (pass == PointerEventPass.Initial) {
            currentEvent = pointerEvent
        }
        dispatchPointerEvent(pointerEvent, pass)

        lastPointerEvent = pointerEvent.takeIf { event ->
            !event.changes.fastAll { it.changedToUpIgnoreConsumed() }
        }
    }

    override fun onCancel() {
        // Synthesize a cancel event for whatever state we previously saw, if one is applicable.
        // A cancel event is one where all previously down pointers are now up, the change in
        // down-ness is consumed, and we omit any pointers that previously went up entirely.
        val lastEvent = lastPointerEvent ?: return

        val newChanges = lastEvent.changes.mapNotNull { old ->
            if (old.pressed) {
                old.copy(
                    currentPressed = false,
                    previousPosition = old.position,
                    previousTime = old.uptimeMillis,
                    previousPressed = old.pressed,
                    consumed = DownChangeConsumed
                )
            } else null
        }

        val cancelEvent = PointerEvent(newChanges)

        // Dispatch the synthetic cancel for all three passes
        dispatchPointerEvent(cancelEvent, PointerEventPass.Initial)
        dispatchPointerEvent(cancelEvent, PointerEventPass.Main)
        dispatchPointerEvent(cancelEvent, PointerEventPass.Final)

        lastPointerEvent = null
    }

    override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
    }

    override suspend fun <R> awaitPointerEventScope(
        block: suspend AwaitPointerEventScope.() -> R
    ): R = suspendCancellableCoroutine { continuation ->
        val handlerCoroutine = PointerEventHandlerCoroutine(continuation)
        synchronized(pointerHandlers) {
            pointerHandlers += handlerCoroutine

            // NOTE: We resume the new continuation while holding this lock.
            // We do this since it runs in a RestrictsSuspension scope and therefore
            // will only suspend when awaiting a new event. We don't release this
            // synchronized lock until we know it has an awaiter and any future dispatch
            // would succeed.

            // We also create the coroutine with both a receiver and a completion continuation
            // of the handlerCoroutine itself; we don't use our currently available suspended
            // continuation as the resume point because handlerCoroutine needs to remove the
            // ContinuationInterceptor from the supplied CoroutineContext to have undispatched
            // behavior in our restricted suspension scope. This is required so that we can
            // process event-awaits synchronously and affect the next stage in the pipeline
            // without running too late due to dispatch.
            block.createCoroutine(handlerCoroutine, handlerCoroutine).resume(Unit)
        }

        // Restricted suspension handler coroutines can't propagate structured job cancellation
        // automatically as the context must be EmptyCoroutineContext; do it manually instead.
        continuation.invokeOnCancellation { handlerCoroutine.cancel(it) }
    }

    /**
     * Implementation of the inner coroutine created to run a single call to
     * [awaitPointerEventScope].
     *
     * [PointerEventHandlerCoroutine] implements [AwaitPointerEventScope] to provide the
     * input handler DSL, and [Continuation] so that it can wrap [completion] and remove the
     * [ContinuationInterceptor] from the calling context and run undispatched.
     */
    private inner class PointerEventHandlerCoroutine<R>(
        private val completion: Continuation<R>,
    ) : AwaitPointerEventScope, Density by this@SuspendingPointerInputFilter, Continuation<R> {
        private var pointerAwaiter: CancellableContinuation<PointerEvent>? = null
        private var awaitPass: PointerEventPass = PointerEventPass.Main

        override val currentEvent: PointerEvent
            get() = checkNotNull(this@SuspendingPointerInputFilter.currentEvent) {
                "cannot access currentEvent outside of input dispatch"
            }
        override val size: IntSize
            get() = this@SuspendingPointerInputFilter.boundsSize
        override val viewConfiguration: ViewConfiguration
            get() = this@SuspendingPointerInputFilter.viewConfiguration

        fun offerPointerEvent(event: PointerEvent, pass: PointerEventPass) {
            if (pass == awaitPass) {
                pointerAwaiter?.run {
                    pointerAwaiter = null
                    resume(event)
                }
            }
        }

        // Called to run any finally blocks in the awaitPointerEventScope block
        fun cancel(cause: Throwable?) {
            pointerAwaiter?.cancel(cause)
            pointerAwaiter = null
        }

        // context must be EmptyCoroutineContext for restricted suspension coroutines
        override val context: CoroutineContext = EmptyCoroutineContext

        // Implementation of Continuation; clean up and resume our wrapped continuation.
        override fun resumeWith(result: Result<R>) {
            synchronized(pointerHandlers) {
                pointerHandlers -= this
            }
            completion.resumeWith(result)
        }

        override suspend fun awaitPointerEvent(
            pass: PointerEventPass
        ): PointerEvent = suspendCancellableCoroutine { continuation ->
            awaitPass = pass
            pointerAwaiter = continuation
        }
    }
}