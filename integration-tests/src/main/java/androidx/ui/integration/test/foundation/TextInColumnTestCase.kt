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

package androidx.ui.integration.test.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Test case that puts the given amount of rectangles into a column layout and makes changes by
 * modifying the color used in the model.
 *
 * Note: Each rectangle has its own model so changes should always affect only the first one.
 */
class TextInColumnTestCase(
    private val numberOfTexts: Int
) : ComposeTestCase, ToggleableTestCase {

    private val fontSize = mutableStateOf(20.sp)

    @Composable
    override fun Content() {
        MaterialTheme {
            Surface {
                Column {
                    repeat(numberOfTexts) {
                        // 32-character text to match dashboards
                        Text(
                            "Hello World Hello World Hello W",
                            style = TextStyle(fontSize = fontSize.value)
                        )
                    }
                }
            }
        }
    }

    override fun toggleState() {
        fontSize.value = if (fontSize.value == 20.sp) {
            15.sp
        } else {
            20.sp
        }
    }
}