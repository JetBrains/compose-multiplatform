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

import androidx.compose.ui.text.ExpireAfterAccessCache
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.font.LoadedFontFamily
import androidx.compose.ui.util.fastForEach
import org.jetbrains.skija.Data
import org.jetbrains.skija.FontMgr
import org.jetbrains.skija.Typeface
import org.jetbrains.skija.paragraph.FontCollection
import org.jetbrains.skija.paragraph.TypefaceFontProvider
import java.io.File
import java.security.MessageDigest
import androidx.compose.ui.text.font.Font as ComposeFont
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

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

sealed class DesktopFont : ComposeFont {
    abstract val identity: String

    internal val cacheKey: String
        get() = "${this::class.qualifiedName}|$identity"
}

/**
 * Defines a Font using byte array with loaded font data.
 *
 * @param identity Unique identity for a font. Used internally to distinguish fonts.
 * @param data Byte array with loaded font data.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
data class LoadedFont(
    override val identity: String,
    val data: ByteArray,
    override val weight: FontWeight,
    override val style: FontStyle
) : DesktopFont()

/**
 * Creates a Font using byte array with loaded font data.
 *
 * @param identity Unique identity for a font. Used internally to distinguish fonts.
 * @param data Byte array with loaded font data.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
fun Font(
    identity: String,
    data: ByteArray,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): DesktopFont = LoadedFont(identity, data, weight, style)

/**
 * Defines a Font using file path.
 *
 * @param file File path to font.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
data class FileFont(
    val file: File,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : DesktopFont() {
    override val identity
        get() = file.toString()
}

/**
 * Creates a Font using file path.
 *
 * @param file File path to font.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
fun Font(
    file: File,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): DesktopFont = FileFont(file, weight, style)

/**
 * Defines a Font using resource name.
 *
 * @param name The resource name in classpath.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */

data class ResourceFont(
    val name: String,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : DesktopFont() {
    override val identity
        get() = name
}

/**
 * Creates a Font using resource name.
 *
 * @param resource The resource name in classpath.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 *
 * @see FontFamily
 */
fun Font(
    resource: String,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): DesktopFont = ResourceFont(resource, weight, style)

internal class DesktopTypeface(
    val alias: String?,
    val nativeTypeface: Typeface
) : ComposeTypeface {
    override val fontFamily: FontFamily? = null
}

/**
 * Returns a Compose [ComposeTypeface] from Skija [Typeface].
 *
 * @param typeface Android Typeface instance
 */
fun Typeface(typeface: Typeface, alias: String? = null): ComposeTypeface {
    return DesktopTypeface(alias, typeface)
}

internal fun FontListFontFamily.makeAlias(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    fonts.fastForEach { font ->
        when (font) {
            is DesktopFont -> {
                digest.update(font.identity.toByteArray())
            }
        }
    }
    return "-compose-${digest.digest().toHexString()}"
}

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

class FontLoader : ComposeFont.ResourceLoader {
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

    private val registered = HashSet<String>()

    internal fun ensureRegistered(fontFamily: FontFamily): List<String> =
        when (fontFamily) {
            is FontListFontFamily -> {
                val alias = fontFamily.makeAlias()
                if (!registered.contains(alias)) {
                    fontFamily.fonts.forEach {
                        fontProvider.registerTypeface(load(it), alias)
                    }
                    registered.add(alias)
                }
                listOf(alias)
            }
            is LoadedFontFamily -> {
                val typeface = fontFamily.typeface as DesktopTypeface
                val alias = typeface.alias ?: typeface.nativeTypeface.familyName
                if (!registered.contains(alias)) {
                    fontProvider.registerTypeface(typeface.nativeTypeface, alias)
                    registered.add(alias)
                }
                listOf(alias)
            }
            is GenericFontFamily -> mapGenericFontFamily(fontFamily)
            FontFamily.Default -> listOf()
            else -> throw IllegalArgumentException("Unknown font family type: $fontFamily")
        }

    // TODO: we need to support:
    //  1. font collection (.ttc). Looks like skia currently doesn't have
    //  proper interfaces or they are broken (.makeFromFile(*, 1) always fails)
    //  2. variable fonts. for them we also need to extend definition interfaces to support
    //  custom variation settings
    override fun load(font: ComposeFont): Typeface {
        if (font !is DesktopFont) {
            throw IllegalArgumentException("Unsupported font type: $font")
        }
        return typefacesCache.get(font.cacheKey) {
            when (font) {
                is ResourceFont -> typefaceResource(font.name)
                is FileFont -> Typeface.makeFromFile(font.file.toString())
                is LoadedFont -> Typeface.makeFromData(Data.makeFromBytes(font.data))
            }
        }
    }

    internal fun findTypeface(
        fontFamily: FontFamily,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal
    ): Typeface? {
        return when (fontFamily) {
            FontFamily.Default -> fonts.defaultFallback()
            else -> {
                val aliases = ensureRegistered(fontFamily)
                val style = fontStyle.toSkFontStyle().withWeight(fontWeight.weight)
                fonts.findTypefaces(aliases.toTypedArray(), style).first()
            }
        }
    }
}

private val typefacesCache = ExpireAfterAccessCache<String, Typeface>(
    60_000_000_000 // 1 minute
)

private fun typefaceResource(resourceName: String): Typeface {
    val resource = Thread
        .currentThread()
        .contextClassLoader
        .getResourceAsStream(resourceName) ?: error("Can't load font from $resourceName")
    val bytes = resource.readAllBytes()
    return Typeface.makeFromData(Data.makeFromBytes(bytes))
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