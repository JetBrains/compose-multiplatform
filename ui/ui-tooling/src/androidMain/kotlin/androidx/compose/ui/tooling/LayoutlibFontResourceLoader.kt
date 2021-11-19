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

package androidx.compose.ui.tooling

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.ResourceFont
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Layoutlib implementation for [Font.ResourceLoader]
 */
internal class LayoutlibFontResourceLoader(private val context: Context) : Font.ResourceLoader {
    @ExperimentalTextApi
    override fun loadBlocking(font: Font): Any? {
        return when (font) {
            is ResourceFont -> font.load(context)
            is AndroidFont -> font.typefaceLoader.loadBlocking(context, font)
            else -> throw IllegalArgumentException("Unknown font type: ${font.javaClass.name}")
        }
    }

    @ExperimentalTextApi
    override suspend fun awaitLoad(font: Font): Any? {
        return when (font) {
            is AndroidFont -> font.typefaceLoader.awaitLoad(context, font)
            is ResourceFont -> font.loadAsync(context)
            else -> throw java.lang.IllegalArgumentException("Unknown font type: $font")
        }
    }

    @ExperimentalTextApi
    override val cacheKey: String = "androidx.compose.ui.tooling.LayoutlibFontResourceLoader"
}

private fun ResourceFont.load(context: Context): Typeface =
    ResourcesCompat.getFont(context, resId)!!

// TODO(seanmcq): Move to core-ktx to dedup
private suspend fun ResourceFont.loadAsync(context: Context): Typeface {
    return suspendCancellableCoroutine { continuation ->
        ResourcesCompat.getFont(context, resId, object : ResourcesCompat.FontCallback() {
            override fun onFontRetrieved(typeface: Typeface) {
                continuation.resume(typeface)
            }

            override fun onFontRetrievalFailed(reason: Int) {
                continuation.cancel(
                    IllegalStateException("Unable to load font ${this@loadAsync} (reason=$reason)")
                )
            }
        }, null)
    }
}