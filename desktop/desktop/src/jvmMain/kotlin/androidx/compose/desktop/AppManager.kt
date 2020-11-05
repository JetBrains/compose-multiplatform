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

import androidx.compose.ui.window.MenuBar

object AppManager {

    init {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            public override fun run() {
                onAppExit?.invoke()
            }
        })
    }

    internal var sharedMenuBar: MenuBar? = null

    val defaultActionOnWindowsEmpty: () -> Unit = { System.exit(0) }

    private var onWindowsEmptyAction: (() -> Unit)? = defaultActionOnWindowsEmpty

    private var onAppStart: (() -> Unit)? = null

    private var onAppExit: (() -> Unit)? = null

    private val windowsList = mutableSetOf<AppFrame>()

    val windows: List<AppFrame>
        get() = windowsList.toList()

    val focusedWindow: AppFrame?
        get() = currentFocusedWindow()

    private fun currentFocusedWindow(): AppFrame? {
        for (current in windowsList) {
            if (current.window.isFocused) {
                return current
            }
        }
        return null
    }

    internal fun addWindow(window: AppFrame): Boolean {
        if (windowsList.isEmpty()) {
            onAppStart?.invoke()
        }
        if (windowsList.contains(window)) {
            return false
        }
        windowsList.add(window)
        return true
    }

    internal fun removeWindow(window: AppFrame) {
        windowsList.remove(window)
        window.dispose()
        if (windowsList.isEmpty()) {
            onWindowsEmptyAction?.invoke()
        }
    }

    fun setEvents(
        onAppStart: (() -> Unit)? = null,
        onAppExit: (() -> Unit)? = null,
        onWindowsEmpty: (() -> Unit)? = null
    ) {
        this.onAppStart = onAppStart
        this.onAppExit = onAppExit
        this.onWindowsEmptyAction = onWindowsEmpty
    }

    fun setMenu(menuBar: MenuBar?) {
        sharedMenuBar = menuBar
    }

    fun exit() {
        val dialogOrderedWindowsList = mutableListOf<AppFrame>()
        for (frame in windowsList) {
            if (frame.invoker != null) {
                dialogOrderedWindowsList.add(frame)
            }
        }
        for (frame in dialogOrderedWindowsList.union(windowsList)) {
            frame.close()
        }
    }
}
