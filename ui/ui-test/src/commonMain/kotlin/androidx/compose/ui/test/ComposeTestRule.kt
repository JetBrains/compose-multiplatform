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

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize

/**
 * Enables to run tests of individual composables without having to do manual setup. Normally this
 * rule is obtained by using [createComposeRule] factory that provides proper implementation
 * (depending if running host side or Android side).
 */
interface ComposeTestRule {
    /**
     * Current device screen's density.
     */
    val density: Density

    /**
     * Current device display's size.
     */
    val displaySize: IntSize get

    /**
     * Finds a semantics node that matches the given condition.
     *
     * Any subsequent operation on its result will expect exactly one element found (unless
     * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw [AssertionError] if
     * none or more than one element is found.
     *
     * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
     *
     * @param useUnmergedTree Find within merged composables like Buttons.
     * @see onAllNodes to work with multiple elements
     */
    fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = false
    ): SemanticsNodeInteraction {
        return SemanticsNodeInteraction(useUnmergedTree, SemanticsSelector(matcher))
    }

    /**
     * Finds all semantics nodes that match the given condition.
     *
     * If you are working with elements that are not supposed to occur multiple times use [onNode]
     * instead.
     *
     * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
     *
     * @param useUnmergedTree Find within merged composables like Buttons.
     * @see onNode
     */
    fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = false
    ): SemanticsNodeInteractionCollection {
        return SemanticsNodeInteractionCollection(useUnmergedTree, SemanticsSelector(matcher))
    }

    /**
     * Runs the given action on the UI thread.
     *
     * This method is blocking until the action is complete.
     */
    fun <T> runOnUiThread(action: () -> T): T

    /**
     * Executes the given action in the same way as [runOnUiThread] but also makes sure Compose
     * is idle before executing it. This is great place for doing your assertions on shared
     * variables.
     *
     * This method is blocking until the action is complete.
     */
    fun <T> runOnIdle(action: () -> T): T

    /**
     * Waits for compose to be idle.
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * Can crash in case there is a time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     */
    fun waitForIdle()

    /**
     * Suspends until compose is idle. Compose is idle if there are no pending compositions, no
     * pending changes that could lead to another composition, and no pending draw calls.
     */
    @ExperimentalTesting
    suspend fun awaitIdle()
}
