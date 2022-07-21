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

package androidx.compose.ui.input.nestedscroll

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * Interface to connect to the nested scroll system.
 *
 * Pass this connection to the [nestedScroll] modifier to participate in the nested scroll
 * hierarchy and to receive nested scroll events when they are dispatched by the scrolling child
 * (scrolling child - the element that actually receives scrolling events and dispatches them via
 * [NestedScrollDispatcher]).
 *
 * @see NestedScrollDispatcher to learn how to dispatch nested scroll events to become a
 * scrolling child
 * @see nestedScroll to attach this connection to the nested scroll system
 */
@JvmDefaultWithCompatibility
interface NestedScrollConnection {

    /**
     * Pre scroll event chain. Called by children to allow parents to consume a portion of a drag
     * event beforehand
     *
     * @param available the delta available to consume for pre scroll
     * @param source the source of the scroll event
     *
     * @see NestedScrollSource
     *
     * @return the amount this connection consumed
     */
    fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero

    /**
     * Post scroll event pass. This pass occurs when the dispatching (scrolling) descendant made
     * their consumption and notifies ancestors with what's left for them to consume.
     *
     * @param consumed the amount that was consumed by all nested scroll nodes below the hierarchy
     * @param available the amount of delta available for this connection to consume
     * @param source source of the scroll
     *
     * @see NestedScrollSource
     *
     * @return the amount that was consumed by this connection
     */
    fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = Offset.Zero

    /**
     * Pre fling event chain. Called by children when they are about to perform fling to
     * allow parents to intercept and consume part of the initial velocity
     *
     * @param available the velocity which is available to pre consume and with which the child
     * is about to fling
     *
     * @return the amount this connection wants to consume and take from the child
     */
    suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero

    /**
     * Post fling event chain. Called by the child when it is finished flinging (and sending
     * [onPreScroll] & [onPostScroll] events)
     *
     * @param consumed the amount of velocity consumed by the child
     * @param available the amount of velocity left for a parent to fling after the child (if
     * desired)
     * @return the amount of velocity consumed by the fling operation in this connection
     */
    suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return Velocity.Zero
    }
}

/**
 * Nested scroll events dispatcher to notify the nested scroll system about the scrolling events
 * that are happening on the element.
 *
 * If the element/modifier itself is able to receive scroll events (from the touch, fling,
 * mouse, etc) and it would like to respect nested scrolling by notifying elements above, it should
 * properly dispatch nested scroll events when being scrolled
 *
 * It is important to dispatch these events at the right time, provide valid information to the
 * parents and react to the feedback received from them in order to provide good user experience
 * with other nested scrolling nodes.
 *
 * @see nestedScroll for the reference of the nested scroll process and more details
 * @see NestedScrollConnection to connect to the nested scroll system
 */
class NestedScrollDispatcher {

    // lambda to calculate the most outer nested scroll scope for this dispatcher on demand
    internal var calculateNestedScrollScope: () -> CoroutineScope? = { originNestedScrollScope }

    // the original nested scroll scope for this dispatcher (immediate scope it was created in)
    internal var originNestedScrollScope: CoroutineScope? = null

    /**
     * Get the outer coroutine scope to dispatch nested fling on.
     *
     * There might be situations when then component that is dispatching preFling or postFling to
     * parent can be disposed together with its scope, so it's recommended to use launch nested
     * fling dispatch using this scope to prevent abrupt scrolling user experience.
     *
     * **Note:** this scope is retrieved from the parent nestedScroll participants, unless the node
     * knows its parent (which is usually after first composition commits), this will throw
     * [IllegalStateException].
     *
     * @throws IllegalStateException when this field is accessed before the [nestedScroll] modifier
     * with this [NestedScrollDispatcher] provided knows its nested scroll parent. Should be safe
     * to access after the initial composition commits.
     */
    val coroutineScope: CoroutineScope
        /**
         * @throws IllegalStateException when this field is accessed before the [nestedScroll] modifier
         * with this [NestedScrollDispatcher] provided knows its nested scroll parent. Should be safe
         * to access after the initial composition commits.
         */
        get() = calculateNestedScrollScope.invoke() ?: throw IllegalStateException(
            "in order to access nested coroutine scope you need to attach dispatcher to the " +
                "`Modifier.nestedScroll` first."
        )

    /**
     * Parent to be set when attached to nested scrolling chain. `null` is valid and means there no
     * nested scrolling parent above
     */
    internal var parent: NestedScrollConnection? = null

    /**
     * Dispatch pre scroll pass. This triggers [NestedScrollConnection.onPreScroll] on all the
     * ancestors giving them possibility to pre-consume delta if they desire so.
     *
     * @param available the delta arrived from a scroll event
     * @param source the source of the scroll event
     *
     * @return total delta that is pre-consumed by all ancestors in the chain. This delta is
     * unavailable for this node to consume, so it should adjust the consumption accordingly
     */
    fun dispatchPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return parent?.onPreScroll(available, source) ?: Offset.Zero
    }

    /**
     * Dispatch nested post-scrolling pass. This triggers [NestedScrollConnection.onPostScroll] on
     * all the ancestors giving them possibility to react of the scroll deltas that are left
     * after the dispatching node itself and other [NestedScrollConnection]s below consumed the
     * desired amount.
     *
     * @param consumed the amount that this node consumed already
     * @param available the amount of delta left for ancestors
     * @param source source of the scroll
     *
     * @return the amount of scroll that was consumed by all ancestors
     */
    fun dispatchPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return parent?.onPostScroll(consumed, available, source) ?: Offset.Zero
    }

    /**
     * Dispatch pre fling pass and suspend until all the interested participants performed
     * velocity pre consumption. This triggers [NestedScrollConnection.onPreFling] on all the
     * ancestors giving them a possibility to react on the fling that is about to happen and
     * consume part of the velocity.
     *
     * @param available velocity from the scroll evens that this node is about to fling with
     *
     * @return total velocity that is pre-consumed by all ancestors in the chain. This velocity is
     * unavailable for this node to consume, so it should adjust the consumption accordingly
     */
    suspend fun dispatchPreFling(available: Velocity): Velocity {
        return parent?.onPreFling(available) ?: Velocity.Zero
    }

    /**
     * Dispatch post fling pass and suspend until all the interested participants performed
     * velocity process. This triggers [NestedScrollConnection.onPostFling] on all the ancestors,
     * giving them possibility to react of the velocity that is left after the dispatching node
     * itself flung with the desired amount.
     *
     * @param consumed velocity already consumed by this node
     * @param available velocity that is left for ancestors to consume
     *
     * @return velocity that has been consumed by all the ancestors
     */
    suspend fun dispatchPostFling(consumed: Velocity, available: Velocity): Velocity {
        return parent?.onPostFling(consumed, available) ?: Velocity.Zero
    }
}

/**
 * Possible sources of scroll events in the [NestedScrollConnection]
 */
@kotlin.jvm.JvmInline
value class NestedScrollSource internal constructor(
    @Suppress("unused") private val value: Int
) {
    override fun toString(): String {
        @Suppress("DEPRECATION")
        return when (this) {
            Drag -> "Drag"
            Fling -> "Fling"
            @OptIn(ExperimentalComposeUiApi::class)
            Relocate -> "Relocate"
            else -> "Invalid"
        }
    }

    companion object {
        /**
         * Dragging via mouse/touch/etc events.
         */
        val Drag: NestedScrollSource = NestedScrollSource(1)

        /**
         * Flinging after the drag has ended with velocity.
         */
        val Fling: NestedScrollSource = NestedScrollSource(2)

        /**
         * Relocating when a component asks parents to scroll to bring it into view.
         */
        @ExperimentalComposeUiApi
        @Deprecated("Do not use. Will be removed in the future.")
        val Relocate: NestedScrollSource = NestedScrollSource(3)
    }
}

/**
 * Modify element to make it participate in the nested scrolling hierarchy.
 *
 * There are two ways to participate in the nested scroll: as a scrolling child by dispatching
 * scrolling events via [NestedScrollDispatcher] to the nested scroll chain; and as a member of
 * nested scroll chain by providing [NestedScrollConnection], which will be called when another
 * nested scrolling child below dispatches scrolling events.
 *
 * It's a mandatory to participate as a [NestedScrollConnection] in the chain, but scrolling
 * events dispatch is optional since there are cases when element wants to participate in the
 * nested scroll, but not a scrollable thing itself.
 *
 * Here's the collapsing toolbar example that participates in a chain, but doesn't dispatch:
 * @sample androidx.compose.ui.samples.NestedScrollConnectionSample
 *
 * On the other side, dispatch via [NestedScrollDispatcher] is optional. It's needed if a component
 * is able to receive and react to the drag/fling events and you want this components to be able to
 * notify parents when scroll occurs, resulting in better overall coordination.
 *
 * Here's the example of the component that is draggable and dispatches nested scroll to
 * participate in the nested scroll chain:
 * @sample androidx.compose.ui.samples.NestedScrollDispatcherSample
 *
 * **Note:** It is recommended to reuse [NestedScrollConnection] and [NestedScrollDispatcher]
 * objects
 * between recompositions since different object will cause nested scroll graph to be
 * recalculated unnecessary.
 *
 * There are 4 main phases in nested scrolling system:
 *
 * 1. Pre-scroll. This callback is triggered when the descendant is about to perform a scroll
 * operation and gives parent an opportunity to consume part of child's delta beforehand. This
 * pass should happen every time scrollable components receives delta and dispatches it via
 * [NestedScrollDispatcher]. Dispatching child should take into account how much all ancestors
 * above the hierarchy consumed and adjust the consumption accordingly.
 *
 * 2. Post-scroll. This callback is triggered when the descendant consumed the delta already
 * (after taking into account what parents pre-consumed in 1.) and wants to notify the ancestors
 * with the amount of delta unconsumed. This pass should happen every time scrollable components
 * receives delta and dispatches it via [NestedScrollDispatcher]. Any parent that receives
 * [NestedScrollConnection.onPostScroll] should consume no more than `left` and return the amount
 * consumed.
 *
 * 3. Pre-fling. Pass that happens when the scrolling descendant stopped dragging and about to
 * fling with the some velocity. This callback allows ancestors to consume part of the velocity.
 * This pass should happen before the fling itself happens. Similar to pre-scroll, parent can
 * consume part of the velocity and nodes below (including the dispatching child) should adjust
 * their logic to accommodate only the velocity left.
 *
 * 4. Post-fling. Pass that happens after the scrolling descendant stopped flinging and wants to
 * notify ancestors about that fact, providing velocity left to consume as a part of this. This
 * pass should happen after the fling itself happens on the scrolling child. Ancestors of the
 * dispatching node will have opportunity to fling themselves with the `velocityLeft` provided.
 * Parent must call `notifySelfFinish` callback in order to continue the propagation of the
 * velocity that is left to ancestors above.
 *
 * [androidx.compose.foundation.lazy.LazyColumn], [androidx.compose.foundation.verticalScroll] and
 * [androidx.compose.foundation.gestures.scrollable] have build in support for nested scrolling,
 * however, it's desirable to be able to react and influence their scroll via nested scroll system.
 *
 * **Note:** The nested scroll system is orientation independent. This mean it is based off the
 * screen direction (x and y coordinates) rather than being locked to a specific orientation.
 *
 * @param connection connection to the nested scroll system to participate in the event chaining,
 * receiving events when scrollable descendant is being scrolled.
 * @param dispatcher object to be attached to the nested scroll system on which `dispatch*`
 * methods can be called to notify ancestors within nested scroll system about scrolling happening
 */
fun Modifier.nestedScroll(
    connection: NestedScrollConnection,
    dispatcher: NestedScrollDispatcher? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "nestedScroll"
        properties["connection"] = connection
        properties["dispatcher"] = dispatcher
    }
) {
    val scope = rememberCoroutineScope()
    // provide noop dispatcher if needed
    val resolvedDispatcher = dispatcher ?: remember { NestedScrollDispatcher() }
    remember(connection, resolvedDispatcher, scope) {
        resolvedDispatcher.originNestedScrollScope = scope
        NestedScrollModifierLocal(resolvedDispatcher, connection)
    }
}