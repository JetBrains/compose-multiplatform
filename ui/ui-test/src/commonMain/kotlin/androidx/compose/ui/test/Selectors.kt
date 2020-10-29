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

internal val SemanticsNode.siblings: List<SemanticsNode>
    get() {
        val node = this
        return parent?.run { this.children.filter { it.id != node.id } } ?: emptyList()
    }

/**
 * Returns a parent of this node.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * none or more than one element is found.
 */
fun SemanticsNodeInteraction.onParent(): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(
        testContext,
        useUnmergedTree,
        selector.addSelectionFromSingleNode("parent") { listOfNotNull(it.parent) }
    )
}

/**
 * Returns children of this node.
 */
fun SemanticsNodeInteraction.onChildren(): SemanticsNodeInteractionCollection {
    return SemanticsNodeInteractionCollection(
        testContext,
        useUnmergedTree,
        selector.addSelectionFromSingleNode("children") { it.children }
    )
}

/**
 * Returns exactly one child of this node.
 *
 * Use this only if this node has exactly one child.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * none or more than one element is found.
 */
fun SemanticsNodeInteraction.onChild(): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(
        testContext,
        useUnmergedTree,
        selector.addSelectionFromSingleNode("child") { it.children }
    )
}

/**
 * Returns child of this node at the given index.
 *
 * This is just a shortcut for "children[index]".
 */
fun SemanticsNodeInteraction.onChildAt(index: Int): SemanticsNodeInteraction = onChildren()[index]

/**
 * Returns all siblings of this node.
 *
 * Example: For the following tree
 * ```
 * |-A
 *   |-B1
 *   |-B2 <- this node
 *   |-B3
 * Returns B1, B3
 * ```
 */
fun SemanticsNodeInteraction.onSiblings(): SemanticsNodeInteractionCollection {
    return SemanticsNodeInteractionCollection(
        testContext,
        useUnmergedTree,
        selector.addSelectionFromSingleNode("siblings") { it.siblings }
    )
}

/**
 * Returns exactly one sibling of this node.
 *
 * Use this only if this node has exactly one sibling.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * none or more than one element is found.
 */
fun SemanticsNodeInteraction.onSibling(): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(
        testContext,
        useUnmergedTree,
        selector.addSelectionFromSingleNode("sibling") { it.siblings }
    )
}

/**
 * Returns all the ancestors of this node.
 *
 * Example: For the following tree
 * ```
 * |-A
 *   |-B
 *     |-C <- this node
 * Returns B, A
 * ```
 */
fun SemanticsNodeInteraction.onAncestors(): SemanticsNodeInteractionCollection {
    return SemanticsNodeInteractionCollection(
        testContext,
        useUnmergedTree,
        selector.addSelectionFromSingleNode("ancestors") { it.ancestors.toList() }
    )
}

/**
 * Returns the first node in this collection.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * no element is found.
 */
fun SemanticsNodeInteractionCollection.onFirst(): SemanticsNodeInteraction {
    return get(0)
}

/**
 * Returns the last node in this collection.
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * no element is found.
 */
fun SemanticsNodeInteractionCollection.onLast(): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(
        testContext,
        useUnmergedTree,
        selector.addLastNodeSelector()
    )
}

/**
 * Returns all the nodes matching the given [matcher].
 *
 * @param matcher Matcher to use for the filtering.
 */
fun SemanticsNodeInteractionCollection.filter(
    matcher: SemanticsMatcher
): SemanticsNodeInteractionCollection {
    return SemanticsNodeInteractionCollection(
        testContext,
        useUnmergedTree,
        selector.addSelectorViaMatcher("filter", matcher)
    )
}

/**
 * Expects to return exactly one node matching the given [matcher].
 *
 * Any subsequent operation on its result will expect exactly one element found (unless
 * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
 * no element is found.
 *
 * @param matcher Matcher to use for the filtering.
 */
fun SemanticsNodeInteractionCollection.filterToOne(
    matcher: SemanticsMatcher
): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(
        testContext,
        useUnmergedTree,
        selector.addSelectorViaMatcher("filterToOne", matcher)
    )
}