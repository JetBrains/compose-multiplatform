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

package androidx.compose.ui.text.intl

import java.awt.ComponentOrientation
import java.util.Locale as JavaLocale

internal class DesktopLocale(val javaLocale: JavaLocale) : PlatformLocale {
    override val language: String
        get() = javaLocale.language

    override val script: String
        get() = javaLocale.script

    override val region: String
        get() = javaLocale.country

    override fun toLanguageTag(): String = javaLocale.toLanguageTag()
}

internal actual fun createPlatformLocaleDelegate() = object : PlatformLocaleDelegate {
    override val current: LocaleList
        get() = LocaleList(listOf(Locale(DesktopLocale(JavaLocale.getDefault()))))

    override fun parseLanguageTag(languageTag: String): PlatformLocale =
        DesktopLocale(
            JavaLocale.forLanguageTag(
                languageTag
            )
        )
}

internal actual fun PlatformLocale.isRtl(): Boolean =
    // TODO Get rid of AWT reference here
    !ComponentOrientation.getOrientation((this as DesktopLocale).javaLocale).isLeftToRight
