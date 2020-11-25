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

package androidx.compose.foundation.layout

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Providers
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SmallTest
class TextLayoutDirectionModifierTest : LayoutTest() {

    @Test
    fun test_CoreTextField_RtlLayoutDirection_changesDirectionTo_Rtl() {
        val latch = CountDownLatch(1)
        var layoutDirection: LayoutDirection? = null

        show {
            Providers(AmbientLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    value = TextFieldValue("..."),
                    onValueChange = {},
                    onTextLayout = { result ->
                        layoutDirection = result.layoutInput.layoutDirection
                        latch.countDown()
                    }
                )
            }
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(layoutDirection).isNotNull()
        assertThat(layoutDirection!!).isEqualTo(LayoutDirection.Rtl)
    }

    @Test
    fun test_CoreText_RtlLayoutDirection_changesDirectionTo_Rtl() {
        val latch = CountDownLatch(1)
        var layoutDirection: LayoutDirection? = null
        show {
            Providers(AmbientLayoutDirection provides LayoutDirection.Rtl) {
                BasicText(
                    text = AnnotatedString("..."),
                    style = TextStyle.Default,
                    onTextLayout = { result ->
                        layoutDirection = result.layoutInput.layoutDirection
                        latch.countDown()
                    },
                    softWrap = true,
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                    inlineContent = mapOf()
                )
            }
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(layoutDirection).isNotNull()
        assertThat(layoutDirection!!).isEqualTo(LayoutDirection.Rtl)
    }
}
