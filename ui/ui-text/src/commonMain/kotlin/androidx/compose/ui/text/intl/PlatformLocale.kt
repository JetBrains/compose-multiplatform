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

/**
 * Interface for providing platform dependent locale object.
 */
internal interface PlatformLocale {

    /**
     * Implementation must give ISO 639 compliant language code.
     */
    val language: String

    /**
     * Implementation must give ISO 15924 compliant 4-letter script code.
     */
    val script: String

    /**
     * Implementation must give ISO 3166 compliant region code.
     */
    val region: String

    /**
     * Implementation must return IETF BCP47 compliant language tag representation of this Locale.
     */
    fun toLanguageTag(): String
}

/**
 * Interface for providing platform dependent locale non-instance helper functions.
 *
 */
internal interface PlatformLocaleDelegate {
    /**
     * Returns the list of current locales.
     *
     * The implementation must return at least one locale.
     */
    val current: LocaleList

    /**
     * Parse the IETF BCP47 compliant language tag.
     *
     * @return The locale
     */
    fun parseLanguageTag(languageTag: String): PlatformLocale
}

internal expect fun createPlatformLocaleDelegate(): PlatformLocaleDelegate

internal val platformLocaleDelegate = createPlatformLocaleDelegate()
