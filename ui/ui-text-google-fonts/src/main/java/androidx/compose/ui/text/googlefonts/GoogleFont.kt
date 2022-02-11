/*
 * Copyright 2022 The Android Open Source Project
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

// this file provides integration with fonts.google.com, which is called Google Fonts
@file:Suppress("MentionsGoogle")

package androidx.compose.ui.text.googlefonts

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.provider.FontRequest

/**
 * To learn more about the features supported by Google Fonts, see
 * [Get Started with the Google Fonts for Android](https://developers.google.com/fonts/docs/android)
 *
 * @param name of font such as "Roboto" or "Open Sans"
 * @param weight font weight to load, or weight to closest match if [bestEffort] is true
 * @param style italic or normal font
 * @param width 'wdth' parameter from variable font feature, if supported for this font
 * @param bestEffort If besteffort is true and your query specifies a valid family name but the
 * requested width/weight/italic value is not supported Google Fonts will return the best match it
 * can find within the family.
 */
// this function provides integration with fonts.google.com, which is called Google Fonts
@Suppress("MentionsGoogle")
fun GoogleFont(
    name: String,
    weight: FontWeight = FontWeight.W400,
    style: FontStyle = FontStyle.Normal,
    width: Float = 100f,
    bestEffort: Boolean = true
): Font {
    require(name.isNotEmpty()) { "name cannot be empty" }
    require(width > 0f) { "width must be greater than 0.0f (passed $width)" }
    return GoogleFontImpl(name, weight, style, width, bestEffort)
}

internal class GoogleFontImpl(
    val name: String,
    override val weight: FontWeight,
    override val style: FontStyle,
    val width: Float? = null,
    val bestEffort: Boolean? = null
) : AndroidFont(FontLoadingStrategy.Async) {
    override val typefaceLoader: TypefaceLoader
        get() = GoogleFontTypefaceLoader

    fun toFontRequest(): FontRequest {
        TODO("Not yet implemented")
        // 1. urlencode everything
        // 2. generate font request
    }
}

private object GoogleFontTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        error("GoogleFont only support async loading: $font")
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        require(font is GoogleFontImpl) { "Only GoogleFontImpl supported" }
        val fontRequest = font.toFontRequest()

        TODO("Actually fetch font using androidx.core")
    }
}