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

package androidx.compose.ui.text.android.style

import android.graphics.Typeface
import android.text.TextPaint
import androidx.test.filters.SmallTest
import androidx.compose.ui.text.android.InternalPlatformTextApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalPlatformTextApi::class)
@SmallTest
@RunWith(JUnit4::class)
class FontSpanTest {
    @Test
    fun updatePaint() {
        val textPaint = TextPaint()
        val typeface = Typeface.defaultFromStyle(Typeface.BOLD_ITALIC)
        val span = FontSpan { _, _ -> typeface }

        assertThat(textPaint.typeface).isNotSameInstanceAs(typeface)
        span.updatePaint(textPaint)
        assertThat(textPaint.typeface).isSameInstanceAs(typeface)
    }
}