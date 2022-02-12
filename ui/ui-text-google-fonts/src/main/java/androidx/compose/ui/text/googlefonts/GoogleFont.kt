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
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import java.lang.IllegalStateException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Load a font from Google Fonts via Downloadable Fonts.
 *
 * To learn more about the features supported by Google Fonts, see
 * [Get Started with the Google Fonts for Android](https://developers.google.com/fonts/docs/android)
 *
 * @param name of font such as "Roboto" or "Open Sans"
 * @param fontProvider configuration for downloadable font provider
 * @param weight font weight to load, or weight to closest match if [bestEffort] is true
 * @param style italic or normal font
 * @param bestEffort If besteffort is true and your query specifies a valid family name but the
 * requested width/weight/italic value is not supported Google Fonts will return the best match it
 * can find within the family. If false, exact matches will be returned only.
 */
// contains Google in name because this function provides integration with fonts.google.com
@Suppress("MentionsGoogle")
@ExperimentalTextApi
fun GoogleFont(
    name: String,
    fontProvider: GoogleFontProvider,
    weight: FontWeight = FontWeight.W400,
    style: FontStyle = FontStyle.Normal,
    bestEffort: Boolean = true
): Font {
    require(name.isNotEmpty()) { "name cannot be empty" }
    return GoogleFontImpl(
        name = name,
        fontProvider = fontProvider,
        weight = weight,
        style = style,
        bestEffort = bestEffort
    )
}

/**
 * Attributes used to create a [FontRequest] for a [GoogleFont].
 *
 * @see FontRequest
 */
@ExperimentalTextApi
// contains Google in name because this function provides integration with fonts.google.com
@Suppress("MentionsGoogle")
class GoogleFontProvider(
    internal val providerAuthority: String,
    internal val providerPackage: String,
    internal val certificates: List<List<ByteArray>>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GoogleFontProvider

        if (providerAuthority != other.providerAuthority) return false
        if (providerPackage != other.providerPackage) return false
        if (certificates != other.certificates) return false

        return true
    }

    override fun hashCode(): Int {
        var result = providerAuthority.hashCode()
        result = 31 * result + providerPackage.hashCode()
        result = 31 * result + certificates.hashCode()
        return result
    }
}

@ExperimentalTextApi
internal data class GoogleFontImpl constructor(
    val name: String,
    private val fontProvider: GoogleFontProvider,
    override val weight: FontWeight,
    override val style: FontStyle,
    val bestEffort: Boolean
) : AndroidFont(FontLoadingStrategy.Async) {
    override val typefaceLoader: TypefaceLoader
        get() = GoogleFontTypefaceLoader

    fun toFontRequest(): FontRequest {
        val query = "name=${name.encode()}&weight=${weight.weight}" +
            "&italic=${style.toQueryParam()}&besteffort=${bestEffortQueryParam()}"
        return FontRequest(
            fontProvider.providerAuthority,
            fontProvider.providerPackage,
            query,
            fontProvider.certificates
        )
    }

    private fun bestEffortQueryParam() = if (bestEffort) "true" else "false"
    private fun FontStyle.toQueryParam(): Int = if (this == FontStyle.Italic) 1 else 0
    private fun String.encode() = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

    fun toTypefaceStyle(): Int {
        val isItalic = style == FontStyle.Italic
        val isBold = weight >= FontWeight.Bold
        return when {
            isItalic && isBold -> Typeface.BOLD_ITALIC
            isItalic -> Typeface.ITALIC
            isBold -> Typeface.BOLD
            else -> Typeface.NORMAL
        }
    }

    override fun toString(): String {
        return "GoogleFont(name=\"$name\", weight=$weight, style=$style, bestEffort=$bestEffort)"
    }
}

@ExperimentalTextApi
internal object GoogleFontTypefaceLoader : AndroidFont.TypefaceLoader {
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
        error("GoogleFont only support async loading: $font")
    }

    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
        return awaitLoad(context, font, DefaultFontsContractCompatLoader)
    }

    internal suspend fun awaitLoad(
        context: Context,
        font: AndroidFont,
        loader: FontsContractCompatLoader
    ): Typeface? {
        require(font is GoogleFontImpl) { "Only GoogleFontImpl supported" }
        val fontRequest = font.toFontRequest()
        val typefaceStyle = font.toTypefaceStyle()

        return suspendCancellableCoroutine { continuation ->
            val callback = object : FontsContractCompat.FontRequestCallback() {
                override fun onTypefaceRetrieved(typeface: Typeface?) {
                    // this is entered from any thread
                    continuation.resume(typeface)
                }

                override fun onTypefaceRequestFailed(reason: Int) {
                    // this is entered from any thread
                    continuation.cancel(
                        IllegalStateException("Failed to load $font (reason=$reason)")
                    )
                }
            }

            loader.requestFont(
                context = context,
                fontRequest = fontRequest,
                typefaceStyle = typefaceStyle,
                handler = asyncHandlerForCurrentThreadOrMainIfNoLooper(),
                callback = callback
            )
        }
    }

    private fun asyncHandlerForCurrentThreadOrMainIfNoLooper(): Handler {
        val looper = Looper.myLooper() ?: Looper.getMainLooper()
        return HandlerHelper.createAsync(looper)
    }
}

/**
 * To allow mocking for tests
 */
internal interface FontsContractCompatLoader {
    fun requestFont(
        context: Context,
        fontRequest: FontRequest,
        typefaceStyle: Int,
        handler: Handler,
        callback: FontsContractCompat.FontRequestCallback
    )
}

/**
 * Actual implementation of requestFont using androidx.core
 */
private object DefaultFontsContractCompatLoader : FontsContractCompatLoader {
    override fun requestFont(
        context: Context,
        fontRequest: FontRequest,
        typefaceStyle: Int,
        handler: Handler,
        callback: FontsContractCompat.FontRequestCallback
    ) {
        FontsContractCompat.requestFont(
            context,
            fontRequest,
            typefaceStyle,
            false, /* isBlockingFetch*/
            0, /* timeout - not used when isBlockingFetch=false */
            handler,
            callback
        )
    }
}