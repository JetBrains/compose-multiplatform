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
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.Cache
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.font.LoadedFontFamily
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface as SkTypeface
import org.jetbrains.skia.paragraph.FontCollection
import org.jetbrains.skia.paragraph.TypefaceFontProvider
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.font.createFontFamilyResolver

expect sealed class PlatformFont : Font {
    constructor()
    abstract val identity: String
    internal val cacheKey: String
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
class LoadedFont internal constructor(
    override val identity: String,
    val data: ByteArray,
    override val weight: FontWeight,
    override val style: FontStyle
) : PlatformFont() {
    @ExperimentalTextApi
    override val loadingStrategy: FontLoadingStrategy = FontLoadingStrategy.Blocking

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoadedFont) return false
        if (identity != other.identity) return false
        if (!data.contentEquals(other.data)) return false
        if (weight != other.weight) return false
        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identity.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }

    override fun toString(): String {
        return "LoadedFont(identity='$identity', weight=$weight, style=$style)"
    }
}

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
): Font = LoadedFont(identity, data, weight, style)

internal class SkiaBackedTypeface(
    val alias: String?,
    val nativeTypeface: SkTypeface
) : Typeface {
    override val fontFamily: FontFamily? = null
}

/**
 * Returns a Compose [Typeface] from Skia [SkTypeface].
 *
 * @param typeface Android Typeface instance
 */
fun Typeface(typeface: SkTypeface, alias: String? = null): Typeface {
    return SkiaBackedTypeface(alias, typeface)
}

// TODO: may be have an expect for MessageDigest?
// It has the static .getInstance() method, how would that work?
internal expect fun FontListFontFamily.makeAlias(): String

@Suppress("DEPRECATION", "OverridingDeprecatedMember")
@Deprecated(
    "Replaced with PlatformFontLoader during the introduction of async fonts, all usages" +
        " should be replaced",
    ReplaceWith("PlatformFontLoader"),
)
class FontLoader : Font.ResourceLoader {

    internal val fontCache: FontCache = FontCache()
    internal val fontFamilyResolver: FontFamily.Resolver = createFontFamilyResolver(fontCache)

    // TODO: we need to support:
    //  1. font collection (.ttc). Looks like skia currently doesn't have
    //  proper interfaces or they are broken (.makeFromFile(*, 1) always fails)
    //  2. variable fonts. for them we also need to extend definition interfaces to support
    //  custom variation settings
    @Deprecated(
        "Replaced by FontFamily.Resolver, this method should not be called",
        ReplaceWith("FontFamily.Resolver.resolve(font, )"),
    )
    override fun load(font: Font): SkTypeface {
        if (font !is PlatformFont) {
            throw IllegalArgumentException("Unsupported font type: $font")
        }
        return fontCache.load(font).typeface!!
    }
}

class FontLoadResult(val typeface: SkTypeface?, val aliases: List<String>)

internal class FontCache {
    internal val fonts = FontCollection()
    private val fontProvider = TypefaceFontProvider()

    init {
        fonts.setDefaultFontManager(FontMgr.default)
        fonts.setAssetFontManager(fontProvider)
    }

    private fun mapGenericFontFamily(generic: GenericFontFamily): List<String> {
        return GenericFontFamiliesMapping[generic.name]
            ?: error("Unknown generic font family ${generic.name}")
    }

    private val registered = HashSet<String>()

    internal fun load(font: PlatformFont): FontLoadResult {
        val typeface = loadFromTypefacesCache(font)
        ensureRegistered(typeface, font.cacheKey)
        return FontLoadResult(typeface, listOf(font.cacheKey))
    }

    internal fun loadPlatformTypes(
        fontFamily: FontFamily,
        fontWeight: FontWeight = FontWeight.Normal,
        fontStyle: FontStyle = FontStyle.Normal
    ): FontLoadResult {
        val aliases = ensureRegistered(fontFamily)
        val style = fontStyle.toSkFontStyle().withWeight(fontWeight.weight)
        return FontLoadResult(fonts.findTypefaces(aliases.toTypedArray(), style).first(), aliases)
    }

    private fun ensureRegistered(typeface: SkTypeface, key: String) {
        if (!registered.contains(key)) {
            fontProvider.registerTypeface(typeface, key)
            registered.add(key)
        }
    }

    private fun ensureRegistered(fontFamily: FontFamily): List<String> =
        when (fontFamily) {
            is FontListFontFamily -> {
                // not supported
                throw IllegalArgumentException(
                    "Don't load FontListFontFamily through ensureRegistered: $fontFamily"
                )
            }
            is LoadedFontFamily -> {
                val typeface = fontFamily.typeface as SkiaBackedTypeface
                val alias = typeface.alias ?: typeface.nativeTypeface.familyName
                if (!registered.contains(alias)) {
                    fontProvider.registerTypeface(typeface.nativeTypeface, alias)
                    registered.add(alias)
                }
                listOf(alias)
            }
            is GenericFontFamily -> mapGenericFontFamily(fontFamily)
            FontFamily.Default -> mapGenericFontFamily(FontFamily.SansSerif)
            else -> throw IllegalArgumentException("Unknown font family type: $fontFamily")
        }
}

internal expect val GenericFontFamiliesMapping: Map<String, List<String>>
internal expect val typefacesCache: Cache<String, SkTypeface>
internal expect fun loadFromTypefacesCache(font: Font): SkTypeface