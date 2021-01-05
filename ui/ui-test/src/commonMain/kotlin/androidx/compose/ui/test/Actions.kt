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
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.min

/**
 * Performs a click action on the element represented by the given semantics node.
 */
fun SemanticsNodeInteraction.performClick(): SemanticsNodeInteraction {
    // TODO(jellefresen): Replace with semantics action when semantics merging is done
    // The problem we currently have is that the click action might be defined on a different
    // semantics node than we're interacting with now, even though it is "semantically" the same.
    // E.g., findByText(buttonText) finds the Text's semantics node, but the click action is
    // defined on the wrapping Button's semantics node.
    // Since in general the intended click action can be on a wrapping node or a child node, we
    // can't just forward to the correct node, as we don't know if we should search up or down the
    // tree.
    return performGesture {
        click()
    }
}

/**
 * Scrolls the closest enclosing scroll parent by the smallest amount such that this node is fully
 * visible in its viewport. If this node is larger than the viewport, scrolls the scroll parent
 * by the smallest amount such that this node fills the entire viewport. A scroll parent is a
 * parent node that has the semantics action [SemanticsActions.ScrollBy] (usually implemented by
 * defining [scrollBy][androidx.compose.ui.semantics.scrollBy]).
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
    val viewPortInParent = scrollableNode.layoutInfo.coordinates.boundsInParent
    val parentInRoot = scrollableNode.layoutInfo.coordinates.parentCoordinates
        ?.positionInRoot ?: Offset.Zero

    val viewPort = viewPortInParent.translate(parentInRoot)
    val target = Rect(node.positionInRoot, node.size.toSize())

    val mustScrollUp = target.bottom > viewPort.bottom
    val mustScrollDown = target.top < viewPort.top
    val mustScrollLeft = target.right > viewPort.right
    val mustScrollRight = target.left < viewPort.left

    val dx = if (mustScrollLeft && !mustScrollRight) {
        // scroll left: positive dx
        min(target.left - viewPort.left, target.right - viewPort.right)
    } else if (mustScrollRight && !mustScrollLeft) {
        // scroll right: negative dx
        max(target.left - viewPort.left, target.right - viewPort.right)
    } else {
        // already in viewport
        0f
    }

    val dy = if (mustScrollUp && !mustScrollDown) {
        // scroll up: positive dy
        min(target.top - viewPort.top, target.bottom - viewPort.bottom)
    } else if (mustScrollDown && !mustScrollUp) {
        // scroll down: negative dy
        max(target.top - viewPort.top, target.bottom - viewPort.bottom)
    } else {
        // already in viewport
        0f
    }

    @OptIn(InternalTestApi::class)
    testContext.testOwner.runOnUiThread {
        scrollableNode.config[SemanticsActions.ScrollBy].action(dx, dy)
    }

    return this
}

/**
 * Executes the (partial) gesture specified in the given [block]. The gesture doesn't need to be
 * complete and can be resumed in a later invocation of [performGesture]. It is the
 * responsibility of the caller to make sure partial gestures don't leave the test in an
 * inconsistent state.
 *
 * All events that are injected from the [block] are batched together and sent after [block] is
 * complete. This method blocks until all those events have been injected, which normally takes
 * as long as the duration of the gesture. If an error occurs during execution of [block] or
 * injection of the events, all (subsequent) events are dropped and the error is thrown here.
 *
 * This method must not be called from the main thread. The block will be executed on the same
 * thread as the caller.
 *
 * Example usage:
 * ```
 * onNodeWithTag("myWidget")
 *     .performGesture { swipeUp() }
 *
 * onNodeWithTag("myWidget")
 *     .performGesture { click(center) }
 *
 * onNodeWithTag("myWidget")
 *     .performGesture { down(topLeft) }
 *     .assertHasClickAction()
 *     .performGesture { up(topLeft) }
 * ```
 */
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
    if (key !in node.config) {
        throw AssertionError(
            buildGeneralErrorMessage(
                "Failed to perform ${key.name} action as it is not defined on the node.",
                selector, node
            )
        )
    }

    @Suppress("DEPRECATION")
    @OptIn(InternalTestApi::class)
    testContext.testOwner.runOnUiThread {
        invocation(node.config[key].action)
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
