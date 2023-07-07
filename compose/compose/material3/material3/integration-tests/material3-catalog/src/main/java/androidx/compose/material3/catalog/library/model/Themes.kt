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

package androidx.compose.material3.catalog.library.model

import androidx.compose.runtime.saveable.Saver

data class Theme(
    val themeMode: ThemeMode = ThemeMode.System,
    val colorMode: ColorMode = ColorMode.Baseline,
    val fontScale: Float = 1.0f,
    val fontScaleMode: FontScaleMode = FontScaleMode.System,
    val textDirection: TextDirection = TextDirection.System,
)

/**
 * A class for defining layout directions.
 *
 * A layout direction can be left-to-right (LTR) or right-to-left (RTL).
 */
enum class TextDirection {
    System,

    /** Horizontal layout direction is from Left to Right. */
    LTR,

    /** Horizontal layout direction is from Right to Left. */
    RTL,
}

/**
 * Determines what color scheme should be used when viewing the catalog in the Google Material 3
 * theme.
 */
enum class ColorMode(val label: String) {
    /**
     * The baseline light/dark colors schemes.
     *
     * This is the default behavior, and the fallback if dynamic colors are not available on the
     * current device.
     */
    Baseline("Baseline"),
    /**
     * Build a color scheme from a pre-selected color palette that behaves the same as a dynamic color
     * palette.
     *
     * Useful for testing dynamic color schemes on devices that don't support dynamic colors.
     */
    Custom("Custom"),
    /**
     * Build a color scheme from the dynamic colors taken from the Android System.
     *
     * If the dynamic colors are not available, the baseline color scheme will be used as a fallback.
     */
    Dynamic("Dynamic (Android 12+)"),
}

enum class FontScaleMode(val label: String) {
    Custom("Custom"),
    System("System"),
}

enum class ThemeMode {
    System,
    Light,
    Dark,
}

val ThemeSaver =
    Saver<Theme, Map<String, Int>>(
        save = { theme ->
            mapOf(
                ThemeModeKey to theme.themeMode.ordinal,
                ColorModeKey to theme.colorMode.ordinal,
                FontScaleKey to theme.fontScale.toInt(),
                TextDirectionKey to theme.textDirection.ordinal,
            )
        },
        restore = { map ->
            Theme(
                themeMode = ThemeMode.values()[map.getValue(ThemeModeKey)],
                colorMode = ColorMode.values()[map.getValue(ColorModeKey)],
                fontScale = map.getValue(FontScaleKey).toFloat(),
                textDirection = TextDirection.values()[map.getValue(TextDirectionKey)],
            )
        }
    )

const val MinFontScale = 0.4f
const val MaxFontScale = 2f

private const val ThemeModeKey = "themeMode"
private const val ColorModeKey = "colorMode"
private const val FontScaleKey = "fontScale"
private const val TextDirectionKey = "textDirection"
