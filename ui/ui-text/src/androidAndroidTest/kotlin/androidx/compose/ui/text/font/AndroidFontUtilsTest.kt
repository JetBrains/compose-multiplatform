/*
 * Copyright 2021 The Android Open Source Project
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

import android.graphics.Typeface
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTextApi::class)
class AndroidFontUtilsTest {

    @Test
    fun androidBold_is600() {
        assertThat(FontWeight.AndroidBold).isEqualTo(FontWeight(600))
    }

    @Test
    fun getPlatformTypeface_isNotBold_isNotItalic_returnsItalicValue() {
        for (weight in 1 until FontWeight.AndroidBold.weight) {
            assertThat(getAndroidTypefaceStyle(FontWeight(weight), FontStyle.Normal))
                .isEqualTo(Typeface.NORMAL)
            assertThat(getAndroidTypefaceStyle(FontWeight(weight), FontStyle.Italic))
                .isEqualTo(Typeface.ITALIC)
        }
    }

    @Test
    fun getPlatformTypeface_isBold_returnsItalicValue() {
        for (weight in FontWeight.AndroidBold.weight until 1000) {
            assertThat(getAndroidTypefaceStyle(FontWeight(weight), FontStyle.Normal))
                .isEqualTo(Typeface.BOLD)
            assertThat(getAndroidTypefaceStyle(FontWeight(weight), FontStyle.Italic))
                .isEqualTo(Typeface.BOLD_ITALIC)
        }
    }
}