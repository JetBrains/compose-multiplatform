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

package androidx.compose.ui.tooling.preview

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.ResourceFont

/**
 * Layoutlib implementation for [Font.ResourceLoader]
 */
internal class LayoutlibFontResourceLoader(private val context: Context) : Font.ResourceLoader {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun load(font: Font): Typeface {
        return when (font) {
            is ResourceFont -> context.resources.getFont(font.resId)
            else -> throw IllegalArgumentException("Unknown font type: ${font.javaClass.name}")
        }
    }
}
