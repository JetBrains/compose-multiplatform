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
import androidx.test.core.app.ActivityScenario
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

@ExperimentalTestApi
actual fun runComposeUiTest(block: ComposeUiTest.() -> Unit) {
    runAndroidComposeUiTest(ComponentActivity::class.java, block)
}

/**
 * Variant of [runComposeUiTest] that allows you to specify which Activity should be launched. Be
 * aware that if the Activity [sets content][androidx.activity.compose.setContent] during its
 * launch, you cannot use [setContent][ComposeUiTest.setContent] on the ComposeUiTest anymore as
 * this would override the content and can lead to subtle bugs.
 *
 * @param A The Activity type to be launched, which typically (but not necessarily) hosts the
 * Compose content
 */
@ExperimentalTestApi
inline fun <reified A : ComponentActivity> runAndroidComposeUiTest(
    noinline block: AndroidComposeUiTest<A>.() -> Unit
) {
    runAndroidComposeUiTest(A::class.java, block)
}

/**
 * Variant of [runComposeUiTest] that allows you to specify which Activity should be launched. Be
 * aware that if the Activity [sets content][androidx.activity.compose.setContent] during its
 * launch, you cannot use [setContent][ComposeUiTest.setContent] on the ComposeUiTest anymore as
 * this would override the content and can lead to subtle bugs.
 *
 * @param A The Activity type to be launched, which typically (but not necessarily) hosts the
 * Compose content
 */
@ExperimentalTestApi
fun <A : ComponentActivity> runAndroidComposeUiTest(
    activityClass: Class<A>,
    block: AndroidComposeUiTest<A>.() -> Unit
) {
    // Don't start the scenario now, wait until we're inside runTest { },
    // in case the Activity's onCreate/Start/Resume calls setContent
    var scenario: ActivityScenario<A>? = null
    val environment = AndroidComposeUiTestEnvironment {
        requireNotNull(scenario) {
            "ActivityScenario has not yet been launched, or has already finished. Make sure that " +
                "any call to ComposeUiTest.setContent() and AndroidComposeUiTest.getActivity() " +
                "is made within the lambda passed to AndroidComposeUiTestEnvironment.runTest()"
        }.getActivity()
    }
    try {
        environment.runTest {
            scenario = ActivityScenario.launch(activityClass)
            block()
        }
    } finally {
        // Close the scenario outside runTest to avoid getting stuck.
        //
        // ActivityScenario.close() calls Instrumentation.waitForIdleSync(), which would time out
        // if there is an infinite self-invalidating measure, layout, or draw loop. If the
        // Compose content was set through the test's setContent method, it will remove the
        // AndroidComposeView from the view hierarchy which breaks this loop, which is why we
        // call close() outside the runTest lambda. This will not help if the content is not set
        // through the test's setContent method though, in which case we'll still time out here.
        scenario?.close()
    }
}

/**
 * Variant of [runComposeUiTest] that does not launch an Activity. Use this if you need to have
 * control over the timing and method of launching the Activity, for example when you want to
 * launch it with a custom Intent, or if you have a complex test setup.
 *
 * When using this method, calling [ComposeUiTest.setContent] will throw an IllegalStateException.
 * Instead, you'll have to set the content in the Activity that you have launched yourself,
 * either directly on the Activity or on an [androidx.compose.ui.platform.AbstractComposeView].
 * You will need to do this from within the [test lambda][block], or the test framework will not
 * be able to find the content.
 */
@ExperimentalTestApi
fun runComposeUiTestWithoutActivity(block: ComposeUiTest.() -> Unit) {
    AndroidComposeUiTestEnvironment {
        error(
            "runComposeUiTestWithoutActivity {} does not provide an Activity to " +
                "set Compose content in. Launch and use the Activity yourself, " +
                "or use runAndroidComposeUiTest {}"
        )
    }.runTest(block)
}

/**
 * Variant of [ComposeUiTest] for when you want to have access to the current [activity] of type
 * [A]. The activity might not always be available, for example if the test navigates to another
 * activity. In such cases, [activity] will return `null`.
 *
 * An instance of [AndroidComposeUiTest] can be obtained by calling [runAndroidComposeUiTest], the
 * argument to which will have it as the receiver scope.
 *
 * Note that any Compose content can be found and tested, regardless if it is hosted by [activity]
 * or not. What is important, is that the content is set _during_ the lambda passed to
 * [runAndroidComposeUiTest] (not before, and not after), and that the activity that is actually
 * hosting the Compose content is in resumed state.
 *
 * @param A The Activity type to be interacted with, which typically (but not necessarily) is the
 * activity that was launched and hosts the Compose content
 */
@ExperimentalTestApi
sealed interface AndroidComposeUiTest<A : ComponentActivity> : ComposeUiTest {
    /**
     * Returns the current activity of type [A] used in this [ComposeUiTest]. If no such activity
     * is available, for example if you've navigated to a different activity and the original host
     * has now been destroyed, this will return `null`.
     *
     * Note that you should never hold on to a reference to the Activity, always use [activity]
     * to interact with the Activity.
     */
    val activity: A?
}

/**
 * Creates an [AndroidComposeUiTestEnvironment] that retrieves the
 * [host Activity][AndroidComposeUiTest.activity] by delegating to the given [activityProvider].
 * Use this if you need to launch an Activity in a way that is not compatible with any of the
 * existing [runComposeUiTest], [runAndroidComposeUiTest], or [runComposeUiTestWithoutActivity]
 * methods.
 *
 * Valid use cases include, but are not limited to, creating your own JUnit test rule that
 * implements [AndroidComposeUiTest] by delegating to [AndroidComposeUiTestEnvironment.test].
 * See [AndroidComposeTestRule][androidx.compose.ui.test.junit4.AndroidComposeTestRule] for a
 * reference implementation.
 *
 * The [activityProvider] is called every time [activity][AndroidComposeUiTest.activity] is
 * called, which in turn is called when [setContent][ComposeUiTest.setContent] is called.
 *
 * The most common implementation of an [activityProvider] retrieves the activity from a backing
 * [ActivityScenario] (that the caller launches _within_ the lambda passed to [runTest]), but
 * one is not limited to this pattern.
 *
 * @param activityProvider A lambda that should return the current Activity instance of type [A],
 * if it is available. If it is not available, it should return `null`.
 * @param A The Activity type to be interacted with, which typically (but not necessarily) is the
 * activity that was launched and hosts the Compose content
 */
@ExperimentalTestApi
inline fun <A : ComponentActivity> AndroidComposeUiTestEnvironment(
    crossinline activityProvider: () -> A?
): AndroidComposeUiTestEnvironment<A> {
    return object : AndroidComposeUiTestEnvironment<A>() {
        override val activity: A?
            get() = activityProvider.invoke()
    }
}

/**
 * A test environment that can [run tests][runTest] using the [test receiver scope][test]. Note
 * that some of the properties and methods on [test] will only work during the call to [runTest],
 * as they require that the environment has been set up.
 *
 * @param A The Activity type to be interacted with, which typically (but not necessarily) is the
 * activity that was launched and hosts the Compose content
 */
@ExperimentalTestApi
@OptIn(InternalTestApi::class, ExperimentalCoroutinesApi::class)
abstract class AndroidComposeUiTestEnvironment<A : ComponentActivity> {
    private val idlingResourceRegistry = IdlingResourceRegistry()

    internal val composeRootRegistry = ComposeRootRegistry()

    private val mainClockImpl: MainTestClockImpl
    private val composeIdlingResource: ComposeIdlingResource
    private var idlingStrategy: IdlingStrategy = EspressoLink(idlingResourceRegistry)

    private val recomposer: Recomposer
    private val testCoroutineDispatcher = UnconfinedTestDispatcher()
    private val testCoroutineScope = TestScope(testCoroutineDispatcher)
    private val recomposerContinuationInterceptor =
        ApplyingContinuationInterceptor(testCoroutineDispatcher)
    private val recomposerCoroutineScope: CoroutineScope
    private val coroutineExceptionHandler = UncaughtExceptionHandler()

    init {
        val frameClock = TestMonotonicFrameClock(testCoroutineScope)
        mainClockImpl = MainTestClockImpl(testCoroutineDispatcher.scheduler, frameClock)
        val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
            override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
                if (mainClockImpl.autoAdvance) {
                    throw CancellationException("Infinite animations are disabled on tests")
                }
                return block()
            }
        }
        recomposerCoroutineScope = CoroutineScope(
            recomposerContinuationInterceptor + frameClock + infiniteAnimationPolicy +
                coroutineExceptionHandler + Job()
        )
        recomposer = Recomposer(recomposerCoroutineScope.coroutineContext)
        composeIdlingResource = ComposeIdlingResource(
            composeRootRegistry, mainClockImpl, recomposer
        )
    }

    internal val testReceiverScope = AndroidComposeUiTestImpl()
    private val testOwner = AndroidTestOwner()
    private val testContext = createTestContext(testOwner)

    /**
     * Returns the current host activity of type [A]. If no such activity is available, for
     * example if you've navigated to a different activity and the original host has now been
     * destroyed, this will return `null`.
     */
    protected abstract val activity: A?

    /**
     * The receiver scope of the test passed to [runTest]. Note that some of the properties and
     * methods will only work during the call to [runTest], as they require that the environment
     * has been set up.
     */
    val test: AndroidComposeUiTest<A> = testReceiverScope

    /**
     * Runs the given [block], setting up all test hooks before running the test and tearing them
     * down after running the test.
     */
    fun <R> runTest(block: AndroidComposeUiTest<A>.() -> R): R {
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
                                    testReceiverScope.withDisposableContent(block)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun waitForIdle(atLeastOneRootExpected: Boolean) {
        // First wait until we have a compose root (in case an Activity is being started)
        composeRootRegistry.waitForComposeRoots(atLeastOneRootExpected)
        // Then await composition(s)
        idlingStrategy.runUntilIdle()
        // Check if a coroutine threw an uncaught exception
        coroutineExceptionHandler.throwUncaught()
    }

    private fun <R> withWindowRecomposer(block: () -> R): R {
        @OptIn(InternalComposeUiApi::class)
        return WindowRecomposerPolicy.withFactory({ recomposer }) {
            try {
                // Start the recomposer:
                recomposerCoroutineScope.launch {
                    recomposer.runRecomposeAndApplyChanges()
                }
                block()
            } finally {
                // Stop the recomposer:
                recomposer.cancel()
                // Cancel our scope to ensure there are no active coroutines when
                // cleanupTestCoroutines is called in the CleanupCoroutinesStatement
                recomposerCoroutineScope.cancel()
            }
        }
    }

    private fun <R> withTestCoroutines(block: () -> R): R {
        try {
            return block()
        } finally {
            // runTest {} as the last step -
            // to replace deprecated TestCoroutineScope.cleanupTestCoroutines
            testCoroutineScope.runTest {}
            testCoroutineScope.cancel()
            coroutineExceptionHandler.throwUncaught()
        }
    }

    private fun <R> withComposeIdlingResource(block: () -> R): R {
        try {
            test.registerIdlingResource(composeIdlingResource)
            return block()
        } finally {
            test.unregisterIdlingResource(composeIdlingResource)
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

    internal inner class AndroidComposeUiTestImpl : AndroidComposeUiTest<A> {
        private var disposeContentHook: (() -> Unit)? = null

        override val activity: A?
            get() = this@AndroidComposeUiTestEnvironment.activity

        override val density: Density by lazy {
            Density(ApplicationProvider.getApplicationContext())
        }

        override val mainClock: MainTestClock
            get() = mainClockImpl

        override fun <T> runOnUiThread(action: () -> T): T {
            return testOwner.runOnUiThread(action)
        }

        override fun <T> runOnIdle(action: () -> T): T {
            // Method below make sure that compose is idle.
            waitForIdle()
            // Execute the action on ui thread in a blocking way.
            return runOnUiThread(action)
        }

        override fun waitForIdle() {
            waitForIdle(atLeastOneRootExpected = true)
        }

        override suspend fun awaitIdle() {
            // First wait until we have a compose root (in case an Activity is being started)
            composeRootRegistry.awaitComposeRoots()
            // Then await composition(s)
            idlingStrategy.awaitIdle()
            // Check if a coroutine threw an uncaught exception
            coroutineExceptionHandler.throwUncaught()
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

        override fun setContent(composable: @Composable () -> Unit) {
            check(disposeContentHook == null) {
                "Cannot call setContent twice per test!"
            }

            // We always make sure we have the latest activity when setting a content
            val currentActivity = checkNotNull(activity) {
                "Cannot set content, host activity not found"
            }
            // Check if the current activity hasn't already called setContent itself
            val root = currentActivity.findViewById<ViewGroup>(android.R.id.content)
            check(root == null || root.childCount == 0) {
                "$currentActivity has already set content. If you have populated the Activity " +
                    "with a ComposeView, make sure to call setContent on that ComposeView " +
                    "instead of on the test rule; and make sure that that call to " +
                    "`setContent {}` is done after the ComposeTestRule has run"
            }

            runOnUiThread {
                currentActivity.setContent(recomposer, composable)
                disposeContentHook = {
                    // Removing a default ComposeView from the view hierarchy will
                    // dispose its composition.
                    activity?.let { it.setContentView(View(it)) }
                }
            }

            // Synchronizing from the UI thread when we can't leads to a dead lock
            if (idlingStrategy.canSynchronizeOnUiThread || !isOnUiThread()) {
                waitForIdle()
            }
        }

        fun <R> withDisposableContent(block: AndroidComposeUiTest<A>.() -> R): R {
            try {
                return block.invoke(this)
            } finally {
                // Dispose the content. The content is disposed by replacing the activity's content
                // with an empty View, breaking potential infinite loops. Just cancelling the
                // Recomposer is not enough, as the infinite loop might not involve recomposition.
                // For example, when there is a layout or draw lambda that keeps invalidating
                // itself. Note that this won't have any effect if the content is not set with
                // ComposeUiTest.setContent, but directly with ComponentActivity.setContent, which
                // would be the typical case when testing an Activity that sets Compose content.
                disposeContentHook?.let {
                    disposeContentHook = null
                    runOnUiThread {
                        // NOTE: currently, calling dispose after an exception that happened during
                        // composition is not a safe call. Compose runtime should fix this, and then
                        // this call will be okay. At the moment, however, calling this could
                        // itself produce an exception which will then obscure the original
                        // exception. To fix this, we will just wrap this call in a try/catch of
                        // its own
                        try {
                            it.invoke()
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    internal inner class AndroidTestOwner : TestOwner {
        override val mainClock: MainTestClock
            get() = mainClockImpl

        override fun sendTextInputCommand(node: SemanticsNode, command: List<EditCommand>) {
            val owner = node.root as ViewRootForTest

            test.runOnIdle {
                val textInputService = owner.getTextInputServiceOrDie()
                val onEditCommand = textInputService.onEditCommand
                    ?: throw IllegalStateException("No input session started. Missing a focus?")
                onEditCommand(command)
            }
        }

        override fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction) {
            val owner = node.root as ViewRootForTest

            test.runOnIdle {
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

internal fun <A : ComponentActivity> ActivityScenario<A>.getActivity(): A? {
    var activity: A? = null
    onActivity { activity = it }
    return activity
}

@ExperimentalTestApi
actual sealed interface ComposeUiTest : SemanticsNodeInteractionsProvider {
    actual val density: Density
    actual val mainClock: MainTestClock
    actual fun <T> runOnUiThread(action: () -> T): T
    actual fun <T> runOnIdle(action: () -> T): T
    actual fun waitForIdle()
    actual suspend fun awaitIdle()
    actual fun waitUntil(timeoutMillis: Long, condition: () -> Boolean)
    actual fun registerIdlingResource(idlingResource: IdlingResource)
    actual fun unregisterIdlingResource(idlingResource: IdlingResource)
    actual fun setContent(composable: @Composable () -> Unit)
}
