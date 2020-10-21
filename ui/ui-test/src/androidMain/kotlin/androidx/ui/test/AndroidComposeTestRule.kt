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

package androidx.ui.test

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.createAndroidComposeRule

/** @Deprecated Moved to androidx.compose.ui.test */
typealias AndroidComposeTestRule = androidx.compose.ui.test.AndroidComposeTestRule<*>

/** @Deprecated Moved to androidx.compose.ui.test */
inline fun <reified T : ComponentActivity> createAndroidComposeRule() =
    createAndroidComposeRule<T>()

/** @Deprecated Moved to androidx.compose.ui.test */
fun <T : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<T>
) = createAndroidComposeRule(activityClass)