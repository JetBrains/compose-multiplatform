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

/**
 * Finds a semantics node identified by the given tag.
 *
 * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
 *
 * @param useUnmergedTree Find within merged composables like Buttons.
 *
 * @see SemanticsNodeInteractionsProvider.onNode for general find method.
 */
fun SemanticsNodeInteractionsProvider.onNodeWithTag(
    testTag: String,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteraction = onNode(hasTestTag(testTag), useUnmergedTree)

/**
 * Finds all semantics nodes identified by the given tag.
 *
 * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
 *
 * @param useUnmergedTree Find within merged composables like Buttons.
 *
 * @see SemanticsNodeInteractionsProvider.onAllNodes for general find method.
 */
fun SemanticsNodeInteractionsProvider.onAllNodesWithTag(
    testTag: String,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteractionCollection = onAllNodes(hasTestTag(testTag), useUnmergedTree)

/**
 * Finds a semantics node with the given contentDescription.
 *
 * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
 *
 * @param substring Whether to use substring matching.
 * @param ignoreCase Whether case should be ignored.
 * @param useUnmergedTree Find within merged composables like Buttons.
 *
 * @see SemanticsNodeInteractionsProvider.onNode for general find method.
 */
fun SemanticsNodeInteractionsProvider.onNodeWithContentDescription(
    label: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteraction =
    onNode(hasContentDescription(label, substring, ignoreCase), useUnmergedTree)

/**
 * Finds a semantincs node with the given text.
 *
 * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
 *
 * @param substring Whether to use substring matching.
 * @param ignoreCase Whether case should be ignored.
 * @param useUnmergedTree Find within merged composables like Buttons.
 * @see SemanticsNodeInteractionsProvider.onNode for general find method.
 */
fun SemanticsNodeInteractionsProvider.onNodeWithText(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteraction = onNode(hasText(text, substring, ignoreCase), useUnmergedTree)

/**
 * Finds all semantics nodes with the given text.
 *
 * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
 *
 * @param substring Whether to use substring matching.
 * @param ignoreCase Whether case should be ignored.
 * @param useUnmergedTree Find within merged composables like Buttons.
 * @see SemanticsNodeInteractionsProvider.onAllNodes for general find method.
 */
fun SemanticsNodeInteractionsProvider.onAllNodesWithText(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteractionCollection =
    onAllNodes(hasText(text, substring, ignoreCase), useUnmergedTree)

/**
 * Finds all semantics nodes with the given label as ContentDescription.
 *
 * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
 *
 * @param substring Whether to use substring matching.
 * @param ignoreCase Whether case should be ignored.
 * @param useUnmergedTree Find within merged composables like Buttons.
 * @see SemanticsNodeInteractionsProvider.onAllNodes for general find method.
 */
fun SemanticsNodeInteractionsProvider.onAllNodesWithContentDescription(
    label: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteractionCollection =
    onAllNodes(hasContentDescription(label, substring, ignoreCase), useUnmergedTree)

/**
 * Finds the root semantics node of the Compose tree.
 *
 * Useful for example for screenshot tests of the entire scene.
 *
 * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
 *
 * @param useUnmergedTree Find within merged composables like Buttons.
 */
fun SemanticsNodeInteractionsProvider.onRoot(
    useUnmergedTree: Boolean = false
): SemanticsNodeInteraction = onNode(isRoot(), useUnmergedTree)
