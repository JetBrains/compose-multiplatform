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

package androidx.compose.ui.text.android

import android.text.BoringLayout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BulletSpan
import androidx.compose.ui.text.font.test.R
import androidx.core.content.res.ResourcesCompat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class LayoutIntrinsicsTest {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val typeface = ResourcesCompat.getFont(instrumentation.context, R.font.sample_font)!!

    @Test
    fun boringMetrics_returns_nonnull_for_boring_text() {
        val boringMetrics = LayoutIntrinsics(
            "a",
            TextPaint(),
            LayoutCompat.TEXT_DIRECTION_LTR
        ).boringMetrics

        assertThat(boringMetrics).isNotNull()
        assertThat(boringMetrics).isInstanceOf(BoringLayout.Metrics::class.java)
    }

    @Test
    fun boringMetrics_returns_null_for_rtl_text() {
        assertThat(
            LayoutIntrinsics(
                "\u05D0",
                TextPaint(),
                LayoutCompat.TEXT_DIRECTION_RTL
            ).boringMetrics
        ).isNull()
    }

    @Test
    fun boringMetrics_returns_null_for_spannable_with_paragraph_style() {
        val spannable = SpannableString("a").apply {
            setSpan(BulletSpan(), 0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }

        assertThat(
            LayoutIntrinsics(
                spannable,
                TextPaint(),
                LayoutCompat.TEXT_DIRECTION_LTR
            ).boringMetrics
        ).isNull()
    }
}