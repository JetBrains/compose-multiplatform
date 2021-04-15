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

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.collection.LruCache
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.font.AndroidAssetFont
import androidx.compose.ui.text.font.AndroidFileDescriptorFont
import androidx.compose.ui.text.font.AndroidFileFont
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontMatcher
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.content.res.ResourcesCompat

/**
 * An implementation of [AndroidTypeface] for [FontListFontFamily]
 */
// internal constructor for injecting FontMatcher for testing purpose
internal class AndroidFontListTypeface(
    fontFamily: FontListFontFamily,
    context: Context,
    necessaryStyles: List<Pair<FontWeight, FontStyle>>? = null,
    val fontMatcher: FontMatcher = AndroidFontListTypeface.fontMatcher
) : AndroidTypeface {

    private companion object {
        val fontMatcher = FontMatcher()
    }
    private val loadedTypefaces: Map<Font, Typeface>

    init {
        val targetFonts = necessaryStyles?.fastMap { (weight, style) ->
            fontMatcher.matchFont(fontFamily, weight, style)
        }?.let { LinkedHashSet(it).toList() } ?: fontFamily.fonts
        val typefaces = mutableMapOf<Font, Typeface>()

        targetFonts.fastForEach {
            try {
                typefaces[it] = AndroidTypefaceCache.getOrCreate(context, it)
            } catch (e: Exception) {
                throw IllegalStateException("Cannot create Typeface from $it")
            }
        }

        loadedTypefaces = typefaces
    }

    override val fontFamily: FontFamily = fontFamily

    override fun getNativeTypeface(
        fontWeight: FontWeight,
        fontStyle: FontStyle,
        synthesis: FontSynthesis
    ): Typeface {
        val font = fontMatcher.matchFont(loadedTypefaces.keys, fontWeight, fontStyle)
        val typeface = loadedTypefaces.get(font)
        requireNotNull(typeface)
        if ((font.weight == fontWeight && font.style == fontStyle) ||
            synthesis == FontSynthesis.None
        ) {
            return typeface
        }
        return TypefaceAdapter.synthesize(typeface, font, fontWeight, fontStyle, synthesis)
    }
}

/**
 * Global Android NativeTypeface cache.
 */
internal object AndroidTypefaceCache {

    // TODO multiple TypefaceCache's, would be good to unify
    private val cache = LruCache<String, Typeface>(16)

    /**
     * Returns NativeTypeface for [font] if it is in cache. Otherwise create new NativeTypeface and
     * put it into internal cache.
     */
    @OptIn(InternalPlatformTextApi::class, ExperimentalTextApi::class)
    fun getOrCreate(context: Context, font: Font): Typeface {
        val key = getKey(context, font)

        key?.let {
            cache.get(key)?.let { return it }
        }

        val typeface = when (font) {
            is ResourceFont ->
                if (Build.VERSION.SDK_INT >= 26) {
                    AndroidResourceFontLoaderHelper.create(context, font.resId)
                } else {
                    ResourcesCompat.getFont(context, font.resId)!!
                }
            is AndroidFont -> font.typeface
            else -> throw IllegalArgumentException("Unknown font type: $font")
        }

        key?.let { cache.put(key, typeface) }

        return typeface
    }

    /**
     * Utility method to generate a key for caching purposes.
     */
    fun getKey(context: Context, font: Font): String? {
        return when (font) {
            is ResourceFont -> {
                val value = TypedValue()
                context.resources.getValue(font.resId, value, true)
                "res:${value.string?.toString()!!}"
            }
            is AndroidAssetFont -> {
                "asset:${font.path}"
            }
            // do not cache File based Fonts, since the user might change the font
            is AndroidFileFont -> null
            is AndroidFileDescriptorFont -> null
            else -> throw IllegalArgumentException("Unknown font type: $font")
        }
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(26)
private object AndroidResourceFontLoaderHelper {
    @RequiresApi(26)
    @DoNotInline
    fun create(context: Context, resourceId: Int): Typeface {
        return context.resources.getFont(resourceId)
    }
}
