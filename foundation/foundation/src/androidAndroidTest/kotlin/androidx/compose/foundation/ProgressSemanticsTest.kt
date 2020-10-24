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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.AccessibilityRangeInfo
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.ui.test.assertRangeInfoEquals
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class ProgressSemanticsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun determinateProgress_testSemantics() {
        val tag = "linear"
        val progress = mutableStateOf(0f)

        rule.setContent {
            Box(
                Modifier
                    .testTag(tag)
                    .progressSemantics(progress.value)
                    .preferredSize(50.dp)
                    .background(color = Color.Cyan)
            )
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals("0 percent")
            .assertRangeInfoEquals(AccessibilityRangeInfo(0f, 0f..1f))

        rule.runOnUiThread {
            progress.value = 0.005f
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals("1 percent")
            .assertRangeInfoEquals(AccessibilityRangeInfo(0.005f, 0f..1f))

        rule.runOnUiThread {
            progress.value = 0.5f
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals("50 percent")
            .assertRangeInfoEquals(AccessibilityRangeInfo(0.5f, 0f..1f))
    }

    @Test
    fun indeterminateProgress_testSemantics() {
        val tag = "linear"

        rule.setContent {
            Box(
                Modifier
                    .testTag(tag)
                    .progressSemantics()
                    .preferredSize(50.dp)
                    .background(color = Color.Cyan)
            )
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals(Strings.InProgress)
    }
}