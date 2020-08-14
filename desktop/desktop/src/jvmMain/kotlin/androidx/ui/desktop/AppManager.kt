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
package androidx.ui.desktop

object AppManager {

    val defaultActionOnWindowsEmpty: () -> Unit = { System.exit(0) }

    var onWindowsEmptyAction = defaultActionOnWindowsEmpty

    private val windows = mutableSetOf<AppFrame>()

    internal fun addWindow(window: AppFrame): Boolean {
        if (windows.contains(window)) {
            return false
        }
        windows.add(window)
        return true
    }

    internal fun removeWindow(window: AppFrame) {
        windows.remove(window)
        if (windows.isEmpty()) {
            onWindowsEmptyAction.invoke()
        }
    }

    fun getWindows(): Collection<AppFrame> {
        return windows.toList()
    }

    fun getCurrentFocusedWindow(): AppFrame? {
        for (current in windows) {
            if (current.window!!.isFocused) {
                return current
            }
        }
        return null
    }
}
