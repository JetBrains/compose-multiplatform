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

package androidx.compose.ui.text.font

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Async
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Blocking
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.OptionalLocal
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation for [PlatformFontLoader].
 *
 * It is designed to work both on-device and for Preview.
 */
internal class AndroidFontLoader(
    context: Context
) : PlatformFontLoader {
    private val context = context.applicationContext

    @OptIn(ExperimentalTextApi::class)
    override fun loadBlocking(font: Font): Typeface? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.loadBlocking(context, font)
            is ResourceFont -> when (font.loadingStrategy) {
                Blocking -> font.load(context)
                OptionalLocal -> runCatching { font.load(context) }.getOrNull()
                Async -> throw UnsupportedOperationException("Unsupported Async font load path")
                else -> throw IllegalArgumentException(
                    "Unknown loading type ${font.loadingStrategy}"
                )
            }
            else -> null
        }
    }

    override suspend fun awaitLoad(font: Font): Typeface? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.awaitLoad(context, font)
            is ResourceFont -> font.loadAsync(context)
            else -> throw IllegalArgumentException("Unknown font type: $font")
        }
    }

    override val cacheKey: Any? = null
}

private fun ResourceFont.load(context: Context): Typeface =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ResourceFontHelper.load(context, this)
    } else {
        ResourcesCompat.getFont(context, resId)!!
    }

// This uses withContext to a blocking call to support Preview, which is not capable of displaying
// when async ResourcesCompat is used.
private suspend fun ResourceFont.loadAsync(context: Context): Typeface {
    return withContext(Dispatchers.IO) { load(context) }
}

@RequiresApi(Build.VERSION_CODES.O)
private object ResourceFontHelper {
    @DoNotInline
    fun load(context: Context, font: ResourceFont): Typeface {
        return context.resources.getFont(font.resId)
    }
}