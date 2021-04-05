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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
class OffsetTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        // b/151728444
        Assume.assumeFalse(
            Build.MODEL.contains("Nexus 5") && Build.VERSION.SDK_INT == Build.VERSION_CODES.M
        )
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun offset_positionIsModified() = with(rule.density) {
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
                        positionX = coordinates.positionInRoot().x.roundToInt()
                        positionY = coordinates.positionInRoot().y.roundToInt()
                    }
            ) {
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertEquals(offsetX.roundToPx(), positionX)
            assertEquals(offsetY.roundToPx(), positionY)
        }
    }

    @Test
    fun offset_positionIsModified_rtl() = with(rule.density) {
        val containerWidth = 30.dp
        val boxSize = 1
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0
        var positionY = 0
        rule.setContent {
            CompositionLocalProvider((LocalLayoutDirection provides LayoutDirection.Rtl)) {
                Box(
                    Modifier.testTag("box")
                        .wrapContentSize(Alignment.TopEnd)
                        .width(containerWidth)
                        .wrapContentSize(Alignment.TopStart)
                        .offset(offsetX, offsetY)
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            positionX = coordinates.positionInRoot().x.roundToInt()
                            positionY = coordinates.positionInRoot().y.roundToInt()
                        }
                ) {
                    // TODO(soboleva): this box should not be needed after b/154758475 is fixed.
                    Box(Modifier.requiredSize(boxSize.toDp()))
                }
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertEquals(containerWidth.roundToPx() - offsetX.roundToPx() - boxSize, positionX)
            assertEquals(offsetY.roundToPx(), positionY)
        }
    }

    @Test
    fun absoluteOffset_positionModified() = with(rule.density) {
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
                        positionX = coordinates.positionInRoot().x.roundToInt()
                        positionY = coordinates.positionInRoot().y.roundToInt()
                    }
            ) {
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertEquals(offsetX.roundToPx(), positionX)
            assertEquals(offsetY.roundToPx(), positionY)
        }
    }

    @Test
    fun absoluteOffset_positionModified_rtl() = with(rule.density) {
        val containerWidth = 30.dp
        val boxSize = 1
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0
        var positionY = 0
        rule.setContent {
            CompositionLocalProvider((LocalLayoutDirection provides LayoutDirection.Rtl)) {
                Box(
                    Modifier.testTag("box")
                        .wrapContentSize(Alignment.TopEnd)
                        .width(containerWidth)
                        .wrapContentSize(Alignment.TopStart)
                        .absoluteOffset(offsetX, offsetY)
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            positionX = coordinates.positionInRoot().x.roundToInt()
                            positionY = coordinates.positionInRoot().y.roundToInt()
                        }
                ) {
                    // TODO(soboleva): this box should not be needed after b/154758475 is fixed.
                    Box(Modifier.requiredSize(boxSize.toDp()))
                }
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            assertEquals(containerWidth.roundToPx() - boxSize + offsetX.roundToPx(), positionX)
            assertEquals(offsetY.roundToPx(), positionY)
        }
    }

    @Test
    fun offsetPx_positionIsModified() = with(rule.density) {
        val offsetX = 10f
        val offsetY = 20f
        var positionX = 0f
        var positionY = 0f
        rule.setContent {
            Box(
                Modifier.testTag("box")
                    .wrapContentSize(Alignment.TopStart)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.positionInRoot().x
                        positionY = coordinates.positionInRoot().y
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
    fun offsetPx_positionIsModified_rtl() = with(rule.density) {
        val containerWidth = 30.dp
        val boxSize = 1
        val offsetX = 10
        val offsetY = 20
        var positionX = 0
        var positionY = 0
        rule.setContent {
            CompositionLocalProvider((LocalLayoutDirection provides LayoutDirection.Rtl)) {
                Box(
                    Modifier.testTag("box")
                        .wrapContentSize(Alignment.TopEnd)
                        .width(containerWidth)
                        .wrapContentSize(Alignment.TopStart)
                        .offset { IntOffset(offsetX, offsetY) }
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            positionX = coordinates.positionInRoot().x.roundToInt()
                            positionY = coordinates.positionInRoot().y.roundToInt()
                        }
                ) {
                    // TODO(soboleva): this box should not be needed after b/154758475 is fixed.
                    Box(Modifier.requiredSize(boxSize.toDp()))
                }
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            Assert.assertEquals(
                containerWidth.roundToPx() - offsetX - boxSize,
                positionX
            )
            Assert.assertEquals(offsetY, positionY)
        }
    }

    @Test
    fun absoluteOffsetPx_positionIsModified() = with(rule.density) {
        val offsetX = 10
        val offsetY = 20
        var positionX = 0
        var positionY = 0
        rule.setContent {
            Box(
                Modifier.testTag("box")
                    .wrapContentSize(Alignment.TopStart)
                    .absoluteOffset { IntOffset(offsetX, offsetY) }
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.positionInRoot().x.roundToInt()
                        positionY = coordinates.positionInRoot().y.roundToInt()
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
    fun absoluteOffsetPx_positionIsModified_rtl() = with(rule.density) {
        val containerWidth = 30.dp
        val boxSize = 1
        val offsetX = 10
        val offsetY = 20
        var positionX = 0
        var positionY = 0
        rule.setContent {
            CompositionLocalProvider((LocalLayoutDirection provides LayoutDirection.Rtl)) {
                Box(
                    Modifier.testTag("box")
                        .wrapContentSize(Alignment.TopEnd)
                        .width(containerWidth)
                        .wrapContentSize(Alignment.TopStart)
                        .absoluteOffset { IntOffset(offsetX, offsetY) }
                        .onGloballyPositioned { coordinates: LayoutCoordinates ->
                            positionX = coordinates.positionInRoot().x.roundToInt()
                            positionY = coordinates.positionInRoot().y.roundToInt()
                        }
                ) {
                    // TODO(soboleva): this box should not be needed after b/154758475 is fixed.
                    Box(Modifier.requiredSize(boxSize.toDp()))
                }
            }
        }

        rule.onNodeWithTag("box").assertExists()
        rule.runOnIdle {
            Assert.assertEquals(
                containerWidth.roundToPx() - boxSize + offsetX,
                positionX
            )
            Assert.assertEquals(offsetY, positionY)
        }
    }

    @Test
    fun testOffsetInspectableValue() {
        val modifier = Modifier.offset(3.0.dp, 4.5.dp) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("offset")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("x", 3.0.dp),
            ValueElement("y", 4.5.dp)
        )
    }

    @Test
    fun testAbsoluteOffsetInspectableValue() {
        val modifier = Modifier.absoluteOffset(3.0.dp, 1.5.dp) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("absoluteOffset")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("x", 3.0.dp),
            ValueElement("y", 1.5.dp)
        )
    }

    @Test
    fun testOffsetPxInspectableValue() {
        val modifier = Modifier.offset { IntOffset(10, 20) } as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("offset")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.map { it.name }.asIterable())
            .containsExactly("offset")
    }

    @Test
    fun testAbsoluteOffsetPxInspectableValue() {
        val modifier = Modifier.absoluteOffset { IntOffset(10, 20) } as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("absoluteOffset")
        assertThat(modifier.valueOverride).isNull()
        assertThat(modifier.inspectableElements.map { it.name }.asIterable())
            .containsExactly("offset")
    }

    @Test
    fun contentNotRedrawnWhenOffsetPxChanges() {
        var contentRedrawsCount = 0
        var offset by mutableStateOf(0f)
        rule.setContent {
            Box(
                Modifier
                    .requiredSize(10.dp)
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .drawBehind {
                        contentRedrawsCount ++
                    }
            )
        }

        rule.runOnIdle {
            offset = 5f
        }

        rule.runOnIdle {
            assertEquals(1, contentRedrawsCount)
        }
    }
}
