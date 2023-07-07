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

package androidx.compose.ui.input

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickNotPlacedChildTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childIsDisplayedWhenItWasNotPlacedOriginallyButPlacedLater() {
        var firstClickedTimes = 0
        var secondClickedTimes = 0
        composeTestRule.setContent {
            Layout(
                content = {
                    Box(
                        Modifier.fillMaxSize().clickable {
                            firstClickedTimes++
                        }
                    )
                    Box(
                        Modifier.fillMaxSize().clickable {
                            secondClickedTimes++
                        }
                    )
                },
                modifier = Modifier.requiredSize(100.dp).testTag("parent")
            ) { measutables, constraints ->
                val first = measutables[0].measure(constraints)
                measutables[1].measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                }
            }
        }

        composeTestRule.onNodeWithTag("parent")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(0, secondClickedTimes)
            assertEquals(1, firstClickedTimes)
        }
    }
}
