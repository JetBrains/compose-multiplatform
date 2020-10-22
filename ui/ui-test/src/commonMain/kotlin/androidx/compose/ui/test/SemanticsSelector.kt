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

package androidx.compose.ui.test

import androidx.compose.ui.semantics.SemanticsNode

/**
 * Projects the given set of nodes to a new set of nodes.
 *
 * @param description Description that is displayed to the developer in error outputs.
 * @param requiresExactlyOneNode Whether this selector should expect to receive exactly 1 node.
 * @param chainedInputSelector Optional selector to apply before this selector gets applied.
 * @param selector The lambda that implements the projection.
 */
class SemanticsSelector(
    val description: String,
    private val requiresExactlyOneNode: Boolean,
    private val chainedInputSelector: SemanticsSelector? = null,
    private val selector: (Iterable<SemanticsNode>) -> SelectionResult
) {

    /**
     * Maps the given list of nodes to a new list of nodes.
     *
     * @throws AssertionError if required prerequisites to perform the selection were not satisfied.
     */
    fun map(nodes: Iterable<SemanticsNode>, errorOnFail: String): SelectionResult {
        val chainedResult = chainedInputSelector?.map(nodes, errorOnFail)
        val inputNodes = chainedResult?.selectedNodes ?: nodes
        if (requiresExactlyOneNode && inputNodes.count() != 1) {
            throw AssertionError(
                chainedResult?.customErrorOnNoMatch ?: buildErrorMessageForCountMismatch(
                    errorMessage = errorOnFail,
                    foundNodes = inputNodes.toList(),
                    expectedCount = 1,
                    selector = chainedInputSelector ?: this
                )
            )
        }
        return selector(inputNodes)
    }
}

/**
 * Creates a new [SemanticsSelector] based on the given [SemanticsMatcher].
 */
internal fun SemanticsSelector(matcher: SemanticsMatcher): SemanticsSelector {
    return SemanticsSelector(
        matcher.description,
        requiresExactlyOneNode = false,
        chainedInputSelector = null
    ) {
        nodes ->
        SelectionResult(nodes.filter { matcher.matches(it) })
    }
}

/**
 * Result of [SemanticsSelector] projection.
 *
 * @param selectedNodes The result nodes found.
 * @param customErrorOnNoMatch If the projection failed to map nodes due to wrong input (e.g.
 * selector expected only 1 node but got multiple) it will provide a custom error exactly explaining
 * what selection was performed and what nodes it received.
 */
class SelectionResult(
    val selectedNodes: List<SemanticsNode>,
    val customErrorOnNoMatch: String? = null
)

/**
 * Chains the given selector to be performed after this one.
 *
 * The new selector will expect to receive exactly one node (otherwise will fail).
 */
internal fun SemanticsSelector.addSelectionFromSingleNode(
    description: String,
    selector: (SemanticsNode) -> List<SemanticsNode>
): SemanticsSelector {
    return SemanticsSelector(
        "(${this.description}).$description",
        requiresExactlyOneNode = true,
        chainedInputSelector = this
    ) {
        nodes ->
        SelectionResult(selector(nodes.first()))
    }
}

/**
 * Chains a new selector that retrieves node from this selector at the given [index].
 */
internal fun SemanticsSelector.addIndexSelector(
    index: Int
): SemanticsSelector {
    return SemanticsSelector(
        "(${this.description})[$index]",
        requiresExactlyOneNode = false,
        chainedInputSelector = this
    ) { nodes ->
        val nodesList = nodes.toList()
        if (index >= 0 && index < nodesList.size) {
            SelectionResult(listOf(nodesList[index]))
        } else {
            val errorMessage = buildIndexErrorMessage(index, this, nodesList)
            SelectionResult(emptyList(), errorMessage)
        }
    }
}

/**
 * Chains a new selector that retrieves the last node returned from this selector.
 */
internal fun SemanticsSelector.addLastNodeSelector(): SemanticsSelector {
    return SemanticsSelector(
        "(${this.description}).last",
        requiresExactlyOneNode = false,
        chainedInputSelector = this
    ) { nodes ->
        SelectionResult(nodes.toList().takeLast(1))
    }
}

/**
 * Chains a new selector that selects all the nodes matching the given [matcher] from the nodes
 * returned by this selector.
 */
internal fun SemanticsSelector.addSelectorViaMatcher(
    selectorName: String,
    matcher: SemanticsMatcher
): SemanticsSelector {
    return SemanticsSelector(
        "(${this.description}).$selectorName(${matcher.description})",
        requiresExactlyOneNode = false,
        chainedInputSelector = this
    ) { nodes ->
        SelectionResult(nodes.filter { matcher.matches(it) })
    }
}