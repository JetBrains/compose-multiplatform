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

package androidx.compose.integration.demos

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.integration.demos.DemoSettingsActivity.SettingsFragment
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.plusAssign

/**
 * Shell [AppCompatActivity] around [SettingsFragment], as we need a FragmentActivity subclass
 * to host the [SettingsFragment].
 */
class DemoSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

            val general = PreferenceCategory(context).apply {
                title = "General options"
                screen += this
            }

            general += CheckBoxPreference(context).apply {
                title = "Dynamic theming (android S+)"
                isEnabled = IsDynamicThemingAvailable
                key = IsDynamicThemeOnKey
                setDefaultValue(isDynamicThemeSettingOn(context))
            }
            preferenceScreen = screen
        }
    }
}

internal fun isDynamicThemeSettingOn(context: Context): Boolean {
    return PreferenceManager
        .getDefaultSharedPreferences(context)
        .getBoolean(IsDynamicThemeOnKey, IsDynamicThemingAvailable)
}

private const val IsDynamicThemeOnKey = "material3_isDynamicThemeOn"
internal val IsDynamicThemingAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S