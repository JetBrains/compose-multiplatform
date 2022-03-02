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

import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction

/**
 * Provides necessary services to facilitate testing.
 *
 * This is typically implemented by entities like test rule.
 */
@InternalTestApi
interface TestOwner {
    /**
     * Clock that drives frames and recompositions in compose tests.
     */
    val mainClock: MainTestClock

    /**
     * Sends the given list of text commands to the given semantics node.
     */
    fun sendTextInputCommand(node: SemanticsNode, command: List<EditCommand>)

    /**
     * Sends the given IME action to the given semantics node.
     */
    fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction)

    /**
     * Runs the given [action] on the ui thread.
     *
     * This is a blocking call.
     */
    // TODO: Does ui-test really need it? Can it use coroutine context on Owner?
    fun <T> runOnUiThread(action: () -> T): T

    /**
     * Collects all [RootForTest]s from all compose hierarchies.
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * Can crash in case it hits time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     *
     * @param atLeastOneRootExpected Whether the caller expects that at least one compose root is
     * present in the tested app. This affects synchronization efforts / timeouts of this API.
     */
    fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest>
}

@InternalTestApi
fun createTestContext(owner: TestOwner): TestContext {
    return TestContext(owner)
}

@OptIn(InternalTestApi::class)
class TestContext internal constructor(internal val testOwner: TestOwner) {

    /**
     * Stores the [InputDispatcherState] of each [RootForTest]. The state will be restored in an
     * [InputDispatcher] when it is created for an owner that has a state stored. To avoid leaking
     * the [RootForTest], the [identityHashCode] of the root is used as the key instead of the
     * actual object.
     */
    internal val states = mutableMapOf<Int, InputDispatcherState>()

    /**
     * Collects all [SemanticsNode]s from all compose hierarchies.
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * Can crash in case it hits time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     */
    internal fun getAllSemanticsNodes(
        atLeastOneRootRequired: Boolean,
        useUnmergedTree: Boolean
    ): Iterable<SemanticsNode> {
        val roots = testOwner.getRoots(atLeastOneRootRequired).also {
            check(!atLeastOneRootRequired || it.isNotEmpty()) {
                "No compose hierarchies found in the app. Possible reasons include: " +
                    "(1) the Activity that calls setContent did not launch; " +
                    "(2) setContent was not called; " +
                    "(3) setContent was called before the ComposeTestRule ran. " +
                    "If setContent is called by the Activity, make sure the Activity is " +
                    "launched after the ComposeTestRule runs"
            }
        }

        return roots.flatMap {
            it.semanticsOwner.getAllSemanticsNodes(mergingEnabled = !useUnmergedTree)
        }
    }
}
