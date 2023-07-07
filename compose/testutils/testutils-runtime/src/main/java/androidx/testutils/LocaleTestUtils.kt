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

package androidx.testutils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Utility class to save and restore the locale of the system.
 *
 * Inspired by [com.android.dialer.util.LocaleTestUtils](https://android.googlesource.com/platform/packages/apps/Dialer/+/94b10b530c0fc297e2974e57e094c500d3ee6003/tests/src/com/android/dialer/util/LocaleTestUtils.java)
 *
 * This can be used for tests that assume to be run in a certain locale, e.g., because they check
 * against strings in a particular language or require an assumption on how the system will behave
 * in a specific locale.
 *
 * In your test, you can change the locale with the following code:
 * <pre>
 * public class CanadaFrenchTest extends AndroidTestCase {
 *     private LocaleTestUtils mLocaleTestUtils;
 *
 *     &#64;Override
 *     public void setUp() throws Exception {
 *         super.setUp();
 *         mLocaleTestUtils = new LocaleTestUtils(getContext());
 *         mLocaleTestUtils.setLocale(Locale.CANADA_FRENCH);
 *     }
 *
 *     &#64;Override
 *     public void tearDown() throws Exception {
 *         mLocaleTestUtils.resetLocale();
 *         mLocaleTestUtils = null;
 *         super.tearDown();
 *     }
 *
 *     ...
 * }
 * </pre>
 * Note that one should not call [setLocale] more than once without calling [resetLocale] first.
 *
 * This class is not thread-safe. Usually its methods should be invoked only from the test thread.
 *
 * @property mContext the context on which to alter the locale
 * @constructor Create a new instance that can be used to set and reset the locale for the given
 * context.
 */
class LocaleTestUtils(private val mContext: Context) {
    companion object {
        const val DEFAULT_TEST_LANGUAGE = "en_US"
        const val RTL_LANGUAGE = "ar_SA"
        const val LTR_LANGUAGE = "fr_FR"
    }

    private var saved: Boolean = false
    private var savedContextLocale: LocaleListCompat? = null
    private var savedSystemLocale: LocaleListCompat? = null
    private var locale: Locale? = null
    private var canSave: Boolean = true

    /**
     * Set the locale to the given value and saves the previous value.
     *
     * @param lang the language to which the locale should be set, in the same format as
     * Locale.toString()
     * @throws IllegalStateException if the locale was already set
     */
    fun setLocale(lang: String) {
        synchronized(this) {
            val locale = findLocale(lang)
            if (saved) {
                throw IllegalStateException("call restoreLocale() before calling setLocale() again")
            }
            if (!canSave) {
                throw IllegalStateException(
                    "can't set locale after isLocaleChangedAndLock() is " +
                        "called"
                )
            }
            this.locale = locale
            val locales = LocaleListCompat.create(locale)
            savedContextLocale = updateResources(mContext.resources, locales)
            savedSystemLocale = updateResources(Resources.getSystem(), locales)
            saved = true
            canSave = false
        }
    }

    /**
     * Restores the original locale, if it was changed, and unlocks the ability to change the locale
     * for this object, if it was locked by [isLocaleChangedAndLock]. If the locale wasn't changed,
     * it leaves the locales untouched, but will still unlock this object if it was locked.
     */
    fun resetLocale() {
        synchronized(this) {
            canSave = true
            if (!saved) {
                return
            }
            updateResources(mContext.resources, savedContextLocale!!)
            updateResources(Resources.getSystem(), savedSystemLocale!!)
            saved = false
        }
    }

    /**
     * Gets the Locale to which the locale has been changed, or null if the locale hasn't been
     * changed.
     */
    fun getLocale(): Locale? {
        synchronized(this) {
            return locale
        }
    }

    /**
     * Returns if the locale has been changed.
     */
    fun isLocaleChanged(): Boolean {
        synchronized(this) {
            return saved
        }
    }

    /**
     * Returns if the locale has been changed, and disables all future locale changes until
     * [resetLocale] has been called. Calling [setLocale] after calling this method will throw an
     * exception.
     *
     * Use this check-and-lock if the behavior of a component depends on whether or not the locale
     * has been changed, and it only checks it when initializing the component. E.g., when starting
     * a test Activity.
     */
    fun isLocaleChangedAndLock(): Boolean {
        synchronized(this) {
            canSave = false
            return saved
        }
    }

    /**
     * Finds the best matching Locale on the system for the given language
     */
    private fun findLocale(lang: String): Locale {
        // Build list of prefixes ("ar_SA_xx_foo_bar" -> ["ar", "ar_SA", "ar_SA_xx", etc..])
        val prefixes = lang.split("_").fold(mutableListOf<String>()) { prefixes, elem ->
            prefixes.also { it.add(if (it.isEmpty()) elem else "${it.last()}_$elem") }
        }

        // Build lists of matches per prefix
        val matches = List<MutableList<Locale>>(prefixes.size) { mutableListOf() }
        for (locale in Locale.getAvailableLocales()) {
            val language = locale.toString()
            prefixes.forEachIndexed { i, prefix ->
                if (language.startsWith(prefix)) {
                    if (language == prefix) {
                        // Exact matches are preferred, so put them in front
                        matches[i].add(0, locale)
                    } else if (matches[i].isEmpty()) {
                        // We only need one inexact match per prefix
                        matches[i].add(locale)
                    }
                }
            }
        }

        // Find best match: the locale that has the longest common prefix with the given language
        return matches.lastOrNull { it.isNotEmpty() }?.first() ?: Locale.getDefault()
    }

    /**
     * Sets the locale(s) for the given resources and returns the previous locales.
     *
     * @param resources the resources on which to set the locales
     * @param locales the value(s) to which to set the locales
     * @return the previous value of the locales for the resources
     */
    private fun updateResources(resources: Resources, locales: LocaleListCompat): LocaleListCompat {
        val savedLocales = ConfigurationCompat.getLocales(resources.configuration)
        val newConfig = Configuration(resources.configuration)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ->
                newConfig.setLocales(locales.unwrap() as LocaleList)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ->
                newConfig.setLocale(locales.get(0))
            else ->
                @Suppress("DEPRECATION")
                newConfig.locale = locales.get(0)
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(newConfig, resources.displayMetrics)
        return savedLocales
    }
}