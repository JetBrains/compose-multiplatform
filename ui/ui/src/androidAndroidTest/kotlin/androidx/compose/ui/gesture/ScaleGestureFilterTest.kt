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
import androidx.compose.runtime.emptyContent
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

// TODO(shepshapard): Test that all events related to scaling are consumed.

@LargeTest
@RunWith(AndroidJUnit4::class)
class ScaleGestureFilterTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var scaleObserver: ScaleObserver
    private lateinit var view: View
    private var touchSlop: Float = Float.NEGATIVE_INFINITY

    private val LayoutDimensionFactor = 7
    private val TinyNum = .01f

    @Before
    fun setup() {
        scaleObserver = spy(MyScaleObserver())

        val activity = activityTestRule.activity
        assertTrue(activity.hasFocusLatch.await(5, TimeUnit.SECONDS))

        val setupLatch = CountDownLatch(2)
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Box {
                    touchSlop = with(DensityAmbient.current) { TouchSlop.toPx() }
                    Layout(
                        modifier = Modifier.scaleGestureFilter(scaleObserver),
                        measureBlock = { _, _ ->
                            layout(
                                ceil(touchSlop * LayoutDimensionFactor).toInt(),
                                ceil(touchSlop * LayoutDimensionFactor).toInt()
                            ) {
                                setupLatch.countDown()
                            }
                        },
                        children = emptyContent()
                    )
                }
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }
        assertTrue(setupLatch.await(1000, TimeUnit.SECONDS))
    }

    @Test
    fun ui_pointerMovementWithinTouchSlop_noCallbacksCalled() {

        val touchSlop = touchSlop

        val down1 = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(touchSlop * 1, 50f)),
            view
        )
        val down2 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(PointerProperties(0), PointerProperties(1)),
            arrayOf(
                PointerCoords(touchSlop * 1, 50f),
                PointerCoords(touchSlop * 3, 50f)
            ),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 0 + TinyNum, 50f),
                PointerCoords(touchSlop * 4 - TinyNum, 50f)
            ),
            view
        )
        val up = MotionEvent(
            30,
            MotionEvent.ACTION_POINTER_UP,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 0 + TinyNum, 50f),
                PointerCoords(touchSlop * 4 - TinyNum, 50f)
            ),
            view
        )
        val up2 = MotionEvent(
            40,
            MotionEvent.ACTION_POINTER_UP,
            1,
            0,
            arrayOf(
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 4 - TinyNum, 50f)
            ),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down1)
            view.dispatchTouchEvent(down2)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
            view.dispatchTouchEvent(up2)
        }

        verifyNoMoreInteractions(scaleObserver)
    }

    @Test
    fun ui_pointerMovementBeyondTouchSlop_correctCallbacksInOrder() {

        val touchSlop = touchSlop

        val down1 = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(touchSlop * 1, 50f)),
            view
        )
        val down2 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(PointerProperties(0), PointerProperties(1)),
            arrayOf(
                PointerCoords(touchSlop * 1, 50f),
                PointerCoords(touchSlop * 3, 50f)
            ),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 0 - TinyNum, 50f),
                PointerCoords(touchSlop * 4 + TinyNum, 50f)
            ),
            view
        )
        val up = MotionEvent(
            30,
            MotionEvent.ACTION_POINTER_UP,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 0 - TinyNum, 50f),
                PointerCoords(touchSlop * 4 + TinyNum, 50f)
            ),
            view
        )
        val up2 = MotionEvent(
            40,
            MotionEvent.ACTION_POINTER_UP,
            1,
            0,
            arrayOf(
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 4 + TinyNum, 50f)
            ),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down1)
            view.dispatchTouchEvent(down2)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(up)
            view.dispatchTouchEvent(up2)
        }

        scaleObserver.inOrder {
            verify().onStart()
            verify().onScale(any())
            verify().onStop()
        }
        verifyNoMoreInteractions(scaleObserver)
    }

    @Test
    fun ui_downMoveBeyondSlopCancel_correctCallbacksInOrder() {

        val touchSlop = touchSlop

        val down1 = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(touchSlop * 1, 50f)),
            view
        )
        val down2 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(PointerProperties(0), PointerProperties(1)),
            arrayOf(
                PointerCoords(touchSlop * 1, 50f),
                PointerCoords(touchSlop * 3, 50f)
            ),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 0 - TinyNum, 50f),
                PointerCoords(touchSlop * 4 + TinyNum, 50f)
            ),
            view
        )
        val cancel = MotionEvent(
            30,
            MotionEvent.ACTION_CANCEL,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 0 - TinyNum, 50f),
                PointerCoords(touchSlop * 4 + TinyNum, 50f)
            ),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down1)
            view.dispatchTouchEvent(down2)
            view.dispatchTouchEvent(move)
            view.dispatchTouchEvent(cancel)
        }

        scaleObserver.inOrder {
            verify().onStart()
            verify().onScale(any())
            verify().onCancel()
        }
        verifyNoMoreInteractions(scaleObserver)
    }

    @Test
    fun ui_pointerMovementScalesUp_scaleValueCorrect() {

        val touchSlop = touchSlop

        val down1 = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(touchSlop * 1, 50f)),
            view
        )
        val down2 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(PointerProperties(0), PointerProperties(1)),
            arrayOf(
                PointerCoords(touchSlop * 1, 50f),
                PointerCoords(touchSlop * 3, 50f)
            ),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * -1, 50f),
                PointerCoords(touchSlop * 5, 50f)
            ),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down1)
            view.dispatchTouchEvent(down2)
            view.dispatchTouchEvent(move)
        }

        verify(scaleObserver).onScale(3f)
    }

    @Test
    fun ui_pointerMovementScalesDown_scaleValueCorrect() {

        val touchSlop = touchSlop

        val down1 = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(touchSlop * 1, 50f)),
            view
        )
        val down2 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 1, 50f),
                PointerCoords(touchSlop * 6, 50f)
            ),
            view
        )
        val move = MotionEvent(
            20,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(0),
                PointerProperties(1)
            ),
            arrayOf(
                PointerCoords(touchSlop * 3, 50f),
                PointerCoords(touchSlop * 4, 50f)
            ),
            view
        )

        activityTestRule.runOnUiThreadIR {
            view.dispatchTouchEvent(down1)
            view.dispatchTouchEvent(down2)
            view.dispatchTouchEvent(move)
        }

        verify(scaleObserver).onScale(.2f)
    }
}

@Suppress("RedundantOverride")
open class MyScaleObserver : ScaleObserver {
    override fun onStart() {
        super.onStart()
    }

    override fun onScale(scaleFactor: Float) {}

    override fun onStop() {
        super.onStop()
    }
}
