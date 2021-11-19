/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.ResourceFont
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Android implementation for [Font.ResourceLoader]. It is designed to load only [ResourceFont].
 */
internal class AndroidFontResourceLoader(
    private val context: Context
) : Font.ResourceLoader {

    @ExperimentalTextApi
    override fun loadOrNull(font: Font): Typeface? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.load(context, font)
            is ResourceFont -> runCatching { font.load(context) }.getOrNull()
            else -> null
        }
    }

    @ExperimentalTextApi
    override suspend fun loadAsync(font: Font): Typeface? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.loadAsync(context, font)
            is ResourceFont -> font.loadAsync(context)
            else -> throw IllegalArgumentException("Unknown font type: $font")
        }
    }

    @ExperimentalTextApi
    override val cacheKey: String? = null
}

/**
 * This is typically provided by [LocalFontLoader].
 *
 * This function is available for preloading fonts prior to starting compose, such as an Application
 * context.
 *
 * All instances of this will share the same font cache when passed to FontFamily.Resolver. This is
 * the "platform default" loader on Android with respect to [Font.ResourceLoader.cacheKey].
 */
@ExperimentalTextApi
fun Font.Companion.AndroidResourceLoader(
    @SuppressLint("ContextFirst") context: Context
): Font.ResourceLoader = AndroidFontResourceLoader(context.applicationContext)

private fun ResourceFont.load(context: Context): Typeface =
    ResourcesCompat.getFont(context, resId)!!

class ResourceFontLoadException(font: Font, val reason: Int) :
    RuntimeException("Unable to load font $font (reason=$reason)")

// TODO(seanmcq): Move to core-ktx to dedup
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun ResourceFont.loadAsync(context: Context): Typeface {
    return suspendCancellableCoroutine { continuation ->
        ResourcesCompat.getFont(context, resId, object : ResourcesCompat.FontCallback() {
            override fun onFontRetrieved(typeface: Typeface) {
                continuation.resume(typeface) {
                    /* ignore */
                }
            }

            override fun onFontRetrievalFailed(reason: Int) {
                continuation.cancel(
                    ResourceFontLoadException(this@loadAsync, reason)
                )
            }
        }, null)
    }
}