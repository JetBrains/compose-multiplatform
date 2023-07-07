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

package androidx.compose.foundation.text

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextLayoutDirectionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testCoreTextField_getsCorrectLayoutDirection() {
        var layoutDirection: LayoutDirection? = null

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CoreTextField(
                    value = TextFieldValue("..."),
                    onValueChange = {},
                    onTextLayout = { result ->
                        layoutDirection = result.layoutInput.layoutDirection
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(layoutDirection).isNotNull()
            assertThat(layoutDirection!!).isEqualTo(LayoutDirection.Rtl)
        }
    }
}
