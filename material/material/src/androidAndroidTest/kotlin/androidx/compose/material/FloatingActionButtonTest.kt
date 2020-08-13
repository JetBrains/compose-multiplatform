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

package androidx.compose.material

import android.os.Build
import androidx.compose.runtime.Providers
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Box
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.geometry.Rect
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertShape
import androidx.ui.test.assertWidthIsAtLeast
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.performClick
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithText
import androidx.ui.test.runOnIdle
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class FloatingActionButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fabDefaultSemantics() {
        composeTestRule.setMaterialContent {
            Stack {
                FloatingActionButton(modifier = Modifier.testTag("myButton"), onClick = {}) {
                    Icon(Icons.Filled.Favorite)
                }
            }
        }

        onNodeWithTag("myButton")
            .assertIsEnabled()
    }

    @Test
    fun extendedFab_findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        composeTestRule.setMaterialContent {
            Stack {
                ExtendedFloatingActionButton(text = { Text(text) }, onClick = onClick)
            }
        }

        onNodeWithText(text)
            .performClick()

        runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun defaultFabHasSizeFromSpec() {
        composeTestRule
            .setMaterialContentForSizeAssertions {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Filled.Favorite)
                }
            }
            .assertIsSquareWithSize(56.dp)
    }

    @Test
    fun extendedFab_longText_HasHeightFromSpec() {
        composeTestRule.setMaterialContent {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("FAB"),
                text = { Text("Extended FAB Text") },
                icon = { Icon(Icons.Filled.Favorite) },
                onClick = {}
            )
        }

        onNodeWithTag("FAB")
            .assertHeightIsEqualTo(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun extendedFab_shortText_HasMinimumSizeFromSpec() {
        composeTestRule.setMaterialContent {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("FAB"),
                text = { Text(".") },
                onClick = {}
            )
        }

        onNodeWithTag("FAB")
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun fab_weightModifier() {
        var item1Bounds = Rect(0f, 0f, 0f, 0f)
        var buttonBounds = Rect(0f, 0f, 0f, 0f)
        composeTestRule.setMaterialContent {
            Column {
                Spacer(Modifier.size(10.dp).weight(1f).onPositioned {
                    item1Bounds = it.boundsInRoot
                })

                FloatingActionButton(onClick = {}, modifier = Modifier.weight(1f).onPositioned {
                    buttonBounds = it.boundsInRoot
                }) {
                    Text("Button")
                }

                Spacer(Modifier.size(10.dp).weight(1f))
            }
        }

        assertThat(item1Bounds.top).isNotEqualTo(0f)
        assertThat(buttonBounds.left).isEqualTo(0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun fab_shapeAndColorFromThemeIsUsed() {
        val themeShape = CutCornerShape(4.dp)
        val realShape = CutCornerShape(50)
        var surface = Color.Transparent
        var fabColor = Color.Transparent
        composeTestRule.setMaterialContent {
            Stack {
                surface = MaterialTheme.colors.surface
                fabColor = MaterialTheme.colors.secondary
                Providers(ShapesAmbient provides Shapes(small = themeShape)) {
                    FloatingActionButton(
                        modifier = Modifier.testTag("myButton"),
                        onClick = {},
                        elevation = 0.dp
                    ) {
                        Box(Modifier.preferredSize(10.dp, 10.dp))
                    }
                }
            }
        }

        onNodeWithTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = realShape,
                shapeColor = fabColor,
                backgroundColor = surface,
                shapeOverlapPixelCount = with(composeTestRule.density) { 1.dp.toPx() }
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun extendedFab_shapeAndColorFromThemeIsUsed() {
        val themeShape = CutCornerShape(4.dp)
        val realShape = CutCornerShape(50)
        var surface = Color.Transparent
        var fabColor = Color.Transparent
        composeTestRule.setMaterialContent {
            Stack {
                surface = MaterialTheme.colors.surface
                fabColor = MaterialTheme.colors.secondary
                Providers(ShapesAmbient provides Shapes(small = themeShape)) {
                    ExtendedFloatingActionButton(
                        modifier = Modifier.testTag("myButton"),
                        onClick = {},
                        elevation = 0.dp,
                        text = { Box(Modifier.preferredSize(10.dp, 50.dp)) }
                    )
                }
            }
        }

        onNodeWithTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = realShape,
                shapeColor = fabColor,
                backgroundColor = surface,
                shapeOverlapPixelCount = with(composeTestRule.density) { 1.dp.toPx() }
            )
    }

    @Test
    fun contentIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack {
                FloatingActionButton({}, Modifier.onPositioned { buttonCoordinates = it }) {
                    Box(Modifier.preferredSize(2.dp)
                        .onPositioned { contentCoordinates = it }
                    )
                }
            }
        }

        runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot
            val contentBounds = contentCoordinates!!.boundsInRoot
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(composeTestRule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.toIntPx().toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.toIntPx().toFloat())
            }
            assertWithinOnePixel(buttonBounds.center, contentBounds.center)
        }
    }

    @Test
    fun extendedFabTextIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack {
                ExtendedFloatingActionButton(
                    text = {
                        Box(Modifier.preferredSize(2.dp)
                            .onPositioned { contentCoordinates = it }
                        )
                    },
                    onClick = {},
                    modifier = Modifier.onPositioned { buttonCoordinates = it }
                )
            }
        }

        runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot
            val contentBounds = contentCoordinates!!.boundsInRoot
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(composeTestRule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.toIntPx().toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.toIntPx().toFloat())
            }
            assertWithinOnePixel(buttonBounds.center, contentBounds.center)
        }
    }

    @Test
    fun extendedFabTextAndIconArePositionedCorrectly() {
        var buttonCoordinates: LayoutCoordinates? = null
        var textCoordinates: LayoutCoordinates? = null
        var iconCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack {
                ExtendedFloatingActionButton(
                    text = {
                        Box(Modifier.preferredSize(2.dp)
                            .onPositioned { textCoordinates = it }
                        )
                    },
                    icon = {
                        Box(Modifier.preferredSize(10.dp)
                            .onPositioned { iconCoordinates = it }
                        )
                    },
                    onClick = {},
                    modifier = Modifier.onPositioned { buttonCoordinates = it }
                )
            }
        }

        runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot
            val textBounds = textCoordinates!!.boundsInRoot
            val iconBounds = iconCoordinates!!.boundsInRoot
            with(composeTestRule.density) {
                assertThat(textBounds.width).isEqualTo(2.dp.toIntPx().toFloat())
                assertThat(textBounds.height).isEqualTo(2.dp.toIntPx().toFloat())
                assertThat(iconBounds.width).isEqualTo(10.dp.toIntPx().toFloat())
                assertThat(iconBounds.height).isEqualTo(10.dp.toIntPx().toFloat())

                assertWithinOnePixel(buttonBounds.center.y, iconBounds.center.y)
                assertWithinOnePixel(buttonBounds.center.y, textBounds.center.y)
                val halfPadding = 6.dp.toIntPx().toFloat()
                assertWithinOnePixel(
                    iconBounds.center.x + iconBounds.width / 2 + halfPadding,
                    textBounds.center.x - textBounds.width / 2 - halfPadding
                )
            }
        }
    }
}
