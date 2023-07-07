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

package androidx.compose.ui.draw

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.assertCenterPixelColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class InvalidatingNotPlacedChildTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childIsDisplayedWhenItWasNotPlacedOriginallyButPlacedLater() {
        val shouldPlace = mutableStateOf(false)
        composeTestRule.setContent {
            ConditionallyPlacedChild(
                shouldPlace,
                Modifier.background(Color.Blue)
                    .testTag("node")
            ) {
                Spacer(
                    Modifier.requiredSize(5.dp)
                        .graphicsLayer()
                        .background(Color.Red)
                )
            }
        }

        composeTestRule.onNodeWithTag("node")
            .captureToImage()
            .assertCenterPixelColor(Color.Blue)

        composeTestRule.runOnIdle {
            shouldPlace.value = true
        }

        composeTestRule.onNodeWithTag("node")
            .captureToImage()
            .assertCenterPixelColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun grandChildIsDisplayedWhenItWasNotPlacedOriginallyButPlacedLater() {
        val shouldPlace = mutableStateOf(false)
        composeTestRule.setContent {
            ConditionallyPlacedChild(
                shouldPlace,
                Modifier.background(Color.Blue)
                    .testTag("node")
            ) {
                MeasureInLayoutBlock {
                    Spacer(
                        Modifier.fillMaxSize()
                            .graphicsLayer()
                            .background(Color.Gray)
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("node")
            .captureToImage()
            .assertCenterPixelColor(Color.Blue)

        composeTestRule.runOnIdle {
            shouldPlace.value = true
        }

        composeTestRule.onNodeWithTag("node")
            .captureToImage()
            .assertCenterPixelColor(Color.Gray)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childIsNotDisplayedWhenIsNotPlacedAnymore() {
        val shouldPlace = mutableStateOf(true)
        composeTestRule.setContent {
            ConditionallyPlacedChild(
                shouldPlace,
                Modifier.background(Color.Blue)
                    .graphicsLayer()
                    .testTag("node")
            ) {
                Spacer(
                    Modifier.fillMaxSize()
                        .graphicsLayer()
                        .background(Color.Red)
                )
            }
        }

        composeTestRule.runOnIdle {
            shouldPlace.value = false
        }

        composeTestRule.onNodeWithTag("node")
            .captureToImage()
            .assertCenterPixelColor(Color.Blue)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childRedrawRequestedWhileNotPlaced() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                Spacer(
                    Modifier.fillMaxSize()
                        .drawBehind {
                            drawRect(color.value)
                        }
                )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childRedrawRequestedWhileNotPlaced_hadLayer() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                Spacer(
                    Modifier.fillMaxSize()
                        .graphicsLayer()
                        .drawBehind {
                            drawRect(color.value)
                        }
                )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childRedrawRequestedWhileNotPlaced_hadLayerAsLastModifierInTheChain() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                Box(Modifier.graphicsLayer()) {
                    Spacer(
                        Modifier.fillMaxSize()
                            .drawBehind {
                                drawRect(color.value)
                            }
                    )
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childRedrawRequestedWhileNotPlaced_placedWithLayer() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace, placeWithLayer = true) {
                Spacer(
                    Modifier.fillMaxSize()
                        .drawBehind {
                            drawRect(color.value)
                        }
                )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childRedrawAndRemeasureRequestedWhileNotPlaced() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                Spacer(
                    Modifier.fillMaxSize()
                        .layout(useDuringMeasure = color)
                        .drawBehind {
                            drawRect(color.value)
                        }
                )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childRedrawAndRelayoutRequestedWhileNotPlaced() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                Spacer(
                    Modifier.fillMaxSize()
                        .layout(useDuringLayout = color)
                        .drawBehind {
                            drawRect(color.value)
                        }
                )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun grandChildRedrawRequestedWhileNotPlaced() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                MeasureInLayoutBlock {
                    Spacer(
                        Modifier.fillMaxSize()
                            .drawBehind {
                                drawRect(color.value)
                            }
                    )
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun grandChildRedrawRequestedWhileNotPlaced_hadLayer() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                MeasureInLayoutBlock {
                    Spacer(
                        Modifier.fillMaxSize()
                            .graphicsLayer()
                            .drawBehind {
                                drawRect(color.value)
                            }
                    )
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun grandChildRedrawRequestedWhileNotPlaced_hadLayerAsLastModifierInTheChain() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                MeasureInLayoutBlock {
                    Box(Modifier.graphicsLayer()) {
                        Spacer(
                            Modifier.fillMaxSize()
                                .drawBehind {
                                    drawRect(color.value)
                                }
                        )
                    }
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun grandChildRedrawRequestedWhileNotPlaced_placedWithLayer() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                MeasureInLayoutBlock(placeWithLayer = true) {
                    Spacer(
                        Modifier.fillMaxSize()
                            .drawBehind {
                                drawRect(color.value)
                            }
                    )
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun grandChildRedrawAndRemeasureRequestedWhileNotPlaced() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                MeasureInLayoutBlock {
                    Spacer(
                        Modifier.fillMaxSize()
                            .layout(useDuringMeasure = color)
                            .drawBehind {
                                drawRect(color.value)
                            }
                    )
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun grandChildRedrawAndRelayoutRequestedWhileNotPlaced() {
        assertChangeWhileNotPlacedIsApplied { shouldPlace, color ->
            ConditionallyPlacedChild(shouldPlace) {
                MeasureInLayoutBlock {
                    Spacer(
                        Modifier.fillMaxSize()
                            .layout(useDuringLayout = color)
                            .drawBehind {
                                drawRect(color.value)
                            }
                    )
                }
            }
        }
    }

    // captureToImage() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    fun assertChangeWhileNotPlacedIsApplied(
        content: @Composable (State<Boolean>, State<Color>) -> Unit
    ) {
        val shouldPlace = mutableStateOf(true)
        var color = mutableStateOf(Color.Gray)
        composeTestRule.setContent {
            Box(
                Modifier.background(Color.Blue)
                    .testTag("node")
            ) {
                content(shouldPlace, color)
            }
        }

        composeTestRule.runOnIdle {
            shouldPlace.value = false
        }

        composeTestRule.runOnIdle {
            color.value = Color.Red
        }

        composeTestRule.runOnIdle {
            shouldPlace.value = true
        }

        composeTestRule.onNodeWithTag("node")
            .captureToImage()
            .assertCenterPixelColor(Color.Red)
    }
}

@Composable
private fun ConditionallyPlacedChild(
    shouldPlace: State<Boolean>,
    modifier: Modifier = Modifier,
    placeWithLayer: Boolean = false,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        layout(placeable.width, placeable.height) {
            if (shouldPlace.value) {
                if (placeWithLayer) {
                    placeable.placeWithLayer(0, 0)
                } else {
                    placeable.place(0, 0)
                }
            }
        }
    }
}

@Composable
private fun MeasureInLayoutBlock(
    modifier: Modifier = Modifier,
    placeWithLayer: Boolean = false,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val size = 5.dp.roundToPx()
        layout(size, size) {
            val placeable = measurables.first().measure(constraints)
            if (placeWithLayer) {
                placeable.placeWithLayer(0, 0)
            } else {
                placeable.place(0, 0)
            }
        }
    }
}

private fun Modifier.layout(
    useDuringMeasure: State<*>? = null,
    useDuringLayout: State<*>? = null,
): Modifier = layout { measurable, constraints ->
    useDuringMeasure?.value
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        useDuringLayout?.value
        placeable.place(0, 0)
    }
}