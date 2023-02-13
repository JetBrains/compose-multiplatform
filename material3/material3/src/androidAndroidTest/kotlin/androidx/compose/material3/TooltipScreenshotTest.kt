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

package androidx.compose.material3

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
class TooltipScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun plainTooltip_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) { PlainTooltipTest() }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(AnchorTestTag)
            .performTouchInput { longClick() }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        assertAgainstGolden("plainTooltip_lightTheme")
    }

    @Test
    fun plainTooltip_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) { PlainTooltipTest() }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(AnchorTestTag)
            .performTouchInput { longClick() }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        assertAgainstGolden("plainTooltip_darkTheme")
    }

    @Test
    fun richTooltip_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) { RichTooltipTest() }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(AnchorTestTag)
            .performTouchInput { longClick() }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        assertAgainstGolden("richTooltip_lightTheme")
    }

    @Test
    fun richTooltip_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) { RichTooltipTest() }

        // Stop auto advance for test consistency
        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(AnchorTestTag)
            .performTouchInput { longClick() }

        // Advance by the fade in time
        rule.mainClock.advanceTimeBy(TooltipFadeInDuration.toLong())

        rule.waitForIdle()
        assertAgainstGolden("richTooltip_darkTheme")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(TooltipTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    @Composable
    private fun PlainTooltipTest() {
        val tooltipState = remember { PlainTooltipState() }
        PlainTooltipBox(
            tooltip = { Text("Tooltip Text") },
            modifier = Modifier.testTag(TooltipTestTag),
            tooltipState = tooltipState
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier
                    .testTag(AnchorTestTag)
                    .tooltipAnchor()
            )
        }
    }

    @Composable
    private fun RichTooltipTest() {
        val tooltipState = remember { RichTooltipState() }
        RichTooltipBox(
            title = { Text("Title") },
            text = {
                Text(
                    "Area for supportive text, providing a descriptive " +
                        "message for the composable that the tooltip is anchored to."
                )
            },
            action = { Text("Action Text") },
            tooltipState = tooltipState,
            modifier = Modifier.testTag(TooltipTestTag)
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier
                    .testTag(AnchorTestTag)
                    .tooltipAnchor()
            )
        }
    }
}

private const val AnchorTestTag = "Anchor"
private const val TooltipTestTag = "tooltip"