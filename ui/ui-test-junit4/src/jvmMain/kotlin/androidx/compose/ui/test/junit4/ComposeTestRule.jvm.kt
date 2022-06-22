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
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.unit.Density
import org.junit.rules.TestRule
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * A [TestRule] that allows you to test and control composables and applications using Compose.
 * Most of the functionality in this interface provides some form of test synchronization: the
 * test will block until the app or composable is idle, to ensure the tests are deterministic.
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
 * An instance of [ComposeTestRule] can be created with [createComposeRule], which will also
 * create a host for the compose content for you (see [ComposeContentTestRule]). If you need to
 * specify which particular Activity is started on Android, you can use [createAndroidComposeRule].
 *
 * If you don't want any Activity to be started automatically by the test rule on Android, you
 * can use [createEmptyComposeRule]. In such a case, you will have to set content using one of
 * Compose UI's setters (like [ComponentActivity.setContent][androidx.compose.ui.platform
 * .setContent]).
 */
@JvmDefaultWithCompatibility
interface ComposeTestRule : TestRule, SemanticsNodeInteractionsProvider {
    /**
     * Current device screen's density.
     */
    val density: Density

    /**
     * Clock that drives frames and recompositions in compose tests.
     */
    val mainClock: MainTestClock

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
     *
     * In case the main clock auto advancement is enabled (by default is) this will also keep
     * advancing the clock until it is idle (meaning there are no recompositions, animations, etc.
     * pending). If not, this will wait only for other idling resources.
     */
    fun <T> runOnIdle(action: () -> T): T

    /**
     * Waits for compose to be idle.
     *
     * This is a blocking call. Returns only after compose is idle.
     *
     * In case the main clock auto advancement is enabled (by default is) this will also keep
     * advancing the clock until it is idle (meaning there are no recompositions, animations, etc.
     * pending). If not, this will wait only for other idling resources.
     *
     * Can crash in case there is a time out. This is not supposed to be handled as it
     * surfaces only in incorrect tests.
     */
    fun waitForIdle()

    /**
     * Suspends until compose is idle. Compose is idle if there are no pending compositions, no
     * pending changes that could lead to another composition, and no pending draw calls.
     *
     * In case the main clock auto advancement is enabled (by default is) this will also keep
     * advancing the clock until it is idle (meaning there are no recompositions, animations, etc.
     * pending). If not, this will wait only for other idling resources.
     */
    suspend fun awaitIdle()

    /**
     * Blocks until the given condition is satisfied.
     *
     * In case the main clock auto advancement is enabled (by default is), this will also keep
     * advancing the clock on a frame by frame basis and yield for other async work at the end of
     * each frame. If the advancement of the main clock is not enabled this will work as a
     * countdown latch without any other advancements.
     *
     * There is also [MainTestClock.advanceTimeUntil] which is faster as it does not yield back
     * the UI thread.
     *
     * This method should be used in cases where [MainTestClock.advanceTimeUntil]
     * is not enough.
     *
     * @param timeoutMillis The time after which this method throws an exception if the given
     * condition is not satisfied. This is the wall clock time not the main clock one.
     * @param condition Condition that must be satisfied in order for this method to successfully
     * finish.
     *
     * @throws ComposeTimeoutException If the condition is not satisfied after [timeoutMillis].
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
 * A [ComposeTestRule] that allows you to set content without the necessity to provide a host for
 * the content. The host, such as an Activity, will be created by the test rule.
 *
 * An instance of [ComposeContentTestRule] can be created with [createComposeRule]. If you need to
 * specify which particular Activity is started on Android, you can use [createAndroidComposeRule].
 *
 * If you don't want any host to be started automatically by the test rule on Android, you
 * can use [createEmptyComposeRule]. In such a case, you will have to create a host in your test
 * and set the content using one of Compose UI's setters (like [ComponentActivity
 * .setContent][androidx.activity.compose.setContent]).
 */
@JvmDefaultWithCompatibility
interface ComposeContentTestRule : ComposeTestRule {
    /**
     * Sets the given composable as a content of the current screen.
     *
     * Use this in your tests to setup the UI content to be tested. This should be called exactly
     * once per test.
     *
     * @throws IllegalStateException if called more than once per test.
     */
    fun setContent(composable: @Composable () -> Unit)
}

/**
 * Factory method to provide an implementation of [ComposeContentTestRule].
 *
 * This method is useful for tests in compose libraries where it is irrelevant where the compose
 * content is hosted (e.g. an Activity on Android). Such tests typically set compose content
 * themselves via [setContent][ComposeContentTestRule.setContent] and only instrument and assert
 * that content.
 *
 * For Android this will use the default Activity (android.app.Activity). You need to add a
 * reference to this activity into the manifest file of the corresponding tests (usually in
 * androidTest/AndroidManifest.xml). If your Android test requires a specific Activity to be
 * launched, see [createAndroidComposeRule].
 */
expect fun createComposeRule(): ComposeContentTestRule
