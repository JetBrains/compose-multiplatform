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

import androidx.compose.ui.semantics.SemanticsNode

/**
 * Represents a semantics node and the path to fetch it from the semantics tree. One can interact
 * with this node by performing actions such as [performClick], assertions such as
 * [assertHasClickAction], or navigate to other nodes such as [onChildren].
 *
 * An instance of [SemanticsNodeInteraction] can be obtained from
 * [onNode][SemanticsNodeInteractionsProvider.onNode] and convenience
 * methods that use a specific filter, such as [onNodeWithText].
 *
 * Here you can see how you can locate a checkbox, click it and verify that it's checked:
 * @sample androidx.compose.ui.test.samples.clickAndVerifyCheckbox
 *
 * [useUnmergedTree] is for tests with a special need to inspect implementation detail within
 * children. For example:
 * @sample androidx.compose.ui.test.samples.useUnmergedTree
 */
class SemanticsNodeInteraction constructor(
    internal val testContext: TestContext,
    internal val useUnmergedTree: Boolean,
    internal val selector: SemanticsSelector
) {
    constructor(
        testContext: TestContext,
        useUnmergedTree: Boolean,
        matcher: SemanticsMatcher
    ) : this(testContext, useUnmergedTree, SemanticsSelector(matcher))

    /**
     * Anytime we refresh semantics we capture it here. This is then presented to the user in case
     * their tests fails deu to a missing node. This helps to see what was the last state of the
     * node before it disappeared. We dump it to string because trying to dump the node later can
     * result in failure as it gets detached from its layout.
     */
    private var lastSeenSemantics: String? = null

    internal fun fetchSemanticsNodes(
        atLeastOneRootRequired: Boolean,
        errorMessageOnFail: String? = null
    ): SelectionResult {
        return selector
            .map(
                testContext.getAllSemanticsNodes(atLeastOneRootRequired, useUnmergedTree),
                errorMessageOnFail.orEmpty()
            )
    }

    /**
     * Returns the semantics node captured by this object.
     *
     * Note: Accessing this object involves synchronization with your UI. If you are accessing this
     * multiple times in one atomic operation, it is better to cache the result instead of calling
     * this API multiple times.
     *
     * This will fail if there is 0 or multiple nodes matching.
     *
     * @throws AssertionError if 0 or multiple nodes found.
     */
    fun fetchSemanticsNode(errorMessageOnFail: String? = null): SemanticsNode {
        return fetchOneOrDie(errorMessageOnFail)
    }

    /**
     * Asserts that no item was found or that the item is no longer in the hierarchy.
     *
     * This will synchronize with the UI and fetch all the nodes again to ensure it has latest data.
     *
     * @throws [AssertionError] if the assert fails.
     */
    fun assertDoesNotExist() {
        val result = fetchSemanticsNodes(
            atLeastOneRootRequired = false,
            errorMessageOnFail = "Failed: assertDoesNotExist."
        )
        if (result.selectedNodes.isNotEmpty()) {
            throw AssertionError(
                buildErrorMessageForCountMismatch(
                    errorMessage = "Failed: assertDoesNotExist.",
                    selector = selector,
                    foundNodes = result.selectedNodes,
                    expectedCount = 0
                )
            )
        }
    }

    /**
     * Asserts that the component was found and is part of the component tree.
     *
     * This will synchronize with the UI and fetch all the nodes again to ensure it has latest data.
     * If you are using [fetchSemanticsNode] you don't need to call this. In fact you would just
     * introduce additional overhead.
     *
     * @param errorMessageOnFail Error message prefix to be added to the message in case this
     * asserts fails. This is typically used by operations that rely on this assert. Example prefix
     * could be: "Failed to perform doOnClick.".
     *
     * @throws [AssertionError] if the assert fails.
     */
    fun assertExists(errorMessageOnFail: String? = null): SemanticsNodeInteraction {
        fetchOneOrDie(errorMessageOnFail)
        return this
    }

    private fun fetchOneOrDie(errorMessageOnFail: String? = null): SemanticsNode {
        val finalErrorMessage = errorMessageOnFail
            ?: "Failed: assertExists."

        val result = fetchSemanticsNodes(atLeastOneRootRequired = true, finalErrorMessage)
        if (result.selectedNodes.count() != 1) {
            if (result.selectedNodes.isEmpty() && lastSeenSemantics != null) {
                // This means that node we used to have is no longer in the tree.
                throw AssertionError(
                    buildErrorMessageForNodeMissingInTree(
                        errorMessage = finalErrorMessage,
                        selector = selector,
                        lastSeenSemantics = lastSeenSemantics!!
                    )
                )
            }

            if (result.customErrorOnNoMatch != null) {
                throw AssertionError(finalErrorMessage + "\n" + result.customErrorOnNoMatch)
            }

            throw AssertionError(
                buildErrorMessageForCountMismatch(
                    errorMessage = finalErrorMessage,
                    foundNodes = result.selectedNodes,
                    expectedCount = 1,
                    selector = selector,
                    foundNodesUnmerged = getNodesInUnmergedTree(errorMessageOnFail)
                )
            )
        }

        lastSeenSemantics = result.selectedNodes.first().printToString()
        return result.selectedNodes.first()
    }

    /**
     * If using the merged tree, performs the same search in the unmerged tree.
     */
    private fun getNodesInUnmergedTree(errorMessageOnFail: String?): List<SemanticsNode> {
        return if (!useUnmergedTree) {
            selector
                .map(
                    testContext.getAllSemanticsNodes(
                        atLeastOneRootRequired = true,
                        useUnmergedTree = true
                    ),
                    errorMessageOnFail.orEmpty()
                ).selectedNodes
        } else {
            emptyList()
        }
    }
}

/**
 * Represents a collection of semantics nodes and the path to fetch them from the semantics tree.
 * One can interact with these nodes by performing assertions such as [assertCountEquals], or
 * navigate to other nodes such as [get].
 *
 * An instance of [SemanticsNodeInteractionCollection] can be obtained from
 * [onAllNodes][SemanticsNodeInteractionsProvider.onAllNodes] and convenience
 * methods that use a specific filter, such as [onAllNodesWithText].
 *
 * For example, here is how you verify that there are exactly two clickable items:
 * @sample androidx.compose.ui.test.samples.verifyTwoClickableNodes
 */
class SemanticsNodeInteractionCollection constructor(
    internal val testContext: TestContext,
    internal val useUnmergedTree: Boolean,
    internal val selector: SemanticsSelector
) {
    private var nodeIds: List<Int>? = null

    constructor(
        testContext: TestContext,
        useUnmergedTree: Boolean,
        matcher: SemanticsMatcher
    ) : this(testContext, useUnmergedTree, SemanticsSelector(matcher))

    /**
     * Returns the semantics nodes captured by this object.
     *
     * Note: Accessing this object involves synchronization with your UI. If you are accessing this
     * multiple times in one atomic operation, it is better to cache the result instead of calling
     * this API multiple times.
     *
     * @param atLeastOneRootRequired Whether to throw an error in case there is no compose
     * content in the current test app.
     * @param errorMessageOnFail Custom error message to append when this fails to retrieve the
     * nodes.
     */
    fun fetchSemanticsNodes(
        atLeastOneRootRequired: Boolean = true,
        errorMessageOnFail: String? = null
    ): List<SemanticsNode> {
        if (nodeIds == null) {
            return selector
                .map(
                    testContext.getAllSemanticsNodes(atLeastOneRootRequired, useUnmergedTree),
                    errorMessageOnFail.orEmpty()
                )
                .apply { nodeIds = selectedNodes.map { it.id }.toList() }
                .selectedNodes
        }

        return testContext.getAllSemanticsNodes(atLeastOneRootRequired, useUnmergedTree)
            .filter { it.id in nodeIds!! }
    }

    /**
     * Retrieve node at the given index of this collection.
     *
     * Any subsequent operation on its result will expect exactly one element found (unless
     * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
     * none or more than one element is found.
     */
    operator fun get(index: Int): SemanticsNodeInteraction {
        return SemanticsNodeInteraction(
            testContext,
            useUnmergedTree,
            selector.addIndexSelector(index)
        )
    }
}
