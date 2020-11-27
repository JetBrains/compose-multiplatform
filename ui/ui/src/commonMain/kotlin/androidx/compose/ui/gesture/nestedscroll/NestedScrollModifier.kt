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

package androidx.compose.ui.gesture.nestedscroll

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity

/**
 * A [Modifier.Element] that represents nested scroll node in the hierarchy
 */
internal interface NestedScrollModifier : Modifier.Element {

    /**
     * Nested scroll events dispatcher to notify nested scrolling system about scroll events.
     * This is to be used by the nodes that are scrollable themselves to notify
     * [NestedScrollConnection]s in the tree.
     *
     * Note: The [connection] passed to the [NestedScrollModifier] doesn't count as an ancestor
     * since it's the node itself
     */
    val dispatcher: NestedScrollDispatcher

    /**
     * Nested scroll connection to participate in the nested scroll events chain. Implementing
     * this connection allows to react on the nested scroll related events and influence
     * scrolling descendants and ascendants
     */
    val connection: NestedScrollConnection
}

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
    fun onPreFling(available: Velocity): Velocity = Velocity.Zero

    /**
     * Post fling event chain. Called by the child when it is finished flinging (and sending
     * [onPreScroll] & [onPostScroll] events)
     *
     * @param consumed the amount of velocity consumed by the child
     * @param available the amount of velocity left for a parent to fling after the child (if
     * desired)
     * @param onFinished callback to be called when this connection finished flinging, to
     * be called with the amount of velocity consumed by the fling operation. This callback is
     * crucial to be called in order to ensure nodes above will receive their [onPostFling].
     */
    // TODO: remove notifySelfFinish when b/174485541
    fun onPostFling(
        consumed: Velocity,
        available: Velocity,
        onFinished: (Velocity) -> Unit
    ) {
        onFinished(Velocity.Zero)
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
     * Dispatch pre fling pass. This triggers [NestedScrollConnection.onPreFling] on all the
     * ancestors giving them a possibility to react on the fling that is about to happen and
     * consume part of the velocity.
     *
     * @param available velocity from the scroll evens that this node is about to fling with
     *
     * @return total velocity that is pre-consumed by all ancestors in the chain. This velocity is
     * unavailable for this node to consume, so it should adjust the consumption accordingly
     */
    fun dispatchPreFling(available: Velocity): Velocity {
        return parent?.onPreFling(available) ?: Velocity.Zero
    }

    /**
     * Dispatch post fling pass. This triggers [NestedScrollConnection.onPostFling] on all the
     * ancestors, giving them possibility to react of the velocity that is left after the
     * dispatching node itself flung with the desired amount.
     *
     * @param consumed velocity already consumed by this node
     * @param available velocity that is left for ancestors to consume
     */
    fun dispatchPostFling(consumed: Velocity, available: Velocity) {
        parent?.onPostFling(consumed, available) {}
    }
}

/**
 * Possible sources of scroll events in the [NestedScrollConnection]
 */
enum class NestedScrollSource {
    /**
     * Dragging via mouse/touch/etc events
     */
    Drag,

    /**
     * Flinging after the drag has ended with velocity
     */
    Fling
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
 * Note: It is recommended to reuse [NestedScrollConnection] and [NestedScrollDispatcher] objects
 * between recompositions since different object will cause nested scroll graph to be
 * recalculated unnecessary.
 *
 * There are 4 main passes in nested scrolling system:
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
 * Example of the nested scrolling interaction where component both dispatches and consumed
 * children's delta:
 * @sample androidx.compose.ui.samples.NestedScrollSample
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
    // provide noop dispatcher if needed
    val resolvedDispatcher = dispatcher ?: remember { NestedScrollDispatcher() }
    remember(connection, resolvedDispatcher) {
        object : NestedScrollModifier {
            override val dispatcher: NestedScrollDispatcher = resolvedDispatcher
            override val connection: NestedScrollConnection = connection
        }
    }
}