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
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.preference.CheckBoxPreference

internal object DynamicThemeSetting : DemoSetting<Boolean> {

    private const val IsDynamicThemeOnKey = "material3_isDynamicThemeOn"
    private val IsDynamicThemingAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override fun createPreference(context: Context) = CheckBoxPreference(context).apply {
        title = "Dynamic theming (android S+)"
        isEnabled = IsDynamicThemingAvailable
        key = IsDynamicThemeOnKey
        setDefaultValue(IsDynamicThemingAvailable)
    }

    @Composable
    fun asState() = preferenceAsState(IsDynamicThemeOnKey) {
        getBoolean(IsDynamicThemeOnKey, IsDynamicThemingAvailable)
    }
}