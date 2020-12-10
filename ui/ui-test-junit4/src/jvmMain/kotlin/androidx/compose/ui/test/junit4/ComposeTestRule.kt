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
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.junit.rules.TestRule

/**
 * Enables to run tests of individual composables without having to do manual setup. For Android
 * tests see [createAndroidComposeRule]. Normally this rule is obtained by using [createComposeRule]
 * factory that provides proper implementation (depending if running host side or Android side).
 *
 * However if you really need Android specific dependencies and don't want your test to be abstract
 * you can still create [createAndroidComposeRule] directly and access its underlying Activity.
 */
interface ComposeTestRule : TestRule, SemanticsNodeInteractionsProvider {
    /**
     * Current device screen's density.
     */
    val density: Density

    /**
     * Current device display's size.
     */
    val displaySize: IntSize get

    /**
     * A test rule that allows you to control the animation clock
     */
    @OptIn(ExperimentalTesting::class)
    val clockTestRule: AnimationClockTestRule

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

    /**
     * Registers an [IdlingResource] in this test.
     */
    fun registerIdlingResource(idlingResource: IdlingResource)

    /**
     * Unregisters an [IdlingResource] from this test.
     */
    fun unregisterIdlingResource(idlingResource: IdlingResource)

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
 * Factory method to provide implementation of [ComposeTestRule].
 *
 * This method is useful for tests in compose libraries where no custom Activity is usually
 * needed. For app tests or launching custom activities, see [createAndroidComposeRule].
 *
 * For Android this will use the default Activity (android.app.Activity). You need to add a
 * reference to this activity into the manifest file of the corresponding tests (usually in
 * androidTest/AndroidManifest.xml).
 */
expect fun createComposeRule(): ComposeTestRule
