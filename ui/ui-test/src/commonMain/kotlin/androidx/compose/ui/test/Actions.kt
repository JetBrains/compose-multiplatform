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
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties.IndexForKey
import androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.min

internal expect fun SemanticsNodeInteraction.performClickImpl(): SemanticsNodeInteraction

/**
 * Performs a click action on the element represented by the given semantics node. Depending on
 * the platform this may be implemented by a touch click (tap), a mouse click, or another more
 * appropriate method for that platform.
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
 */
fun SemanticsNodeInteraction.performScrollTo(): SemanticsNodeInteraction {
    // Find a parent node with a scroll action
    val errorMessageOnFail = "Action performScrollTo() failed."
    val node = fetchSemanticsNode(errorMessageOnFail)
    val scrollableNode = node.findClosestParentNode {
        hasScrollAction().matches(it)
    } ?: throw AssertionError("Semantic Node has no parent layout with a Scroll SemanticsAction")

    // Figure out the (clipped) bounds of the viewPort in its direct parent's content area, in
    // root coordinates. We only want the clipping from the direct parent on the scrollable, not
    // from any other ancestors.
    val viewPortInParent = scrollableNode.layoutInfo.coordinates.boundsInParent()
    val parentInRoot = scrollableNode.layoutInfo.coordinates.parentLayoutCoordinates
        ?.positionInRoot() ?: Offset.Zero

    val viewPort = viewPortInParent.translate(parentInRoot)
    val target = Rect(node.positionInRoot, node.size.toSize())

    val mustScrollUp = target.bottom > viewPort.bottom
    val mustScrollDown = target.top < viewPort.top
    val mustScrollLeft = target.right > viewPort.right
    val mustScrollRight = target.left < viewPort.left

    val rawDx = if (mustScrollLeft && !mustScrollRight) {
        // scroll left: positive dx
        min(target.left - viewPort.left, target.right - viewPort.right)
    } else if (mustScrollRight && !mustScrollLeft) {
        // scroll right: negative dx
        max(target.left - viewPort.left, target.right - viewPort.right)
    } else {
        // already in viewport
        0f
    }

    val rawDy = if (mustScrollUp && !mustScrollDown) {
        // scroll up: positive dy
        min(target.top - viewPort.top, target.bottom - viewPort.bottom)
    } else if (mustScrollDown && !mustScrollUp) {
        // scroll down: negative dy
        max(target.top - viewPort.top, target.bottom - viewPort.bottom)
    } else {
        // already in viewport
        0f
    }

    val dx = if (scrollableNode.isReversedHorizontally) -rawDx else rawDx
    val dy = if (scrollableNode.isReversedVertically) -rawDy else rawDy

    @OptIn(InternalTestApi::class)
    testContext.testOwner.runOnUiThread {
        scrollableNode.config[SemanticsActions.ScrollBy].action?.invoke(dx, dy)
    }

    return this
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
 * @see hasScrollToIndexAction
 */
fun SemanticsNodeInteraction.performScrollToIndex(index: Int): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed: performScrollToIndex($index)")
    requireSemantics(node, ScrollToIndex) {
        "Failed to scroll to index $index"
    }

    @OptIn(InternalTestApi::class)
    testContext.testOwner.runOnUiThread {
        node.config[ScrollToIndex].action!!.invoke(index)
    }
    return this
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
 * Example usage:
 * ```
 * testRule.onNodeWithTag("myWidget")
 *     .performGesture { swipeUp() }
 *
 * testRule.onNodeWithTag("myWidget")
 *     .performGesture { click(center) }
 *
 * testRule.onNodeWithTag("myWidget")
 *     .performGesture { down(topLeft) }
 *     .assertHasClickAction()
 *     .performGesture { up(topLeft) }
 *
 * testRule.onNodeWithTag("myWidget")
 *     .performGesture { click() }
 * testRule.mainClock.advanceTimeBy(100)
 * testRule.onNodeWithTag("myWidget")
 *     .performGesture(true) { swipeUp() }
 * ```
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
            try {
                inputDispatcher.sendAllSynchronous()
            } finally {
                dispose()
            }
        }
    }
    return this
}

// TODO(fresen): create sample module like in the rest of Compose
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
 * Example usage:
 * ```
 * // Perform a swipe up
 * testRule.onNodeWithTag("myWidget")
 *     .performTouchInput { swipeUp() }
 *
 * // Perform a click off-center
 * testRule.onNodeWithTag("myWidget")
 *     .performTouchInput { click(percentOffset(.2f, .5f) }
 *
 * // Do an assertion while performing a click
 * testRule.onNodeWithTag("myWidget")
 *     .performTouchInput { down(topLeft) }
 *     .assertHasClickAction()
 *     .performTouchInput { up(topLeft) }
 *
 * // Perform a click-and-drag
 * testRule.onNodeWithTag("myWidget").performTouchInput {
 *     click()
 *     advanceEventTime(100)
 *     swipeUp()
 * }
 * ```
 *
 * @see TouchInjectionScope
 */
fun SemanticsNodeInteraction.performTouchInput(
    block: TouchInjectionScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to inject touch input.")
    with(MultiModalInjectionScopeImpl(node, testContext)) {
        try {
            block.invoke(Touch)
        } finally {
            try {
                inputDispatcher.sendAllSynchronous()
            } finally {
                dispose()
            }
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
 * Example usage:
 * ```
 * onNodeWithTag("myWidget")
 *    .performMouseInput {
 *        click(center)
 *    }
 *
 * onNodeWithTag("myWidget")
 *    // Scroll down while the primary mouse button is down:
 *    .performMouseInput {
 *        down()
 *        repeat(6) {
 *            advanceEventTime()
 *            scroll(-1f)
 *        }
 *        advanceEventTime()
 *        up()
 *    }
 * ```
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
            block.invoke(Mouse)
        } finally {
            try {
                inputDispatcher.sendAllSynchronous()
            } finally {
                dispose()
            }
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
 * Each input modality is made available via a property of that modality's scope type, like
 * [Touch][MultiModalInjectionScope.Touch] of type [TouchInjectionScope]. This allows you to
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
 * @see MultiModalInjectionScope
 */
// TODO(fresen): add example of multi-modal input when Keyboard input is added (touch and mouse
//  don't work together, so an example with those two doesn't make sense)
fun SemanticsNodeInteraction.performMultiModalInput(
    block: MultiModalInjectionScope.() -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to inject multi-modal input.")
    with(MultiModalInjectionScopeImpl(node, testContext)) {
        try {
            block.invoke(this)
        } finally {
            try {
                inputDispatcher.sendAllSynchronous()
            } finally {
                dispose()
            }
        }
    }
    return this
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
 *
 * @throws AssertionError If the semantics action is not defined on this node.
 */
fun <T : Function<Boolean>> SemanticsNodeInteraction.performSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<T>>,
    invocation: (T) -> Unit
) {
    val node = fetchSemanticsNode("Failed to perform ${key.name} action.")
    requireSemantics(node, key) {
        "Failed to perform action ${key.name}"
    }

    @OptIn(InternalTestApi::class)
    testContext.testOwner.runOnUiThread {
        node.config[key].action?.let(invocation)
    }
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
 *
 * @throws AssertionError If the semantics action is not defined on this node.
 */
fun SemanticsNodeInteraction.performSemanticsAction(
    key: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
) {
    performSemanticsAction(key) { it.invoke() }
}

private val SemanticsNode.isReversedHorizontally: Boolean
    get() = config.getOrNull(HorizontalScrollAxisRange)?.reverseScrolling == true

private val SemanticsNode.isReversedVertically: Boolean
    get() = config.getOrNull(VerticalScrollAxisRange)?.reverseScrolling == true

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
