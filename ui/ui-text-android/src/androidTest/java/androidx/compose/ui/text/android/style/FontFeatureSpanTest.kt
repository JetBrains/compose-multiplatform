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
package androidx.compose.ui.text.android.style

import android.text.TextPaint
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class FontFeatureSpanTest {
    @Test
    fun updateMeasureState_changePaintFontFeatureSettings() {
        val fontFeatureSettings = "\"kern\" 0"
        val span = FontFeatureSpan(fontFeatureSettings)
        val textPaint = TextPaint()
        // Make sure baseline shift is zero when created
        Truth.assertThat(textPaint.fontFeatureSettings).isNull()
        span.updateMeasureState(textPaint)
        Truth.assertThat(textPaint.fontFeatureSettings).isEqualTo(fontFeatureSettings)
    }

    @Test
    fun updateDrawState_changePaintBaselineShift() {
        val fontFeatureSettings = "\"kern\" 0"
        val span = FontFeatureSpan(fontFeatureSettings)
        val textPaint = TextPaint()
        // Make sure baseline shift is zero when created
        Truth.assertThat(textPaint.fontFeatureSettings).isNull()
        span.updateDrawState(textPaint)
        Truth.assertThat(textPaint.fontFeatureSettings).isEqualTo(fontFeatureSettings)
    }
}