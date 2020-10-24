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

package androidx.compose.foundation.text.selection

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.CoreText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.platform.HapticFeedBackAmbient
import androidx.compose.ui.selection.Selection
import androidx.compose.ui.selection.SelectionContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.font.asFontFamily
import androidx.compose.ui.text.font.test.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.createAndroidComposeRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max

@LargeTest
@RunWith(AndroidJUnit4::class)
class SelectionContainerTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private val textContent = "Text Demo Text Demo"
    private val fontFamily = ResourceFont(
        resId = R.font.sample_font,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ).asFontFamily()

    private lateinit var gestureCountDownLatch: CountDownLatch

    private val selection = mutableStateOf<Selection?>(null)
    private val fontSize = 10.sp
    private val log = PointerInputChangeLog()

    private val hapticFeedback = mock<HapticFeedback>()

    @Before
    fun setup() {
        rule.setContent {
            Providers(
                HapticFeedBackAmbient provides hapticFeedback
            ) {
                TestParent(Modifier.gestureSpy(log)) {
                    SelectionContainer(
                        selection = selection.value,
                        onSelectionChange = {
                            selection.value = it
                            gestureCountDownLatch.countDown()
                        }
                    ) {
                        CoreText(
                            AnnotatedString(textContent),
                            Modifier.fillMaxSize(),
                            style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            maxLines = Int.MAX_VALUE,
                            inlineContent = mapOf(),
                            onTextLayout = {}
                        )
                    }
                }
            }
        }
        rule.activityRule.scenario.onActivity {
            view = it.findViewById<ViewGroup>(android.R.id.content)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun press_to_cancel() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        val position = 50f
        longPress(x = position, y = position)
        rule.runOnIdle {
            assertThat(selection.value).isNotNull()
        }

        // Act.
        press(x = position, y = position)

        // Assert.
        rule.runOnIdle {
            assertThat(selection.value).isNull()
            verify(
                hapticFeedback,
                times(2)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun tapToCancelDoesNotBlockUp() {
        // Setup. Long press to create a selection.
        // A reasonable number.
        val position = 50f
        longPress(x = position, y = position)

        log.entries.clear()

        // Act.
        press(x = position, y = position)

        // Assert.
        rule.runOnIdle {
            // Press has a down event over 3 passes and then an up event over 3 passes.  We are
            // interested in looking at the final up event.
            assertThat(log.entries).hasSize(6)
            assertThat(log.entries[5].pass).isEqualTo(PointerEventPass.Final)
            assertThat(log.entries[5].changes).hasSize(1)
            assertThat(log.entries[5].changes[0].changedToUp()).isTrue()
        }
    }

    @Test
    fun long_press_select_a_word() {
        // Setup.
        val characterSize = with(rule.density) { fontSize.toPx() }

        // Act.
        longPress(
            x = textContent.indexOf('m') * characterSize,
            y = 0.5f * characterSize
        )

        // Assert. Should select "Demo".
        rule.runOnIdle {
            assertThat(selection.value!!.start.offset).isEqualTo(textContent.indexOf('D'))
            assertThat(selection.value!!.end.offset).isEqualTo(textContent.indexOf('o') + 1)
            verify(
                hapticFeedback,
                times(1)
            ).performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    private fun longPress(x: Float, y: Float) {
        waitForLongPress {
            view.dispatchTouchEvent(getDownEvent(x, y))
        }
        waitForOtherGesture {
            view.dispatchTouchEvent(getUpEvent(x, y))
        }
    }

    private fun longPressAndDrag(startX: Float, startY: Float, endX: Float, endY: Float) {
        waitForLongPress {
            view.dispatchTouchEvent(getDownEvent(startX, startY))
        }
        waitForOtherGesture {
            view.dispatchTouchEvent(getMoveEvent(endX, endY))
        }
    }

    private fun press(x: Float, y: Float) {
        waitForOtherGesture {
            view.dispatchTouchEvent(getDownEvent(x, y))
        }
        waitForOtherGesture {
            view.dispatchTouchEvent(getUpEvent(x, y))
        }
    }

    private fun getDownEvent(x: Float, y: Float): MotionEvent {
        return MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(x, y))
        )
    }

    private fun getUpEvent(x: Float, y: Float): MotionEvent {
        return MotionEvent(
            0,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(x, y))
        )
    }

    private fun getMoveEvent(x: Float, y: Float): MotionEvent {
        return MotionEvent(
            0,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(x, y))
        )
    }

    private fun waitForLongPress(block: () -> Unit) {
        gestureCountDownLatch = CountDownLatch(1)
        rule.runOnIdle(block)
        gestureCountDownLatch.await(750, TimeUnit.MILLISECONDS)
    }

    private fun waitForOtherGesture(block: () -> Unit) {
        rule.runOnIdle(block)
    }
}

private class PointerInputChangeLog : (PointerEvent, PointerEventPass) -> Unit {

    val entries = mutableListOf<PointerInputChangeLogEntry>()

    override fun invoke(p1: PointerEvent, p2: PointerEventPass) {
        entries.add(PointerInputChangeLogEntry(p1.changes.map { it }, p2))
    }
}

private data class PointerInputChangeLogEntry(
    val changes: List<PointerInputChange>,
    val pass: PointerEventPass
)

private fun Modifier.gestureSpy(
    onPointerInput: (PointerEvent, PointerEventPass) -> Unit
): Modifier = composed {
    val spy = remember { GestureSpy() }
    spy.onPointerInput = onPointerInput
    spy
}

private class GestureSpy : PointerInputModifier {

    lateinit var onPointerInput: (PointerEvent, PointerEventPass) -> Unit

    override val pointerInputFilter = object : PointerInputFilter() {
        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize
        ) {
            onPointerInput(pointerEvent, pass)
        }

        override fun onCancel() {
            // Nothing to implement
        }
    }
}

@Composable
fun TestParent(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(modifier = modifier, children = children) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val width = placeables.fold(0) { maxWidth, placeable ->
            max(maxWidth, (placeable.width))
        }

        val height = placeables.fold(0) { minWidth, placeable ->
            max(minWidth, (placeable.height))
        }

        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.place(0, 0)
            }
        }
    }
}