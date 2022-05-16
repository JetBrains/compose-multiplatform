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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.integration.demos.DemoSettingsActivity.SettingsFragment
import androidx.compose.integration.demos.settings.DecorFitsSystemWindowsSetting
import androidx.compose.integration.demos.settings.DynamicThemeSetting
import androidx.compose.integration.demos.settings.SoftInputModeSetting
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.plusAssign

private val allSettings = listOf(
    DynamicThemeSetting,
    SoftInputModeSetting,
    DecorFitsSystemWindowsSetting,
)

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

            allSettings.forEach {
                general += it.createPreference(context)
            }

            preferenceScreen = screen
        }
    }
}