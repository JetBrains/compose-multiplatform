// ktlint-disable filename

/*
 * Copyright 2022 The Android Open Source Project
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

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.platform.textInputServiceFactory
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.ComposeIdlingResource
import androidx.compose.ui.test.junit4.ComposeRootRegistry
import androidx.compose.ui.test.junit4.EspressoLink
import androidx.compose.ui.test.junit4.IdlingResourceRegistry
import androidx.compose.ui.test.junit4.IdlingStrategy
import androidx.compose.ui.test.junit4.MainTestClockImpl
import androidx.compose.ui.test.junit4.RobolectricIdlingStrategy
import androidx.compose.ui.test.junit4.TextInputServiceForTests
import androidx.compose.ui.test.junit4.UncaughtExceptionHandler
import androidx.compose.ui.test.junit4.awaitComposeRoots
import androidx.compose.ui.test.junit4.isOnUiThread
import androidx.compose.ui.test.junit4.waitForComposeRoots
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(InternalTestApi::class, ExperimentalCoroutinesApi::class)
internal class AndroidComposeTest<A : ComponentActivity>(
    private val activityProvider: () -> A
) : ComposeTest {
    /**
     * Provides the current activity.
     */
    val activity: A get() = activityProvider()

    private val idlingResourceRegistry = IdlingResourceRegistry()

    internal val composeRootRegistry = ComposeRootRegistry()

    private val mainClockImpl: MainTestClockImpl
    private val composeIdlingResource: ComposeIdlingResource
    private var idlingStrategy: IdlingStrategy = EspressoLink(idlingResourceRegistry)

    private val recomposer: Recomposer
    private val testCoroutineDispatcher = UnconfinedTestDispatcher()
    private val frameCoroutineScope = TestScope(testCoroutineDispatcher)
    private val recomposerApplyCoroutineScope: CoroutineScope
    private val coroutineExceptionHandler = UncaughtExceptionHandler()

    override val mainClock: MainTestClock
        get() = mainClockImpl

    init {
        val frameClock = TestMonotonicFrameClock(frameCoroutineScope)
        mainClockImpl = MainTestClockImpl(testCoroutineDispatcher, frameClock)
        val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
            override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
                if (mainClockImpl.autoAdvance) {
                    throw CancellationException("Infinite animations are disabled on tests")
                }
                return block()
            }
        }
        recomposerApplyCoroutineScope = CoroutineScope(
            testCoroutineDispatcher + frameClock + infiniteAnimationPolicy +
                coroutineExceptionHandler + Job()
        )
        recomposer = Recomposer(recomposerApplyCoroutineScope.coroutineContext)
        composeIdlingResource = ComposeIdlingResource(
            composeRootRegistry, mainClockImpl, recomposer
        )
    }

    private var disposeContentHook: (() -> Unit)? = null

    private val testOwner = AndroidTestOwner()
    private val testContext = createTestContext(testOwner)

    override val density: Density by lazy {
        Density(ApplicationProvider.getApplicationContext())
    }

    fun <R> runTest(block: AndroidComposeTest<A>.() -> R): R {
        if (Build.FINGERPRINT.lowercase() == "robolectric") {
            idlingStrategy = RobolectricIdlingStrategy(composeRootRegistry, composeIdlingResource)
        }
        // Need to await quiescence before registering our ComposeIdlingResource because the host
        // activity might still be launching. If it is going to set compose content, we want that
        // to happen before we install our hooks to avoid a race.
        idlingStrategy.runUntilIdle()
        return composeRootRegistry.withRegistry {
            idlingResourceRegistry.withRegistry {
                idlingStrategy.withStrategy {
                    withTestCoroutines {
                        withWindowRecomposer {
                            withComposeIdlingResource {
                                withTextInputService {
                                    withAndroidComposeTest(block)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @throws IllegalStateException if called more than once per test.
     */
    override fun setContent(composable: @Composable () -> Unit) {
        check(disposeContentHook == null) {
            "Cannot call setContent twice per test!"
        }

        // We always make sure we have the latest activity when setting a content
        val currentActivity = activity
        // Check if the current activity hasn't already called setContent itself
        val root = currentActivity.findViewById<ViewGroup>(android.R.id.content)
        check(root == null || root.childCount == 0) {
            "$currentActivity has already set content. If you have populated the Activity with " +
                "a ComposeView, make sure to call setContent on that ComposeView instead of on " +
                "the test rule; and make sure that that call to `setContent {}` is done after " +
                "the ComposeTestRule has run"
        }

        runOnUiThread {
            currentActivity.setContent(recomposer, composable)
            disposeContentHook = {
                // Removing a default ComposeView from the view hierarchy will
                // dispose its composition.
                activity.setContentView(View(activity))
            }
        }

        // Synchronizing from the UI thread when we can't leads to a dead lock
        if (idlingStrategy.canSynchronizeOnUiThread || !isOnUiThread()) {
            waitForIdle()
        }
    }

    override fun waitForIdle() {
        waitForIdle(atLeastOneRootExpected = true)
    }

    private fun waitForIdle(atLeastOneRootExpected: Boolean) {
        // First wait until we have a compose root (in case an Activity is being started)
        composeRootRegistry.waitForComposeRoots(atLeastOneRootExpected)
        // Then await composition(s)
        idlingStrategy.runUntilIdle()
        // Check if a coroutine threw an uncaught exception
        coroutineExceptionHandler.throwUncaught()
    }

    override suspend fun awaitIdle() {
        // First wait until we have a compose root (in case an Activity is being started)
        composeRootRegistry.awaitComposeRoots()
        // Then await composition(s)
        idlingStrategy.awaitIdle()
        // Check if a coroutine threw an uncaught exception
        coroutineExceptionHandler.throwUncaught()
    }

    override fun <T> runOnUiThread(action: () -> T): T {
        return testOwner.runOnUiThread(action)
    }

    override fun <T> runOnIdle(action: () -> T): T {
        // Method below make sure that compose is idle.
        waitForIdle()
        // Execute the action on ui thread in a blocking way.
        return runOnUiThread(action)
    }

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val startTime = System.nanoTime()
        while (!condition()) {
            if (mainClockImpl.autoAdvance) {
                mainClock.advanceTimeByFrame()
            }
            // Let Android run measure, draw and in general any other async operations.
            Thread.sleep(10)
            if (System.nanoTime() - startTime > timeoutMillis * NanoSecondsPerMilliSecond) {
                throw ComposeTimeoutException(
                    "Condition still not satisfied after $timeoutMillis ms"
                )
            }
        }
    }

    override fun registerIdlingResource(idlingResource: IdlingResource) {
        idlingResourceRegistry.registerIdlingResource(idlingResource)
    }

    override fun unregisterIdlingResource(idlingResource: IdlingResource) {
        idlingResourceRegistry.unregisterIdlingResource(idlingResource)
    }

    private fun <R> withAndroidComposeTest(block: AndroidComposeTest<A>.() -> R): R {
        try {
            return block()
        } finally {
            // Dispose the content
            if (disposeContentHook != null) {
                runOnUiThread {
                    // NOTE: currently, calling dispose after an exception that happened during
                    // composition is not a safe call. Compose runtime should fix this, and then
                    // this call will be okay. At the moment, however, calling this could
                    // itself produce an exception which will then obscure the original
                    // exception. To fix this, we will just wrap this call in a try/catch of
                    // its own
                    try {
                        disposeContentHook!!()
                    } catch (e: Exception) {
                        // ignore
                    }
                    disposeContentHook = null
                }
            }
        }
    }

    private fun <R> withWindowRecomposer(block: () -> R): R {
        @OptIn(InternalComposeUiApi::class)
        return WindowRecomposerPolicy.withFactory({ recomposer }) {
            try {
                // Start the recomposer:
                recomposerApplyCoroutineScope.launch {
                    recomposer.runRecomposeAndApplyChanges()
                }
                block()
            } finally {
                // Stop the recomposer:
                recomposer.cancel()
                // Cancel our scope to ensure there are no active coroutines when
                // cleanupTestCoroutines is called in the CleanupCoroutinesStatement
                recomposerApplyCoroutineScope.cancel()
            }
        }
    }

    private fun <R> withTestCoroutines(block: () -> R): R {
        try {
            return block()
        } finally {
            // runTest {} as the last step -
            // to replace deprecated TestCoroutineScope.cleanupTestCoroutines
            frameCoroutineScope.runTest {}
            frameCoroutineScope.cancel()
            coroutineExceptionHandler.throwUncaught()
        }
    }

    private fun <R> withComposeIdlingResource(block: () -> R): R {
        try {
            registerIdlingResource(composeIdlingResource)
            return block()
        } finally {
            unregisterIdlingResource(composeIdlingResource)
        }
    }

    @OptIn(InternalComposeUiApi::class)
    private fun <R> withTextInputService(block: () -> R): R {
        val oldTextInputFactory = textInputServiceFactory
        try {
            textInputServiceFactory = {
                TextInputServiceForTests(it)
            }
            return block()
        } finally {
            textInputServiceFactory = oldTextInputFactory
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

    internal inner class AndroidTestOwner : TestOwner {

        override val mainClock: MainTestClock
            get() = mainClockImpl

        override fun sendTextInputCommand(node: SemanticsNode, command: List<EditCommand>) {
            val owner = node.root as ViewRootForTest

            runOnIdle {
                val textInputService = owner.getTextInputServiceOrDie()
                val onEditCommand = textInputService.onEditCommand
                    ?: throw IllegalStateException("No input session started. Missing a focus?")
                onEditCommand(command)
            }
        }

        override fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction) {
            val owner = node.root as ViewRootForTest

            runOnIdle {
                val textInputService = owner.getTextInputServiceOrDie()
                val onImeActionPerformed = textInputService.onImeActionPerformed
                    ?: throw IllegalStateException("No input session started. Missing a focus?")
                onImeActionPerformed.invoke(actionSpecified)
            }
        }

        override fun <T> runOnUiThread(action: () -> T): T {
            return androidx.compose.ui.test.junit4.runOnUiThread(action)
        }

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            waitForIdle(atLeastOneRootExpected)
            return composeRootRegistry.getRegisteredComposeRoots()
        }

        private fun ViewRootForTest.getTextInputServiceOrDie(): TextInputServiceForTests {
            return textInputService as? TextInputServiceForTests
                ?: throw IllegalStateException(
                    "Text input service wrapper not set up! Did you use ComposeTestRule?"
                )
        }
    }
}
