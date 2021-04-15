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

import android.graphics.Typeface
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.collection.SparseArrayCompat
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily

/**
 * An implementation of [AndroidTypeface] for [GenericFontFamily]
 */
internal class AndroidGenericFontFamilyTypeface(
    fontFamily: GenericFontFamily
) : AndroidTypeface {

    override val fontFamily: FontFamily = fontFamily

    override fun getNativeTypeface(
        fontWeight: FontWeight,
        fontStyle: FontStyle,
        synthesis: FontSynthesis
    ): Typeface = getOrPut(fontWeight, fontStyle)

    // Platform never return null with Typeface.create
    private val nativeTypeface = Typeface.create(fontFamily.name, Typeface.NORMAL)!!

    // TODO multiple TypefaceCache's, would be good to unify
    // Cached styled Typeface.
    private val lock = Any()
    @GuardedBy("lock") private val styledCache = SparseArrayCompat<Typeface>(4)

    /**
     * Returns the cached Typeface if cached, otherwise build new one, put it to cache, then
     * return it.
     *
     * @param fontWeight the weight of the font.
     * @param fontStyle the style of the font
     * @return a cached typeface.
     */
    fun getOrPut(fontWeight: FontWeight, fontStyle: FontStyle): Typeface {
        val key = fontWeight.weight shl 1 or if (fontStyle == FontStyle.Italic) 1 else 0
        synchronized(lock) {
            return styledCache.get(key) ?: buildStyledTypeface(fontWeight, fontStyle).also {
                styledCache.append(key, it)
            }
        }
    }

    private fun buildStyledTypeface(fontWeight: FontWeight, fontStyle: FontStyle) =
        if (Build.VERSION.SDK_INT < 28) {
            Typeface.create(
                nativeTypeface,
                TypefaceAdapter.getTypefaceStyle(fontWeight, fontStyle)
            )
        } else {
            TypefaceAdapterHelperMethods.create(
                nativeTypeface,
                fontWeight.weight,
                fontStyle == FontStyle.Italic
            )
        }
}