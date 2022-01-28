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

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.matchers.assertThat
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@SmallTest
@Suppress("DEPRECATION")
class GenericFontFamilyCacheTest {

    @Test
    @Suppress("DEPRECATION")
    fun cached_instance_for_the_same_input() {
        val typeface = AndroidGenericFontFamilyTypeface(FontFamily.SansSerif)
        assertThat(
            typeface.getNativeTypeface(FontWeight.Bold, FontStyle.Normal, FontSynthesis.None)
        ).isSameInstanceAs(
            typeface.getNativeTypeface(FontWeight.Bold, FontStyle.Normal, FontSynthesis.None)
        )
    }

    @Test
    @Suppress("DEPRECATION")
    fun not_cached_instance_if_different_input() {
        val typeface = AndroidGenericFontFamilyTypeface(FontFamily.SansSerif)
        assertThat(
            typeface.getNativeTypeface(FontWeight.Bold, FontStyle.Normal, FontSynthesis.None)
        ).isNotSameInstanceAs(
            typeface.getNativeTypeface(FontWeight.Bold, FontStyle.Italic, FontSynthesis.None)
        )
    }
}