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
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.TOOL_TYPE_FINGER
import android.view.MotionEvent.TOOL_TYPE_MOUSE
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.OpenComposeView
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.findAndroidComposeView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.PointerCoords
import androidx.compose.ui.gesture.PointerProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
                ACTION_DOWN,
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
    fun dispatchTouchEvent_infiniteCoordinates() {
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
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))
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
                ACTION_DOWN,
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
                ACTION_DOWN,
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
        val latch = CountDownLatch(1)
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
                ACTION_DOWN,
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
                ACTION_DOWN,
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
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(x, y))
            )

            val move = MotionEvent(
                0,
                ACTION_MOVE,
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
                ACTION_DOWN,
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

    @Test
    fun detectTapGestures_blockedMainThread() {
        var didLongPress = false
        var didTap = false
        var inputLatch = CountDownLatch(1)
        val positionedLatch = CountDownLatch(1)

        rule.runOnUiThread {
            container.setContent {
                FillLayout(
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { didLongPress = true; inputLatch.countDown() },
                                onTap = { didTap = true; inputLatch.countDown() }
                            )
                        }
                        .onGloballyPositioned { positionedLatch.countDown() }
                )
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        val locationInWindow = IntArray(2)
        container.getLocationInWindow(locationInWindow)

        val handler = Handler(Looper.getMainLooper())

        val touchUpDelay = 100
        val sleepTime = android.view.ViewConfiguration.getLongPressTimeout() + 100L

        repeat(5) { iteration ->
            rule.runOnUiThread {
                val downEvent = createPointerEventAt(
                    iteration * sleepTime.toInt(),
                    ACTION_DOWN,
                    locationInWindow
                )
                findRootView(container).dispatchTouchEvent(downEvent)
            }

            rule.runOnUiThread {
                val upEvent = createPointerEventAt(
                    touchUpDelay + iteration * sleepTime.toInt(),
                    ACTION_UP,
                    locationInWindow
                )
                handler.postDelayed(
                    Runnable {
                        findRootView(container).dispatchTouchEvent(upEvent)
                    },
                    touchUpDelay.toLong()
                )

                // Block the UI thread from now until past the long-press
                // timeout. This tests that even in pathological situations,
                // the upEvent is still processed before the long-press timeout.
                Thread.sleep(sleepTime)
            }

            assertTrue(inputLatch.await(1, TimeUnit.SECONDS))
            assertFalse(didLongPress)
            assertTrue(didTap)

            didTap = false
            inputLatch = CountDownLatch(1)
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

            val downEvent = createPointerEventAt(0, ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }

        rule.runOnUiThread {
            val upEvent = createPointerEventAt(200, ACTION_UP, locationInWindow)
            findRootView(container).dispatchTouchEvent(upEvent)
        }

        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
        tapLatch = CountDownLatch(1)

        positionedLatch = CountDownLatch(1)
        tap2Enabled = true
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            val downEvent = createPointerEventAt(1000, ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }
        // Need to wait for long press timeout (at least)
        rule.runOnUiThread {
            val upEvent = createPointerEventAt(
                1030,
                ACTION_UP,
                locationInWindow
            )
            findRootView(container).dispatchTouchEvent(upEvent)
        }
        assertTrue(tapLatch2.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        tap2Enabled = false
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            val downEvent = createPointerEventAt(2000, ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }
        rule.runOnUiThread {
            val upEvent = createPointerEventAt(2200, ACTION_UP, locationInWindow)
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

        val down = createPointerEventAt(0, ACTION_DOWN, intArrayOf(105, 205))
        down.offsetLocation(-100f, -200f)
        val composeView = findAndroidComposeView(container) as AndroidComposeView
        composeView.dispatchTouchEvent(down)

        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
    }

    /**
     * When a scale(0, 0) is used, there is no valid inverse matrix. A touch should not reach
     * an item that is scaled to 0.
     */
    @Test
    fun badInverseMatrix() {
        val tapLatch = CountDownLatch(1)
        val layoutLatch = CountDownLatch(1)
        var insideTap = 0
        rule.runOnUiThread {
            container.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier
                            .layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    layoutLatch.countDown()
                                    p.place(0, 0)
                                }
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown()
                                    tapLatch.countDown()
                                }
                            }
                            .requiredSize(10.toDp())
                            .scale(0f, 0f)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown()
                                    insideTap++
                                }
                            }
                            .requiredSize(10.toDp())
                    )
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        rule.runOnUiThread { }

        val down = createPointerEventAt(0, ACTION_DOWN, intArrayOf(5, 5))
        val composeView = findAndroidComposeView(container) as AndroidComposeView
        composeView.dispatchTouchEvent(down)

        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
        rule.runOnUiThread {
            assertEquals(0, insideTap)
        }
    }

    @Test
    fun dispatchNotAttached() {
        val tapLatch = CountDownLatch(1)
        val layoutLatch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier
                            .onPlaced {
                                layoutLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown()
                                    tapLatch.countDown()
                                }
                            }
                            .requiredSize(10.toDp())
                    )
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        val composeView = findAndroidComposeView(container) as AndroidComposeView
        rule.runOnUiThread {
            container.removeAllViews()
            val down = createPointerEventAt(0, ACTION_DOWN, intArrayOf(5, 5))
            assertFalse(composeView.dispatchTouchEvent(down))
        }
    }

    private fun assertHoverEvent(
        event: PointerEvent,
        isEnter: Boolean = false,
        isExit: Boolean = false
    ) {
        assertThat(event.changes).hasSize(1)
        val change = event.changes[0]
        assertThat(change.pressed).isFalse()
        assertThat(change.previousPressed).isFalse()
        val expectedHoverType = when {
            isEnter -> PointerEventType.Enter
            isExit -> PointerEventType.Exit
            else -> PointerEventType.Move
        }
        assertThat(event.type).isEqualTo(expectedHoverType)
    }

    private fun assertScrollEvent(
        event: PointerEvent,
        scrollExpected: Offset
    ) {
        assertThat(event.changes).hasSize(1)
        val change = event.changes[0]
        assertThat(change.pressed).isFalse()
        assertThat(event.type).isEqualTo(PointerEventType.Scroll)
        // we agreed to reverse Y in android to be in line with other platforms
        assertThat(change.scrollDelta).isEqualTo(scrollExpected.copy(y = scrollExpected.y * -1))
    }

    private fun dispatchMouseEvent(
        action: Int,
        layoutCoordinates: LayoutCoordinates,
        offset: Offset = Offset.Zero,
        scrollDelta: Offset = Offset.Zero
    ) {
        rule.runOnUiThread {
            val root = layoutCoordinates.findRoot()
            val pos = root.localPositionOf(layoutCoordinates, offset)
            val event = MotionEvent(
                0,
                action,
                1,
                0,
                arrayOf(PointerProperties(0).also { it.toolType = MotionEvent.TOOL_TYPE_MOUSE }),
                arrayOf(PointerCoords(pos.x, pos.y, scrollDelta.x, scrollDelta.y))
            )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            when (action) {
                ACTION_HOVER_ENTER, ACTION_HOVER_MOVE, ACTION_HOVER_EXIT ->
                    androidComposeView.dispatchHoverEvent(event)
                ACTION_SCROLL -> androidComposeView.dispatchGenericMotionEvent(event)
                else -> androidComposeView.dispatchTouchEvent(event)
            }
        }
    }

    private fun dispatchStylusEvents(
        layoutCoordinates: LayoutCoordinates,
        offset: Offset,
        vararg actions: Int
    ) {
        rule.runOnUiThread {
            val root = layoutCoordinates.findRoot()
            val pos = root.localPositionOf(layoutCoordinates, offset)
            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView

            for (action in actions) {
                val event = MotionEvent(
                    0,
                    action,
                    1,
                    0,
                    arrayOf(PointerProperties(0).also {
                        it.toolType = MotionEvent.TOOL_TYPE_STYLUS
                    }),
                    arrayOf(PointerCoords(pos.x, pos.y))
                )

                when (action) {
                    ACTION_HOVER_ENTER, ACTION_HOVER_MOVE, ACTION_HOVER_EXIT ->
                        androidComposeView.dispatchHoverEvent(event)
                    else -> androidComposeView.dispatchTouchEvent(event)
                }
            }
        }
    }

    private fun dispatchTouchEvent(
        action: Int,
        layoutCoordinates: LayoutCoordinates,
        offset: Offset = Offset.Zero
    ) {
        rule.runOnUiThread {
            val root = layoutCoordinates.findRoot()
            val pos = root.localPositionOf(layoutCoordinates, offset)
            val event = MotionEvent(
                0,
                action,
                1,
                0,
                arrayOf(PointerProperties(0).also { it.toolType = MotionEvent.TOOL_TYPE_FINGER }),
                arrayOf(PointerCoords(pos.x, pos.y))
            )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            androidComposeView.dispatchTouchEvent(event)
        }
    }

    @Test
    fun dispatchHoverEnter() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        layoutCoordinates = it
                        latch.countDown()
                    }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes[0].consume()
                                events += event
                            }
                        }
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_HOVER_ENTER, layoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(events).hasSize(1)
            assertHoverEvent(events[0], isEnter = true)
        }
    }

    @Test
    fun dispatchHoverExit() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    events += event
                                }
                            }
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_HOVER_ENTER, layoutCoordinates!!)
        dispatchMouseEvent(ACTION_HOVER_EXIT, layoutCoordinates!!, Offset(-1f, -1f))

        rule.runOnUiThread {
            assertThat(events).hasSize(2)
            assertHoverEvent(events[0], isEnter = true)
            assertHoverEvent(events[1], isExit = true)
        }
    }

    @Test
    fun dispatchHoverMove() {
        var layoutCoordinates: LayoutCoordinates? = null
        var layoutCoordinates2: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLatch = CountDownLatch(1)
        var anyOtherEvent = false

        var move1 = false
        var move2 = false
        var move3 = false

        var enter: PointerEvent? = null
        var move: PointerEvent? = null
        var exit: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }.pointerInput(Unit) {
                            awaitPointerEventScope {
                                awaitPointerEvent() // enter
                                assertHoverEvent(awaitPointerEvent()) // move
                                move1 = true
                                assertHoverEvent(awaitPointerEvent()) // move
                                move2 = true
                                assertHoverEvent(awaitPointerEvent()) // move
                                move3 = true
                                awaitPointerEvent() // exit
                                eventLatch.countDown()
                            }
                        }
                ) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(50.dp)
                            .onGloballyPositioned {
                                layoutCoordinates2 = it
                            }.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    enter = awaitPointerEvent()
                                    move = awaitPointerEvent()
                                    exit = awaitPointerEvent()
                                    awaitPointerEvent()
                                    anyOtherEvent = true
                                }
                            }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        // Enter outer Box
        dispatchMouseEvent(ACTION_HOVER_ENTER, layoutCoordinates!!)

        // Move to inner Box
        dispatchMouseEvent(ACTION_HOVER_MOVE, layoutCoordinates2!!)
        rule.runOnUiThread {
            assertThat(move1).isTrue()
            assertThat(enter).isNotNull()
            assertHoverEvent(enter!!, isEnter = true)
        }

        // Move within inner Box
        dispatchMouseEvent(ACTION_HOVER_MOVE, layoutCoordinates2!!, Offset(1f, 1f))
        rule.runOnUiThread {
            assertThat(move2).isTrue()
            assertThat(move).isNotNull()
            assertHoverEvent(move!!)
        }

        // Move to outer Box
        dispatchMouseEvent(ACTION_HOVER_MOVE, layoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(move3).isTrue()
            assertThat(exit).isNotNull()
            assertHoverEvent(exit!!, isExit = true)
        }

        // Leave outer Box
        dispatchMouseEvent(ACTION_HOVER_EXIT, layoutCoordinates!!)

        rule.runOnUiThread {
            assertThat(anyOtherEvent).isFalse()
        }
        assertTrue(eventLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun dispatchScroll() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        val scrollDelta = Offset(0.35f, 0.65f)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        layoutCoordinates = it
                        latch.countDown()
                    }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes[0].consume()
                                events += event
                            }
                        }
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_SCROLL, layoutCoordinates!!, scrollDelta = scrollDelta)
        rule.runOnUiThread {
            assertThat(events).hasSize(2) // synthetic enter and scroll
            assertHoverEvent(events[0], isEnter = true)
            assertScrollEvent(events[1], scrollExpected = scrollDelta)
        }
    }

    @Test
    fun dispatchScroll_whenButtonPressed() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        val scrollDelta = Offset(0.35f, 0.65f)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        layoutCoordinates = it
                        latch.countDown()
                    }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes[0].consume()
                                events += event
                            }
                        }
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        // press the button first before scroll
        dispatchMouseEvent(ACTION_DOWN, layoutCoordinates!!)
        dispatchMouseEvent(ACTION_SCROLL, layoutCoordinates!!, scrollDelta = scrollDelta)
        rule.runOnUiThread {
            assertThat(events).hasSize(3) // synthetic enter, button down, scroll
            assertHoverEvent(events[0], isEnter = true)
            assert(events[1].changes.fastAll { it.changedToDownIgnoreConsumed() })
            assertScrollEvent(events[2], scrollExpected = scrollDelta)
        }
    }

    @Test
    fun dispatchScroll_batch() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        val scrollDelta1 = Offset(0.32f, -0.75f)
        val scrollDelta2 = Offset(0.14f, 0.35f)
        val scrollDelta3 = Offset(-0.30f, -0.12f)
        val scrollDelta4 = Offset(-0.05f, 0.68f)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        layoutCoordinates = it
                        latch.countDown()
                    }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes[0].consume()
                                events += event
                            }
                        }
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        listOf(scrollDelta1, scrollDelta2, scrollDelta3, scrollDelta4).fastForEach {
            dispatchMouseEvent(ACTION_SCROLL, layoutCoordinates!!, scrollDelta = it)
        }
        rule.runOnUiThread {
            assertThat(events).hasSize(5) // 4 + synthetic enter
            assertHoverEvent(events[0], isEnter = true)
            assertScrollEvent(events[1], scrollExpected = scrollDelta1)
            assertScrollEvent(events[2], scrollExpected = scrollDelta2)
            assertScrollEvent(events[3], scrollExpected = scrollDelta3)
            assertScrollEvent(events[4], scrollExpected = scrollDelta4)
        }
    }

    @Test
    fun mouseScroll_ignoredAsDownEvent() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        val scrollDelta = Offset(0.35f, 0.65f)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        layoutCoordinates = it
                        latch.countDown()
                    }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes[0].consume()
                                events += event
                            }
                        }
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_SCROLL, layoutCoordinates!!, scrollDelta = scrollDelta)
        rule.runOnUiThread {
            assertThat(events).hasSize(2) // hover enter + scroll
            assertThat(events[1].changes).isNotEmpty()
            assertThat(events[1].changes[0].changedToDown()).isFalse()
        }
    }

    @Test
    fun hoverEnterPressExitEnterExitRelease() {
        var outerCoordinates: LayoutCoordinates? = null
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLog = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        outerCoordinates = it
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.align(Alignment.Center).size(50.dp).pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    eventLog += event
                                }
                            }
                        }.onGloballyPositioned { innerCoordinates = it }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_HOVER_ENTER, outerCoordinates!!)
        dispatchMouseEvent(ACTION_HOVER_MOVE, innerCoordinates!!)
        dispatchMouseEvent(ACTION_DOWN, innerCoordinates!!)
        dispatchMouseEvent(ACTION_MOVE, outerCoordinates!!)
        dispatchMouseEvent(ACTION_MOVE, innerCoordinates!!)
        dispatchMouseEvent(ACTION_MOVE, outerCoordinates!!)
        dispatchMouseEvent(ACTION_UP, outerCoordinates!!)
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(6)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[5].type).isEqualTo(PointerEventType.Release)
        }
    }

    @Test
    fun hoverPressEnterRelease() {
        var missCoordinates: LayoutCoordinates? = null
        var hitCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLog = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.align(AbsoluteAlignment.TopLeft).size(50.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent()
                                    }
                                }
                            }.onGloballyPositioned {
                                missCoordinates = it
                                latch.countDown()
                            }
                    )
                    Box(
                        Modifier.align(AbsoluteAlignment.BottomRight).size(50.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes[0].consume()
                                        eventLog += event
                                    }
                                }
                            }.onGloballyPositioned { hitCoordinates = it }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_HOVER_ENTER, missCoordinates!!)
        dispatchMouseEvent(ACTION_HOVER_EXIT, missCoordinates!!)
        dispatchMouseEvent(ACTION_DOWN, missCoordinates!!)
        dispatchMouseEvent(ACTION_MOVE, hitCoordinates!!)
        dispatchMouseEvent(ACTION_UP, hitCoordinates!!)
        dispatchMouseEvent(ACTION_HOVER_ENTER, hitCoordinates!!)
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(1)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
        }
    }

    @Test
    fun pressInsideExitWindow() {
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLog = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .size(50.dp)
                            .graphicsLayer { translationY = 25.dp.roundToPx().toFloat() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes[0].consume()
                                        eventLog += event
                                    }
                                }
                            }.onGloballyPositioned {
                                innerCoordinates = it
                                latch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        val coords = innerCoordinates!!
        dispatchMouseEvent(ACTION_HOVER_ENTER, coords)
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_MOVE, coords, Offset(0f, coords.size.height / 2 - 1f))
        dispatchMouseEvent(ACTION_MOVE, coords, Offset(0f, coords.size.height - 1f))
        dispatchMouseEvent(ACTION_UP, coords, Offset(0f, coords.size.height - 1f))
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(5)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Move)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Release)
        }
    }

    @Test
    fun pressInsideClippedContent() {
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLog = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.align(Alignment.TopCenter).requiredSize(50.dp).clipToBounds()) {
                        Box(
                            Modifier
                                .requiredSize(50.dp)
                                .graphicsLayer { translationY = 25.dp.roundToPx().toFloat() }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes[0].consume()
                                            eventLog += event
                                        }
                                    }
                                }.onGloballyPositioned {
                                    innerCoordinates = it
                                    latch.countDown()
                                }
                        )
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        val coords = innerCoordinates!!
        dispatchMouseEvent(ACTION_HOVER_ENTER, coords)
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_MOVE, coords, Offset(0f, coords.size.height - 1f))
        dispatchMouseEvent(ACTION_UP, coords, Offset(0f, coords.size.height - 1f))
        dispatchMouseEvent(ACTION_HOVER_ENTER, coords, Offset(0f, coords.size.height - 1f))
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(5)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Move)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Release)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Exit)
        }
    }

    private fun setSimpleLayout(eventLog: MutableList<PointerEvent>): LayoutCoordinates {
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes[0].consume()
                                eventLog += event
                            }
                        }
                    }.onGloballyPositioned {
                        innerCoordinates = it
                        latch.countDown()
                    }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        return innerCoordinates!!
    }

    @Test
    fun cancelOnDeviceChange() {
        // When a pointer has had a surprise removal, a "cancel" event should be sent if it was
        // pressed.
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        dispatchMouseEvent(ACTION_HOVER_ENTER, coords)
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_MOVE, coords, Offset(0f, 1f))

        val motionEvent = MotionEvent(
            5,
            ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(10).also { it.toolType = TOOL_TYPE_FINGER }),
            arrayOf(PointerCoords(1f, 1f))
        )

        container.dispatchTouchEvent(motionEvent)
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(5)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Move)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Release)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Press)
        }
    }

    @Test
    fun testSyntheticEventPosition() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)
        dispatchMouseEvent(ACTION_DOWN, coords)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(2)
            assertThat(eventLog[0].changes[0].position).isEqualTo(eventLog[1].changes[0].position)
        }
    }

    @Test
    fun testStylusHoverExitDueToPress() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT, ACTION_DOWN)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(2)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
        }
    }

    @Test
    fun testStylusHoverExitNoFollowingEvent() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        // Exit followed by nothing should just send the exit
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(2)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Exit)
        }
    }

    @Test
    fun testStylusHoverExitWithFollowingHoverEvent() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        // Exit immediately followed by enter with the same device should send both
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT, ACTION_HOVER_ENTER)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(3)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Enter)
        }
    }

    @Test
    fun testStylusHoverExitWithFollowingTouchEvent() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        // Exit followed by cancel should send both
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT, ACTION_CANCEL)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(2)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Exit)
        }
    }

    @Test
    fun testStylusHoverExitWithFollowingDownOnDifferentDevice() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        // Exit followed by a different device should send the exit
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        rule.runOnUiThread {
            val root = coords.findRoot()
            val pos = root.localPositionOf(coords, Offset.Zero)
            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            val exit = MotionEvent(
                0,
                ACTION_HOVER_EXIT,
                1,
                0,
                arrayOf(PointerProperties(0).also {
                    it.toolType = MotionEvent.TOOL_TYPE_STYLUS
                }),
                arrayOf(PointerCoords(pos.x, pos.y))
            )

            androidComposeView.dispatchHoverEvent(exit)

            val down = MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0).also {
                    it.toolType = MotionEvent.TOOL_TYPE_FINGER
                }),
                arrayOf(PointerCoords(pos.x, pos.y))
            )
            androidComposeView.dispatchTouchEvent(down)
        }

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(3)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Press)
        }
    }

    @Test
    fun syntheticEventSentAfterUp() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        dispatchMouseEvent(ACTION_HOVER_ENTER, coords)
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_UP, coords)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(5)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Release)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Enter)
        }
    }

    @Test
    fun clippedHasNoInputIfLargeEnough() {
        val eventLog = mutableListOf<PointerEventType>()
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.size(50.dp).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    })
                    Box(Modifier.size(50.dp).clipToBounds()) {
                        Box(Modifier.size(50.dp)
                            .graphicsLayer {
                                translationY = -25.dp.roundToPx().toFloat()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes[0].consume()
                                        eventLog += event.type
                                    }
                                }
                            }.onGloballyPositioned {
                                innerCoordinates = it
                                latch.countDown()
                            }
                        )
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val coords = innerCoordinates!!

        // Hit the top Box
        dispatchMouseEvent(ACTION_HOVER_ENTER, coords, Offset(0f, -1f))

        // Hit the bottom box, but clipped
        dispatchMouseEvent(ACTION_HOVER_MOVE, coords)
        dispatchMouseEvent(
            ACTION_HOVER_MOVE, coords,
            Offset(0f, (coords.size.height / 2 - 1).toFloat())
        )

        rule.runOnUiThread {
            assertThat(eventLog).isEmpty()
        }

        // Now hit the box in the unclipped region
        dispatchMouseEvent(
            ACTION_HOVER_MOVE, coords,
            Offset(0f, (coords.size.height / 2 + 1).toFloat())
        )

        // Now hit the bottom of the clipped region
        dispatchMouseEvent(
            ACTION_HOVER_MOVE, coords,
            Offset(0f, (coords.size.height - 1).toFloat())
        )

        // Now leave
        dispatchMouseEvent(
            ACTION_HOVER_MOVE, coords,
            Offset(0f, coords.size.height.toFloat() + 1f)
        )

        rule.runOnUiThread {
            assertThat(eventLog).containsExactly(
                PointerEventType.Enter, PointerEventType.Move, PointerEventType.Exit
            )
        }
    }

    @Test
    fun unclippedTakesPrecedenceWithMinimumTouchTarget() {
        val eventLog = mutableListOf<PointerEventType>()
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.size(50.dp).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    })
                    Box(Modifier.size(20.dp).clipToBounds()) {
                        Box(Modifier.size(20.dp)
                            .graphicsLayer {
                                translationY = -10.dp.roundToPx().toFloat()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes[0].consume()
                                        eventLog += event.type
                                    }
                                }
                            }.onGloballyPositioned {
                                innerCoordinates = it
                                latch.countDown()
                            }
                        )
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val coords = innerCoordinates!!

        // Hit the top Box, but in the minimum touch target area of the bottom Box
        dispatchTouchEvent(ACTION_DOWN, coords, Offset(0f, -1f))
        dispatchTouchEvent(ACTION_UP, coords, Offset(0f, -1f))

        // Hit the top Box in the clipped region of the bottom Box
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_UP, coords)

        rule.runOnUiThread {
            assertThat(eventLog).isEmpty()
        }

        // Hit the bottom box in the unclipped region
        val topOfUnclipped = Offset(0f, (coords.size.height / 2 + 1).toFloat())
        dispatchMouseEvent(ACTION_DOWN, coords, topOfUnclipped)
        dispatchMouseEvent(ACTION_UP, coords, topOfUnclipped)

        // Continue to the bottom of the bottom Box
        val bottomOfBox = Offset(0f, (coords.size.height - 1).toFloat())
        dispatchMouseEvent(ACTION_DOWN, coords, bottomOfBox)
        dispatchMouseEvent(ACTION_UP, coords, bottomOfBox)

        // Now exit the bottom box
        val justBelow = Offset(0f, (coords.size.height + 1).toFloat())
        dispatchMouseEvent(ACTION_DOWN, coords, justBelow)
        dispatchMouseEvent(ACTION_UP, coords, justBelow)

        rule.runOnUiThread {
            assertThat(eventLog).containsExactly(
                PointerEventType.Press,
                PointerEventType.Release,
                PointerEventType.Press,
                PointerEventType.Release,
                PointerEventType.Press,
                PointerEventType.Release,
            )
        }
    }

    @Test
    fun stylusEnterExitPointerArea() {
        // Stylus hover enter/exit events should be sent to pointer input areas
        val eventLog = mutableListOf<PointerEvent>()
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }
                ) {
                    Box(Modifier.size(50.dp).align(AbsoluteAlignment.BottomRight)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                    eventLog += event
                                }
                            }
                        }.onGloballyPositioned {
                            innerCoordinates = it
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val coords = innerCoordinates!!
        val outside = Offset(-100f, -100f)
        dispatchStylusEvents(coords, outside, ACTION_HOVER_ENTER)
        rule.runOnUiThread {
            // The event didn't land inside the box, so it shouldn't get the hover enter
            assertThat(eventLog).isEmpty()
        }
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_MOVE)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT, ACTION_DOWN)
        dispatchStylusEvents(coords, outside, ACTION_MOVE)
        dispatchStylusEvents(coords, outside, ACTION_UP, ACTION_HOVER_ENTER)
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(4)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Release)
        }
    }

    @Test
    fun restartStreamAfterNotProcessing() {
        // Stylus hover enter/exit events should be sent to pointer input areas
        val eventLog = mutableListOf<PointerEvent>()
        var hitCoordinates: LayoutCoordinates? = null
        var missCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(2)
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.size(50.dp).align(AbsoluteAlignment.TopLeft)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                    eventLog += event
                                }
                            }
                        }.onGloballyPositioned {
                            hitCoordinates = it
                            latch.countDown()
                        }
                    )
                    Box(Modifier.size(50.dp).align(AbsoluteAlignment.BottomRight)
                        .onGloballyPositioned {
                            missCoordinates = it
                            latch.countDown()
                        }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        val miss = missCoordinates!!
        val hit = hitCoordinates!!

        // This should hit
        dispatchTouchEvent(ACTION_DOWN, hit)
        dispatchTouchEvent(ACTION_UP, hit)

        // This should miss
        dispatchTouchEvent(ACTION_DOWN, miss)

        // This should hit
        dispatchTouchEvent(ACTION_DOWN, hit)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(3)
            val down1 = eventLog[0]
            val up1 = eventLog[1]
            val down2 = eventLog[2]
            assertThat(down1.changes).hasSize(1)
            assertThat(up1.changes).hasSize(1)
            assertThat(down2.changes).hasSize(1)

            assertThat(down1.type).isEqualTo(PointerEventType.Press)
            assertThat(up1.type).isEqualTo(PointerEventType.Release)
            assertThat(down2.type).isEqualTo(PointerEventType.Press)

            assertThat(up1.changes[0].id).isEqualTo(down1.changes[0].id)
            assertThat(down2.changes[0].id.value).isEqualTo(down1.changes[0].id.value + 2)
        }
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
                it.consume()
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
                it.consume()
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
): MotionEvent {
    val buttonState = if (pointerProperties[0].toolType == MotionEvent.TOOL_TYPE_MOUSE &&
        (action == ACTION_DOWN || action == ACTION_MOVE)
    ) {
        MotionEvent.BUTTON_PRIMARY
    } else {
        0
    }
    val source = if (pointerProperties[0].toolType == TOOL_TYPE_MOUSE) {
        InputDevice.SOURCE_MOUSE
    } else {
        InputDevice.SOURCE_TOUCHSCREEN
    }
    return MotionEvent.obtain(
        0,
        eventTime.toLong(),
        action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
        numPointers,
        pointerProperties,
        pointerCoords,
        0,
        buttonState,
        0f,
        0f,
        0,
        0,
        source,
        0
    )
}

internal fun findRootView(view: View): View {
    val parent = view.parent
    if (parent is View) {
        return findRootView(parent)
    }
    return view
}
