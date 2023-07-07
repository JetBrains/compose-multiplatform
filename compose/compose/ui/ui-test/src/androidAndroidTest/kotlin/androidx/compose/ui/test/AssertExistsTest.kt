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

package androidx.compose.ui.test

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.expectAssertionError
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AssertExistsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @FlakyTest
    fun toggleTextInHierarchy_assertExistsAndNotExists() {
        rule.setContent {
            MaterialTheme {
                Surface {
                    val (showText, toggle) = remember { mutableStateOf(true) }
                    Column {
                        Button(
                            modifier = Modifier.testTag("MyButton"),
                            onClick = { toggle(!showText) }
                        ) {
                            Text("Toggle")
                        }
                        if (showText) {
                            Text("Hello")
                        }
                    }
                }
            }
        }

        rule.onNodeWithText("Hello")
            .assertExists()

        expectAssertionError(true) {
            rule.onNodeWithText("Hello")
                .assertDoesNotExist()
        }

        val cachedResult = rule.onNodeWithText("Hello")

        // Hide
        rule.onNodeWithTag("MyButton")
            .performClick()

        rule.onNodeWithText("Hello")
            .assertDoesNotExist()

        cachedResult
            .assertDoesNotExist()

        expectAssertionError(true) {
            rule.onNodeWithText("Hello")
                .assertExists()
        }

        expectAssertionError(true) {
            cachedResult.assertExists()
        }

        // Show
        rule.onNodeWithTag("MyButton")
            .performClick()

        rule.onNodeWithText("Hello")
            .assertExists()

        expectAssertionError(true) {
            rule.onNodeWithText("Hello")
                .assertDoesNotExist()
        }
    }
}