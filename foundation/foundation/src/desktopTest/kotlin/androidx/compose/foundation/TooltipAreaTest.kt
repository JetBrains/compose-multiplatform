/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
internal class TooltipAreaTest {
    @get:Rule
    val rule = createComposeRule()

    // https://github.com/JetBrains/compose-jb/issues/2821
    @Test
    fun `simple tooltip is shown`(): Unit = runBlocking(Dispatchers.Main) {
        rule.setContent {
            TooltipArea(
                tooltip = {
                    Box {
                        BasicText(
                            text = "Tooltip",
                            modifier = Modifier.testTag("tooltipText")
                        )
                    }
                }
            ) {
                BasicText("Text", modifier = Modifier.size(50.dp).testTag("elementWithTooltip"))
            }
        }

        rule.onNodeWithTag("elementWithTooltip").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        rule.waitForIdle()

        rule.onNodeWithTag("tooltipText").assertExists()
    }
}