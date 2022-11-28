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

/**
 * Runs the given action on the UI thread.
 *
 * This method is blocking until the action is complete.
 */
internal actual fun <T> runOnUiThread(action: () -> T): T {
    return action()
}

/**
 * Returns if the call is made on the main thread.
 */
internal actual fun isOnUiThread(): Boolean = true
