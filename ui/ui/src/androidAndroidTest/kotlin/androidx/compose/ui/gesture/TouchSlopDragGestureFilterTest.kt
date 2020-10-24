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
package androidx.compose.ui.gesture

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.test.TestActivity
import androidx.test.filters.LargeTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class TouchSlopDragGestureFilterTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var dragObserver: MyDragObserver
    private lateinit var view: View
    private var touchSlop: Float = Float.NEGATIVE_INFINITY

    private val TinyNum = .01f

    @Test
    fun ui_pointerMovementWithinTouchSlop_noCallbacksCalled() {
        setup(false)

        val touchSlop = touchSlop

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f)),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop - TinyNum, 50f)),
            view
        )
        val up = MotionEvent(
            30,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop - TinyNum, 50f)),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
        }
        verifyNoMoreInteractions(dragObserver)
    }

    @Test
    fun ui_pointerDownMovementBeyondTouchSlopUp_correctCallbacksInOrder() {
        setup(false)

        val touchSlop = touchSlop

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f)),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop + TinyNum, 50f)),
            view
        )
        val up = MotionEvent(
            30,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop + TinyNum, 50f)),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
        }

        dragObserver.inOrder {
            verify().onStart(dragObserver.arg())
            verify().onStartMock(Offset(50f, 50f))
            verify().onDrag(dragObserver.arg())
            verify().onDragMock(any())
            verify().onStop(dragObserver.arg())
            verify().onStopMock(any())
        }
        verifyNoMoreInteractions(dragObserver)
    }

    @Test
    fun ui_pointerDownMovementBeyondTouchSlopCancel_correctCallbacksInOrder() {
        setup(false)

        val touchSlop = touchSlop

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f)),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop + TinyNum, 50f)),
            view
        )
        val cancel = MotionEvent(
            30,
            MotionEvent.ACTION_CANCEL,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + touchSlop, 50f)),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(cancel)
        }

        dragObserver.inOrder {
            verify().onStart(dragObserver.arg())
            verify().onStartMock(Offset(50f, 50f))
            verify().onDrag(dragObserver.arg())
            verify().onDragMock(any())
            verify().onCancel()
        }
        verifyNoMoreInteractions(dragObserver)
    }

    @Test
    fun ui_startDragImmediatelyTrueDown_onStartOnlyCalled() {
        setup(true)

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f)),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
        }

        dragObserver.inOrder {
            verify().onStart(dragObserver.arg())
            verify().onStartMock(Offset(50f, 50f))
        }
        verifyNoMoreInteractions(dragObserver)
    }

    @Test
    fun ui_movement_onDragCalledWithCorrectValue() {
        setup(false)

        // Guaranteed to be over slop
        val movement = (touchSlop + 1 * 2).toInt()

        val down = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f, 50f)),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(50f + movement, 50f)),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down)
            view.dispatchTouchEvent(move)
        }

        verify(dragObserver).onDrag(Offset(movement.toFloat(), 0.0f))
    }

    private fun setup(startDragImmediately: Boolean) {
        dragObserver = spy(MyDragObserver())

        val activity = activityTestRule.activity
        assertTrue(activity.hasFocusLatch.await(5, TimeUnit.SECONDS))

        val setupLatch = CountDownLatch(2)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Box {
                    touchSlop = with(DensityAmbient.current) { TouchSlop.toPx() }
                    Layout(
                        modifier = Modifier.dragGestureFilter(
                            dragObserver,
                            startDragImmediately = startDragImmediately
                        ),
                        measureBlock = { _, _ ->
                            layout(100, 100) {
                                setupLatch.countDown()
                            }
                        },
                        children = {}
                    )
                }
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }
        assertTrue(setupLatch.await(1000, TimeUnit.SECONDS))
    }
}

@Suppress("RedundantOverride")
open class MyDragObserver : DragObserver {
    private val params = mutableListOf<Offset>()

    override fun onStart(downPosition: Offset) = onStartMock(downPosition)

    open fun onStartMock(downPosition: Any) {
        params.add(downPosition as Offset)
        super.onStart(downPosition)
    }

    override fun onDrag(dragDistance: Offset): Offset = onDragMock(dragDistance)

    open fun onDragMock(dragDistance: Any): Offset {
        params.add(dragDistance as Offset)
        return super.onDrag(dragDistance)
    }

    override fun onStop(velocity: Offset) = onStopMock(velocity)

    open fun onStopMock(velocity: Any) {
        params.add(velocity as Offset)
        super.onStop(velocity)
    }

    fun arg(): Offset = params.removeAt(0)
}
