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
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.plusAssign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import kotlin.reflect.full.memberProperties

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

            general += Preference(context).apply {
                title = "Shuffle all colors"
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    generateRandomPalette().saveColors(context)
                    requireActivity().finish()
                    true
                }
            }

            general += Preference(context).apply {
                title = "Reset colors to default"
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                    sharedPreferences.edit().clear().apply()
                    requireActivity().finish()
                    true
                }
            }

            val light = PreferenceCategory(context).apply {
                title = "Light colors"
                screen += this
            }
            // Create new Colors to resolve defaults
            lightColors().forEachColorProperty { name, color ->
                light += EditTextPreference(context).apply {
                    key = name + true
                    title = name
                    // set the default value to be the default for Colors
                    setDefaultValue(Integer.toHexString(color.toArgb()))
                    summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
                }
            }

            val dark = PreferenceCategory(context).apply {
                title = "Dark colors"
                screen += this
            }

            darkColors().forEachColorProperty { name, color ->
                dark += EditTextPreference(context).apply {
                    key = name + false
                    title = name
                    // set the default value to be the default for Colors
                    setDefaultValue(Integer.toHexString(color.toArgb()))
                    summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
                }
            }
            preferenceScreen = screen
        }
    }
}

/**
 * Iterates over each color present in a given [Colors].
 *
 * @param action the action to take on each property, where name is the name of the property,
 * such as 'primary' for [Colors.primary], and color is the resolved [Color] of the
 * property.
 */
private fun Colors.forEachColorProperty(action: (name: String, color: Color) -> Unit) {
    Colors::class.memberProperties.forEach { property ->
        val name = property.name
        val color = property.get(this) as? Color ?: return@forEach
        action(name, color)
    }
}

/**
 * Persists the current [DemoColors] to [SharedPreferences].
 */
private fun DemoColors.saveColors(context: Context) {
    light.forEachColorProperty { name, color ->
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(name + true, Integer.toHexString(color.toArgb()))
            .apply()
    }
    dark.forEachColorProperty { name, color ->
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(name + false, Integer.toHexString(color.toArgb()))
            .apply()
    }
}

/**
 * Generates a [DemoColors] with random [Colors.primary], [Colors.onPrimary],
 * [Colors.secondary] and [Colors.onSecondary] as random dark-on-light or light-on-dark
 * pairs. Other colors are kept from the baseline.
 */
private fun generateRandomPalette(): DemoColors {
    return DemoColors().apply {
        val (lightPrimary, lightOnPrimary) = generateColorPair(true)
        val (lightSecondary, lightOnSecondary) = generateColorPair(true)
        light = lightColors(
            primary = lightPrimary,
            secondary = lightSecondary,
            onPrimary = lightOnPrimary,
            onSecondary = lightOnSecondary
        )
        val (darkPrimary, darkOnPrimary) = generateColorPair(false)
        val (darkSecondary, darkOnSecondary) = generateColorPair(false)
        dark = darkColors(
            primary = darkPrimary,
            secondary = darkSecondary,
            onPrimary = darkOnPrimary,
            onSecondary = darkOnSecondary
        )
    }
}

/**
 * Generate a random dark and light color from the palette, and returns either a dark-on-light
 * or light-on-dark color pair.
 */
private fun generateColorPair(isLightTheme: Boolean): Pair<Color, Color> {
    val darkColor = Color(DarkPaletteColors.random())
    val lightColor = Color(LightPaletteColors.random())
    return if (isLightTheme) {
        darkColor to lightColor
    } else {
        lightColor to darkColor
    }
}

// Colors taken from https://material.io/design/color -> 2014 Material Design color palettes
val LightPaletteColors = listOf(
    0xFFEF5350,
    0xFFF44336,
    0xFFE53935,
    0xFFD32F2F,
    0xFFC62828,
    0xFFB71C1C,
    0xFFFF5252,
    0xFFFF1744,
    0xFFD50000,
    0xFFEC407A,
    0xFFE91E63,
    0xFFD81B60,
    0xFFC2185B,
    0xFFAD1457,
    0xFF880E4F,
    0xFFFF4081,
    0xFFF50057,
    0xFFC51162,
    0xFFBA68C8,
    0xFFAB47BC,
    0xFF9C27B0,
    0xFF8E24AA,
    0xFF7B1FA2,
    0xFF6A1B9A,
    0xFF4A148C,
    0xFFE040FB,
    0xFFD500F9,
    0xFFAA00FF,
    0xFF9575CD,
    0xFF7E57C2,
    0xFF673AB7,
    0xFF5E35B1,
    0xFF512DA8,
    0xFF4527A0,
    0xFF311B92,
    0xFF7C4DFF,
    0xFF651FFF,
    0xFF6200EA,
    0xFF7986CB,
    0xFF5C6BC0,
    0xFF3F51B5,
    0xFF3949AB,
    0xFF303F9F,
    0xFF283593,
    0xFF1A237E,
    0xFF536DFE,
    0xFF3D5AFE,
    0xFF304FFE,
    0xFF1E88E5,
    0xFF1976D2,
    0xFF1565C0,
    0xFF0D47A1,
    0xFF448AFF,
    0xFF2979FF,
    0xFF2962FF,
    0xFF0288D1,
    0xFF0277BD,
    0xFF01579B,
    0xFF0091EA,
    0xFF0097A7,
    0xFF00838F,
    0xFF006064,
    0xFF009688,
    0xFF00897B,
    0xFF00796B,
    0xFF00695C,
    0xFF004D40,
    0xFF43A047,
    0xFF388E3C,
    0xFF2E7D32,
    0xFF1B5E20,
    0xFF558B2F,
    0xFF33691E,
    0xFF827717,
    0xFFE65100,
    0xFFF4511E,
    0xFFE64A19,
    0xFFD84315,
    0xFFBF360C,
    0xFFFF3D00,
    0xFFDD2C00,
    0xFFA1887F,
    0xFF8D6E63,
    0xFF795548,
    0xFF6D4C41,
    0xFF5D4037,
    0xFF4E342E,
    0xFF3E2723,
    0xFF757575,
    0xFF616161,
    0xFF424242,
    0xFF212121,
    0xFF78909C,
    0xFF607D8B,
    0xFF546E7A,
    0xFF455A64,
    0xFF37474F,
    0xFF263238
)

val DarkPaletteColors = listOf(
    0xFFFFCDD2,
    0xFFEF9A9A,
    0xFFE57373,
    0xFFFF8A80,
    0xFFF8BBD0,
    0xFFF48FB1,
    0xFFF06292,
    0xFFFF80AB,
    0xFFE1BEE7,
    0xFFCE93D8,
    0xFFEA80FC,
    0xFFD1C4E9,
    0xFFB39DDB,
    0xFFB388FF,
    0xFFC5CAE9,
    0xFF9FA8DA,
    0xFF8C9EFF,
    0xFFBBDEFB,
    0xFF90CAF9,
    0xFF64B5F6,
    0xFF42A5F5,
    0xFF2196F3,
    0xFF82B1FF,
    0xFFB3E5FC,
    0xFF81D4FA,
    0xFF4FC3F7,
    0xFF29B6F6,
    0xFF03A9F4,
    0xFF039BE5,
    0xFF80D8FF,
    0xFF40C4FF,
    0xFF00B0FF,
    0xFFB2EBF2,
    0xFF80DEEA,
    0xFF4DD0E1,
    0xFF26C6DA,
    0xFF00BCD4,
    0xFF00ACC1,
    0xFF84FFFF,
    0xFF18FFFF,
    0xFF00E5FF,
    0xFF00B8D4,
    0xFFB2DFDB,
    0xFF80CBC4,
    0xFF4DB6AC,
    0xFF26A69A,
    0xFFA7FFEB,
    0xFF64FFDA,
    0xFF1DE9B6,
    0xFF00BFA5,
    0xFFC8E6C9,
    0xFFA5D6A7,
    0xFF81C784,
    0xFF66BB6A,
    0xFF4CAF50,
    0xFFB9F6CA,
    0xFF69F0AE,
    0xFF00E676,
    0xFF00C853,
    0xFFDCEDC8,
    0xFFC5E1A5,
    0xFFAED581,
    0xFF9CCC65,
    0xFF8BC34A,
    0xFF7CB342,
    0xFF689F38,
    0xFFCCFF90,
    0xFFB2FF59,
    0xFF76FF03,
    0xFF64DD17,
    0xFFF0F4C3,
    0xFFE6EE9C,
    0xFFDCE775,
    0xFFD4E157,
    0xFFCDDC39,
    0xFFC0CA33,
    0xFFAFB42B,
    0xFF9E9D24,
    0xFFF4FF81,
    0xFFEEFF41,
    0xFFC6FF00,
    0xFFAEEA00,
    0xFFFFF9C4,
    0xFFFFF59D,
    0xFFFFF176,
    0xFFFFEE58,
    0xFFFFEB3B,
    0xFFFDD835,
    0xFFFBC02D,
    0xFFF9A825,
    0xFFF57F17,
    0xFFFFFF8D,
    0xFFFFFF00,
    0xFFFFEA00,
    0xFFFFD600,
    0xFFFFECB3,
    0xFFFFE082,
    0xFFFFD54F,
    0xFFFFCA28,
    0xFFFFC107,
    0xFFFFB300,
    0xFFFFA000,
    0xFFFF8F00,
    0xFFFF6F00,
    0xFFFFE57F,
    0xFFFFD740,
    0xFFFFC400,
    0xFFFFAB00,
    0xFFFFE0B2,
    0xFFFFCC80,
    0xFFFFB74D,
    0xFFFFA726,
    0xFFFF9800,
    0xFFFB8C00,
    0xFFF57C00,
    0xFFEF6C00,
    0xFFFFD180,
    0xFFFFAB40,
    0xFFFF9100,
    0xFFFF6D00,
    0xFFFFCCBC,
    0xFFFFAB91,
    0xFFFF8A65,
    0xFFFF7043,
    0xFFFF5722,
    0xFFFF9E80,
    0xFFFF6E40,
    0xFFD7CCC8,
    0xFFBCAAA4,
    0xFFF5F5F5,
    0xFFEEEEEE,
    0xFFE0E0E0,
    0xFFBDBDBD,
    0xFF9E9E9E,
    0xFFCFD8DC,
    0xFFB0BEC5,
    0xFF90A4AE,
    0xFFFFFFFF
)