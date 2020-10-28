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

package androidx.ui.test.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.AndroidComposeTestRule

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * activity class type [T].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * app tests. Make sure that you add the provided activity into your app's manifest file (usually
 * in main/AndroidManifest.xml).
 *
 * If you don't care about specific activity and just want to test composables in general, see
 * [createComposeRule].
 *
 * @Deprecated Moved to androidx.compose.ui.test
 */
@Deprecated(
    "Moved to androidx.compose.ui.test",
    ReplaceWith("androidx.compose.ui.test.createAndroidComposeRule<T>()")
)
@Suppress("UNUSED_PARAMETER")
inline fun <reified T : ComponentActivity> createAndroidComposeRule(
    disableTransitions: Boolean = false,
    disableBlinkingCursor: Boolean = true
): AndroidComposeTestRule<T> = androidx.compose.ui.test.junit4.createAndroidComposeRule()

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * [activityClass].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * app tests. Make sure that you add the provided activity into your app's manifest file (usually
 * in main/AndroidManifest.xml).
 *
 * If you don't care about specific activity and just want to test composables in general, see
 * [createComposeRule].
 *
 * @Deprecated Moved to androidx.compose.ui.test
 */
@Deprecated(
    "Moved to androidx.compose.ui.test",
    ReplaceWith("androidx.compose.ui.test.createAndroidComposeRule(activityClass)")
)
@Suppress("UNUSED_PARAMETER")
fun <T : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<T>,
    disableTransitions: Boolean = false,
    disableBlinkingCursor: Boolean = true
): AndroidComposeTestRule<T> =
    androidx.compose.ui.test.junit4.createAndroidComposeRule(activityClass)
