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
 * Builds error message for case where expected amount of nodes does not match reality.
 *
 * Provide [errorMessage] to explain which operation you were about to perform. This makes it
 * easier for developer to find where the failure happened.
 *
 * In case of only one node that went missing (was seen before) use
 * [buildErrorMessageForNodeMissingInTree] for better clarity.
 *
 * To see some examples, check out "ErrorMessagesTest".
 */
internal fun buildErrorMessageForCountMismatch(
    errorMessage: String,
    selector: SemanticsSelector?,
    foundNodes: List<SemanticsNode>,
    expectedCount: Int,
    foundNodesUnmerged: List<SemanticsNode> = emptyList()
): String {
    val sb = StringBuilder()

    sb.append(errorMessage)
    sb.append("\n")

    sb.append("Reason: ")
    if (expectedCount == 0) {
        sb.append("Did not expect any node")
    } else if (expectedCount == 1) {
        sb.append("Expected exactly '1' node")
    } else {
        sb.append("Expected '$expectedCount' nodes")
    }

    if (foundNodes.isEmpty()) {
        sb.append(" but could not find any")
    } else {
        sb.append(" but found '${foundNodes.size}'")
    }

    if (selector != null) {
        if (foundNodes.size <= 1) {
            sb.append(" node that satisfies: (${selector.description})")
        } else {
            sb.append(" nodes that satisfy: (${selector.description})")
        }
    } else {
        sb.append(".")
    }

    // If no nodes were found but they exist in the unmerged tree, display a warning.
    if (foundNodes.isEmpty() && foundNodesUnmerged.isNotEmpty()) {
        sb.appendLine()
        sb.append("However, the unmerged tree contains ")
        if (foundNodesUnmerged.size == 1) {
            sb.append("'1' node that matches. ")
        } else {
            sb.append("'${foundNodesUnmerged.size}' nodes that match. ")
        }
        sb.append("Are you missing `useUnmergedNode = true` in your finder?")
    }

    sb.appendLine()

    if (foundNodes.isNotEmpty()) {
        if (foundNodes.size == 1) {
            sb.appendLine("Node found:")
        } else {
            sb.appendLine("Nodes found:")
        }
        sb.appendLine(foundNodes.printToString())
    }

    return sb.toString()
}

/**
 * Builds error message for case where node is no longer in the tree but is expected to be.
 *
 * Provide [errorMessage] to explain which operation you were about to perform. This makes it
 * easier for developer to find where the failure happened.
 *
 * Note that [lastSeenSemantics] is the last semantics we have seen before we couldn't find the node
 * anymore. This can provide more info to the developer on what could have happened.
 *
 * To see some examples, check out "ErrorMessagesTest".
 */
internal fun buildErrorMessageForNodeMissingInTree(
    errorMessage: String,
    selector: SemanticsSelector,
    lastSeenSemantics: String
): String {
    val sb = StringBuilder()

    sb.append(errorMessage)
    sb.append("\n")

    sb.appendLine("The node is no longer in the tree, last known semantics:")
    sb.appendLine(lastSeenSemantics)
    sb.append("Original selector: ")
    sb.appendLine(selector.description)

    return sb.toString()
}

internal fun buildErrorMessageForAssertAnyFail(
    selector: SemanticsSelector,
    nodes: List<SemanticsNode>,
    assertionMatcher: SemanticsMatcher
): String {
    val sb = StringBuilder()

    sb.appendLine("Failed to assertAny(${assertionMatcher.description})")

    sb.appendLine("None of the following nodes match:")
    sb.appendLine(nodes.printToString())

    sb.append("Selector used: '")
    sb.append(selector.description)
    sb.appendLine("'")

    return sb.toString()
}

internal fun buildErrorMessageForAssertAllFail(
    selector: SemanticsSelector,
    nodesNotMatching: List<SemanticsNode>,
    assertionMatcher: SemanticsMatcher
): String {
    val sb = StringBuilder()

    sb.appendLine("Failed to assertAll(${assertionMatcher.description})")

    sb.append("Found '${nodesNotMatching.size}' ")
    sb.append(if (nodesNotMatching.size == 1) "node" else "nodes")
    sb.appendLine(" not matching:")
    sb.appendLine(nodesNotMatching.printToString())

    sb.append("Selector used: '")
    sb.append(selector.description)
    sb.appendLine("'")

    return sb.toString()
}

internal fun buildErrorMessageForAtLeastOneNodeExpected(
    errorMessage: String,
    selector: SemanticsSelector
): String {
    val sb = StringBuilder()

    sb.appendLine(errorMessage)

    sb.append("Assert needs to receive at least 1 node but 0 nodes were found for selector: ")
    sb.append("'")
    sb.append(selector.description)
    sb.appendLine("'")

    return sb.toString()
}

internal fun buildGeneralErrorMessage(
    errorMessage: String,
    selector: SemanticsSelector,
    node: SemanticsNode
): String {
    val sb = StringBuilder()

    sb.appendLine(errorMessage)

    sb.appendLine("Semantics of the node:")
    sb.appendLine(node.printToString())

    sb.append("Selector used: (")
    sb.append(selector.description)
    sb.appendLine(")")

    return sb.toString()
}

internal fun buildIndexErrorMessage(
    index: Int,
    selector: SemanticsSelector,
    nodes: List<SemanticsNode>
): String {
    val sb = StringBuilder()

    sb.append("Can't retrieve node at index '$index' of '")
    sb.append(selector.description)
    sb.appendLine("'")

    if (nodes.isEmpty()) {
        sb.appendLine("There are no existing nodes for that selector.")
    } else if (nodes.size == 1) {
        sb.appendLine("There is 1 node only:")
        sb.appendLine(nodes.printToString())
    } else {
        sb.appendLine("There are '${nodes.size}' nodes only:")
        sb.appendLine(nodes.printToString())
    }

    return sb.toString()
}
