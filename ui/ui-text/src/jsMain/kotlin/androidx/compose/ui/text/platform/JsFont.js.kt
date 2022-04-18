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
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.Cache
import androidx.compose.ui.text.ExpireAfterAccessCache
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.fastForEach
import org.jetbrains.skia.Data
import org.jetbrains.skia.Typeface as SkTypeface

internal actual val GenericFontFamiliesMapping by lazy {
    when (Platform.Current) {
        Platform.Windows ->
            mapOf(
                FontFamily.SansSerif.name to listOf("Arial"),
                FontFamily.Serif.name to listOf("Times New Roman"),
                FontFamily.Monospace.name to listOf("Consolas"),
                FontFamily.Cursive.name to listOf("Comic Sans MS")
            )
        Platform.MacOS ->
            mapOf(
                FontFamily.SansSerif.name to listOf(
                    "Helvetica Neue",
                    "Helvetica"
                ),
                FontFamily.Serif.name to listOf("Times"),
                FontFamily.Monospace.name to listOf("Courier"),
                FontFamily.Cursive.name to listOf("Apple Chancery")
            )
        Platform.Linux ->
            mapOf(
                FontFamily.SansSerif.name to listOf("Noto Sans", "DejaVu Sans"),
                FontFamily.Serif.name to listOf("Noto Serif", "DejaVu Serif", "Times New Roman"),
                FontFamily.Monospace.name to listOf("Noto Sans Mono", "DejaVu Sans Mono"),
                // better alternative?
                FontFamily.Cursive.name to listOf("Comic Sans MS")
            )
        Platform.Unknown ->
            mapOf(
                FontFamily.SansSerif.name to listOf("Arial"),
                FontFamily.Serif.name to listOf("Times New Roman"),
                FontFamily.Monospace.name to listOf("Consolas"),
                FontFamily.Cursive.name to listOf("Comic Sans MS")
            )
    }
}

internal actual fun FontListFontFamily.makeAlias(): String {
    TODO("implement native FontListFontFamily.makeAlias()")
}

internal actual fun loadFromTypefacesCache(font: Font): SkTypeface {
    if (font !is PlatformFont) {
        throw IllegalArgumentException("Unsupported font type: $font")
    }
    return when (font) {
        is LoadedFont -> SkTypeface.makeFromData(Data.makeFromBytes(font.data))
        else -> throw IllegalArgumentException("Unsupported font type: $font")
    }
}

internal actual val typefacesCache = object : Cache<String, SkTypeface> {
    // TODO: no cache, actually.
    // override fun get(key: String, loader: (String) -> SkTypeface): SkTypeface = loader(key)
    override fun get(key: String, loader: (String) -> SkTypeface): SkTypeface =
        TODO("implement js typefacesCache")
}

private enum class Platform {
    Linux,
    Windows,
    MacOS,
    Unknown;

    companion object {
        val Current by lazy {
            println("TODO: selecting MacOS unconditionally")
            MacOS
            /*
            val name = System.getProperty("os.name")
            when {
                name.startsWith("Linux") -> Linux
                name.startsWith("Win") -> Windows
                name == "Mac OS X" -> MacOS
                else -> Unknown
            }
             */
        }
    }
}