/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.snapshotFlow
import kotlinx.coroutines.flow.collect

/**
 * Provides information about the Window that is hosting this compose hierarchy.
 */
@Stable
interface WindowInfo {
    /**
     * Indicates whether the window hosting this compose hierarchy is in focus.
     *
     * When there are multiple windows visible, either in a multi-window environment or if a
     * popup or dialog is visible, this property can be used to determine if the current window
     * is in focus.
     */
    val isWindowFocused: Boolean
}

// TODO(b/177085155): Remove after Alpha 11.
/**
 * Provides information about the Window that is hosting this compose hierarchy.
 */
@Stable
@Deprecated(
    message = "Use WindowInfo instead.",
    replaceWith = ReplaceWith("WindowInfo", "androidx.compose.ui.platform.WindowInfo"),
    level = DeprecationLevel.ERROR
)
interface WindowManager {
    val isWindowFocused: Boolean
}

// TODO(b/177085040):  Remove after Alpha 11.
/**
 * Provides a callback that is called whenever the window gains or loses focus.
 */
@Deprecated(
    message = "Use AmbientWindowInfo.current.isWIndowFocused instead.",
    level = DeprecationLevel.ERROR
)
@Composable
fun WindowFocusObserver(onWindowFocusChanged: (isWindowFocused: Boolean) -> Unit) {
    WindowFocusObserver1(onWindowFocusChanged)
}

// TODO(b/177085040): Rename this to WindowFocusObserver after removing WindowFocusObserver.
@OptIn(ExperimentalComposeApi::class)
@Composable
internal fun WindowFocusObserver1(onWindowFocusChanged: (isWindowFocused: Boolean) -> Unit) {
    val windowInfo = AmbientWindowInfo.current
    val callback = rememberUpdatedState(onWindowFocusChanged)
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { callback.value(it) }
    }
}

internal class WindowInfoImpl : WindowInfo {
    private val _isWindowFocused = mutableStateOf(false)
    override var isWindowFocused: Boolean
        set(value) { _isWindowFocused.value = value }
        get() = _isWindowFocused.value
}