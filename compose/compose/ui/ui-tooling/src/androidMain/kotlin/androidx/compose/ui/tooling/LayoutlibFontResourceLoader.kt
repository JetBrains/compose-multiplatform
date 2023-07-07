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
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.ResourceFont

/**
 * Layoutlib implementation for [Font.ResourceLoader]
 */
@Suppress("DEPRECATION")
internal class LayoutlibFontResourceLoader(private val context: Context) : Font.ResourceLoader {
    @Deprecated(
        "Replaced by FontFamily.Resolver, this method should not be called",
        replaceWith = ReplaceWith("FontFamily.Resolver.resolve(font, )")
    )
    override fun load(font: Font): Typeface {
        return if (font is ResourceFont && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ResourceFontHelper.load(context, font)
        } else {
            throw IllegalArgumentException("Unknown font type: ${font.javaClass.name}")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private object ResourceFontHelper {
    @DoNotInline
    fun load(context: Context, font: ResourceFont): Typeface {
        return context.resources.getFont(font.resId)
    }
}