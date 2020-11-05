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

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.PlatformStringDelegate
import androidx.compose.ui.text.intl.DesktopLocale
import androidx.compose.ui.text.intl.PlatformLocale

/**
 * An Desktop implementation of StringDelegate
 */
internal class DesktopStringDelegate : PlatformStringDelegate {
    override fun toUpperCase(string: String, locale: PlatformLocale): String =
        string.toUpperCase((locale as DesktopLocale).locale)

    override fun toLowerCase(string: String, locale: PlatformLocale): String =
        string.toLowerCase((locale as DesktopLocale).locale)

    override fun capitalize(string: String, locale: PlatformLocale): String =
        string.capitalize((locale as DesktopLocale).locale)

    override fun decapitalize(string: String, locale: PlatformLocale): String =
        string.decapitalize((locale as DesktopLocale).locale)
}

internal actual fun ActualStringDelegate(): PlatformStringDelegate =
    DesktopStringDelegate()