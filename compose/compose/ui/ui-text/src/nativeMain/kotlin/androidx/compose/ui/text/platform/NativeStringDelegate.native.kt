/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.text.PlatformStringDelegate
import androidx.compose.ui.text.intl.NativeLocale
import androidx.compose.ui.text.intl.PlatformLocale
import platform.Foundation.*

/**
 * A native implementation of StringDelegate
 */

// TODO Remove once https://youtrack.jetbrains.com/issue/KT-23978 fixed
@Suppress("CAST_NEVER_SUCCEEDS")
internal class NativeStringDelegate : PlatformStringDelegate {
    override fun toUpperCase(string: String, locale: PlatformLocale): String =
        toUpperCase(string as NSString, locale as NativeLocale)

    private inline fun toUpperCase(string: NSString, locale: NativeLocale): String =
        string.uppercaseStringWithLocale(locale.locale)

    override fun toLowerCase(string: String, locale: PlatformLocale): String =
        toLowerCase(string as NSString, locale as NativeLocale)

    private inline fun toLowerCase(string: NSString, locale: NativeLocale): String =
        string.lowercaseStringWithLocale(locale.locale)

    override fun capitalize(string: String, locale: PlatformLocale): String =
        string.replaceFirstChar {
            if (it.isLowerCase())
                capitalize(it.toString() as NSString, locale as NativeLocale)
            else
                it.toString()
        }

    private inline fun capitalize(string: NSString, locale: NativeLocale): String =
        string.capitalizedStringWithLocale(locale.locale)

    override fun decapitalize(string: String, locale: PlatformLocale): String =
        string.replaceFirstChar { decapitalize(it.toString() as NSString, locale as NativeLocale) }

    private inline fun decapitalize(string: NSString, locale: NativeLocale): String =
        string.lowercaseStringWithLocale(locale.locale)
}

internal actual fun ActualStringDelegate(): PlatformStringDelegate =
    NativeStringDelegate()
