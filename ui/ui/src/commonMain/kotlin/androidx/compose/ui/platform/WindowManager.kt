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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.snapshotFlow
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect

/**
 * Provides information about the Window that is hosting this compose hierarchy.
 */
@Stable
interface WindowManager {
    /**
     * Indicates whether the window hosting this compose hierarchy is in focus.
     *
     * When there are multiple windows visible, either in a multi-window environment or if a
     * popup or dialog is visible, this property can be used to determine if the current window
     * is in focus.
     */
    val isWindowFocused: Boolean
}

/**
 * Provides a callback that is called whenever the window gains or loses focus.
 */
@OptIn(
    ExperimentalComposeApi::class,
    InternalCoroutinesApi::class
)
@Composable
fun WindowFocusObserver(onWindowFocusChanged: (isWindowFocused: Boolean) -> Unit) {
    val windowManager = AmbientWindowManager.current
    val callback = rememberUpdatedState(onWindowFocusChanged)
    LaunchedEffect(windowManager) {
        snapshotFlow { windowManager.isWindowFocused }.collect { callback.value(it) }
    }
}

internal class WindowManagerImpl : WindowManager {
    private val _isWindowFocused = mutableStateOf(false)
    override var isWindowFocused: Boolean
        set(value) { _isWindowFocused.value = value }
        get() = _isWindowFocused.value
}