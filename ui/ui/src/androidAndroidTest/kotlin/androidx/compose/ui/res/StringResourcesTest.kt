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

package androidx.compose.ui.res

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.R
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@MediumTest
class StringResourcesTest {

    // Constants defined in strings.xml
    private val NotLocalizedText = "NotLocalizedText"
    private val DefaultLocalizedText = "DefaultLocaleText"
    private val SpanishLocalizedText = "SpanishText"

    // Constants defined in strings.xml with formatting with integer 100.
    private val NotLocalizedFormatText = "NotLocalizedFormatText:100"
    private val DefaultLocalizedFormatText = "DefaultLocaleFormatText:100"
    private val SpanishLocalizedFormatText = "SpanishFormatText:100"

    // Constant used for formatting string in test.
    private val FormatValue = 100

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun stringResource_not_localized_defaultLocale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(stringResource(R.string.not_localized)).isEqualTo(NotLocalizedText)
            }
        }
    }

    @Test
    fun stringResource_not_localized() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val spanishContext = context.createConfigurationContext(
            context.resources.configuration.apply {
                setLocale(Locale.forLanguageTag("es-ES"))
            }
        )

        rule.setContent {
            CompositionLocalProvider(LocalContext provides spanishContext) {
                assertThat(stringResource(R.string.not_localized)).isEqualTo(NotLocalizedText)
            }
        }
    }

    @Test
    fun stringResource_localized_defaultLocale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(stringResource(R.string.localized))
                    .isEqualTo(DefaultLocalizedText)
            }
        }
    }

    @Test
    fun stringResource_localized() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val spanishContext = context.createConfigurationContext(
            context.resources.configuration.apply {
                setLocale(Locale.forLanguageTag("es-ES"))
            }
        )

        rule.setContent {
            CompositionLocalProvider(LocalContext provides spanishContext) {
                assertThat(stringResource(R.string.localized))
                    .isEqualTo(SpanishLocalizedText)
            }
        }
    }

    @Test
    fun stringResource_not_localized_format_defaultLocale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(stringResource(R.string.not_localized_format, FormatValue))
                    .isEqualTo(NotLocalizedFormatText)
            }
        }
    }

    @Test
    fun stringResource_not_localized_format() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val spanishContext = context.createConfigurationContext(
            context.resources.configuration.apply {
                setLocale(Locale.forLanguageTag("es-ES"))
            }
        )

        rule.setContent {
            CompositionLocalProvider(LocalContext provides spanishContext) {
                assertThat(stringResource(R.string.not_localized_format, FormatValue))
                    .isEqualTo(NotLocalizedFormatText)
            }
        }
    }

    @Test
    fun stringResource_localized_format_defaultLocale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(stringResource(R.string.localized_format, FormatValue))
                    .isEqualTo(DefaultLocalizedFormatText)
            }
        }
    }

    @Test
    fun stringResource_localized_format() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val spanishContext = context.createConfigurationContext(
            context.resources.configuration.apply {
                setLocale(Locale.forLanguageTag("es-ES"))
            }
        )

        rule.setContent {
            CompositionLocalProvider(LocalContext provides spanishContext) {
                assertThat(stringResource(R.string.localized_format, FormatValue))
                    .isEqualTo(SpanishLocalizedFormatText)
            }
        }
    }

    @Test
    fun stringArrayResource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(stringArrayResource(R.array.string_array))
                    .isEqualTo(arrayOf("string1", "string2"))
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun pluralStringResource_withoutArguments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(pluralStringResource(R.plurals.plurals_without_arguments, 1))
                    .isEqualTo("There is one Android here")
                assertThat(pluralStringResource(R.plurals.plurals_without_arguments, 42))
                    .isEqualTo("There are a number of Androids here")
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun pluralStringResource_withArguments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        rule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                assertThat(pluralStringResource(R.plurals.plurals_with_arguments, 1, 1))
                    .isEqualTo("There is 1 Android here")
                assertThat(pluralStringResource(R.plurals.plurals_with_arguments, 42, 42))
                    .isEqualTo("There are 42 Androids here")
            }
        }
    }
}