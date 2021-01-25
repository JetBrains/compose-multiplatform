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
import android.util.TypedValue
import androidx.collection.LruCache
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontMatcher
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.util.fastForEach
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
        val targetFonts = necessaryStyles?.map { (weight, style) ->
            fontMatcher.matchFont(fontFamily, weight, style)
        }?.distinct() ?: fontFamily.fonts
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
    /**
     * Returns NativeTypeface for [font] if it is in cache. Otherwise create new NativeTypeface and
     * put it into internal cache.
     */
    fun getOrCreate(context: Context, font: Font): Typeface = when (font) {
        is ResourceFont -> getOrCreateByResourceId(context, font.resId)
        else -> throw IllegalArgumentException("Unknown font type: $font")
    }

    private val cache = LruCache<String, Typeface>(16)

    private fun getOrCreateByResourceId(context: Context, resId: Int): Typeface {
        val value = TypedValue()
        context.resources.getValue(resId, value, true)
        // We use the file path as a key of the request cache.
        val key = value.string?.toString() ?: return createTypeface(context, resId)

        cache.get(key)?.let { return it }
        val typeface = createTypeface(context, resId)
        cache.put(key, typeface) // eventually consistent
        return typeface
    }

    private fun createTypeface(context: Context, resId: Int): Typeface =
        ResourcesCompat.getFont(context, resId)
            ?: throw IllegalArgumentException("Unable to load Font $resId")
}