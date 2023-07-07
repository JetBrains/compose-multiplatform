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

import androidx.compose.ui.test.internal.JvmDefaultWithCompatibility

/**
 * Represents a resource of an application under test which can cause asynchronous background
 * work to happen during test execution (e.g. an http request in response to a button click).
 *
 * By default, all interactions from the test with the compose tree (finding nodes, performing
 * gestures, making assertions) will be synchronized with pending work in Compose's internals
 * (such as applying state changes, recomposing, measuring, etc). This ensures that the UI is in
 * a stable state when the interactions are performed, so that e.g. no pending recompositions are
 * still scheduled that could potentially change the UI. However, any asynchronous work that is
 * not done through one of Compose's mechanisms won't be included in the default synchronization.
 * For such work, test authors can create an [IdlingResource] and register it into the test with
 * [registerIdlingResource][androidx.compose.ui.test.junit4.ComposeTestRule
 * .registerIdlingResource], and the interaction will wait for that resource to become idle prior
 * to performing it.
 */
@JvmDefaultWithCompatibility
interface IdlingResource {
    /**
     * Whether or not the [IdlingResource] is idle when reading this value. This should always be
     * called from the main thread, which is why it should be lightweight and fast.
     *
     * If one idling resource returns `false`, the synchronization system will keep polling all
     * idling resources until they are all idle.
     */
    val isIdleNow: Boolean

    /**
     * Returns diagnostics that explain why the idling resource is busy, or `null` if the
     * resource is not busy. Default implementation returns `null`.
     */
    fun getDiagnosticMessageIfBusy(): String? = null
}
