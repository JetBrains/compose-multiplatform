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

package androidx.compose.material.catalog.library.model

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp

data class Theme(
    val primaryColor: ThemeColor = ThemeColor.Purple,
    val secondaryColor: ThemeColor = ThemeColor.Teal,
    val fontFamily: ThemeFontFamily = ThemeFontFamily.Default,
    val shapeCornerFamily: ThemeShapeCornerFamily = ThemeShapeCornerFamily.Rounded,
    val smallShapeCornerSize: Int = 4,
    val mediumShapeCornerSize: Int = 4,
    val largeShapeCornerSize: Int = 0
)

val DefaultTheme = Theme()

enum class ThemeColor {
    Blue,
    Brown,
    Green,
    Indigo,
    Orange,
    Pink,
    Purple,
    Red,
    Teal,
    Yellow
}

fun ThemeColor.getColor(darkTheme: Boolean): Color = when (this) {
    ThemeColor.Blue -> if (!darkTheme) Color(0xFF2196F3) else Color(0xFF90CAF9)
    ThemeColor.Brown -> if (!darkTheme) Color(0xFF795548) else Color(0xFFBCAAA4)
    ThemeColor.Green -> if (!darkTheme) Color(0xFF43A047) else Color(0xFFA5D6A7)
    ThemeColor.Indigo -> if (!darkTheme) Color(0xFF3F51B5) else Color(0xFFC5CAE9)
    ThemeColor.Orange -> if (!darkTheme) Color(0xFFE65100) else Color(0xFFFFB74D)
    ThemeColor.Pink -> if (!darkTheme) Color(0xFFE91E63) else Color(0xFFF48FB1)
    ThemeColor.Purple -> if (!darkTheme) Color(0xFF6200EE) else Color(0xFFBB86FC)
    ThemeColor.Red -> if (!darkTheme) Color(0xFFB00020) else Color(0xFFCF6679)
    ThemeColor.Teal -> if (!darkTheme) Color(0xFF03DAC6) else Color(0xFF03DAC6)
    ThemeColor.Yellow -> if (!darkTheme) Color(0xFFFFEB3B) else Color(0xFFFFF59D)
}

enum class ThemeFontFamily(val label: String) {
    Default("Default"),
    SansSerif("Sans serif"),
    Serif("Serif"),
    Monospace("Monospace"),
    Cursive("Cursive")
}

fun ThemeFontFamily.getFontFamily(): FontFamily = when (this) {
    ThemeFontFamily.Default -> FontFamily.Default
    ThemeFontFamily.SansSerif -> FontFamily.SansSerif
    ThemeFontFamily.Serif -> FontFamily.Serif
    ThemeFontFamily.Monospace -> FontFamily.Monospace
    ThemeFontFamily.Cursive -> FontFamily.Cursive
}

enum class ThemeShapeCornerFamily(val label: String) {
    Rounded("Rounded"),
    Cut("Cut")
}

fun ThemeShapeCornerFamily.getShape(size: Dp): CornerBasedShape = when (this) {
    ThemeShapeCornerFamily.Rounded -> RoundedCornerShape(size = size)
    ThemeShapeCornerFamily.Cut -> CutCornerShape(size = size)
}

val ThemeSaver = Saver<Theme, Map<String, Int>>(
    save = { theme ->
        mapOf(
            PrimaryColorKey to theme.primaryColor.ordinal,
            SecondaryColorKey to theme.secondaryColor.ordinal,
            FontFamilyKey to theme.fontFamily.ordinal,
            ShapeCornerFamilyKey to theme.shapeCornerFamily.ordinal,
            SmallShapeCornerSizeKey to theme.smallShapeCornerSize,
            MediumShapeCornerSizeKey to theme.mediumShapeCornerSize,
            LargeShapeCornerSizeKey to theme.largeShapeCornerSize
        )
    },
    restore = { map ->
        Theme(
            primaryColor = ThemeColor.values()[map[PrimaryColorKey]!!],
            secondaryColor = ThemeColor.values()[map[SecondaryColorKey]!!],
            fontFamily = ThemeFontFamily.values()[map[FontFamilyKey]!!],
            shapeCornerFamily = ThemeShapeCornerFamily.values()[map[ShapeCornerFamilyKey]!!],
            smallShapeCornerSize = map[SmallShapeCornerSizeKey]!!,
            mediumShapeCornerSize = map[MediumShapeCornerSizeKey]!!,
            largeShapeCornerSize = map[LargeShapeCornerSizeKey]!!
        )
    }
)

const val MaxSmallShapeCornerSize = 16
const val MaxMediumShapeCornerSize = 32
const val MaxLargeShapeCornerSize = 48

private const val PrimaryColorKey = "primaryColor"
private const val SecondaryColorKey = "secondaryColor"
private const val FontFamilyKey = "fontFamily"
private const val ShapeCornerFamilyKey = "shapeCornerFamily"
private const val SmallShapeCornerSizeKey = "smallShapeCornerSize"
private const val MediumShapeCornerSizeKey = "mediumShapeCornerSize"
private const val LargeShapeCornerSizeKey = "largeShapeCornerSize"
