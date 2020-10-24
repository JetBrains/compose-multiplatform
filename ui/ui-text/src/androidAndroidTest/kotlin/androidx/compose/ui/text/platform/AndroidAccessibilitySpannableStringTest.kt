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

import android.text.SpannableString
import android.text.style.LocaleSpan
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.annotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidAccessibilitySpannableStringTest {
    @Test
    fun toAccessibilitySpannableString_with_localeSpan() {
        val annotatedString = annotatedString {
            append("hello")
            withStyle(style = SpanStyle(localeList = LocaleList("en-gb"))) {
                append("world")
            }
        }

        val spannableString = annotatedString.toAccessibilitySpannableString()
        assertThat(spannableString).isInstanceOf(SpannableString::class.java)

        assertThat(
            spannableString.getSpans(0, spannableString.length, LocaleSpan::class.java)
        ).isNotEmpty()
    }
}