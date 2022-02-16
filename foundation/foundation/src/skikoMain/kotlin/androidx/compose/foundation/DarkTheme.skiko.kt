/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

/**
 * This function should be used to help build responsive UIs that follow the system setting, to
 * avoid harsh contrast changes when switching between applications.
 *
 * This function returns `true` if the [Configuration.UI_MODE_NIGHT_YES] bit is set. It is
 * also possible for this bit to be [Configuration.UI_MODE_NIGHT_UNDEFINED], in which case
 * light theme is treated as the default, and this function returns `false`.
 *
 * It is also recommended to provide user accessible overrides in your application, so users can
 * choose to force an always-light or always-dark theme. To do this, you should provide the current
 * theme value in a CompositionLocal or similar to components further down your hierarchy, only
 * calling this effect once at the top level if no user override has been set. This also helps
 * avoid multiple calls to this effect, which can be expensive as it queries system configuration.
 *
 * For example, to draw a white rectangle when in dark theme, and a black rectangle when in light
 * theme:
 *
 * @sample androidx.compose.foundation.samples.DarkThemeSample
 *
 * @return `true` if the system is considered to be in 'dark theme'.
 */
@Composable
@ReadOnlyComposable
internal actual fun _isSystemInDarkTheme(): Boolean {
    return currentSystemTheme == SystemTheme.DARK
}
