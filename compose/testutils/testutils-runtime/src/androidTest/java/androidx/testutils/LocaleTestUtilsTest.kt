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
import android.os.Build
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_LTR
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

// Fetch default locale as early as possible (e.g., before initializing LocaleTestUtilsTest class)
private val DEFAULT_LANGUAGE = Locale.getDefault().toString()

@RunWith(AndroidJUnit4::class)
@LargeTest
class LocaleTestUtilsTest {
    private val configuration: Configuration
        get() =
            (ApplicationProvider.getApplicationContext() as Context).resources.configuration
    private val Configuration.language: String get() =
        ConfigurationCompat.getLocales(this).get(0).toString()

    private lateinit var localeUtil: LocaleTestUtils
    private var expectRtlInDefaultLanguage: Boolean = false

    @Before
    fun setUp() {
        localeUtil = LocaleTestUtils(
            ApplicationProvider.getApplicationContext() as Context
        )
        determineDefaultLayoutDirection()
    }

    @After
    fun tearDown() {
        localeUtil.resetLocale()
    }

    @Test
    fun test_defaultValues() {
        assertDefaultValues()
    }

    @Test
    fun test_setAndResetLocale() {
        assertDefaultValues()
        localeUtil.setLocale(LocaleTestUtils.LTR_LANGUAGE)
        assertLocaleIs(LocaleTestUtils.LTR_LANGUAGE, false)
        localeUtil.resetLocale()
        assertDefaultValues()
        localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        assertLocaleIs(LocaleTestUtils.RTL_LANGUAGE, true)
        localeUtil.resetLocale()
        assertDefaultValues()
    }

    @Test
    fun test_ltrRtlLanguagesExist() {
        val availableLanguages = Locale.getAvailableLocales().map { it.toString() }
        val getReason: (String, String) -> String = { name, code ->
            "$name test language '$code' does not exist on test device"
        }
        assertThat(
            getReason(
                "Default",
                LocaleTestUtils.DEFAULT_TEST_LANGUAGE
            ),
            availableLanguages,
            CoreMatchers.hasItem(LocaleTestUtils.DEFAULT_TEST_LANGUAGE)
        )
        assertThat(
            getReason("LTR", LocaleTestUtils.LTR_LANGUAGE),
            availableLanguages,
            CoreMatchers.hasItem(LocaleTestUtils.LTR_LANGUAGE)
        )
        assertThat(
            getReason("RTL", LocaleTestUtils.RTL_LANGUAGE),
            availableLanguages,
            CoreMatchers.hasItem(LocaleTestUtils.RTL_LANGUAGE)
        )
    }

    private fun assertDefaultValues() {
        assertLocaleIs(DEFAULT_LANGUAGE, expectRtlInDefaultLanguage)
    }

    private fun assertLocaleIs(lang: String, expectRtl: Boolean) {
        assertThat(
            "Locale should be $lang",
            configuration.language,
            CoreMatchers.equalTo(lang)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            assertThat(
                "Layout direction should be ${if (expectRtl) "RTL" else "LTR"}",
                configuration.layoutDirection,
                CoreMatchers.equalTo(if (expectRtl) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR)
            )
        }
    }

    private fun determineDefaultLayoutDirection() {
        assertThat(
            "Locale must still be the default when determining the default layout direction",
            configuration.language,
            CoreMatchers.equalTo(DEFAULT_LANGUAGE)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            expectRtlInDefaultLanguage = configuration.layoutDirection == LAYOUT_DIRECTION_RTL
        }
    }
}
