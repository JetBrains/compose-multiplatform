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

package androidx.compose.ui.text

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.intl.PlatformLocale
import androidx.compose.ui.text.platform.ActualStringDelegate

/**
 * Interface for providing platform dependent string related operations.
 */
internal interface PlatformStringDelegate {
    /**
     * Implementation must return uppercase transformed String.
     *
     * @param string an input string
     * @param locale a locale object
     * @return a transformed string
     */
    fun toUpperCase(string: String, locale: PlatformLocale): String

    /**
     * Implementation must return lowercase transformed String.
     *
     * @param string an input string
     * @param locale a locale object
     * @return a transformed string
     */
    fun toLowerCase(string: String, locale: PlatformLocale): String

    /**
     * Implementation must return capitalized String.
     *
     * @param string an input string
     * @param locale a locale object
     * @return a transformed string
     */
    fun capitalize(string: String, locale: PlatformLocale): String

    /**
     * Implementation must return decapitalized String.
     *
     * @param string an input string
     * @param locale a locale object
     * @return a transformed string
     */
    fun decapitalize(string: String, locale: PlatformLocale): String
}

/**
 * Returns uppercase transformed String.
 *
 * @param locale a locale object
 * @return a transformed text
 */
fun String.toUpperCase(locale: Locale): String =
    stringDelegate.toUpperCase(this, locale.platformLocale)

/**
 * Returns lowercase transformed String.
 *
 * @param locale a locale object
 * @return a transformed text
 */
fun String.toLowerCase(locale: Locale): String =
    stringDelegate.toLowerCase(this, locale.platformLocale)
/**
 * Returns capitalized String.
 *
 * @param locale a locale object
 * @return a transformed text
 */
fun String.capitalize(locale: Locale): String =
    stringDelegate.capitalize(this, locale.platformLocale)
/**
 * Returns decapitalized String.
 *
 * @param locale a locale object
 * @return a transformed text
 */
fun String.decapitalize(locale: Locale): String =
    stringDelegate.decapitalize(this, locale.platformLocale)

/**
 * Returns uppercase transformed String.
 *
 * @param localeList a locale list object. If empty locale list object is passed, use current locale
 *                   instead.
 * @return a transformed text
 */
fun String.toUpperCase(localeList: LocaleList): String =
    if (localeList.isEmpty()) toUpperCase(Locale.current) else toUpperCase(localeList[0])

/**
 * Returns lowercase transformed String.
 *
 * @param localeList a locale list object. If empty locale list object is passed, use current locale
 *                   instead.
 * @return a transformed text
 */
fun String.toLowerCase(localeList: LocaleList): String =
    if (localeList.isEmpty()) toLowerCase(Locale.current) else toLowerCase(localeList[0])

/**
 * Returns capitalized String.
 *
 * @param localeList a locale list object. If empty locale list object is passed, use current locale
 *                   instead.
 * @return a transformed text
 */
fun String.capitalize(localeList: LocaleList): String =
    if (localeList.isEmpty()) capitalize(Locale.current) else capitalize(localeList[0])

/**
 * Returns decapitalized String.
 *
 * @param localeList a locale list object. If empty locale list object is passed, use current locale
 *                   instead.
 */
fun String.decapitalize(localeList: LocaleList): String =
    if (localeList.isEmpty()) decapitalize(Locale.current) else decapitalize(localeList[0])

private val stringDelegate = ActualStringDelegate()