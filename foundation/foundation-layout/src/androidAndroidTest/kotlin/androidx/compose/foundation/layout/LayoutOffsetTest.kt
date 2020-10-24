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

package androidx.compose.foundation.layout

import android.os.Build
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.roundToInt

@SmallTest
@RunWith(AndroidJUnit4::class)
class LayoutOffsetTest : LayoutTest() {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        // b/151728444
        Assume.assumeFalse(
            Build.MODEL.contains("Nexus 5") && Build.VERSION.SDK_INT == Build.VERSION_CODES.M
        )
    }

    @Test
    fun offset_positionIsModified() = with(density) {
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0
        var positionY = 0
        rule.setContent {
            Box(
                Modifier.testTag("box")
                    .wrapContentSize(Alignment.TopStart)
                    .offset(offsetX, offsetY)
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.positionInRoot.x.roundToInt()
                        positionY = coordinates.positionInRoot.y.roundToInt()
                    }
            ) {
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertEquals(offsetX.toIntPx(), positionX)
            assertEquals(offsetY.toIntPx(), positionY)
        }
    }

    @Test
    fun offset_positionIsModified_rtl() = with(density) {
        val containerWidth = 30.dp
        val boxSize = 1
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0
        var positionY = 0
        rule.setContent {
            Providers((LayoutDirectionAmbient provides LayoutDirection.Rtl)) {
                Box(
                    Modifier.testTag("box")
                        .wrapContentSize(Alignment.TopEnd)
                        .preferredWidth(containerWidth)
                        .wrapContentSize(Alignment.TopStart)
                        .offset(offsetX, offsetY)
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            positionX = coordinates.positionInRoot.x.roundToInt()
                            positionY = coordinates.positionInRoot.y.roundToInt()
                        }
                ) {
                    // TODO(soboleva): this box should not be needed after b/154758475 is fixed.
                    Box(Modifier.size(boxSize.toDp()))
                }
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertEquals(containerWidth.toIntPx() - offsetX.toIntPx() - boxSize, positionX)
            assertEquals(offsetY.toIntPx(), positionY)
        }
    }

    @Test
    fun absoluteOffset_positionModified() = with(density) {
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0
        var positionY = 0
        rule.setContent {
            Box(
                Modifier.testTag("box")
                    .wrapContentSize(Alignment.TopStart)
                    .absoluteOffset(offsetX, offsetY)
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.positionInRoot.x.roundToInt()
                        positionY = coordinates.positionInRoot.y.roundToInt()
                    }
            ) {
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertEquals(offsetX.toIntPx(), positionX)
            assertEquals(offsetY.toIntPx(), positionY)
        }
    }

    @Test
    fun absoluteOffset_positionModified_rtl() = with(density) {
        val containerWidth = 30.dp
        val boxSize = 1
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0
        var positionY = 0
        rule.setContent {
            Providers((LayoutDirectionAmbient provides LayoutDirection.Rtl)) {
                Box(
                    Modifier.testTag("box")
                        .wrapContentSize(Alignment.TopEnd)
                        .preferredWidth(containerWidth)
                        .wrapContentSize(Alignment.TopStart)
                        .absoluteOffset(offsetX, offsetY)
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            positionX = coordinates.positionInRoot.x.roundToInt()
                            positionY = coordinates.positionInRoot.y.roundToInt()
                        }
                ) {
                    // TODO(soboleva): this box should not be needed after b/154758475 is fixed.
                    Box(Modifier.size(boxSize.toDp()))
                }
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertEquals(containerWidth.toIntPx() - boxSize + offsetX.toIntPx(), positionX)
            assertEquals(offsetY.toIntPx(), positionY)
        }
    }

    @Test
    fun offsetPx_positionIsModified() = with(density) {
        val offsetX = 10f
        val offsetY = 20f
        var positionX = 0f
        var positionY = 0f
        rule.setContent {
            Box(
                Modifier.testTag("box")
                    .wrapContentSize(Alignment.TopStart)
                    .offsetPx(
                        remember { mutableStateOf(offsetX) },
                        remember { mutableStateOf(offsetY) }
                    )
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.positionInRoot.x
                        positionY = coordinates.positionInRoot.y
                    }
            ) {
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            Assert.assertEquals(offsetX, positionX)
            Assert.assertEquals(offsetY, positionY)
        }
    }

    @Test
    fun offsetPx_positionIsModified_rtl() = with(density) {
        val containerWidth = 30.dp
        val boxSize = 1f
        val offsetX = 10f
        val offsetY = 20f
        var positionX = 0f
        var positionY = 0f
        rule.setContent {
            Providers((LayoutDirectionAmbient provides LayoutDirection.Rtl)) {
                Box(
                    Modifier.testTag("box")
                        .wrapContentSize(Alignment.TopEnd)
                        .preferredWidth(containerWidth)
                        .wrapContentSize(Alignment.TopStart)
                        .offsetPx(
                            remember { mutableStateOf(offsetX) },
                            remember { mutableStateOf(offsetY) }
                        )
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            positionX = coordinates.positionInRoot.x
                            positionY = coordinates.positionInRoot.y
                        }
                ) {
                    // TODO(soboleva): this box should not be needed after b/154758475 is fixed.
                    Box(Modifier.size(boxSize.toDp()))
                }
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            Assert.assertEquals(
                containerWidth.toIntPx() - offsetX.roundToInt() - boxSize,
                positionX
            )
            Assert.assertEquals(offsetY, positionY)
        }
    }

    @Test
    fun absoluteOffsetPx_positionIsModified() = with(density) {
        val offsetX = 10f
        val offsetY = 20f
        var positionX = 0f
        var positionY = 0f
        rule.setContent {
            Box(
                Modifier.testTag("box")
                    .wrapContentSize(Alignment.TopStart)
                    .absoluteOffsetPx(
                        remember { mutableStateOf(offsetX) },
                        remember { mutableStateOf(offsetY) }
                    )
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.positionInRoot.x
                        positionY = coordinates.positionInRoot.y
                    }
            ) {
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            Assert.assertEquals(offsetX, positionX)
            Assert.assertEquals(offsetY, positionY)
        }
    }

    @Test
    fun absoluteOffsetPx_positionIsModified_rtl() = with(density) {
        val containerWidth = 30.dp
        val boxSize = 1f
        val offsetX = 10f
        val offsetY = 20f
        var positionX = 0f
        var positionY = 0f
        rule.setContent {
            Providers((LayoutDirectionAmbient provides LayoutDirection.Rtl)) {
                Box(
                    Modifier.testTag("box")
                        .wrapContentSize(Alignment.TopEnd)
                        .preferredWidth(containerWidth)
                        .wrapContentSize(Alignment.TopStart)
                        .absoluteOffsetPx(
                            remember { mutableStateOf(offsetX) },
                            remember { mutableStateOf(offsetY) }
                        )
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            positionX = coordinates.positionInRoot.x
                            positionY = coordinates.positionInRoot.y
                        }
                ) {
                    // TODO(soboleva): this box should not be needed after b/154758475 is fixed.
                    Box(Modifier.size(boxSize.toDp()))
                }
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            Assert.assertEquals(
                containerWidth.toIntPx() - boxSize + offsetX.roundToInt(),
                positionX
            )
            Assert.assertEquals(offsetY, positionY)
        }
    }
}
