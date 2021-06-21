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

import android.annotation.SuppressLint
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.InfiniteAnimationPolicy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.platform.textInputServiceFactory
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.TestMonotonicFrameClock
import androidx.compose.ui.test.TestOwner
import androidx.compose.ui.test.createTestContext
import androidx.compose.ui.test.junit4.android.ComposeIdlingResource
import androidx.compose.ui.test.junit4.android.ComposeRootRegistry
import androidx.compose.ui.test.junit4.android.EspressoLink
import androidx.compose.ui.test.junit4.android.awaitComposeRoots
import androidx.compose.ui.test.junit4.android.waitForComposeRoots
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

actual fun createComposeRule(): ComposeContentTestRule =
    createAndroidComposeRule<ComponentActivity>()

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * activity class type [A].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * tests where the compose content is set by that Activity, instead of via the test rule's
 * [setContent][ComposeContentTestRule.setContent]. Make sure that you add the provided activity
 * into your app's manifest file (usually in main/AndroidManifest.xml).
 *
 * This creates a test rule that is using [ActivityScenarioRule] as the activity launcher. If you
 * would like to use a different one you can create [AndroidComposeTestRule] directly and supply
 * it with your own launcher.
 *
 * If your test doesn't require a specific Activity, use [createComposeRule] instead.
 */
inline fun <reified A : ComponentActivity> createAndroidComposeRule():
    AndroidComposeTestRule<ActivityScenarioRule<A>, A> {
        // TODO(b/138993381): By launching custom activities we are losing control over what content is
        //  already there. This is issue in case the user already set some compose content and decides
        //  to set it again via our API. In such case we won't be able to dispose the old composition.
        //  Other option would be to provide a smaller interface that does not expose these methods.
        return createAndroidComposeRule(A::class.java)
    }

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * [activityClass].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * tests where the compose content is set by that Activity, instead of via the test rule's
 * [setContent][ComposeContentTestRule.setContent]. Make sure that you add the provided activity
 * into your app's manifest file (usually in main/AndroidManifest.xml).
 *
 * This creates a test rule that is using [ActivityScenarioRule] as the activity launcher. If you
 * would like to use a different one you can create [AndroidComposeTestRule] directly and supply
 * it with your own launcher.
 *
 * If your test doesn't require a specific Activity, use [createComposeRule] instead.
 */
fun <A : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<A>
): AndroidComposeTestRule<ActivityScenarioRule<A>, A> = AndroidComposeTestRule(
    activityRule = ActivityScenarioRule(activityClass),
    activityProvider = { it.getActivity() }
)

/**
 * Factory method to provide an implementation of [ComposeTestRule] that doesn't create a compose
 * host for you in which you can set content.
 *
 * This method is useful for tests that need to create their own compose host during the test.
 * The returned test rule will not create a host, and consequently does not provide a
 * `setContent` method. To set content in tests using this rule, use the appropriate `setContent`
 * methods from your compose host.
 *
 * A typical use case on Android is when the test needs to launch an Activity (the compose host)
 * after one or more dependencies have been injected.
 */
fun createEmptyComposeRule(): ComposeTestRule =
    AndroidComposeTestRule<TestRule, ComponentActivity>(
        activityRule = TestRule { base, _ -> base },
        activityProvider = {
            error(
                "createEmptyComposeRule() does not provide an Activity to set Compose content in." +
                    " Launch and use the Activity yourself, or use createAndroidComposeRule()."
            )
        }
    )

/**
 * Android specific implementation of [ComposeContentTestRule], where compose content is hosted
 * by an Activity.
 *
 * The Activity is normally launched by the given [activityRule] before the test starts, but it
 * is possible to pass a test rule that chooses to launch an Activity on a later time. The
 * Activity is retrieved from the [activityRule] by means of the [activityProvider], which can be
 * thought of as a getter for the Activity on the [activityRule]. If you use an [activityRule]
 * that launches an Activity on a later time, you should make sure that the Activity is launched
 * by the time or while the [activityProvider] is called.
 *
 * The [AndroidComposeTestRule] wraps around the given [activityRule] to make sure the Activity
 * is launched _after_ the [AndroidComposeTestRule] has completed all necessary steps to control
 * and monitor the compose content.
 *
 * @param activityRule Test rule to use to launch the Activity.
 * @param activityProvider Function to retrieve the Activity from the given [activityRule].
 */
@OptIn(InternalTestApi::class)
class AndroidComposeTestRule<R : TestRule, A : ComponentActivity>(
    val activityRule: R,
    private val activityProvider: (R) -> A,
) : ComposeContentTestRule {

    /**
     * Provides the current activity.
     *
     * Avoid calling often as it can involve synchronization and can be slow.
     */
    val activity: A get() = activityProvider(activityRule)

    private val idlingResourceRegistry = IdlingResourceRegistry()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val composeRootRegistry = ComposeRootRegistry()

    private val mainClockImpl: MainTestClockImpl
    private val composeIdlingResource: ComposeIdlingResource
    private val idlingStrategy: IdlingStrategy by lazy { idlingStrategyFactory.invoke() }

    private val recomposer: Recomposer
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testCoroutineDispatcher: TestCoroutineDispatcher
    private val recomposerApplyCoroutineScope: CoroutineScope
    private val frameCoroutineScope: CoroutineScope
    private val coroutineExceptionHandler = UncaughtExceptionHandler()

    override val mainClock: MainTestClock
        get() = mainClockImpl

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        testCoroutineDispatcher = TestCoroutineDispatcher()
        frameCoroutineScope = CoroutineScope(testCoroutineDispatcher)
        @OptIn(ExperimentalCoroutinesApi::class)
        val frameClock = TestMonotonicFrameClock(frameCoroutineScope)
        mainClockImpl = MainTestClockImpl(testCoroutineDispatcher, frameClock)
        val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
            override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
                if (mainClockImpl.autoAdvance) {
                    throw CancellationException()
                }
                return block()
            }
        }
        @OptIn(ExperimentalCoroutinesApi::class)
        recomposerApplyCoroutineScope = CoroutineScope(
            testCoroutineDispatcher + frameClock + infiniteAnimationPolicy +
                coroutineExceptionHandler + Job()
        )
        recomposer = Recomposer(recomposerApplyCoroutineScope.coroutineContext)
            .also { recomposerApplyCoroutineScope.launch { it.runRecomposeAndApplyChanges() } }
        composeIdlingResource = ComposeIdlingResource(
            composeRootRegistry, mainClockImpl, recomposer
        )
        registerIdlingResource(composeIdlingResource)
    }

    private var idlingStrategyFactory: () -> IdlingStrategy = {
        EspressoLink(idlingResourceRegistry)
    }

    internal var disposeContentHook: (() -> Unit)? = null

    private val testOwner = AndroidTestOwner()
    private val testContext = createTestContext(testOwner)

    override val density: Density by lazy {
        Density(ApplicationProvider.getApplicationContext())
    }

    override fun apply(base: Statement, description: Description): Statement {
        if (RobolectricDetector.usesRobolectricTestRunner(description)) {
            setIdlingStrategyFactory {
                RobolectricIdlingStrategy(composeRootRegistry, composeIdlingResource)
            }
        }
        @Suppress("NAME_SHADOWING")
        return RuleChain
            .outerRule { base, _ -> composeRootRegistry.getStatementFor(base) }
            .around { base, _ -> idlingResourceRegistry.getStatementFor(base) }
            .around { base, _ -> idlingStrategy.getStatementFor(base) }
            .around { base, _ -> AndroidComposeStatement(base) }
            .around(activityRule)
            .apply(base, description)
    }

    /**
     * Replaces the current [IdlingStrategy] factory with the given [factory]. The strategy is
     * created lazy with the factory. Note that on Robolectric tests, the factory is usually
     * overwritten during the [apply] method. If you need to set a custom factory on Robolectric,
     * you'll need to do so after rules are applied, e.g. in an @Before method.
     *
     * The default factory creates a strategy built on Espresso, and is set to a Robolectric
     * compatible factory on Robolectric tests.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    internal fun setIdlingStrategyFactory(factory: () -> IdlingStrategy) {
        idlingStrategyFactory = factory
    }

    /**
     * @throws IllegalStateException if called more than once per test.
     */
    @SuppressWarnings("SyntheticAccessor")
    override fun setContent(composable: @Composable () -> Unit) {
        check(disposeContentHook == null) {
            "Cannot call setContent twice per test!"
        }

        // We always make sure we have the latest activity when setting a content
        val currentActivity = activity

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

    @SuppressWarnings("DocumentExceptions") // The interface doc already documents this
    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val startTime = System.nanoTime()
        while (!condition()) {
            if (mainClockImpl.autoAdvance) {
                mainClock.advanceTimeByFrame()
            }
            // Let Android run measure, draw and in general any other async operations.
            Thread.sleep(10)
            if (System.nanoTime() - startTime > timeoutMillis * 1_000_000) {
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

    inner class AndroidComposeStatement(
        private val base: Statement
    ) : Statement() {

        @OptIn(InternalComposeUiApi::class)
        override fun evaluate() {
            WindowRecomposerPolicy.withFactory({ recomposer }) {
                evaluateInner()
            }
        }

        @OptIn(InternalComposeUiApi::class)
        private fun evaluateInner() {
            val oldTextInputFactory = textInputServiceFactory
            try {
                textInputServiceFactory = {
                    TextInputServiceForTests(it)
                }
                base.evaluate()
            } finally {
                textInputServiceFactory = oldTextInputFactory
                recomposer.cancel()
                // FYI: Not canceling these scope below would end up cleanupTestCoroutines
                // throwing errors on active coroutines
                recomposerApplyCoroutineScope.cancel()
                frameCoroutineScope.cancel()
                coroutineExceptionHandler.throwUncaught()
                @OptIn(ExperimentalCoroutinesApi::class)
                testCoroutineDispatcher.cleanupTestCoroutines()
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

    @OptIn(InternalTestApi::class)
    internal inner class AndroidTestOwner : TestOwner {

        override val mainClock: MainTestClock
            get() = mainClockImpl

        @SuppressLint("DocumentExceptions")
        override fun sendTextInputCommand(node: SemanticsNode, command: List<EditCommand>) {
            val owner = node.root as ViewRootForTest

            runOnIdle {
                val textInputService = owner.getTextInputServiceOrDie()
                val onEditCommand = textInputService.onEditCommand
                    ?: throw IllegalStateException("No input session started. Missing a focus?")
                onEditCommand(command)
            }
        }

        @SuppressLint("DocumentExceptions")
        override fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction) {
            val owner = node.root as ViewRootForTest

            runOnIdle {
                val textInputService = owner.getTextInputServiceOrDie()
                val onImeActionPerformed = textInputService.onImeActionPerformed
                    ?: throw IllegalStateException("No input session started. Missing a focus?")
                onImeActionPerformed.invoke(actionSpecified)
            }
        }

        @SuppressLint("DocumentExceptions")
        override fun <T> runOnUiThread(action: () -> T): T {
            return androidx.compose.ui.test.junit4.runOnUiThread(action)
        }

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            // TODO(pavlis): Instead of returning a flatMap, let all consumers handle a tree
            //  structure. In case of multiple AndroidOwners, add a fake root
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

private fun <A : ComponentActivity> ActivityScenarioRule<A>.getActivity(): A {
    var activity: A? = null
    scenario.onActivity { activity = it }
    if (activity == null) {
        throw IllegalStateException("Activity was not set in the ActivityScenarioRule!")
    }
    return activity!!
}
