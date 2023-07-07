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

package androidx.compose.ui.text.intl

import androidx.annotation.RequiresApi
import android.os.LocaleList as AndroidLocaleList
import java.util.Locale as JavaLocale
import androidx.compose.ui.text.platform.createSynchronizedObject

/**
 * An Android implementation of Locale object
 */
internal class AndroidLocale(val javaLocale: JavaLocale) : PlatformLocale {
    override val language: String
        get() = javaLocale.language

    override val script: String
        get() = javaLocale.script

    override val region: String
        get() = javaLocale.country

    override fun toLanguageTag(): String = javaLocale.toLanguageTag()
}

/**
 * An Android implementation of LocaleDelegate object for API 23
 */
internal class AndroidLocaleDelegateAPI23 : PlatformLocaleDelegate {

    override val current: LocaleList
        get() = LocaleList(listOf(Locale(AndroidLocale(JavaLocale.getDefault()))))

    override fun parseLanguageTag(languageTag: String): PlatformLocale =
        AndroidLocale(JavaLocale.forLanguageTag(languageTag))
}

/**
 * An Android implementation of LocaleDelegate object for API 24 and later
 */
@RequiresApi(api = 24)
internal class AndroidLocaleDelegateAPI24 : PlatformLocaleDelegate {
    private var lastPlatformLocaleList: AndroidLocaleList? = null
    private var lastLocaleList: LocaleList? = null
    private val lock = createSynchronizedObject()

    override val current: LocaleList
        get() {
            val platformLocaleList = AndroidLocaleList.getDefault()
            return synchronized(lock) {
                // try to avoid any more allocs
                lastLocaleList?.let {
                    if (platformLocaleList === lastPlatformLocaleList) return it
                }
                // this is faster than adding to an empty mutableList
                val localeList = LocaleList(
                    List(platformLocaleList.size()) { position ->
                        Locale(AndroidLocale(platformLocaleList[position]))
                    }
                )
                // cache the platform result and compose result
                lastPlatformLocaleList = platformLocaleList
                lastLocaleList = localeList
                localeList
            }
        }

    override fun parseLanguageTag(languageTag: String): PlatformLocale =
        AndroidLocale(JavaLocale.forLanguageTag(languageTag))
}