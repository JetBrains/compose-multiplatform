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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.tokens.ExtendedFabPrimaryTokens
import androidx.compose.material3.tokens.FabPrimaryLargeTokens
import androidx.compose.material3.tokens.FabPrimarySmallTokens
import androidx.compose.material3.tokens.FabPrimaryTokens
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FloatingActionButtonTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun fabDefaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FloatingActionButton(modifier = Modifier.testTag("myButton"), onClick = {}) {
                    Icon(Icons.Filled.Favorite, null)
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .assertIsEnabled()
    }

    @Test
    fun extendedFabFindByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ExtendedFloatingActionButton(onClick = onClick, content = { Text(text) })
            }
        }

        rule.onNodeWithText(text)
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun fabHasSizeFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                FloatingActionButton(onClick = {}) {
                    Icon(
                        Icons.Filled.Favorite,
                        null,
                        modifier = Modifier.testTag("icon"))
                }
            }
            .assertIsSquareWithSize(FabPrimaryTokens.ContainerHeight)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(FabPrimaryTokens.IconSize)
            .assertWidthIsEqualTo(FabPrimaryTokens.IconSize)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smallFabHasSizeFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                    SmallFloatingActionButton(onClick = {}) {
                        Icon(
                            Icons.Filled.Favorite,
                            null,
                            modifier = Modifier.testTag("icon")
                        )
                    }
                }
            }
            // Expecting the size to be equal to the token size.
            .assertIsSquareWithSize(FabPrimarySmallTokens.ContainerHeight)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(FabPrimarySmallTokens.IconSize)
            .assertWidthIsEqualTo(FabPrimarySmallTokens.IconSize)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smallFabHasMinTouchTarget() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides true) {
                    SmallFloatingActionButton(onClick = {}) {
                        Icon(Icons.Filled.Favorite, null)
                    }
                }
            }
            // Expecting the size to be equal to the minimum touch target.
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun largeFabHasSizeFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                LargeFloatingActionButton(onClick = {}) {
                    Icon(
                        Icons.Filled.Favorite,
                        null,
                        modifier = Modifier
                            .size(FloatingActionButtonDefaults.LargeIconSize)
                            .testTag("icon")
                    )
                }
            }
            .assertIsSquareWithSize(FabPrimaryLargeTokens.ContainerHeight)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(FloatingActionButtonDefaults.LargeIconSize)
            .assertWidthIsEqualTo(FloatingActionButtonDefaults.LargeIconSize)
    }

    @Test
    fun extendedFabLongTextHasHeightFromSpec() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("FAB"),
                text = { Text("Extended FAB Text") },
                icon = { Icon(Icons.Filled.Favorite, null) },
                onClick = {}
            )
        }

        rule.onNodeWithTag("FAB")
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.ContainerHeight)
            .assertWidthIsAtLeast(FabPrimaryTokens.ContainerHeight)
    }

    @Test
    fun extendedFabShortTextHasMinimumSizeFromSpec() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("FAB"),
                onClick = {},
                content = { Text(".") },
            )
        }

        rule.onNodeWithTag("FAB")
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.ContainerHeight)
            .assertWidthIsEqualTo(80.dp)
    }

    @Test
    fun fabHasCorrectTextStyle() {
        var fontFamily: FontFamily? = null
        var fontWeight: FontWeight? = null
        var fontSize: TextUnit? = null
        var lineHeight: TextUnit? = null
        var letterSpacing: TextUnit? = null
        var expectedTextStyle: TextStyle? = null

        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Favorite, null)
                Text(
                    "Normal FAB with Text",
                    onTextLayout = {
                        fontFamily = it.layoutInput.style.fontFamily
                        fontWeight = it.layoutInput.style.fontWeight
                        fontSize = it.layoutInput.style.fontSize
                        lineHeight = it.layoutInput.style.lineHeight
                        letterSpacing = it.layoutInput.style.letterSpacing
                    }
                )
            }
            expectedTextStyle = MaterialTheme.typography.fromToken(
                ExtendedFabPrimaryTokens.LabelTextFont
            )
        }
        rule.runOnIdle {
            assertThat(fontFamily).isEqualTo(expectedTextStyle!!.fontFamily)
            assertThat(fontWeight).isEqualTo(expectedTextStyle!!.fontWeight)
            assertThat(fontSize).isEqualTo(expectedTextStyle!!.fontSize)
            assertThat(lineHeight).isEqualTo(expectedTextStyle!!.lineHeight)
            assertThat(letterSpacing).isEqualTo(expectedTextStyle!!.letterSpacing)
        }
    }

    @Test
    fun extendedFabHasCorrectTextStyle() {
        var fontFamily: FontFamily? = null
        var fontWeight: FontWeight? = null
        var fontSize: TextUnit? = null
        var lineHeight: TextUnit? = null
        var letterSpacing: TextUnit? = null
        var expectedTextStyle: TextStyle? = null

        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(onClick = {}) {
                Text(
                    "Extended FAB",
                    onTextLayout = {
                        fontFamily = it.layoutInput.style.fontFamily
                        fontWeight = it.layoutInput.style.fontWeight
                        fontSize = it.layoutInput.style.fontSize
                        lineHeight = it.layoutInput.style.lineHeight
                        letterSpacing = it.layoutInput.style.letterSpacing
                    }
                )
            }
            expectedTextStyle = MaterialTheme.typography.fromToken(
                ExtendedFabPrimaryTokens.LabelTextFont
            )
        }
        rule.runOnIdle {
            assertThat(fontFamily).isEqualTo(expectedTextStyle!!.fontFamily)
            assertThat(fontWeight).isEqualTo(expectedTextStyle!!.fontWeight)
            assertThat(fontSize).isEqualTo(expectedTextStyle!!.fontSize)
            assertThat(lineHeight).isEqualTo(expectedTextStyle!!.lineHeight)
            assertThat(letterSpacing).isEqualTo(expectedTextStyle!!.letterSpacing)
        }
    }

    @Test
    fun fabWeightModifier() {
        var item1Bounds = Rect(0f, 0f, 0f, 0f)
        var buttonBounds = Rect(0f, 0f, 0f, 0f)
        rule.setMaterialContent(lightColorScheme()) {
            Column {
                Spacer(
                    Modifier.requiredSize(10.dp).weight(1f).onGloballyPositioned {
                        item1Bounds = it.boundsInRoot()
                    }
                )

                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier.weight(1f)
                        .onGloballyPositioned {
                            buttonBounds = it.boundsInRoot()
                        }
                ) {
                    Text("Button")
                }

                Spacer(Modifier.requiredSize(10.dp).weight(1f))
            }
        }

        assertThat(item1Bounds.top).isNotEqualTo(0f)
        assertThat(buttonBounds.left).isEqualTo(0f)
    }

    @Test
    fun contentIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FloatingActionButton(
                    {},
                    Modifier.onGloballyPositioned {
                        buttonCoordinates = it
                    }
                ) {
                    Box(
                        Modifier.size(2.dp)
                            .onGloballyPositioned { contentCoordinates = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot()
            val contentBounds = contentCoordinates!!.boundsInRoot()
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(rule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.roundToPx().toFloat())
            }
            assertWithinOnePixel(buttonBounds.center, contentBounds.center)
        }
    }

    @Test
    fun extendedFabTextIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ExtendedFloatingActionButton(
                    onClick = {},
                    modifier = Modifier.onGloballyPositioned { buttonCoordinates = it },
                ) {
                    Box(
                        Modifier.size(2.dp)
                            .onGloballyPositioned { contentCoordinates = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot()
            val contentBounds = contentCoordinates!!.boundsInRoot()
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(rule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.roundToPx().toFloat())
            }
            assertWithinOnePixel(buttonBounds.center, contentBounds.center)
        }
    }

    @Test
    fun extendedFabTextAndIconArePositionedCorrectly() {
        var buttonCoordinates: LayoutCoordinates? = null
        var textCoordinates: LayoutCoordinates? = null
        var iconCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ExtendedFloatingActionButton(
                    text = {
                        Box(
                            Modifier.size(2.dp)
                                .onGloballyPositioned { textCoordinates = it }
                        )
                    },
                    icon = {
                        Box(
                            Modifier.size(10.dp)
                                .onGloballyPositioned { iconCoordinates = it }
                        )
                    },
                    onClick = {},
                    modifier = Modifier.onGloballyPositioned { buttonCoordinates = it }
                )
            }
        }

        rule.runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot()
            val textBounds = textCoordinates!!.boundsInRoot()
            val iconBounds = iconCoordinates!!.boundsInRoot()
            with(rule.density) {
                assertThat(textBounds.width).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(textBounds.height).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(iconBounds.width).isEqualTo(10.dp.roundToPx().toFloat())
                assertThat(iconBounds.height).isEqualTo(10.dp.roundToPx().toFloat())

                assertWithinOnePixel(buttonBounds.center.y, iconBounds.center.y)
                assertWithinOnePixel(buttonBounds.center.y, textBounds.center.y)
                val halfPadding = 6.dp.roundToPx().toFloat()
                assertWithinOnePixel(
                    iconBounds.center.x + iconBounds.width / 2 + halfPadding,
                    textBounds.center.x - textBounds.width / 2 - halfPadding
                )
            }
        }
    }

    @Test
    fun expandedExtendedFabTextAndIconHaveSizeFromSpecAndVisible() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                expanded = true,
                onClick = { },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        "Add",
                        modifier = Modifier.testTag("icon"),
                    )
                },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.IconSize)
            .assertWidthIsEqualTo(ExtendedFabPrimaryTokens.IconSize)

        rule.onNodeWithTag("FAB")
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.ContainerHeight)
            .assertWidthIsAtLeast(80.dp)

        rule.onNodeWithTag("text", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("icon", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun collapsedExtendedFabTextAndIconHaveSizeFromSpecAndTextNotVisible() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                expanded = false,
                onClick = { },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        "Add",
                        modifier = Modifier.testTag("icon")
                    )
                },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule.onNodeWithTag("FAB")
            .assertIsSquareWithSize(FabPrimaryTokens.ContainerHeight)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.IconSize)
            .assertWidthIsEqualTo(ExtendedFabPrimaryTokens.IconSize)

        rule.onNodeWithTag("text", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("icon", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun extendedFabAnimates() {
        rule.mainClock.autoAdvance = false

        var expanded by mutableStateOf(true)
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                expanded = expanded,
                onClick = {},
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        "Add",
                        modifier = Modifier.testTag("icon")
                    )
                },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule.onNodeWithTag("FAB")
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.ContainerHeight)
            .assertWidthIsAtLeast(80.dp)

        rule.runOnIdle { expanded = false }
        rule.mainClock.advanceTimeBy(200)

        rule.onNodeWithTag("FAB")
            .assertIsSquareWithSize(FabPrimaryTokens.ContainerHeight)
            .assertHeightIsEqualTo(FabPrimaryTokens.ContainerHeight)
            .assertWidthIsEqualTo(FabPrimaryTokens.ContainerWidth)
    }
}

fun assertWithinOnePixel(expected: Offset, actual: Offset) {
    assertWithinOnePixel(expected.x, actual.x)
    assertWithinOnePixel(expected.y, actual.y)
}

fun assertWithinOnePixel(expected: Float, actual: Float) {
    val diff = abs(expected - actual)
    assertThat(diff).isLessThan(1.1f)
}