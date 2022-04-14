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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class ChipTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent {
            Box {
                Chip(modifier = Modifier.testTag(TestChipTag), onClick = {}) {
                    Text(TestChipTag)
                }
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun disabledSemantics() {
        rule.setMaterialContent {
            Box {
                Chip(
                    modifier = Modifier.testTag(TestChipTag),
                    onClick = {},
                    enabled = false
                ) {
                    Text(TestChipTag)
                }
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun onClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "Test chip"

        rule.setMaterialContent {
            Box {
                Chip(onClick = onClick, modifier = Modifier.testTag(TestChipTag)) {
                    Text(text)
                }
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .performClick()

        rule.runOnIdle {
            Truth.assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun canBeDisabled() {
        rule.setMaterialContent {
            var enabled by remember { mutableStateOf(true) }
            val onClick = { enabled = false }
            Box {
                Chip(
                    modifier = Modifier.testTag(TestChipTag),
                    onClick = onClick,
                    enabled = enabled
                ) {
                    Text("Hello")
                }
            }
        }
        rule.onNodeWithTag(TestChipTag)
            // Confirm the chip starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun heightIsFromSpec() {
        // This test can be reasonable failing on the non default font scales
        // so lets skip it.
        Assume.assumeTrue(rule.density.fontScale <= 1f)
        rule.setMaterialContent {
            Chip(onClick = {}) {
                Text("Test chip")
            }
        }

        rule.onNode(hasClickAction())
            .assertHeightIsEqualTo(ChipDefaults.MinHeight)
    }

    @Test
    fun withLargeFontSizeIsLargerThenMinHeight() {
        rule.setMaterialContent {
            Chip(onClick = {}) {
                Text(
                    text = "Test chip",
                    fontSize = 50.sp
                )
            }
        }

        rule.onNode(hasClickAction())
            .assertHeightIsAtLeast(ChipDefaults.MinHeight + 1.dp)
    }

    @Test
    fun propagateDefaultTextStyle() {
        var textStyle: TextStyle? = null
        var body2TextStyle: TextStyle? = null
        rule.setMaterialContent {
            Chip(onClick = {}) {
                textStyle = LocalTextStyle.current
                body2TextStyle = MaterialTheme.typography.body2
            }
        }

        rule.runOnIdle { Truth.assertThat(textStyle).isEqualTo(body2TextStyle) }
    }

    @Test
    fun horizontalPaddingIsFromSpec() {
        assertHorizontalPaddingIs(12.dp) { modifier, text ->
            Chip(onClick = {}, modifier = modifier, content = text)
        }
    }

    @Test
    fun contentColorIsCorrect() {
        var onSurface = Color.Unspecified
        var content = Color.Unspecified
        rule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            Chip(onClick = {}) {
                content = LocalContentColor.current
            }
        }

        rule.runOnIdle {
            Truth.assertThat(content).isEqualTo(onSurface)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun disabledBackgroundIsCorrect() {
        var surface = Color.Unspecified
        var onSurface = Color.Unspecified
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            onSurface = MaterialTheme.colors.onSurface
            Chip(
                modifier = Modifier.testTag(TestChipTag),
                onClick = {},
                enabled = false,
                shape = RectangleShape
            ) {}
        }

        rule.onNodeWithTag(TestChipTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = 0.dp,
                verticalPadding = 0.dp,
                backgroundColor = surface,
                shapeColor = onSurface.copy(0.38f * 0.12f * 0.87f)
                    .compositeOver(surface)
            )
    }

    @Test
    fun contentIsRow() {
        var chipBounds = Rect(0f, 0f, 0f, 0f)
        var item1Bounds = Rect(0f, 0f, 0f, 0f)
        var item2Bounds = Rect(0f, 0f, 0f, 0f)
        rule.setMaterialContent {
            Chip(
                onClick = {},
                modifier = Modifier.onGloballyPositioned {
                    chipBounds = it.boundsInRoot()
                }
            ) {
                Spacer(
                    Modifier.requiredSize(10.dp).onGloballyPositioned {
                        item1Bounds = it.boundsInRoot()
                    }
                )
                Spacer(
                    Modifier.requiredWidth(10.dp).requiredHeight(5.dp).onGloballyPositioned {
                        item2Bounds = it.boundsInRoot()
                    }
                )
            }
        }

        Truth.assertThat(item1Bounds.center.y).isWithin(1f).of(chipBounds.center.y)
        Truth.assertThat(item2Bounds.center.y).isWithin(1f).of(chipBounds.center.y)
        Truth.assertThat(item1Bounds.right).isWithin(1f).of(chipBounds.center.x)
        Truth.assertThat(item2Bounds.left).isWithin(1f).of(chipBounds.center.x)
    }

    @Test
    fun clickableInMinimumTouchTarget() {
        var clicked = false
        rule.setMaterialContent {
            Box(Modifier.fillMaxSize()) {
                Chip(
                    modifier = Modifier.align(Alignment.Center).testTag(TestChipTag)
                        .requiredSize(10.dp),
                    onClick = { clicked = !clicked }
                ) {
                    Box(Modifier.size(10.dp))
                }
            }
        }

        rule.onNodeWithTag(TestChipTag)
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .performTouchInput {
                click(Offset(-1f, -1f))
            }

        Truth.assertThat(clicked).isTrue()
    }

    private fun assertHorizontalPaddingIs(
        padding: Dp,
        chip: @Composable (Modifier, @Composable RowScope.() -> Unit) -> Unit
    ) {
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent {
            Box {
                chip(Modifier.onGloballyPositioned { parentCoordinates = it }) {
                    Text(
                        "Test chip",
                        Modifier.onGloballyPositioned { childCoordinates = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            val childBounds = childCoordinates!!.boundsInRoot()
            val parentBounds = childCoordinates!!.boundsInRoot()

            val topLeft =
                childCoordinates!!.localToWindow(Offset.Zero).x -
                    parentCoordinates!!.localToWindow(Offset.Zero).x
            val topRight = parentCoordinates!!.localToWindow(
                Offset(
                    parentBounds.right,
                    parentBounds.top
                )
            ).x - childCoordinates!!.localToWindow(Offset(childBounds.right, childBounds.top)).x

            val expectedPadding = with(rule.density) {
                padding.roundToPx().toFloat()
            }
            Truth.assertThat(expectedPadding).isEqualTo(topLeft)
            Truth.assertThat(expectedPadding).isEqualTo(-topRight)
        }
    }
}

private const val TestChipTag = "chip"
