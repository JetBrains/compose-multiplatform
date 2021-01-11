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

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import org.jetbrains.skija.FontMgr
import org.jetbrains.skija.Typeface
import org.jetbrains.skija.paragraph.FontCollection
import org.jetbrains.skija.paragraph.TypefaceFontProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import androidx.compose.ui.text.font.Font as uiFont
import org.jetbrains.skija.FontStyle as SkFontStyle

internal val GenericFontFamiliesMapping by lazy {
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

data class Font(
    val alias: String,
    val path: String,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : uiFont

fun font(
    alias: String,
    path: String,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = Font(alias, path, weight, style)

class FontLoader() : uiFont.ResourceLoader {
    val fonts = FontCollection()
    private val fontProvider = TypefaceFontProvider()

    init {
        fonts.setDefaultFontManager(FontMgr.getDefault())
        fonts.setAssetFontManager(fontProvider)
    }

    private fun mapGenericFontFamily(generic: GenericFontFamily): List<String> {
        return GenericFontFamiliesMapping[generic.name]
            ?: error("Unknown generic font family ${generic.name}")
    }

    fun ensureRegistered(fontFamily: FontFamily): List<String> =
        when (fontFamily) {
            is FontListFontFamily -> fontFamily.fonts.map { load(it) }
            is GenericFontFamily -> mapGenericFontFamily(fontFamily)
            FontFamily.Default -> listOf()
            else -> throw IllegalArgumentException("Unknown font family type: $fontFamily")
        }

    private val registered = mutableMapOf<String, Typeface>()
    override fun load(font: uiFont): String {
        when (font) {
            is Font -> {
                synchronized(this) {
                    if (!registered.contains(font.alias)) {
                        val typeface = typefaceResource(font.path)
                        fontProvider.registerTypeface(typeface, font.alias)
                        registered[font.alias] = typeface
                    }
                }
                return font.alias
            }
            else -> throw IllegalArgumentException("Unknown font type: $font")
        }
    }

    internal fun defaultTypeface(fontFamily: FontFamily): Typeface {
        return when (fontFamily) {
            is FontListFontFamily -> {
                val alias = load(fontFamily.fonts.first())
                return registered[alias]!!
            }
            is GenericFontFamily -> {
                Typeface.makeFromName(mapGenericFontFamily(fontFamily).first(), SkFontStyle.NORMAL)
            }
            FontFamily.Default -> Typeface.makeDefault()
            else -> throw IllegalArgumentException("Unknown font family type: $fontFamily")
        }
    }
}

// TODO: get fontFamily from loaded typeface via SkTypeface.getFamilyName
private fun typefaceResource(resourcePath: String): Typeface {
    val path = getFontPathAsString(resourcePath)
    return Typeface.makeFromFile(path, 0)
}

// TODO: add to skija an ability to load typefaces from memory
fun getFontPathAsString(resourcePath: String): String {
    val tempDir = File(System.getProperty("java.io.tmpdir"), "compose").apply {
        mkdirs()
        deleteOnExit()
    }
    val tempFile = File(tempDir, resourcePath).apply {
        deleteOnExit()
    }
    val tempPath = tempFile.toPath()
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
    if (stream == null) throw Error("Cannot find font $resourcePath")
    Files.createDirectories(tempPath.parent)
    Files.copy(stream, tempPath, StandardCopyOption.REPLACE_EXISTING)
    return tempFile.absolutePath
}

private enum class Platform {
    Linux,
    Windows,
    MacOS,
    Unknown;

    companion object {
        val Current by lazy {
            val name = System.getProperty("os.name")
            when {
                name.startsWith("Linux") -> Linux
                name.startsWith("Win") -> Windows
                name == "Mac OS X" -> MacOS
                else -> Unknown
            }
        }
    }
}