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
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.ArrayRes
import androidx.annotation.WorkerThread
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.provider.FontsContractCompat.FontRequestCallback
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_FONT_UNAVAILABLE
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_MALFORMED_QUERY
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_PROVIDER_NOT_FOUND
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_SECURITY_VIOLATION
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FAIL_REASON_WRONG_CERTIFICATES
import androidx.core.provider.FontsContractCompat.FontRequestCallback.FontRequestFailReason
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Load a font from Google Fonts via Downloadable Fonts.
 *
 * To learn more about the features supported by Google Fonts, see
 * [Get Started with the Google Fonts for Android](https://developers.google.com/fonts/docs/android)
 *
 * @param googleFont A font to load from fonts.google.com
 * @param fontProvider configuration for downloadable font provider
 * @param weight font weight to load
 * @param style italic or normal font
 */
// contains Google in name because this function provides integration with fonts.google.com
@Suppress("MentionsGoogle")
@ExperimentalTextApi
fun Font(
    googleFont: GoogleFont,
    fontProvider: GoogleFont.Provider,
    weight: FontWeight = FontWeight.W400,
    style: FontStyle = FontStyle.Normal
): Font {
    return GoogleFontImpl(
        name = googleFont.name,
        fontProvider = fontProvider,
        weight = weight,
        style = style,
        bestEffort = googleFont.bestEffort
    )
}

/**
 * A downloadable font from fonts.google.com
 *
 * To learn more about the features supported by Google Fonts, see
 * [Get Started with the Google Fonts for Android](https://developers.google.com/fonts/docs/android)
 *
 * @param name Name of a font on Google fonts, such as "Roboto" or "Open Sans"
 * @param bestEffort If besteffort is true and your query specifies a valid family name but the
 * requested width/weight/italic value is not supported Google Fonts will return the best match it
 * can find within the family. If false, exact matches will be returned only.
 *
 * @throws IllegalArgumentException if name is empty
 */
// contains Google in name because this function provides integration with fonts.google.com
@Suppress("MentionsGoogle")
@ExperimentalTextApi
class GoogleFont(val name: String, val bestEffort: Boolean = true) {
    init {
        require(name.isNotEmpty()) { "name cannot be empty" }
    }

    /**
     * Attributes used to create a [FontRequest] for a [GoogleFont] based [Font].
     *
     * @see FontRequest
     */
    @ExperimentalTextApi
    // contains Google in name because this function provides integration with fonts.google.com
    @Suppress("MentionsGoogle")
    class Provider private constructor(
        internal val providerAuthority: String,
        internal val providerPackage: String,
        internal val certificates: List<List<ByteArray>>?,
        @ArrayRes internal val certificatesRes: Int
    ) {

        /**
         * Describe a downloadable fonts provider using a list of certificates.
         *
         * The font provider is matched by `providerAuthority` and `packageName`, then the resulting
         * provider has it's certificates validated against `certificates`.
         *
         * If the certificates check success, the provider is used for downloadable fonts.
         *
         * If the certificates check fails, the provider will not be used and any downloadable fonts
         * requests configured with it will fail.
         *
         * @param providerAuthority The authority of the Font Provider to be used for the request.
         * @param providerPackage The package for the Font Provider to be used for the request. This
         * is used to verify the identity of the provider.
         * @param certificates The list of sets of hashes for the certificates the provider should
         * be signed with. This is used to verify the identity of the provider. Each set in the
         * list represents one collection of signature hashes. Refer to your font provider's
         * documentation for these values.
         */
        constructor(
            providerAuthority: String,
            providerPackage: String,
            certificates: List<List<ByteArray>>
        ) : this(providerAuthority, providerPackage, certificates, 0)

        /**
         * Describe a downloadable fonts provider using a resource array for certificates.
         *
         * The font provider is matched by `providerAuthority` and `packageName`, then the resulting
         * provider has it's certificates validated against `certificates`.
         *
         * If the certificates check success, the provider is used for downloadable fonts.
         *
         * If the certificates check fails, the provider will not be used and any downloadable fonts
         * requests configured with it will fail.
         *
         * @param providerAuthority The authority of the Font Provider to be used for the request.
         * @param providerPackage The package for the Font Provider to be used for the request. This
         * is used to verify the identity of the provider.
         * @param certificates A resource array with the list of sets of hashes for the certificates
         * the provider should be signed with. This is used to verify the identity of the provider.
         * Each set in the list represents one collection of signature hashes. Refer to your
         * font provider's documentation for these values.
         */
        constructor(
            providerAuthority: String,
            providerPackage: String,
            @ArrayRes certificates: Int
        ) : this(providerAuthority, providerPackage, null, certificates)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Provider) return false

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

        @ExperimentalTextApi
        companion object {
            /**
             * Url with a canonical list of all Google Fonts that are currently supported on
             * Android.
             */
            @ExperimentalTextApi
            val AllFontsListUri: Uri = Uri.parse("https://fonts.gstatic.com/s/a/directory.xml")
        }
    }
}

/**
 * Check if the downloadable fonts provider is available on device.
 *
 * This is not necessary for normal usage, but may be useful in debugging downloadable fonts
 * behavior.
 *
 * @param context for looking up font provider in
 * @return true if the provider is usable for downloadable fonts, false if it's not found
 * @throws IllegalStateException if the provider is on device, but certificates don't match
 */
@ExperimentalTextApi
@WorkerThread
fun GoogleFont.Provider.isAvailableOnDevice(
    @Suppress("ContextFirst") context: Context, // extension function
): Boolean = checkAvailable(context.packageManager, context.resources)

@ExperimentalTextApi
internal data class GoogleFontImpl constructor(
    val name: String,
    private val fontProvider: GoogleFont.Provider,
    override val weight: FontWeight,
    override val style: FontStyle,
    val bestEffort: Boolean
) : AndroidFont(FontLoadingStrategy.Async, GoogleFontTypefaceLoader) {
    fun toFontRequest(): FontRequest {
        // note: name is not encoded or quoted per spec
        val query = "name=$name&weight=${weight.weight}" +
            "&italic=${style.toQueryParam()}&besteffort=${bestEffortQueryParam()}"

        val certs = fontProvider.certificates
        return if (certs != null) {
            FontRequest(
                fontProvider.providerAuthority,
                fontProvider.providerPackage,
                query,
                certs
            )
        } else {
            FontRequest(
                fontProvider.providerAuthority,
                fontProvider.providerPackage,
                query,
                fontProvider.certificatesRes
            )
        }
    }

    private fun bestEffortQueryParam() = if (bestEffort) "true" else "false"

    private fun FontStyle.toQueryParam(): Int = if (this == FontStyle.Italic) 1 else 0
    private fun String.encode() = URLEncoder.encode(this, "UTF-8")
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
        return "Font(GoogleFont(\"$name\", bestEffort=$bestEffort), weight=$weight, " +
            "style=$style)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoogleFontImpl) return false

        if (name != other.name) return false
        if (fontProvider != other.fontProvider) return false
        if (weight != other.weight) return false
        if (style != other.style) return false
        if (bestEffort != other.bestEffort) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + fontProvider.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + bestEffort.hashCode()
        return result
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
        require(font is GoogleFontImpl) { "Only GoogleFontImpl supported (actual $font)" }
        val fontRequest = font.toFontRequest()
        val typefaceStyle = font.toTypefaceStyle()

        return suspendCancellableCoroutine { continuation ->
            val callback = object : FontRequestCallback() {
                override fun onTypefaceRetrieved(typeface: Typeface?) {
                    // this is entered from any thread
                    continuation.resume(typeface)
                }

                override fun onTypefaceRequestFailed(reason: Int) {
                    // this is entered from any thread
                    continuation.cancel(
                        IllegalStateException("Failed to load $font (reason=$reason, " +
                            "${reasonToString(reason)})")
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
        callback: FontRequestCallback
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
        callback: FontRequestCallback
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

@OptIn(ExperimentalTextApi::class)
private fun reasonToString(@FontRequestFailReason reasonCode: Int): String {
    return when (reasonCode) {
        FAIL_REASON_PROVIDER_NOT_FOUND -> "The requested provider was not found on this device."
        FAIL_REASON_WRONG_CERTIFICATES -> "The given provider cannot be authenticated with the " +
            "certificates given."
        FAIL_REASON_FONT_LOAD_ERROR -> "Generic error loading font, for example variation " +
            "settings were not parsable"
        FAIL_REASON_FONT_NOT_FOUND -> "Font not found, please check availability on " +
            "GoogleFont.Provider.AllFontsList: ${GoogleFont.Provider.AllFontsListUri}"
        FAIL_REASON_FONT_UNAVAILABLE -> "The provider found the queried font, but it is " +
            "currently unavailable."
        FAIL_REASON_MALFORMED_QUERY -> "The given query was not supported by this provider."
        FAIL_REASON_SECURITY_VIOLATION -> "Font was not loaded due to security issues. This " +
            "usually means the font was attempted to load in a restricted context"
        else -> "Unknown error code"
    }
}