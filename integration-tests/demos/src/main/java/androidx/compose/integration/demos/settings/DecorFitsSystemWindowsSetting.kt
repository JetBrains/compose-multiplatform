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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat.setDecorFitsSystemWindows
import androidx.preference.CheckBoxPreference

/**
 * Setting that determines whether [setDecorFitsSystemWindows] is called with true or false for the
 * demo activity's window.
 */
internal object DecorFitsSystemWindowsSetting : DemoSetting<Boolean> {
    private const val Key = "decorFitsSystemWindows"
    private const val DefaultValue = true

    override fun createPreference(context: Context) = CheckBoxPreference(context).apply {
        title = "Decor fits system windows"
        key = Key
        summaryOff =
            "The framework will not fit the content view to the insets and will just pass through" +
                " the WindowInsetsCompat to the content view."
        summaryOn = "The framework will fit the content view to the insets. WindowInsets APIs " +
            "must be used to add necessary padding. Insets will be animated."
        setDefaultValue(DefaultValue)
    }

    @Composable
    fun asState() = preferenceAsState(Key) {
        getBoolean(Key, DefaultValue)
    }
}

/**
 * Sets the window's [decorFitsSystemWindow][setDecorFitsSystemWindows] property to
 * [decorFitsSystemWindows] as long as this function is composed.
 */
@Composable
internal fun DecorFitsSystemWindowsEffect(decorFitsSystemWindows: Boolean, window: Window) {
    DisposableEffect(decorFitsSystemWindows, window) {
        setDecorFitsSystemWindows(window, decorFitsSystemWindows)
        onDispose {
            setDecorFitsSystemWindows(window, true)
        }
    }
}