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

package androidx.compose.foundation.copyPasteAndroidTests.text

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isNotNull
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TextLayoutDirectionTest {

    @Test
    fun testCoreTextField_getsCorrectLayoutDirection() = runSkikoComposeUiTest {
        var layoutDirection: LayoutDirection? = null

        setContent {
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

        runOnIdle {
            assertThat(layoutDirection).isNotNull()
            assertThat(layoutDirection!!).isEqualTo(LayoutDirection.Rtl)
        }
    }
}
