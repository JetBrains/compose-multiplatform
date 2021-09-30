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

internal class JsLocale(val locale: dynamic) : PlatformLocale {
    override val language: String
        get() = locale.languageCode!!

    override val script: String
        get() = locale.scriptCode!!

    override val region: String
        get() = locale.countryCode!!

    override fun toLanguageTag(): String = TODO("implement native toLanguageTag") // locale.toLanguageTag()
}

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate =
    object : PlatformLocaleDelegate {
        override val current: List<PlatformLocale>
            get() = listOf(JsLocale(Any()))


        override fun parseLanguageTag(languageTag: String): PlatformLocale {
            return JsLocale(Any())
        }
    }


