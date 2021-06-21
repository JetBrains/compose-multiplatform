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

package androidx.compose.ui.input.pointer

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.OpenComposeView
import androidx.compose.ui.composed
import androidx.compose.ui.findAndroidComposeView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.PointerCoords
import androidx.compose.ui.gesture.PointerProperties
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidPointerInputTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule(
        AndroidPointerInputTestActivity::class.java
    )

    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var container: OpenComposeView

    @Before
    fun setup() {
        val activity = rule.activity
        container = spy(OpenComposeView(activity))

        rule.runOnUiThread {
            activity.setContentView(
                container,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    @Test
    fun dispatchTouchEvent_invalidCoordinates() {
        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier
                            .consumeMovementGestureFilter()
                            .onGloballyPositioned { latch.countDown() }
                    )
                }
            }
        }

        rule.runOnUiThread {
            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(Float.NaN, Float.NaN))
            )

            val androidComposeView = findAndroidComposeView(container)!!
            // Act
            val actual = androidComposeView.dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    @Test
    fun dispatchTouchEvent_noPointerInputModifiers_returnsFalse() {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier
                            .onGloballyPositioned { latch.countDown() }
                    )
                }
            }
        }

        rule.runOnUiThread {
            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(0f, 0f))
            )

            // Act
            val actual = findRootView(container).dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    @Test
    fun dispatchTouchEvent_pointerInputModifier_returnsTrue() {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier
                            .consumeMovementGestureFilter()
                            .onGloballyPositioned { latch.countDown() }
                    )
                }
            }
        }

        rule.runOnUiThread {
            val locationInWindow = IntArray(2).also {
                container.getLocationInWindow(it)
            }

            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(locationInWindow[0].toFloat(), locationInWindow[1].toFloat()))
            )

            // Act
            val actual = findRootView(container).dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isTrue()
        }
    }

    @Test
    fun dispatchTouchEvent_movementNotConsumed_requestDisallowInterceptTouchEventNotCalled() {
        dispatchTouchEvent_movementConsumptionInCompose(
            consumeMovement = false,
            callsRequestDisallowInterceptTouchEvent = false
        )
    }

    @Test
    fun dispatchTouchEvent_movementConsumed_requestDisallowInterceptTouchEventCalled() {
        dispatchTouchEvent_movementConsumptionInCompose(
            consumeMovement = true,
            callsRequestDisallowInterceptTouchEvent = true
        )
    }

    @Test
    fun dispatchTouchEvent_notMeasuredLayoutsAreMeasuredFirst() {
        val size = mutableStateOf(10)
        var latch = CountDownLatch(1)
        var consumedDownPosition: Offset? = null
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize().wrapContentSize(align = AbsoluteAlignment.TopLeft)) {
                    Layout(
                        {},
                        Modifier
                            .consumeDownGestureFilter {
                                consumedDownPosition = it
                            }
                            .onGloballyPositioned {
                                latch.countDown()
                            }
                    ) { _, _ ->
                        val sizePx = size.value
                        layout(sizePx, sizePx) {}
                    }
                }
            }
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        rule.runOnUiThread {
            // we update size from 10 to 20 pixels
            size.value = 20
            // this call will synchronously mark the LayoutNode as needs remeasure
            Snapshot.sendApplyNotifications()
            val locationInWindow = IntArray(2).also {
                container.getLocationInWindow(it)
            }

            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(locationInWindow[0] + 15f, locationInWindow[1] + 15f))
            )

            // we expect it to first remeasure and only then process
            findRootView(container).dispatchTouchEvent(motionEvent)

            assertThat(consumedDownPosition).isEqualTo(Offset(15f, 15f))
        }
    }

    // Currently ignored because it fails when run via command line.  Runs successfully in Android
    // Studio.
    @Test
    // TODO(b/158099918): For some reason, this test fails when run from command line but passes
    //  when run from Android Studio.  This seems to be caused by b/158099918.  Once that is
    //  fixed, @Ignore can be removed.
    @Ignore
    fun dispatchTouchEvent_throughLayersOfAndroidAndCompose_hitsChildWithCorrectCoords() {

        // Arrange

        val context = rule.activity

        val log = mutableListOf<List<PointerInputChange>>()

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    AndroidWithCompose(context, 1) {
                        AndroidWithCompose(context, 10) {
                            AndroidWithCompose(context, 100) {
                                Layout(
                                    {},
                                    Modifier
                                        .logEventsGestureFilter(log)
                                        .onGloballyPositioned {
                                            latch.countDown()
                                        }
                                ) { _, _ ->
                                    layout(5, 5) {}
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.runOnUiThread {
            val locationInWindow = IntArray(2).also {
                container.getLocationInWindow(it)
            }

            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(
                    PointerCoords(
                        locationInWindow[0].toFloat() + 1 + 10 + 100,
                        locationInWindow[1].toFloat() + 1 + 10 + 100
                    )
                )
            )

            // Act
            findRootView(container).dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(log).hasSize(1)
            assertThat(log[0]).hasSize(1)
            assertThat(log[0][0].position).isEqualTo(Offset(0f, 0f))
        }
    }

    private fun dispatchTouchEvent_movementConsumptionInCompose(
        consumeMovement: Boolean,
        callsRequestDisallowInterceptTouchEvent: Boolean
    ) {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier
                            .consumeMovementGestureFilter(consumeMovement)
                            .onGloballyPositioned { latch.countDown() }
                    )
                }
            }
        }

        rule.runOnUiThread {
            val (x, y) = IntArray(2).let { array ->
                container.getLocationInWindow(array)
                array.map { item -> item.toFloat() }
            }

            val down = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(x, y))
            )

            val move = MotionEvent(
                0,
                MotionEvent.ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(x + 1, y))
            )

            findRootView(container).dispatchTouchEvent(down)

            // Act
            findRootView(container).dispatchTouchEvent(move)

            // Assert
            if (callsRequestDisallowInterceptTouchEvent) {
                verify(container).requestDisallowInterceptTouchEvent(true)
            } else {
                verify(container, never()).requestDisallowInterceptTouchEvent(any())
            }
        }
    }

    /**
     * This test verifies that if the AndroidComposeView is offset directly by a call to
     * "offsetTopAndBottom(int)", that pointer locations are correct when dispatched down to a child
     * PointerInputModifier.
     */
    @Test
    fun dispatchTouchEvent_androidComposeViewOffset_positionIsCorrect() {

        // Arrange

        val offset = 50
        val log = mutableListOf<List<PointerInputChange>>()

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier
                            .logEventsGestureFilter(log)
                            .onGloballyPositioned { latch.countDown() }
                    )
                }
            }
        }

        rule.runOnUiThread {
            // Get the current location in window.
            val locationInWindow = IntArray(2).also {
                container.getLocationInWindow(it)
            }

            // Offset the androidComposeView.
            container.offsetTopAndBottom(offset)

            // Create a motion event that is also offset.
            val motionEvent = MotionEvent(
                0,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(
                    PointerCoords(
                        locationInWindow[0].toFloat(),
                        locationInWindow[1].toFloat() + offset
                    )
                )
            )

            // Act
            findRootView(container).dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(log).hasSize(1)
            assertThat(log[0]).hasSize(1)
            assertThat(log[0][0].position).isEqualTo(Offset(0f, 0f))
        }
    }

    /**
     * When a modifier is added, it should work, even when it takes the position of a previous
     * modifier.
     */
    @Test
    fun recomposeWithNewModifier() {
        var tap2Enabled by mutableStateOf(false)
        var tapLatch = CountDownLatch(1)
        val tapLatch2 = CountDownLatch(1)
        var positionedLatch = CountDownLatch(1)

        rule.runOnUiThread {
            container.setContent {
                FillLayout(
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { tapLatch.countDown() }
                        }.then(
                            if (tap2Enabled) Modifier.pointerInput(Unit) {
                                detectTapGestures { tapLatch2.countDown() }
                            } else Modifier
                        ).onGloballyPositioned { positionedLatch.countDown() }
                )
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val locationInWindow = IntArray(2)
        rule.runOnUiThread {
            // Get the current location in window.
            container.getLocationInWindow(locationInWindow)

            val downEvent = createPointerEventAt(0, MotionEvent.ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }

        rule.runOnUiThread {
            val upEvent = createPointerEventAt(200, MotionEvent.ACTION_UP, locationInWindow)
            findRootView(container).dispatchTouchEvent(upEvent)
        }

        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
        tapLatch = CountDownLatch(1)

        positionedLatch = CountDownLatch(1)
        tap2Enabled = true
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            val downEvent = createPointerEventAt(1000, MotionEvent.ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }
        // Need to wait for long press timeout (at least)
        rule.runOnUiThread {
            val upEvent = createPointerEventAt(
                1030,
                MotionEvent.ACTION_UP,
                locationInWindow
            )
            findRootView(container).dispatchTouchEvent(upEvent)
        }
        assertTrue(tapLatch2.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        tap2Enabled = false
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            val downEvent = createPointerEventAt(2000, MotionEvent.ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }
        rule.runOnUiThread {
            val upEvent = createPointerEventAt(2200, MotionEvent.ACTION_UP, locationInWindow)
            findRootView(container).dispatchTouchEvent(upEvent)
        }
        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
    }

    /**
     * There are times that getLocationOnScreen() returns (0, 0). Touch input should still arrive
     * at the correct place even if getLocationOnScreen() gives a different result than the
     * rawX, rawY indicate.
     */
    @Test
    fun badGetLocationOnScreen() {
        val tapLatch = CountDownLatch(1)
        val layoutLatch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier
                            .size(250.toDp())
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    p.place(0, 0)
                                    layoutLatch.countDown()
                                }
                            }
                    ) {
                        Box(
                            Modifier
                                .align(AbsoluteAlignment.TopLeft)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        awaitFirstDown()
                                        tapLatch.countDown()
                                    }
                                }.size(10.toDp())
                        )
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        rule.runOnUiThread { }

        val down = createPointerEventAt(0, MotionEvent.ACTION_DOWN, intArrayOf(105, 205))
        down.offsetLocation(-100f, -200f)
        val composeView = findAndroidComposeView(container) as AndroidComposeView
        composeView.dispatchTouchEvent(down)

        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
    }

    private fun createPointerEventAt(eventTime: Int, action: Int, locationInWindow: IntArray) =
        MotionEvent(
            eventTime,
            action,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(
                PointerCoords(
                    locationInWindow[0].toFloat(),
                    locationInWindow[1].toFloat()
                )
            )
        )
}

@Composable
fun AndroidWithCompose(context: Context, androidPadding: Int, content: @Composable () -> Unit) {
    val anotherLayout = ComposeView(context).also { view ->
        view.setContent {
            content()
        }
        view.setPadding(androidPadding, androidPadding, androidPadding, androidPadding)
    }
    AndroidView({ anotherLayout })
}

fun Modifier.consumeMovementGestureFilter(consumeMovement: Boolean = false): Modifier = composed {
    val filter = remember(consumeMovement) { ConsumeMovementGestureFilter(consumeMovement) }
    PointerInputModifierImpl(filter)
}

fun Modifier.consumeDownGestureFilter(onDown: (Offset) -> Unit): Modifier = composed {
    val filter = remember { ConsumeDownChangeFilter() }
    filter.onDown = onDown
    this.then(PointerInputModifierImpl(filter))
}

fun Modifier.logEventsGestureFilter(log: MutableList<List<PointerInputChange>>): Modifier =
    composed {
        val filter = remember { LogEventsGestureFilter(log) }
        this.then(PointerInputModifierImpl(filter))
    }

private class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

private class ConsumeMovementGestureFilter(val consumeMovement: Boolean) : PointerInputFilter() {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (consumeMovement) {
            pointerEvent.changes.fastForEach {
                it.consumePositionChange()
            }
        }
    }

    override fun onCancel() {}
}

private class ConsumeDownChangeFilter : PointerInputFilter() {
    var onDown by mutableStateOf<(Offset) -> Unit>({})
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerEvent.changes.fastForEach {
            if (it.changedToDown()) {
                onDown(it.position)
                it.consumeDownChange()
            }
        }
    }

    override fun onCancel() {}
}

private class LogEventsGestureFilter(val log: MutableList<List<PointerInputChange>>) :
    PointerInputFilter() {

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == PointerEventPass.Initial) {
            log.add(pointerEvent.changes.map { it.copy() })
        }
    }

    override fun onCancel() {}
}

@Suppress("TestFunctionName")
@Composable
private fun FillLayout(modifier: Modifier = Modifier) {
    Layout({}, modifier) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private fun countDown(block: (CountDownLatch) -> Unit) {
    val countDownLatch = CountDownLatch(1)
    block(countDownLatch)
    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
}

class AndroidPointerInputTestActivity : ComponentActivity()

@Suppress("SameParameterValue", "TestFunctionName")
private fun MotionEvent(
    eventTime: Int,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>
) = MotionEvent.obtain(
    0,
    eventTime.toLong(),
    action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
    numPointers,
    pointerProperties,
    pointerCoords,
    0,
    0,
    0f,
    0f,
    0,
    0,
    0,
    0
)

internal fun findRootView(view: View): View {
    val parent = view.parent
    if (parent is View) {
        return findRootView(parent)
    }
    return view
}
