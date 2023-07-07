/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextMeasurerHelperTest {

    @get:Rule
    val rule = createComposeRule()

    val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun whenFontFamilyResolverChanges_TextMeasurerShouldChange() {
        val fontFamilyResolver = mutableStateOf(createFontFamilyResolver(context))
        val measurers = mutableSetOf<TextMeasurer>()

        rule.setContent {
            CompositionLocalProvider(
                LocalFontFamilyResolver provides fontFamilyResolver.value
            ) {
                val textMeasurer = rememberTextMeasurer()
                measurers.add(textMeasurer)
            }
        }

        rule.waitForIdle()
        // FontFamily.Resolver implementation has only instance check for equality
        // new instance should always be unequal to any other instance
        fontFamilyResolver.value = createFontFamilyResolver(context)
        rule.waitForIdle()

        assertThat(measurers.size).isEqualTo(2)
    }

    @Test
    fun whenDensityChanges_TextMeasurerShouldChange() {
        val density = mutableStateOf(Density(1f))
        val measurers = mutableSetOf<TextMeasurer>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides density.value
            ) {
                val textMeasurer = rememberTextMeasurer()
                measurers.add(textMeasurer)
            }
        }

        rule.waitForIdle()
        density.value = Density(2f)
        rule.waitForIdle()

        assertThat(measurers.size).isEqualTo(2)
    }

    @Test
    fun whenLayoutDirectionChanges_TextMeasurerShouldChange() {
        val layoutDirection = mutableStateOf(LayoutDirection.Ltr)
        val measurers = mutableSetOf<TextMeasurer>()

        rule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection.value
            ) {
                val textMeasurer = rememberTextMeasurer()
                measurers.add(textMeasurer)
            }
        }

        rule.waitForIdle()
        layoutDirection.value = LayoutDirection.Rtl
        rule.waitForIdle()

        assertThat(measurers.size).isEqualTo(2)
    }

    @Test
    fun whenCacheSizeChanges_TextMeasurerShouldChange() {
        val cacheSize = mutableStateOf(4)
        val measurers = mutableSetOf<TextMeasurer>()

        rule.setContent {
            val textMeasurer = rememberTextMeasurer(cacheSize = cacheSize.value)
            measurers.add(textMeasurer)
        }

        rule.waitForIdle()
        cacheSize.value = 8
        rule.waitForIdle()

        assertThat(measurers.size).isEqualTo(2)
    }
}