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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.ScrollBy
import androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties.IndexForKey
import androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.sign

internal expect fun SemanticsNodeInteraction.performClickImpl(): SemanticsNodeInteraction

/**
 * Performs a click action on the element represented by the given semantics node. Depending on
 * the platform this may be implemented by a touch click (tap), a mouse click, or another more
 * appropriate method for that platform.
 *
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 */
fun SemanticsNodeInteraction.performClick(): SemanticsNodeInteraction {
    return performClickImpl()
}

/**
 * Scrolls the closest enclosing scroll parent by the smallest amount such that this node is fully
 * visible in its viewport. If this node is larger than the viewport, scrolls the scroll parent
 * by the smallest amount such that this node fills the entire viewport. A scroll parent is a
 * parent node that has the semantics action [SemanticsActions.ScrollBy] (usually implemented by
 * defining [scrollBy][androidx.compose.ui.semantics.scrollBy]).
 *
 * This action should be performed on the [node][SemanticsNodeInteraction] that is part of the
 * scrollable content, not on the scrollable container.
 *
 * Throws an [AssertionError] if there is no scroll parent.
 *
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 */
fun SemanticsNodeInteraction.performScrollTo(): SemanticsNodeInteraction {
    @OptIn(InternalTestApi::class)
    fetchSemanticsNode("Action performScrollTo() failed.").scrollToNode(testContext.testOwner)
    return this
}

/**
 * Implementation of [performScrollTo]
 */
@OptIn(InternalTestApi::class)
private fun SemanticsNode.scrollToNode(testOwner: TestOwner) {
    val scrollableNode = findClosestParentNode {
        hasScrollAction().matches(it)
    } ?: throw AssertionError("Semantic Node has no parent layout with a Scroll SemanticsAction")

    // Figure out the (clipped) bounds of the viewPort in its direct parent's content area, in
    // root coordinates. We only want the clipping from the direct parent on the scrollable, not
    // from any other ancestors.
    val viewportInParent = scrollableNode.layoutInfo.coordinates.boundsInParent()
    val parentInRoot = scrollableNode.layoutInfo.coordinates.parentLayoutCoordinates
        ?.positionInRoot() ?: Offset.Zero
    val viewport = viewportInParent.translate(parentInRoot)
    val target = Rect(positionInRoot, size.toSize())

    // Given the desired scroll value to align either side of the target with the
    // viewport, what delta should we go with?
    // If we need to scroll in opposite directions for both sides, don't scroll at all.
    // Otherwise, take the delta that scrolls the least amount.
    fun scrollDelta(a: Float, b: Float): Float =
        if (sign(a) == sign(b)) if (abs(a) < abs(b)) a else b else 0f

    // Get the desired delta X
    var dx = scrollDelta(target.left - viewport.left, target.right - viewport.right)
    // And adjust for reversing properties
    if (scrollableNode.isReversedHorizontally) dx = -dx
    if (scrollableNode.isRtl) dx = -dx

    // Get the desired delta Y
    var dy = scrollDelta(target.top - viewport.top, target.bottom - viewport.bottom)
    // And adjust for reversing properties
    if (scrollableNode.isReversedVertically) dy = -dy

    testOwner.runOnUiThread {
        scrollableNode.config[ScrollBy].action?.invoke(dx, dy)
    }
}

/**
 * Scrolls a scrollable container with items to the item with the given [index].
 *
 * Note that not all scrollable containers have item indices. For example, a
 * [scrollable][androidx.compose.foundation.gestures.scrollable] doesn't have items with an
 * index, while [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] does.
 *
 * This action should be performed on a [node][SemanticsNodeInteraction] that is a scrollable
 * container, not on a node that is part of the content of that container.
 *
 * Throws an [AssertionError] if the node doesn't have [ScrollToIndex] defined.
 *
 * @param index The index of the item to scroll to
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 *
 * @see hasScrollToIndexAction
 */
fun SemanticsNodeInteraction.performScrollToIndex(index: Int): SemanticsNodeInteraction {
    fetchSemanticsNode("Failed: performScrollToIndex($index)").scrollToIndex(index, this)
    return this
}

/**
 * Implementation of [performScrollToIndex]
 */
private fun SemanticsNode.scrollToIndex(index: Int, nodeInteraction: SemanticsNodeInteraction) {
    nodeInteraction.requireSemantics(this, ScrollToIndex) {
        "Failed to scroll to index $index"
    }

    @OptIn(InternalTestApi::class)
    nodeInteraction.testContext.testOwner.runOnUiThread {
        config[ScrollToIndex].action!!.invoke(index)
    }
}

/**
 * Scrolls a scrollable container with keyed items to the item with the given [key], such as
 * [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] or
 * [LazyRow][androidx.compose.foundation.lazy.LazyRow].
 *
 * This action should be performed on a [node][SemanticsNodeInteraction] that is a scrollable
 * container, not on a node that is part of the content of that container.
 *
 * Throws an [AssertionError] if the node doesn't have [IndexForKey] or [ScrollToIndex] defined.
 *
 * @param key The key of the item to scroll to
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 *
 * @see hasScrollToKeyAction
 */
fun SemanticsNodeInteraction.performScrollToKey(key: Any): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed: performScrollToKey(\"$key\")")
    requireSemantics(node, IndexForKey, ScrollToIndex) {
        "Failed to scroll to the item identified by \"$key\""
    }

    val index = node.config[IndexForKey].invoke(key)
    require(index >= 0) {
        "Failed to scroll to the item identified by \"$key\", couldn't find the key."
    }

    @OptIn(InternalTestApi::class)
    testContext.testOwner.runOnUiThread {
        node.config[ScrollToIndex].action!!.invoke(index)
    }

    return this
}

/**
 * Scrolls a scrollable container to the content that matches the given [matcher]. If the content
 * isn't yet visible, the scrollable container will be scrolled from the start till the end till
 * it finds the content we're looking for. It is not defined where in the viewport the content
 * will be on success of this function, but it will be either fully within the viewport if it is
 * smaller than the viewport, or it will cover the whole viewport if it is larger than the
 * viewport. If it doesn't find the content, the scrollable will be left at the end of the
 * content and an [AssertionError] is thrown.
 *
 * This action should be performed on a [node][SemanticsNodeInteraction] that is a scrollable
 * container, not on a node that is part of the content of that container. If the container is a
 * lazy container, it must support the semantics actions [ScrollToIndex], [ScrollBy], and either
 * [HorizontalScrollAxisRange] or [VerticalScrollAxisRange], for example
 * [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] and
 * [LazyRow][androidx.compose.foundation.lazy.LazyRow]. If the container is not lazy, it must
 * support the semantics action [ScrollBy], for example,
 * [Row][androidx.compose.foundation.layout.Row] or
 * [Column][androidx.compose.foundation.layout.Column].
 *
 * Throws an [AssertionError] if the scrollable node doesn't support the necessary semantics
 * actions.
 *
 * @param matcher A matcher that identifies the content where the scrollable container needs to
 * scroll to
 * @return The [SemanticsNodeInteraction] that is the receiver of this method. Note that this is
 * _not_ an interaction for the node that is identified by the [matcher].
 *
 * @see hasScrollToNodeAction
 */
fun SemanticsNodeInteraction.performScrollToNode(
    matcher: SemanticsMatcher
): SemanticsNodeInteraction {
    var node = fetchSemanticsNode("Failed: performScrollToNode(${matcher.description})")
    matcher.findMatchInDescendants(node)?.also {
        @OptIn(InternalTestApi::class)
        it.scrollToNode(testContext.testOwner)
        return this
    }

    // If this is NOT a lazy list, but we haven't found the node above ..
    if (!node.isLazyList) {
        // .. throw an error that the node doesn't exist
        val msg = "No node found that matches ${matcher.description} in scrollable container"
        throw AssertionError(buildGeneralErrorMessage(msg, selector, node))
    }

    // Go to start of the list
    if (!node.horizontalScrollAxis.isAtStart || !node.verticalScrollAxis.isAtStart) {
        node.scrollToIndex(0, this)
    }

    while (true) {
        // Fetch the node again
        node = fetchSemanticsNode("Failed: performScrollToNode(${matcher.description})")
        matcher.findMatchInDescendants(node)?.also {
            @OptIn(InternalTestApi::class)
            it.scrollToNode(testContext.testOwner)
            return this
        }

        // Are we there yet? Are we there yet? Are we there yet?
        if (node.horizontalScrollAxis.isAtEnd && node.verticalScrollAxis.isAtEnd) {
            // If we're finished and we haven't found the node
            val msg = "No node found that matches ${matcher.description} in scrollable container"
            throw AssertionError(buildGeneralErrorMessage(msg, selector, node))
        }

        val viewPortSize = node.layoutInfo.coordinates.boundsInParent().size
        val dx = node.horizontalScrollAxis?.let { viewPortSize.width } ?: 0f
        val dy = node.verticalScrollAxis?.let { viewPortSize.height } ?: 0f

        // Scroll one screen
        @OptIn(InternalTestApi::class)
        testContext.testOwner.runOnUiThread {
            node.config[ScrollBy].action?.invoke(dx, dy)
        }
    }
}

/**
 * Executes the (partial) gesture specified in the given [block]. The gesture doesn't need to be
 * complete and can be resumed in a later invocation of [performGesture]. The event time is
 * initialized to the current time of the [MainTestClock].
 *
 * Be aware that if you split a gesture over multiple invocations of [performGesture], everything
 * that happens in between will run as if the gesture is still ongoing (imagine a finger still
 * touching the screen).
 *
 * All events that are injected from the [block] are batched together and sent after [block] is
 * complete. This method blocks while the events are injected. If an error occurs during
 * execution of [block] or injection of the events, all (subsequent) events are dropped and the
 * error is thrown here.
 *
 * Due to the batching of events, all events in a block are sent together and no recomposition will
 * take place in between events. Additionally all events will be generated before any of the events
 * take effect. This means that the screen coordinates of all events are resolved before any of
 * the events can cause the position of the node being injected into to change. This has certain
 * advantages, for example, in the cases of nested scrolling or dragging an element around, it
 * prevents the injection of events into a moving target since all events are enqueued before any
 * of them has taken effect.
 *
 * Example of performing a click:
 * @sample androidx.compose.ui.test.samples.gestureClick
 *
 * @param block A lambda with [GestureScope] as receiver that describes the gesture by
 * sending all touch events.
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 */
@Deprecated(
    message = "Replaced by performTouchInput",
    replaceWith = ReplaceWith(
        "performTouchInput(block)",
        "import androidx.compose.ui.test.performGesture"
    )
)
@Suppress("DEPRECATION")
fun SemanticsNodeInteraction.performGesture(
    block: GestureScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to perform a gesture.")
    with(GestureScope(node, testContext)) {
        try {
            block()
        } finally {
            dispose()
        }
    }
    return this
}

/**
 * Executes the touch gesture specified in the given [block]. The gesture doesn't need to be
 * complete and can be resumed in a later invocation of one of the `perform.*Input` methods. The
 * event time is initialized to the current time of the [MainTestClock].
 *
 * Be aware that if you split a gesture over multiple invocations of `perform.*Input`, everything
 * that happens in between will run as if the gesture is still ongoing (imagine a finger still
 * touching the screen).
 *
 * All events that are injected from the [block] are batched together and sent after [block] is
 * complete. This method blocks while the events are injected. If an error occurs during
 * execution of [block] or injection of the events, all (subsequent) events are dropped and the
 * error is thrown here.
 *
 * Due to the batching of events, all events in a block are sent together and no recomposition will
 * take place in between events. Additionally all events will be generated before any of the events
 * take effect. This means that the screen coordinates of all events are resolved before any of
 * the events can cause the position of the node being injected into to change. This has certain
 * advantages, for example, in the cases of nested scrolling or dragging an element around, it
 * prevents the injection of events into a moving target since all events are enqueued before any
 * of them has taken effect.
 *
 * Example of performing a swipe up:
 * @sample androidx.compose.ui.test.samples.touchInputSwipeUp
 *
 * Example of performing an off-center click:
 * @sample androidx.compose.ui.test.samples.touchInputClickOffCenter
 *
 * Example of doing an assertion during a click:
 * @sample androidx.compose.ui.test.samples.touchInputAssertDuringClick
 *
 * Example of performing a click-and-drag:
 * @sample androidx.compose.ui.test.samples.touchInputClickAndDrag
 *
 * @param block A lambda with [TouchInjectionScope] as receiver that describes the gesture by
 * sending all touch events.
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 *
 * @see TouchInjectionScope
 */
fun SemanticsNodeInteraction.performTouchInput(
    block: TouchInjectionScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to inject touch input.")
    with(MultiModalInjectionScopeImpl(node, testContext)) {
        try {
            touch(block)
        } finally {
            dispose()
        }
    }
    return this
}

/**
 * Executes the mouse gesture specified in the given [block]. The gesture doesn't need to be
 * complete and can be resumed in a later invocation of one of the `perform.*Input` methods. The
 * event time is initialized to the current time of the [MainTestClock].
 *
 * Be aware that if you split a gesture over multiple invocations of `perform.*Input`, everything
 * that happens in between will run as if the gesture is still ongoing (imagine a mouse button
 * still being pressed).
 *
 * All events that are injected from the [block] are batched together and sent after [block] is
 * complete. This method blocks while the events are injected. If an error occurs during
 * execution of [block] or injection of the events, all (subsequent) events are dropped and the
 * error is thrown here.
 *
 * Due to the batching of events, all events in a block are sent together and no recomposition will
 * take place in between events. Additionally all events will be generated before any of the events
 * take effect. This means that the screen coordinates of all events are resolved before any of
 * the events can cause the position of the node being injected into to change. This has certain
 * advantages, for example, in the cases of nested scrolling or dragging an element around, it
 * prevents the injection of events into a moving target since all events are enqueued before any
 * of them has taken effect.
 *
 * Example of performing a mouse click:
 * @sample androidx.compose.ui.test.samples.mouseInputClick
 *
 * Example of scrolling the mouse wheel while the mouse button is pressed:
 * @sample androidx.compose.ui.test.samples.mouseInputScrollWhileDown
 *
 * @param block A lambda with [MouseInjectionScope] as receiver that describes the gesture by
 * sending all mouse events.
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 *
 * @see MouseInjectionScope
 */
@ExperimentalTestApi
fun SemanticsNodeInteraction.performMouseInput(
    block: MouseInjectionScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to inject mouse input.")
    with(MultiModalInjectionScopeImpl(node, testContext)) {
        try {
            mouse(block)
        } finally {
            dispose()
        }
    }
    return this
}

/**
 * Executes the key input gesture specified in the given [block]. The gesture doesn't need to be
 * complete and can be resumed in a later invocation of one of the `perform.*Input` methods. The
 * event time is initialized to the current time of the [MainTestClock].
 *
 * All events that are injected from the [block] are batched together and sent after [block] is
 * complete. This method blocks while the events are injected. If an error occurs during
 * execution of [block] or injection of the events, all (subsequent) events are dropped and the
 * error is thrown here.
 *
 * Due to the batching of events, all events in a block are sent together and no recomposition will
 * take place in between events. Additionally all events will be generated before any of the events
 * take effect. This means that the screen coordinates of all events are resolved before any of
 * the events can cause the position of the node being injected into to change. This has certain
 * advantages, for example, in the cases of nested scrolling or dragging an element around, it
 * prevents the injection of events into a moving target since all events are enqueued before any
 * of them has taken effect.
 *
 * @param block A lambda with [KeyInjectionScope] as receiver that describes the gesture by
 * sending all key press events.
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 *
 * @see KeyInjectionScope
 */
@ExperimentalTestApi
fun SemanticsNodeInteraction.performKeyInput(
    block: KeyInjectionScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to inject key input.")
    with(MultiModalInjectionScopeImpl(node, testContext)) {
        try {
            key(block)
        } finally {
            dispose()
        }
    }
    return this
}

/**
 * Executes the multi-modal gesture specified in the given [block]. The gesture doesn't need to be
 * complete and can be resumed in a later invocation of one of the `perform.*Input` methods. The
 * event time is initialized to the current time of the [MainTestClock]. If only a single
 * modality is needed (e.g. touch, mouse, stylus, keyboard, etc), you should use the
 * `perform.*Input` of that modality instead.
 *
 * Functions for each modality can be called by invoking that modality's function, like
 * [touch][MultiModalInjectionScope.touch] to inject touch events. This allows you to
 * inject events for each modality.
 *
 * Be aware that if you split a gesture over multiple invocations of `perform.*Input`, everything
 * that happens in between will run as if the gesture is still ongoing (imagine a finger still
 * touching the screen).
 *
 * All events that are injected from the [block] are batched together and sent after [block] is
 * complete. This method blocks while the events are injected. If an error occurs during
 * execution of [block] or injection of the events, all (subsequent) events are dropped and the
 * error is thrown here.
 *
 * Due to the batching of events, all events in a block are sent together and no recomposition will
 * take place in between events. Additionally all events will be generated before any of the events
 * take effect. This means that the screen coordinates of all events are resolved before any of
 * the events can cause the position of the node being injected into to change. This has certain
 * advantages, for example, in the cases of nested scrolling or dragging an element around, it
 * prevents the injection of events into a moving target since all events are enqueued before any
 * of them has taken effect.
 *
 * @param block A lambda with [MultiModalInjectionScope] as receiver that describes the gesture
 * by sending all multi modal events.
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 *
 * @see MultiModalInjectionScope
 */
// TODO(fresen): add example of multi-modal input when key input is added (touch and mouse
//  don't work together, so an example with those two doesn't make sense)
fun SemanticsNodeInteraction.performMultiModalInput(
    block: MultiModalInjectionScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to inject multi-modal input.")
    with(MultiModalInjectionScopeImpl(node, testContext)) {
        try {
            block.invoke(this)
        } finally {
            dispose()
        }
    }
    return this
}

@Deprecated(
    message = "Replaced with same function, but with SemanticsNodeInteraction as return type",
    level = DeprecationLevel.HIDDEN
)
@Suppress("unused")
@JvmName("performSemanticsAction")
fun <T : Function<Boolean>> SemanticsNodeInteraction.performSemanticsActionUnit(
    key: SemanticsPropertyKey<AccessibilityAction<T>>,
    invocation: (T) -> Unit
) {
    performSemanticsAction(key, invocation)
}

/**
 * Provides support to call custom semantics actions on this node.
 *
 * This method is supposed to be used for actions with parameters.
 *
 * This will properly verify that the actions exists and provide clear error message in case it
 * does not. It also handle synchronization and performing the action on the UI thread. This call
 * is blocking until the action is performed
 *
 * @param key Key of the action to be performed.
 * @param invocation Place where you call your action. In the argument is provided the underlying
 * action from the given Semantics action.
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 *
 * @throws AssertionError If the semantics action is not defined on this node.
 */
fun <T : Function<Boolean>> SemanticsNodeInteraction.performSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<T>>,
    invocation: (T) -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to perform ${key.name} action.")
    requireSemantics(node, key) {
        "Failed to perform action ${key.name}"
    }

    @OptIn(InternalTestApi::class)
    testContext.testOwner.runOnUiThread {
        node.config[key].action?.let(invocation)
    }

    return this
}

@Deprecated(
    message = "Replaced with same function, but with SemanticsNodeInteraction as return type",
    level = DeprecationLevel.HIDDEN
)
@Suppress("unused")
@JvmName("performSemanticsAction")
fun SemanticsNodeInteraction.performSemanticsActionUnit(
    key: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
) {
    performSemanticsAction(key)
}

/**
 * Provides support to call custom semantics actions on this node.
 *
 * This method is for calling actions that have no parameters.
 *
 * This will properly verify that the actions exists and provide clear error message in case it
 * does not. It also handle synchronization and performing the action on the UI thread. This call
 * is blocking until the action is performed
 *
 * @param key Key of the action to be performed.
 * @return The [SemanticsNodeInteraction] that is the receiver of this method
 *
 * @throws AssertionError If the semantics action is not defined on this node.
 */
fun SemanticsNodeInteraction.performSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
): SemanticsNodeInteraction {
    return performSemanticsAction(key) { it.invoke() }
}

/**
 * Send the specified [RotaryScrollEvent] to the focused component.
 *
 * @return true if the event was consumed. False otherwise.
 */
@ExperimentalTestApi
fun SemanticsNodeInteraction.performRotaryScrollInput(
    block: RotaryInjectionScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to send rotary Event")
    with(MultiModalInjectionScopeImpl(node, testContext)) {
        try {
            rotary(block)
        } finally {
            dispose()
        }
    }
    return this
}

// TODO(200928505): get a more accurate indication if it is a lazy list
private val SemanticsNode.isLazyList: Boolean
    get() = ScrollBy in config && ScrollToIndex in config

private val SemanticsNode.horizontalScrollAxis: ScrollAxisRange?
    get() = config.getOrNull(HorizontalScrollAxisRange)

private val SemanticsNode.verticalScrollAxis: ScrollAxisRange?
    get() = config.getOrNull(VerticalScrollAxisRange)

private val SemanticsNode.isReversedHorizontally: Boolean
    get() = horizontalScrollAxis?.reverseScrolling ?: false

private val SemanticsNode.isReversedVertically: Boolean
    get() = verticalScrollAxis?.reverseScrolling ?: false

private val ScrollAxisRange?.isAtStart: Boolean
    get() = this?.let { value() == 0f } ?: true

private val ScrollAxisRange?.isAtEnd: Boolean
    get() = this?.let { value() == maxValue() } ?: true

private val SemanticsNode.isRtl: Boolean
    get() = layoutInfo.layoutDirection == LayoutDirection.Rtl

private fun SemanticsNodeInteraction.requireSemantics(
    node: SemanticsNode,
    vararg properties: SemanticsPropertyKey<*>,
    errorMessage: () -> String
) {
    val missingProperties = properties.filter { it !in node.config }
    if (missingProperties.isNotEmpty()) {
        val msg = "${errorMessage()}, the node is missing [${missingProperties.joinToString()}]"
        throw AssertionError(buildGeneralErrorMessage(msg, selector, node))
    }
}

@Suppress("NOTHING_TO_INLINE") // Avoids doubling the stack depth for recursive search
private inline fun SemanticsMatcher.findMatchInDescendants(root: SemanticsNode): SemanticsNode? {
    return root.children.firstOrNull { it.layoutInfo.isPlaced && findMatchInHierarchy(it) != null }
}

private fun SemanticsMatcher.findMatchInHierarchy(node: SemanticsNode): SemanticsNode? {
    return if (matches(node)) node else findMatchInDescendants(node)
}
