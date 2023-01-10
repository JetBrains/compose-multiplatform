/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class TooltipTest {

    @get:Rule
    val rule = createComposeRule()

    private val tooltipState = TooltipState()

    @Test
    fun plainTooltip_noContent_size() {
        rule.setMaterialContent(lightColorScheme()) { TestTooltip() }

        rule.onNodeWithTag(ContainerTestTag)
            .assertHeightIsEqualTo(TooltipMinHeight)
            .assertWidthIsEqualTo(TooltipMinWidth)
    }

    @Test
    fun plainTooltip_customSize_size() {
        val customWidth = 100.dp
        val customHeight = 100.dp

        rule.setMaterialContent(lightColorScheme()) {
            TestTooltip(modifier = Modifier.size(customWidth, customHeight))
        }

        rule.onNodeWithTag(ContainerTestTag)
            .assertHeightIsEqualTo(customHeight)
            .assertWidthIsEqualTo(customWidth)
    }

    @Test
    fun plainTooltip_content_padding() {
        rule.setMaterialContent(lightColorScheme()) {
            TestTooltip(
                tooltipContent = {
                    Text(
                        text = "Test",
                        modifier = Modifier.testTag(TextTestTag)
                    )
                }
            )
        }

        rule.onNodeWithTag(TextTestTag)
            .assertLeftPositionInRootIsEqualTo(8.dp)
            .assertTopPositionInRootIsEqualTo(4.dp)
    }

    @FlakyTest(bugId = 264887805)
    @Test
    fun plainTooltip_behavior() {
        rule.setMaterialContent(lightColorScheme()) {
            PlainTooltipBox(
                tooltip = { Text(text = "Test", modifier = Modifier.testTag(TextTestTag)) },
                tooltipState = tooltipState,
                modifier = Modifier.testTag(ContainerTestTag)
            ) { Anchor() }
        }

        // Tooltip should initially be not visible
        assert(!tooltipState.isVisible)

        // Long press the icon and check that the tooltip is now showing
        rule.onNodeWithTag(AnchorTestTag)
            .performTouchInput { longClick() }

        assert(tooltipState.isVisible)

        // Tooltip should dismiss itself after 1.5s
        rule.waitUntil(TooltipDuration + 100L) { !tooltipState.isVisible }
    }

    @Composable
    fun TestTooltip(
        modifier: Modifier = Modifier,
        tooltipContent: @Composable () -> Unit = {}
    ) {
        val scope = rememberCoroutineScope()

        PlainTooltipBox(
            tooltip = tooltipContent,
            modifier = modifier.testTag(ContainerTestTag),
            tooltipState = tooltipState
        ) {}

        scope.launch { tooltipState.show() }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Anchor() {
        val scope = rememberCoroutineScope()

        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            modifier = Modifier
                .testTag(AnchorTestTag)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        scope.launch {
                            tooltipState.show()
                        }
                    }
                )
        )
    }
}

private const val AnchorTestTag = "Anchor"
private const val ContainerTestTag = "Container"
private const val TextTestTag = "Text"