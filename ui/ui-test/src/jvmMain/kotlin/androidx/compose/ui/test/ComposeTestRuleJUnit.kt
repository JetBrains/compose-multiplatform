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

import androidx.compose.runtime.Composable
import org.junit.rules.TestRule

/**
 * Enables to run tests of individual composables without having to do manual setup. For Android
 * tests see [createAndroidComposeRule]. Normally this rule is obtained by using [createComposeRule]
 * factory that provides proper implementation (depending if running host side or Android side).
 *
 * However if you really need Android specific dependencies and don't want your test to be abstract
 * you can still create [createAndroidComposeRule] directly and access its underlying Activity.
 */
interface ComposeTestRuleJUnit : ComposeTestRule, TestRule {

    /**
     * Sets the given composable as a content of the current screen.
     *
     * Use this in your tests to setup the UI content to be tested. This should be called exactly
     * once per test.
     *
     * @throws IllegalStateException if called more than once per test.
     */
    // TODO: This should be defined in ComposeTestRule but is currently failing on lambda cast.
    fun setContent(composable: @Composable () -> Unit)

    /**
     * A test rule that allows you to control the animation clock
     */
    val clockTestRule: AnimationClockTestRule
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
expect fun createComposeRule(): ComposeTestRuleJUnit
