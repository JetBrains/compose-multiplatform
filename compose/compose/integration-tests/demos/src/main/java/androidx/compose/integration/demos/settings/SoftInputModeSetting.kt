/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.integration.demos.settings

import android.content.Context
import android.view.Window
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.compose.integration.demos.settings.SoftInputMode.AdjustPan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.DropDownPreference
import androidx.preference.Preference.SummaryProvider

internal enum class SoftInputMode(val flagValue: Int, val summary: String) {
    @Suppress("DEPRECATION")
    AdjustResize(
        SOFT_INPUT_ADJUST_RESIZE,
        summary = "The content view will resize to accommodate the IME.",
    ),
    AdjustPan(
        SOFT_INPUT_ADJUST_PAN,
        summary = "The content view will pan up to accommodate the IME.",
    ),
}

/**
 * Setting that determines which soft input mode the demo activity's window is configured with.
 */
internal object SoftInputModeSetting : DemoSetting<SoftInputMode> {
    private const val Key = "softInputMode"

    override fun createPreference(context: Context) = DropDownPreference(context).apply {
        title = "Soft input mode"
        key = Key
        SoftInputMode.values().map { it.name }.toTypedArray().also {
            entries = it
            entryValues = it
        }
        summaryProvider = SummaryProvider<DropDownPreference> {
            val mode = SoftInputMode.valueOf(value)
            """
                ${mode.name}
                ${mode.summary}
            """.trimIndent()
        }
        setDefaultValue(AdjustPan.name)
    }

    @Composable
    fun asState() = preferenceAsState(Key) {
        val value = getString(Key, AdjustPan.name) ?: AdjustPan.name
        SoftInputMode.valueOf(value)
    }
}

/**
 * Sets the window's [softInputMode][android.view.Window.setSoftInputMode] to [mode] as long as this
 * function is composed.
 */
@Composable
internal fun SoftInputModeEffect(mode: SoftInputMode, window: Window) {
    val updatedMode by rememberUpdatedState(mode)
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle, window) {
        lifecycle.repeatOnLifecycle(RESUMED) {
            // During the first frame, the window may have its soft input mode set to a special
            // "forwarding input" mode. After the first frame, it will be reset to whatever is in
            // the theme. If we set the value during the first frame, it will get immediately
            // cleared, so wait until the second frame to set it. This is super hacky but no real
            // app should need to dynamically change its soft input mode like we do.
            withFrameMillis {}
            snapshotFlow { updatedMode }.collect { mode ->
                window.setSoftInputMode(mode.flagValue)
            }
        }
    }
}