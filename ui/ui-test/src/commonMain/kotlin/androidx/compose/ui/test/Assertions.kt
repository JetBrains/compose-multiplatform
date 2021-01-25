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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties

/**
 * Asserts that the current semantics node has hidden property set to true. A hidden node is a
 * node that is not visible for accessibility. It will still be shown, but it will be skipped by
 * accessibility services.
 *
 * Note that this does not verify parents of the node. For stronger guarantees of visibility
 * see [assertIsNotDisplayed]. If you want to assert that the node is not even in the hierarchy
 * use [SemanticsNodeInteraction.assertDoesNotExist].
 *
 * Throws [AssertionError] if the node is not hidden.
 */
@Deprecated("SemanticsMatcher.assertIsHidden is deprecated without a replacement.")
@Suppress("DEPRECATION")
fun SemanticsNodeInteraction.assertIsHidden(): SemanticsNodeInteraction = assert(isHidden())

/**
 * Asserts that the current semantics node has hidden property set to false.
 *
 * Note that this does not verify parents of the node. For stronger guarantees of visibility
 * see [assertIsDisplayed]. If you only want to assert that the node is in the hierarchy use
 * [SemanticsNodeInteraction.assertExists]
 *
 * Throws [AssertionError] if the node is hidden.
 */
@Deprecated("SemanticsMatcher.assertIsNotHidden is deprecated without a replacement.")
@Suppress("DEPRECATION")
fun SemanticsNodeInteraction.assertIsNotHidden(): SemanticsNodeInteraction = assert(isNotHidden())

/**
 * Asserts that the current semantics node is displayed on screen.
 *
 * Throws [AssertionError] if the node is not displayed.
 */
fun SemanticsNodeInteraction.assertIsDisplayed(): SemanticsNodeInteraction {
    // TODO(b/143607231): check semantics hidden property
    // TODO(b/143608742): check the correct AndroidCraneView is visible

    if (!checkIsDisplayed()) {
        // TODO(b/133217292)
        throw AssertionError("Assert failed: The component is not displayed!")
    }
    return this
}

/**
 * Asserts that the current semantics node is not displayed on screen.
 *
 * Throws [AssertionError] if the node is displayed.
 */
fun SemanticsNodeInteraction.assertIsNotDisplayed(): SemanticsNodeInteraction {
    // TODO(b/143607231): check semantics hidden property
    // TODO(b/143608742): check no AndroidCraneView contains the given component

    if (checkIsDisplayed()) {
        // TODO(b/133217292)
        throw AssertionError("Assert failed: The component is displayed!")
    }
    return this
}

/**
 * Asserts that the current semantics node is enabled.
 *
 * Throws [AssertionError] if the node is not enabled or does not define the property at all.
 */
fun SemanticsNodeInteraction.assertIsEnabled(): SemanticsNodeInteraction = assert(isEnabled())

/**
 * Asserts that the current semantics node is not enabled.
 *
 * Throws [AssertionError] if the node is enabled or does not defined the property at all.
 */
fun SemanticsNodeInteraction.assertIsNotEnabled(): SemanticsNodeInteraction = assert(isNotEnabled())

/**
 * Asserts that the current semantics node is checked.
 *
 * Throws [AssertionError] if the node is not unchecked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOn(): SemanticsNodeInteraction = assert(isOn())

/**
 * Asserts that the current semantics node is unchecked.
 *
 * Throws [AssertionError] if the node is checked, indeterminate, or not toggleable.
 */
fun SemanticsNodeInteraction.assertIsOff(): SemanticsNodeInteraction = assert(isOff())

/**
 * Asserts that the current semantics node is selected.
 *
 * Throws [AssertionError] if the node is unselected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelected(): SemanticsNodeInteraction = assert(isSelected())

/**
 * Asserts that the current semantics node is not selected.
 *
 * Throws [AssertionError] if the node is selected or not selectable.
 */
fun SemanticsNodeInteraction.assertIsNotSelected(): SemanticsNodeInteraction =
    assert(isNotSelected())

/**
 * Asserts that the current semantics node is toggleable.
 *
 * Throws [AssertionError] if the node is not toggleable.
 */
fun SemanticsNodeInteraction.assertIsToggleable(): SemanticsNodeInteraction =
    assert(isToggleable())

/**
 * Asserts that the current semantics node is selectable.
 *
 * Throws [AssertionError] if the node is not selectable.
 */
fun SemanticsNodeInteraction.assertIsSelectable(): SemanticsNodeInteraction =
    assert(isSelectable())

/**
 * Asserts that the current semantics node has a focus.
 *
 * Throws [AssertionError] if the node is not in the focus or does not defined the property at all.
 */
fun SemanticsNodeInteraction.assertIsFocused(): SemanticsNodeInteraction =
    assert(isFocused())

/**
 * Asserts that the current semantics node does not have a focus.
 *
 * Throws [AssertionError] if the node is in the focus or does not defined the property at all.
 */
fun SemanticsNodeInteraction.assertIsNotFocused(): SemanticsNodeInteraction =
    assert(isNotFocused())

/**
 * Asserts the semantics node is in a mutually exclusive group. This is used by radio groups to
 * assert only one is selected at a given time.
 *
 * @Deprecated Replaced with androidx.compose.ui.test.assertIsSelectable
 */
@Deprecated(
    "Replaced with androidx.compose.ui.test.assertIsSelectable",
    ReplaceWith("assertIsSelectable()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsInMutuallyExclusiveGroup(): SemanticsNodeInteraction =
    assertIsSelectable()

/**
 * Asserts the node's label equals the given String.
 * For further details please check [SemanticsProperties.ContentDescription].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertLabelEquals(value: String): SemanticsNodeInteraction =
    assert(hasContentDescription(value))

/**
 * Asserts the node's text equals the given String.
 * For further details please check [SemanticsProperties.Text].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertTextEquals(value: String): SemanticsNodeInteraction =
    assert(hasText(value))

/**
 * Asserts the node's value equals the given value.
 *
 * For further details please check [SemanticsProperties.StateDescription].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertValueEquals(value: String): SemanticsNodeInteraction =
    assert(hasStateDescription(value))

/**
 * Asserts the node's range info equals the given value.
 *
 * For further details please check [SemanticsProperties.ProgressBarRangeInfo].
 * Throws [AssertionError] if the node's value is not equal to `value`, or if the node has no value
 */
fun SemanticsNodeInteraction.assertRangeInfoEquals(value: ProgressBarRangeInfo):
    SemanticsNodeInteraction = assert(hasProgressBarRangeInfo(value))

/**
 * Asserts that the current semantics node has a click action.
 *
 * Throws [AssertionError] if the node is doesn't have a click action.
 */
fun SemanticsNodeInteraction.assertHasClickAction(): SemanticsNodeInteraction =
    assert(hasClickAction())

/**
 * Asserts that the current semantics node has doesn't have a click action.
 *
 * Throws [AssertionError] if the node has a click action.
 */
fun SemanticsNodeInteraction.assertHasNoClickAction(): SemanticsNodeInteraction =
    assert(hasNoClickAction())

/**
 * Asserts that the provided [matcher] is satisfied for this node.
 *
 * @param matcher Matcher to verify.
 * @param messagePrefixOnError Prefix to be put in front of an error that gets thrown in case this
 * assert fails. This can be helpful in situations where this assert fails as part of a bigger
 * operation that used this assert as a precondition check.
 *
 * @throws AssertionError if the matcher does not match or the node can no longer be found.
 */
fun SemanticsNodeInteraction.assert(
    matcher: SemanticsMatcher,
    messagePrefixOnError: (() -> String)? = null
): SemanticsNodeInteraction {
    var errorMessageOnFail = "Failed to assert the following: (${matcher.description})"
    if (messagePrefixOnError != null) {
        errorMessageOnFail = messagePrefixOnError() + "\n" + errorMessageOnFail
    }
    val node = fetchSemanticsNode(errorMessageOnFail)
    if (!matcher.matches(node)) {
        throw AssertionError(buildGeneralErrorMessage(errorMessageOnFail, selector, node))
    }
    return this
}

/**
 * Asserts that this collection of nodes is equal to the given [expectedSize].
 *
 * Provides a detailed error message on failure.
 *
 * @throws AssertionError if the size is not equal to [expectedSize]
 */
fun SemanticsNodeInteractionCollection.assertCountEquals(
    expectedSize: Int
): SemanticsNodeInteractionCollection {
    val errorOnFail = "Failed to assert count of nodes."
    val matchedNodes = fetchSemanticsNodes(errorOnFail)
    if (matchedNodes.size != expectedSize) {
        throw AssertionError(
            buildErrorMessageForCountMismatch(
                errorMessage = errorOnFail,
                selector = selector,
                foundNodes = matchedNodes,
                expectedCount = expectedSize
            )
        )
    }
    return this
}

/**
 * Asserts that this collection contains at least one element that satisfies the given [matcher].
 *
 * @param matcher Matcher that has to be satisfied by at least one of the nodes in the collection.
 *
 * @throws AssertionError if not at least one matching node was node.
 */
fun SemanticsNodeInteractionCollection.assertAny(
    matcher: SemanticsMatcher
): SemanticsNodeInteractionCollection {
    val errorOnFail = "Failed to assertAny(${matcher.description})"
    val nodes = fetchSemanticsNodes(errorOnFail)
    if (nodes.isEmpty()) {
        throw AssertionError(buildErrorMessageForAtLeastOneNodeExpected(errorOnFail, selector))
    }
    if (!matcher.matchesAny(nodes)) {
        throw AssertionError(buildErrorMessageForAssertAnyFail(selector, nodes, matcher))
    }
    return this
}

/**
 * Asserts that all the nodes in this collection satisfy the given [matcher].
 *
 * This passes also for empty collections.
 *
 * @param matcher Matcher that has to be satisfied by all the nodes in the collection.
 *
 * @throws AssertionError if the collection contains at least one element that does not satisfy
 * the given matcher.
 */
fun SemanticsNodeInteractionCollection.assertAll(
    matcher: SemanticsMatcher
): SemanticsNodeInteractionCollection {
    val errorOnFail = "Failed to assertAll(${matcher.description})"
    val nodes = fetchSemanticsNodes(errorOnFail)

    val violations = mutableListOf<SemanticsNode>()
    nodes.forEach {
        if (!matcher.matches(it)) {
            violations.add(it)
        }
    }
    if (violations.isNotEmpty()) {
        throw AssertionError(buildErrorMessageForAssertAllFail(selector, violations, matcher))
    }
    return this
}

internal expect fun SemanticsNodeInteraction.checkIsDisplayed(): Boolean

internal expect fun SemanticsNode.clippedNodeBoundsInWindow(): Rect

internal expect fun SemanticsNode.isInScreenBounds(): Boolean