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

package androidx.compose.ui.text.font

import androidx.compose.ui.text.ExperimentalTextApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class ResourceFontVariationSettingsTest {
    @OptIn(ExperimentalTextApi::class)
    @Test
    fun resourceFont_acceptsVariationSettings() {
        val resourceFont = ResourceFont(
            -1,
            FontWeight(100),
            FontStyle.Italic,
            variationSettings = FontVariation.Settings(FontVariation.grade(3))
        )
        assertThat(resourceFont.variationSettings.settings).containsExactly(
            FontVariation.grade(3)
        )
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun resourceFont_usesVariationSettingsInEquals() {
        val resourceFont = ResourceFont(
            -1,
            FontWeight(100),
            FontStyle.Italic,
            variationSettings = FontVariation.Settings(FontVariation.grade(3))
        )
        val resourceFont2 = ResourceFont(
            -1,
            FontWeight(100),
            FontStyle.Italic,
            variationSettings = FontVariation.Settings(FontVariation.grade(4))
        )

        assertThat(resourceFont).isNotEqualTo(resourceFont2)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun resourceFont_usesVariationSettingsInHashCode() {
        val resourceFont = ResourceFont(
            -1,
            FontWeight(100),
            FontStyle.Italic,
            variationSettings = FontVariation.Settings(FontVariation.grade(3))
        )
        val resourceFont2 = ResourceFont(
            -1,
            FontWeight(100),
            FontStyle.Italic,
            variationSettings = FontVariation.Settings(FontVariation.grade(4))
        )

        assertThat(resourceFont.hashCode()).isNotEqualTo(resourceFont2.hashCode())
    }
}