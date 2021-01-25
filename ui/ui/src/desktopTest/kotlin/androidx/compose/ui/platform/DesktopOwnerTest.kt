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

package androidx.compose.ui.platform

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalComposeApi::class,
    ExperimentalCoroutinesApi::class
)
class DesktopOwnerTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("ui/ui-desktop/ui")

    @Test(timeout = 5000)
    fun `rendering of Box state change`() = renderingTest(width = 40, height = 40) {
        var size by mutableStateOf(20.dp)
        setContent {
            Box(Modifier.size(size).background(Color.Blue))
        }
        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial_size_20")
        assertFalse(hasRenders())

        size = 10.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_change_size_to_10")
        assertFalse(hasRenders())

        size = 5.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_change_size_to_5")
        assertFalse(hasRenders())

        size = 10.dp
        size = 20.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_change_size_to_10_and_20")
        assertFalse(hasRenders())

        size = 20.dp
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of Canvas state change`() = renderingTest(width = 40, height = 40) {
        var x by mutableStateOf(0f)
        var clipToBounds by mutableStateOf(false)
        setContent {
            val modifier = if (clipToBounds) {
                Modifier.size(20.dp).clipToBounds()
            } else {
                Modifier.size(20.dp)
            }
            Canvas(modifier) {
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(x, 0f),
                    size = Size(10f, 10f)
                )
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        x = 15f
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_translate")
        assertFalse(hasRenders())

        clipToBounds = true
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_clipToBounds")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of Layout state change`() = renderingTest(width = 40, height = 40) {
        var width by mutableStateOf(10)
        var height by mutableStateOf(20)
        var x by mutableStateOf(0)
        setContent {
            Row(Modifier.height(height.dp)) {
                Layout({
                    Box(Modifier.fillMaxSize().background(Color.Green))
                }) { measureables, constraints ->
                    val placeables = measureables.map { it.measure(constraints) }
                    layout(width, constraints.maxHeight) {
                        placeables.forEach { it.place(x, 0) }
                    }
                }

                Box(Modifier.background(Color.Red).size(10.dp))
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        width = 20
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_change_width")
        assertFalse(hasRenders())

        x = 10
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_change_x")
        assertFalse(hasRenders())

        height = 10
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_change_height")
        assertFalse(hasRenders())

        width = 10
        height = 20
        x = 0
        awaitNextRender()
        screenshotRule.snap(surface, "frame5_change_all")
        assertFalse(hasRenders())

        width = 10
        height = 20
        x = 0
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of layer offset`() = renderingTest(width = 40, height = 40) {
        var translationX by mutableStateOf(10f)
        var offsetX by mutableStateOf(10.dp)
        setContent {
            Box(Modifier.offset(x = offsetX).graphicsLayer(translationX = translationX)) {
                Box(Modifier.background(Color.Green).size(10.dp))
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        offsetX -= 10.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_offset")
        assertFalse(hasRenders())

        translationX -= 10f
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_translation")
        assertFalse(hasRenders())

        offsetX += 10.dp
        translationX += 10f
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_offset_and_translation")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    @Ignore("enable after we fix https://github.com/JetBrains/compose-jb/issues/137")
    fun `rendering of transition`() = renderingTest(width = 40, height = 40) {
        var targetValue by mutableStateOf(10f)

        setContent {
            val value by animateFloatAsState(
                targetValue,
                animationSpec = TweenSpec(durationMillis = 30, easing = LinearEasing)
            )
            Box(Modifier.size(value.dp).background(Color.Blue))
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")

        currentTimeMillis = 20
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_20ms")

        currentTimeMillis = 30
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_30ms")
        assertFalse(hasRenders())

        targetValue = 40f
        currentTimeMillis = 30
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_30ms_target40")

        currentTimeMillis = 40
        awaitNextRender()
        screenshotRule.snap(surface, "frame5_40ms_target40")

        currentTimeMillis = 50
        awaitNextRender()
        screenshotRule.snap(surface, "frame6_50ms_target40")

        currentTimeMillis = 60
        awaitNextRender()
        screenshotRule.snap(surface, "frame7_60ms_target40")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of clickable`() = renderingTest(width = 40, height = 40) {
        setContent {
            Box(Modifier.size(20.dp).background(Color.Blue).clickable {})
        }
        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        owners.onMousePressed(2, 2)
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_onMousePressed")
        assertFalse(hasRenders())

        owners.onMouseMoved(1, 1)
        assertFalse(hasRenders())

        owners.onMouseReleased(1, 1)
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_onMouseReleased")

        owners.onMouseMoved(1, 1)
        owners.onMousePressed(3, 3)
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_onMouseMoved_onMousePressed")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of LazyColumn`() = renderingTest(
        width = 40,
        height = 40,
        platform = DesktopPlatform.Windows // scrolling behave differently on different platforms
    ) {
        var height by mutableStateOf(10.dp)
        setContent {
            Box(Modifier.padding(10.dp)) {
                LazyColumn {
                    items(
                        listOf(Color.Red, Color.Green, Color.Blue, Color.Black, Color.Gray)
                    ) { color ->
                        Box(Modifier.size(width = 30.dp, height = height).background(color))
                    }
                }
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        owners.onMouseScroll(
            10,
            10,
            MouseScrollEvent(MouseScrollUnit.Page(1f), Orientation.Vertical)
        )
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_onMouseScroll")
        assertFalse(hasRenders())

        owners.onMouseScroll(
            10,
            10,
            MouseScrollEvent(MouseScrollUnit.Page(10f), Orientation.Vertical)
        )
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_onMouseScroll")
        assertFalse(hasRenders())

        height = 5.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_change_height")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    @Ignore("enable after we fix https://github.com/JetBrains/compose-jb/issues/137")
    fun `rendering, change state before first onRender`() = renderingTest(
        width = 40,
        height = 40
    ) {
        var size by mutableStateOf(20.dp)
        setContent {
            Box(Modifier.size(size).background(Color.Blue))
        }

        size = 10.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())
    }
}