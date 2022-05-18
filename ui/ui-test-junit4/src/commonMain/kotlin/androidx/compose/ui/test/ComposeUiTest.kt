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

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density

/**
 * Sets up the test environment, runs the given [test][block] and then tears down the test
 * environment. Use the methods on [ComposeUiTest] in the test to find Compose content and make
 * assertions on it. If you need access to platform specific elements (such as the Activity on
 * Android), use one of the platform specific variants of this method, e.g.
 * [runAndroidComposeUiTest] on Android.
 *
 * Implementations of this method will launch a Compose host (such as an Activity on Android)
 * for you. If your test needs to launch its own host, use a platform specific variant that
 * doesn't launch anything for you (if available), e.g. [runEmptyComposeUiTest] on
 * Android. Always make sure that the Compose content is set during execution of the
 * [test lambda][block] so the test framework is aware of the content. Whether you need to
 * launch the host from within the test lambda as well depends on the platform.
 *
 * Keeping a reference to the [ComposeUiTest] outside of this function is an error.
 */
@ExperimentalTestApi
expect fun runComposeUiTest(block: ComposeUiTest.() -> Unit)

/**
 * A test environment that allows you to test and control composables, either in isolation or in
 * applications. Most of the functionality in this interface provides some form of test
 * synchronization: the test will block until the app or composable is idle, to ensure the tests
 * are deterministic.
 *
 * For example, if you would perform a click on the center of the screen while a button is
 * animating from left to right over the screen, without synchronization the test would sometimes
 * click when the button is in the middle of the screen (button is clicked), and sometimes when
 * the button is past the middle of the screen (button is not clicked). With synchronization, the
 * app would not be idle until the animation is over, so the test will always click when the
 * button is past the middle of the screen (and not click it). If you actually do want to click
 * the button when it's in the middle of the animation, you can do so by controlling the
 * [clock][mainClock]. You'll have to disable [automatic advancing][MainTestClock.autoAdvance],
 * and manually advance the clock by the time necessary to position the button in the middle of
 * the screen.
 *
 * To test a composable in isolation, use [setContent] to set the composable in a host. On Android,
 * a host will mostly be an Activity. When using [runComposeUiTest] or any of its platform specific
 * friends, the host will be started for you automatically, unless otherwise specified. To test an
 * application, use the platform specific variant of [runComposeUiTest] that launches the app.
 *
 * An instance of [ComposeUiTest] can be obtained through [runComposeUiTest] or any of its
 * platform specific variants, the argument to which will have it as the receiver scope.
 */
// Use an `expect sealed interface` with an actual copy for each platform to allow implementations
// per platform. Each platform is considered a separate compilation unit, which means that when
// just using `sealed interface` in commonMain, it would not be allowed to implement the interface
// in platform specific code.
@ExperimentalTestApi
expect sealed interface ComposeUiTest : SemanticsNodeInteractionsProvider {
    /**
     * Current device screen's density. Note that it is technically possible for a Compose
     * hierarchy to define a different density for a certain subtree. Try to use
     * [LayoutInfo.density][androidx.compose.ui.layout.LayoutInfo.density]
     * where possible, which can be obtained from
     * [SemanticsNode.layoutInfo][androidx.compose.ui.semantics.SemanticsNode.layoutInfo].
     */
    val density: Density

    /**
     * Clock that drives frames and recompositions in compose tests.
     */
    val mainClock: MainTestClock

    /**
     * Runs the given [action] on the UI thread.
     *
     * This method blocks until the action is complete.
     */
    fun <T> runOnUiThread(action: () -> T): T

    /**
     * Executes the given [action] in the same way as [runOnUiThread] but [waits][waitForIdle]
     * until the app is idle before executing the action. This is the recommended way of doing
     * your assertions on shared variables.
     *
     * This method blocks until the action is complete.
     */
    fun <T> runOnIdle(action: () -> T): T

    /**
     * Waits for compose to be idle. If [auto advancement][MainTestClock.autoAdvance] is enabled
     * on the [mainClock], this method will actively advance the clock to process any pending
     * composition, invalidation and animation. If auto advancement is not enabled, the clock will
     * not be advanced actively which usually means that the Compose UI appears to be frozen. This
     * is ideal for testing animations in a deterministic way. In either case, this method will
     * wait for all [IdlingResource]s to become idle.
     *
     * Note that some processes are driven by the host operating system and will therefore still
     * execute when auto advancement is disabled. For example, on Android measure, layout and draw
     * can still happen if the host view is invalidated by other parts of the View hierarchy.
     */
    fun waitForIdle()

    /**
     * Suspends until compose is idle. If [auto advancement][MainTestClock.autoAdvance] is enabled
     * on the [mainClock], this method will actively advance the clock to process any pending
     * composition, invalidation and animation. If auto advancement is not enabled, the clock will
     * not be advanced actively which usually means that the Compose UI appears to be frozen. This
     * is ideal for testing animations in a deterministic way. In either case, this method will
     * wait for all [IdlingResource]s to become idle.
     *
     * Note that some processes are driven by the host operating system and will therefore still
     * execute when auto advancement is disabled. For example, on Android measure, layout and draw
     * can still happen if the host view is invalidated by other parts of the View hierarchy.
     */
    suspend fun awaitIdle()

    /**
     * Blocks until the given [condition] is satisfied.
     *
     * If [auto advancement][MainTestClock.autoAdvance] is enabled on the [mainClock], this method
     * will actively advance the clock to process any pending composition, invalidation and
     * animation. If auto advancement is not enabled, the clock will not be advanced actively
     * which usually means that the Compose UI appears to be frozen. This is ideal for testing
     * animations in a deterministic way. In either case, this method will wait for all
     * [IdlingResource]s to become idle.
     *
     * Note that some processes are driven by the host operating system and will therefore still
     * execute when auto advancement is disabled. For example, on Android measure, layout and draw
     * can still happen if the host view is invalidated by other parts of the View hierarchy.
     *
     * Compared to [MainTestClock.advanceTimeUntil], [waitUntil] sleeps after every iteration to
     * give the host operating system the opportunity to do measure/layout/draw passes. This gives
     * [waitUntil] a better integration with the host, but it is less preferred from a performance
     * viewpoint. Therefore, we recommend that you try using [MainTestClock.advanceTimeUntil]
     * before resorting to [waitUntil].
     *
     * @param timeoutMillis The time after which this method throws an exception if the given
     * condition is not satisfied. This observes wall clock time, not [frame time][mainClock].
     * @param condition Condition that must be satisfied in order for this method to successfully
     * finish.
     *
     * @throws ComposeTimeoutException If the condition is not satisfied after [timeoutMillis]
     * (in wall clock time).
     */
    fun waitUntil(timeoutMillis: Long = 1_000, condition: () -> Boolean)

    /**
     * Registers an [IdlingResource] in this test.
     */
    fun registerIdlingResource(idlingResource: IdlingResource)

    /**
     * Unregisters an [IdlingResource] from this test.
     */
    fun unregisterIdlingResource(idlingResource: IdlingResource)

    /**
     * Sets the given [composable] as the content to be tested. This should be called exactly
     * once per test.
     *
     * @throws IllegalStateException if called more than once per test, or if the implementation
     * doesn't have access to a host to set content in.
     */
    fun setContent(composable: @Composable () -> Unit)
}

internal const val NanoSecondsPerMilliSecond = 1_000_000L
