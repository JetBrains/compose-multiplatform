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
 * A test environment that allows you to test and control composables and applications using
 * Compose. Most of the functionality in this interface provides some form of test synchronization:
 * the test will block until the app or composable is idle, to ensure the tests are deterministic.
 *
 * For example, if you would perform a click on the center of the screen while a button is
 * animation from left to right over the screen, without synchronization the test would sometimes
 * click when the button is in the middle of the screen (button is clicked), and sometimes when
 * the button is past the middle of the screen (button is not clicked). With synchronization, the
 * app would not be idle until the animation is over, so the test will always click when the
 * button is past the middle of the screen (and not click it). If you actually do want to click
 * the button when it's in the middle of the animation, you can do so by controlling the
 * [clock][mainClock]. You'll have to disable [automatic advancing][MainTestClock.autoAdvance],
 * and manually advance the clock by the time necessary to position the button in the middle of
 * the screen.
 *
 * Compared to [ComposeTest], [ProvidedComposeContentTest] does not offer a `setContent` method.
 * Instead, users are expected to set up their own Compose host and content after having obtained
 * an instance of [ProvidedComposeContentTest], such as starting an Activity that sets the Compose
 * content in its onCreate method on Android.
 *
 * An instance of [ProvidedComposeContentTest] can be obtained by calling
 * [withProvidedComposeContentTest], the argument to which will have it as the receiver scope.
 * Check the documentation for your platform to find out if it offers a convenience method that
 * sets up a Compose host of your choosing.
 */
// Keep internal while the shape of this API hasn't yet been decided
internal interface ProvidedComposeContentTest : SemanticsNodeInteractionsProvider {
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
}

/**
 * A test environment that allows you to set content without the necessity to provide a host for
 * the content. The host, such as an Activity on Android, will be created by the method through
 * which you obtain an instance of a [ComposeTest]. It will not be accessible though, as it will
 * differ from platform to platform. See [ProvidedComposeContentTest] for all other functionality.
 *
 * An instance of [ComposeTest] can be obtained by calling [withComposeTest], the argument to
 * which will have it as the receiver scope.
 */
// Keep internal while the shape of this API hasn't yet been decided
internal interface ComposeTest : ProvidedComposeContentTest {
    /**
     * Sets the given [composable] as the content to be tested. This should be called exactly
     * once per test.
     *
     * @throws IllegalStateException if called more than once per test.
     */
    fun setContent(composable: @Composable () -> Unit)
}

internal const val NanoSecondsPerMilliSecond = 1_000_000L