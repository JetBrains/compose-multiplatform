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

package androidx.compose.foundation.text

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextSemanticsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun semanticsTextChanges_String() {
        var text by mutableStateOf("before")
        rule.setContent {
            BasicText(text)
        }
        rule.onNodeWithText("before").assertExists()
        text = "after"
        rule.onNodeWithText("after").assertExists()
    }

    @Test
    fun semanticsTextChanges_AnnotatedString() {
        var text by mutableStateOf("before")
        rule.setContent {
            BasicText(AnnotatedString(text))
        }
        rule.onNodeWithText("before").assertExists()
        text = "after"
        rule.onNodeWithText("after").assertExists()
    }
}
