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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.util.fastMap

/**
 * Defines a list of [Locale] objects.
 *
 * @see TextStyle
 * @see SpanStyle
 */
@Immutable
class LocaleList constructor(val localeList: List<Locale>) : Collection<Locale> {
    companion object {
        /**
         * Returns Locale object which represents current locale
         */
        val current: LocaleList
            get() = LocaleList(platformLocaleDelegate.current.fastMap { Locale(it) })
    }

    /**
     * Create a [LocaleList] object from comma separated language tags.
     *
     * @param languageTags A comma separated [IETF BCP47](https://tools.ietf.org/html/bcp47)
     * compliant language tag.
     */
    constructor(languageTags: String) :
        this(languageTags.split(",").fastMap { it.trim() }.fastMap { Locale(it) })

    /**
     * Creates a [LocaleList] object from a list of [Locale]s.
     */
    constructor(vararg locales: Locale) : this(locales.toList())

    operator fun get(i: Int) = localeList[i]

    // Collection overrides for easy iterations.
    override val size: Int = localeList.size

    override operator fun contains(element: Locale): Boolean = localeList.contains(element)

    override fun containsAll(elements: Collection<Locale>): Boolean =
        localeList.containsAll(elements)

    override fun isEmpty(): Boolean = localeList.isEmpty()

    override fun iterator(): Iterator<Locale> = localeList.iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocaleList) return false
        if (localeList != other.localeList) return false
        return true
    }

    override fun hashCode(): Int {
        return localeList.hashCode()
    }

    override fun toString(): String {
        return "LocaleList(localeList=$localeList)"
    }
}
