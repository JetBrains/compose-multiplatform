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

package androidx.compose.ui.res

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.LoadedFontFamily
import androidx.compose.ui.text.font.SystemFontFamily

private val cacheLock = Object()

/**
 * This cache is expected to be used for SystemFontFamily or LoadedFontFamily.
 * FontFamily instance cannot be used as the file based FontFamily.
 */
@GuardedBy("cacheLock")
private val syncLoadedTypefaces = mutableMapOf<FontFamily, Typeface>()

/**
 * Synchronously load an font from [FontFamily].
 *
 * @param fontFamily the fontFamily
 * @return the decoded image data associated with the resource
 */
@Suppress("DEPRECATION")
@Composable
@ReadOnlyComposable
@Deprecated(
    "Prefer to preload fonts using FontFamily.Resolver.",
    replaceWith = ReplaceWith(
        "FontFamily.Resolver.preload(fontFamily, Font.AndroidResourceLoader(context))"
    ),
    level = DeprecationLevel.WARNING
)
fun fontResource(fontFamily: FontFamily): Typeface {
    return fontResourceFromContext(LocalContext.current, fontFamily)
}

@Suppress("DEPRECATION")
@Deprecated(
    "Prefer to preload fonts using FontFamily.Resolver.",
    replaceWith = ReplaceWith(
        "FontFamily.Resolver.preload(fontFamily, Font.AndroidResourceLoader(context))"
    ),
    level = DeprecationLevel.WARNING
)
private fun fontResourceFromContext(context: Context, a: FontFamily): Typeface {
    if (a is SystemFontFamily || a is LoadedFontFamily) {
        synchronized(cacheLock) {
            return syncLoadedTypefaces.getOrPut(a) {
                Typeface(context, a)
            }
        }
    } else {
        return Typeface(context, a)
    }
}
