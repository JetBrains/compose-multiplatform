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

package androidx.compose.ui.test.junit4

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.TestOwner
import androidx.compose.ui.test.createTestContext
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.swing.Swing
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import javax.swing.SwingUtilities.invokeAndWait
import javax.swing.SwingUtilities.isEventDispatchThread

@OptIn(InternalTestApi::class)
actual fun createComposeRule(): ComposeContentTestRule = DesktopComposeTestRule()

@InternalTestApi
class DesktopComposeTestRule : ComposeContentTestRule {

    override val density: Density
        get() = Density(1f, 1f)

    override val mainClock: MainTestClock
        get() = TODO()

    internal val testDisplaySize: IntSize get() = IntSize(1024, 768)

    private var uncaughtExceptionHandler = UncaughtExceptionHandler()
    lateinit var window: TestComposeWindow

    private val testOwner = DesktopTestOwner(this)
    private val testContext = createTestContext(testOwner)

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                window = runOnUiThread(::createWindow)

                try {
                    base.evaluate()
                } finally {
                    runOnUiThread(window::dispose)
                }

                uncaughtExceptionHandler.throwUncaught()
            }
        }
    }

    private fun createWindow() = TestComposeWindow(
        width = testDisplaySize.width,
        height = testDisplaySize.height,
        density = density,
        nanoTime = System::nanoTime, // TODO(demin): use mainClock?
        coroutineContext = Dispatchers.Swing + uncaughtExceptionHandler
    )

    private fun isIdle() =
        !Snapshot.current.hasPendingChanges() &&
            !window.hasInvalidations()

    override fun waitForIdle() {
        // always check even if we are idle
        uncaughtExceptionHandler.throwUncaught()
        while (!isIdle()) {
            Thread.sleep(10)
            uncaughtExceptionHandler.throwUncaught()
        }
    }

    override suspend fun awaitIdle() {
        // always check even if we are idle
        uncaughtExceptionHandler.throwUncaught()
        while (!isIdle()) {
            delay(10)
            uncaughtExceptionHandler.throwUncaught()
        }
    }

    override fun <T> runOnUiThread(action: () -> T): T {
        return if (isEventDispatchThread()) {
            action()
        } else {
            val task: FutureTask<T> = FutureTask(action)
            invokeAndWait(task)
            try {
                return task.get()
            } catch (e: ExecutionException) { // Expose the original exception
                throw e.cause!!
            }
        }
    }

    override fun <T> runOnIdle(action: () -> T): T {
        // We are waiting for idle before and AFTER `action` to guarantee that changes introduced
        // in `action` are propagated to components. In Android's version, it's executed in the
        // Main thread which has similar effects. This code could be reconsidered after
        // stabilization of the new rendering/dispatching model
        waitForIdle()
        return action().also { waitForIdle() }
    }

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
        // TODO: implement
    }

    override fun registerIdlingResource(idlingResource: IdlingResource) {
        // TODO: implement
    }

    override fun unregisterIdlingResource(idlingResource: IdlingResource) {
        // TODO: implement
    }

    override fun setContent(composable: @Composable () -> Unit) {
        if (isEventDispatchThread()) {
            window.setContent(composable)
        } else {
            runOnUiThread {
                window.setContent(composable)
            }

            // Only wait for idleness if not on the UI thread. If we are on the UI thread, the
            // caller clearly wants to keep tight control over execution order, so don't go
            // executing future tasks on the main thread.
            waitForIdle()
        }
    }

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction {
        return SemanticsNodeInteraction(testContext, useUnmergedTree, matcher)
    }

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection {
        return SemanticsNodeInteractionCollection(testContext, useUnmergedTree, matcher)
    }

    private class DesktopTestOwner(val rule: DesktopComposeTestRule) : TestOwner {
        override fun sendTextInputCommand(node: SemanticsNode, command: List<EditCommand>) {
            TODO()
        }

        override fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction) {
            TODO()
        }

        override fun <T> runOnUiThread(action: () -> T): T {
            return rule.runOnUiThread(action)
        }

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            return rule.window.roots
        }

        // TODO(https://github.com/JetBrains/compose-jb/issues/637): support MainTestClock
        override val mainClock = object : MainTestClock {
            override val currentTime: Long
                get() = 0
            override var autoAdvance: Boolean = false

            override fun advanceTimeByFrame() {
                TODO()
            }

            override fun advanceTimeBy(milliseconds: Long, ignoreFrameDuration: Boolean) {
                TODO()
            }

            override fun advanceTimeUntil(timeoutMillis: Long, condition: () -> Boolean) {
                TODO()
            }
        }
    }
}
