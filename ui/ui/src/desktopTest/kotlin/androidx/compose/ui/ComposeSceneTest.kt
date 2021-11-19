/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.keyEvent
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.platform.renderingTest
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.awt.event.KeyEvent

@OptIn(InternalTestApi::class, ExperimentalComposeUiApi::class)
class ComposeSceneTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("compose/ui/ui-desktop")

    @get:Rule
    val composeRule = createComposeRule()

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
    fun `rendering of transition`() = renderingTest(width = 40, height = 40) {
        val startValue = 10f
        var targetValue by mutableStateOf(startValue)
        var lastComposedValue = Float.MIN_VALUE

        setContent {
            val value by animateFloatAsState(
                targetValue,
                animationSpec = TweenSpec(durationMillis = 30, easing = LinearEasing)
            )
            Box(Modifier.size(value.dp).background(Color.Blue))
            lastComposedValue = value
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")

        targetValue = 40f
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_target40_0ms")

        // animation can start not immediately, but on the second/third frame
        // so wait when the animation will change the animating value
        while (lastComposedValue == startValue) {
            currentTimeMillis += 10
            awaitNextRender()
        }

        screenshotRule.snap(surface, "frame3_target40_10ms")

        currentTimeMillis += 10
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_target40_20ms")

        currentTimeMillis += 10
        awaitNextRender()
        screenshotRule.snap(surface, "frame5_target40_30ms")

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

        scene.sendPointerEvent(PointerEventType.Enter, Offset(2f, 2f))
        scene.sendPointerEvent(PointerEventType.Move, Offset(2f, 2f))
        // TODO(demin): why we need extra frame when we send hover + press?
        //  maybe a race between hoverable and clickable?
        awaitNextRender()

        scene.sendPointerEvent(PointerEventType.Press, Offset(2f, 2f))
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_onMousePressed")
        assertFalse(hasRenders())

        scene.sendPointerEvent(PointerEventType.Move, Offset(1f, 1f))
        assertFalse(hasRenders())

        scene.sendPointerEvent(PointerEventType.Release, Offset(1f, 1f))

        // TODO(demin): why we need extra frame when we send hover + press?
        //  maybe a race between hoverable and clickable?
        awaitNextRender()

        scene.sendPointerEvent(PointerEventType.Move, Offset(-1f, -1f))
        scene.sendPointerEvent(PointerEventType.Exit, Offset(-1f, -1f))
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_onMouseReleased")

        scene.sendPointerEvent(PointerEventType.Enter, Offset(1f, 1f))
        scene.sendPointerEvent(PointerEventType.Move, Offset(1f, 1f))

        // TODO(demin): why we need extra frame when we send hover + press?
        //  maybe a race between hoverable and clickable?
        awaitNextRender()

        scene.sendPointerEvent(PointerEventType.Press, Offset(3f, 3f))
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_onMouseMoved_onMousePressed")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `rendering of LazyColumn`() = renderingTest(
        width = 40,
        height = 40
    ) {
        var itemHeight by mutableStateOf(10.dp)
        val padding = 10
        val columnHeight = this.height - padding * 2
        val state = LazyListState()
        setContent {
            Box(Modifier.padding(padding.dp)) {
                LazyColumn(state = state) {
                    items(
                        listOf(Color.Red, Color.Green, Color.Blue, Color.Black, Color.Gray)
                    ) { color ->
                        Box(Modifier.size(width = 30.dp, height = itemHeight).background(color))
                    }
                }
            }
        }

        awaitNextRender()
        screenshotRule.snap(surface, "frame1_initial")
        assertFalse(hasRenders())

        state.scroll {
            scrollBy(columnHeight.toFloat())
        }
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_onMouseScroll")
        assertFalse(hasRenders())

        state.scroll {
            scrollBy(10 * columnHeight.toFloat())
        }
        awaitNextRender()
        screenshotRule.snap(surface, "frame3_onMouseScroll")
        assertFalse(hasRenders())

        itemHeight = 5.dp
        awaitNextRender()
        screenshotRule.snap(surface, "frame4_change_height")
        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
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

    @Test(timeout = 5000)
    fun `launch effect`() = renderingTest(width = 40, height = 40) {
        var effectIsLaunched = false

        setContent {
            LaunchedEffect(Unit) {
                effectIsLaunched = true
            }
        }

        awaitNextRender()
        assertThat(effectIsLaunched).isTrue()
    }

    @Test(timeout = 5000)
    fun `change density`() = renderingTest(width = 40, height = 40) {
        @Composable
        fun redRect() {
            Box(Modifier.size(4.dp).background(Color.Red))
        }

        @Composable
        fun greenRectOnCanvas() {
            Canvas(Modifier.size(100.dp)) {
                drawRect(
                    Color.Green,
                    topLeft = Offset(4f * density, 4f * density),
                    size = Size(4f * density, 4f * density)
                )
            }
        }

        @Composable
        fun blueRectInRoundedLayer() {
            Box(
                Modifier
                    .offset(8.dp, 8.dp)
                    .graphicsLayer(shape = RoundedCornerShape(2.dp), clip = true)
            ) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(Color.Blue)
                )
            }
        }

        @Composable
        fun elevation() {
            Box(
                Modifier
                    .offset(8.dp, 0.dp)
            ) {
                Surface(
                    modifier = Modifier.size(4.dp),
                    elevation = 2.dp
                ) {
                }
            }
        }

        setContent {
            redRect()
            greenRectOnCanvas()
            blueRectInRoundedLayer()
            elevation()
        }

        density = 2f
        awaitNextRender()
        screenshotRule.snap(surface, "frame1_density2")

        density = 3f
        awaitNextRender()
        screenshotRule.snap(surface, "frame2_density3")

        assertFalse(hasRenders())
    }

    @Test(timeout = 5000)
    fun `receive buttons`() = renderingTest(
        width = 40,
        height = 40,
        context = Dispatchers.Unconfined
    ) {
        val receivedButtons = mutableListOf<PointerButtons>()

        setContent {
            Box(
                Modifier.size(40.dp).onPointerEvent(PointerEventType.Press) {
                    receivedButtons.add(it.buttons)
                }
            )
        }

        var buttons = PointerButtons(isSecondaryPressed = true, isBackPressed = true)
        scene.sendPointerEvent(
            PointerEventType.Press,
            Offset(0f, 0f),
            buttons = buttons
        )
        assertThat(receivedButtons.size).isEqualTo(1)
        assertThat(receivedButtons.last()).isEqualTo(buttons)

        buttons = PointerButtons(
            isPrimaryPressed = true,
            isTertiaryPressed = true,
            isForwardPressed = true
        )
        scene.sendPointerEvent(
            PointerEventType.Press,
            Offset(0f, 0f),
            buttons = buttons
        )
        assertThat(receivedButtons.size).isEqualTo(2)
        assertThat(receivedButtons.last()).isEqualTo(buttons)
    }

    @Test(timeout = 5000)
    fun `receive modifiers`() = renderingTest(
        width = 40,
        height = 40,
        context = Dispatchers.Unconfined
    ) {
        val receivedKeyboardModifiers = mutableListOf<PointerKeyboardModifiers>()

        setContent {
            Box(
                Modifier.size(40.dp).onPointerEvent(PointerEventType.Press) {
                    receivedKeyboardModifiers.add(it.keyboardModifiers)
                }
            )
        }

        var keyboardModifiers = PointerKeyboardModifiers(isAltPressed = true)
        scene.sendPointerEvent(
            PointerEventType.Press,
            Offset(0f, 0f),
            keyboardModifiers = keyboardModifiers
        )
        assertThat(receivedKeyboardModifiers.size).isEqualTo(1)
        assertThat(receivedKeyboardModifiers.last()).isEqualTo(keyboardModifiers)

        keyboardModifiers = PointerKeyboardModifiers(
            isCtrlPressed = true,
            isMetaPressed = true,
            isAltPressed = false,
            isShiftPressed = true,
            isAltGraphPressed = true,
            isSymPressed = true,
            isFunctionPressed = true,
            isCapsLockOn = true,
            isScrollLockOn = true,
            isNumLockOn = true,
        )
        scene.sendPointerEvent(
            PointerEventType.Press,
            Offset(0f, 0f),
            keyboardModifiers = keyboardModifiers
        )
        assertThat(receivedKeyboardModifiers.size).isEqualTo(2)
        assertThat(receivedKeyboardModifiers.last()).isEqualTo(keyboardModifiers)
    }

    @Test(expected = TestException::class)
    fun `catch exception in LaunchedEffect`() {
        runBlocking(Dispatchers.Main) {
            composeRule.setContent {
                LaunchedEffect(Unit) {
                    throw TestException()
                }
            }
            composeRule.awaitIdle()
        }
    }

    private class TestException : RuntimeException()

    @ExperimentalComposeUiApi
    @Test
    fun `focus management by keys`() {
        var field1FocusState: FocusState? = null
        var field2FocusState: FocusState? = null
        val (focusItem1, focusItem2) = FocusRequester.createRefs()
        composeRule.setContent {
            var text by remember { mutableStateOf("") }
            Row {
                TextField(
                    text,
                    onValueChange = { text = it },
                    maxLines = 1,
                    modifier = Modifier
                        .onFocusChanged { field1FocusState = it }
                        .focusRequester(focusItem1)
                        .focusProperties {
                            next = focusItem2
                        }
                )
                TextField(
                    text,
                    onValueChange = { text = it },
                    maxLines = 1,
                    modifier = Modifier
                        .onFocusChanged { field2FocusState = it }
                        .focusRequester(focusItem2)
                        .focusProperties {
                            previous = focusItem1
                        }
                )
            }
        }
        composeRule.runOnIdle { focusItem1.requestFocus() }

        composeRule.runOnIdle {
            assertThat(field1FocusState!!.isFocused).isTrue()
            assertThat(field2FocusState!!.isFocused).isFalse()
        }

        composeRule.onRoot().performKeyPress(keyEvent(Key.Tab, KeyEventType.KeyDown))

        composeRule.runOnIdle {
            assertThat(field1FocusState!!.isFocused).isFalse()
            assertThat(field2FocusState!!.isFocused).isTrue()
        }

        composeRule.onRoot().performKeyPress(
            keyEvent(Key.Tab, KeyEventType.KeyDown, KeyEvent.SHIFT_DOWN_MASK)
        )

        composeRule.runOnIdle {
            assertThat(field1FocusState!!.isFocused).isTrue()
            assertThat(field2FocusState!!.isFocused).isFalse()
        }
    }
}
