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
package androidx.compose.desktop

object AppManager {

    init {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            public override fun run() {
                onAppExit?.invoke()
            }
        })
    }

    fun onEvent(
        onAppStart: (() -> Unit)? = null,
        onAppExit: (() -> Unit)? = null,
        onWindowsEmpty: (() -> Unit)? = null
    ) {
        this.onAppStart = onAppStart
        this.onAppExit = onAppExit
        this.onWindowsEmptyAction = onWindowsEmpty
    }

    val defaultActionOnWindowsEmpty: () -> Unit = { System.exit(0) }

    private var onWindowsEmptyAction: (() -> Unit)? = defaultActionOnWindowsEmpty

    private var onAppStart: (() -> Unit)? = null

    private var onAppExit: (() -> Unit)? = null

    private val windows = mutableSetOf<AppFrame>()

    internal fun addWindow(window: AppFrame): Boolean {
        if (windows.isEmpty()) {
            onAppStart?.invoke()
        }
        if (windows.contains(window)) {
            return false
        }
        windows.add(window)
        return true
    }

    internal fun removeWindow(window: AppFrame) {
        windows.remove(window)
        window.dispose()
        if (windows.isEmpty()) {
            onWindowsEmptyAction?.invoke()
        }
    }

    fun getWindows(): Collection<AppFrame> {
        return windows.toList()
    }

    fun getCurrentFocusedWindow(): AppFrame? {
        for (current in windows) {
            if (current.window.isFocused) {
                return current
            }
        }
        return null
    }

    fun exit() {
        val dialogOrderedWindowsList = mutableListOf<AppFrame>()
        for (frame in windows) {
            if (frame.invoker != null) {
                dialogOrderedWindowsList.add(frame)
            }
        }
        for (frame in dialogOrderedWindowsList.union(windows)) {
            frame.close()
        }
    }
}
