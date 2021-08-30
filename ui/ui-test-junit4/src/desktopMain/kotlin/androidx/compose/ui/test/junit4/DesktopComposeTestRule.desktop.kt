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
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeTimeoutException
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.yield
import org.jetbrains.skia.Surface
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.coroutines.cancellation.CancellationException

@OptIn(InternalTestApi::class)
actual fun createComposeRule(): ComposeContentTestRule = DesktopComposeTestRule()

@InternalTestApi
@OptIn(ExperimentalComposeUiApi::class, ExperimentalCoroutinesApi::class)
class DesktopComposeTestRule : ComposeContentTestRule {

    override val density = Density(1f, 1f)

    private val coroutineDispatcher = TestCoroutineDispatcher()
    override val mainClock: MainTestClock =
        MainTestClockImpl(coroutineDispatcher, frameDelayMillis = 16L)
    private var uncaughtExceptionHandler = UncaughtExceptionHandler()
    private val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
        override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
            if (mainClock.autoAdvance) {
                throw CancellationException()
            }
            return block()
        }
    }
    private val coroutineContext =
        coroutineDispatcher + uncaughtExceptionHandler + infiniteAnimationPolicy
    private val surface = Surface.makeRasterN32Premul(1024, 768)

    lateinit var scene: ComposeScene

    private val testOwner = DesktopTestOwner()
    private val testContext = createTestContext(testOwner)

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                scene = runOnUiThread(::createUi)

                try {
                    base.evaluate()
                } finally {
                    runOnUiThread(scene::dispose)
                }

                coroutineDispatcher.cleanupTestCoroutines()
                uncaughtExceptionHandler.throwUncaught()
            }
        }
    }

    private fun renderNextFrame() = runOnUiThread {
        scene.render(
            surface.canvas,
            mainClock.currentTime * 1_000_000
        )
        if (mainClock.autoAdvance) {
            mainClock.advanceTimeByFrame()
        }
    }

    private fun createUi() = ComposeScene(
        density = density,
        coroutineContext = coroutineContext,
        invalidate = { }
    ).apply {
        constraints = Constraints(maxWidth = surface.width, maxHeight = surface.height)
    }

    private fun isIdle() =
        !Snapshot.current.hasPendingChanges() &&
            !scene.hasInvalidations()

    override fun waitForIdle() {
        // always check even if we are idle
        uncaughtExceptionHandler.throwUncaught()
        while (!isIdle()) {
            renderNextFrame()
            uncaughtExceptionHandler.throwUncaught()
        }
    }

    override suspend fun awaitIdle() {
        // always check even if we are idle
        uncaughtExceptionHandler.throwUncaught()
        while (!isIdle()) {
            renderNextFrame()
            uncaughtExceptionHandler.throwUncaught()
            yield()
        }
    }

    override fun <T> runOnUiThread(action: () -> T): T {
        return androidx.compose.ui.test.junit4.runOnUiThread(action)
    }

    override fun <T> runOnIdle(action: () -> T): T {
        // We are waiting for idle before and AFTER `action` to guarantee that changes introduced
        // in `action` are propagated to components. In Android's version, it's executed in the
        // Main thread which has similar effects.
        waitForIdle()
        return action().also { waitForIdle() }
    }

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val startTime = System.nanoTime()
        while (!condition()) {
            renderNextFrame()
            if (System.nanoTime() - startTime > timeoutMillis * 1_000_000) {
                throw ComposeTimeoutException(
                    "Condition still not satisfied after $timeoutMillis ms"
                )
            }
        }
    }

    override fun registerIdlingResource(idlingResource: IdlingResource) {
        // TODO: implement
    }

    override fun unregisterIdlingResource(idlingResource: IdlingResource) {
        // TODO: implement
    }

    override fun setContent(composable: @Composable () -> Unit) {
        if (isOnUiThread()) {
            scene.setContent(content = composable)
        } else {
            runOnUiThread {
                scene.setContent(content = composable)
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

    private inner class DesktopTestOwner : TestOwner {
        override fun sendTextInputCommand(node: SemanticsNode, command: List<EditCommand>) {
            TODO()
        }

        override fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction) {
            TODO()
        }

        override fun <T> runOnUiThread(action: () -> T): T {
            return this@DesktopComposeTestRule.runOnUiThread(action)
        }

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            return this@DesktopComposeTestRule.scene.roots
        }

        override val mainClock get() =
            this@DesktopComposeTestRule.mainClock
    }
}