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

package androidx.compose.ui.input.pointer

import android.content.Context
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.PointerProperties
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests basic operations to make sure that pointerInputModifier(view: AndroidViewHolder) is
// hooked up to PointerInteropFilter correctly.
@MediumTest
@RunWith(AndroidJUnit4::class)
class PointerInteropFilterAndroidViewHookupTest {

    private lateinit var root: View
    private lateinit var child: CustomView2
    private lateinit var captureRequestDisallow: CaptureRequestDisallow
    private val motionEventLog = mutableListOf<MotionEvent?>()
    private val eventStringLog = mutableListOf<String>()
    private val siblingEvents = mutableListOf<PointerEventType>()
    private val motionEventCallback: (MotionEvent?) -> Unit = {
        motionEventLog.add(it)
        eventStringLog.add("motionEvent")
    }

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setup() {
        rule.activityRule.scenario.onActivity { activity ->

            child = CustomView2(activity, motionEventCallback).apply {
                layoutParams = ViewGroup.LayoutParams(100, 100)
            }

            val parent = ComposeView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setContent {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        siblingEvents += event.type
                                    }
                                }
                            }
                        )
                        AndroidView(
                            { child },
                            Modifier.spyGestureFilter { eventStringLog.add(it.name) }
                        )
                    }
                }
            }

            captureRequestDisallow = CaptureRequestDisallow(activity)
            captureRequestDisallow.addView(parent)
            activity.setContentView(captureRequestDisallow)
            root = activity.findViewById(android.R.id.content)
        }
        siblingEvents.clear()
    }

    @Test
    fun ui_down_downMotionEventIsReceived() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
        }

        assertThat(motionEventLog).hasSize(1)
        assertThat(motionEventLog[0]).isSameInstanceAs(down)
    }

    @Test
    fun ui_downUp_upMotionEventIsReceived() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val up =
            MotionEvent(
                10,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
            root.dispatchTouchEvent(up)
        }

        assertThat(motionEventLog).hasSize(2)
        assertThat(motionEventLog[1]).isSameInstanceAs(up)
    }

    @Test
    fun ui_downRetFalseUp_onlyDownIsReceived() {
        child.retVal = false

        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val up =
            MotionEvent(
                10,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
            root.dispatchTouchEvent(up)
        }

        assertThat(motionEventLog).hasSize(1)
        assertThat(motionEventLog[0]).isSameInstanceAs(down)
    }

    @Test
    fun ui_downRetFalseUpMoveUp_onlyDownIsReceived() {
        child.retVal = false

        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val move =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )
        val up =
            MotionEvent(
                20,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
            root.dispatchTouchEvent(move)
            root.dispatchTouchEvent(up)
        }

        assertThat(motionEventLog).hasSize(1)
        assertThat(motionEventLog[0]).isSameInstanceAs(down)
    }

    @Test
    fun ui_downRetFalseUpDown_2ndDownIsReceived() {
        child.retVal = false

        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val up =
            MotionEvent(
                10,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val down2 =
            MotionEvent(
                2,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
            root.dispatchTouchEvent(up)
            root.dispatchTouchEvent(down2)
        }

        assertThat(motionEventLog).hasSize(2)
        assertThat(motionEventLog[1]).isSameInstanceAs(down2)
    }

    @Test
    fun ui_downMove_moveIsDispatchedDuringFinal() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val move =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
            eventStringLog.clear()
            root.dispatchTouchEvent(move)
        }

        assertThat(eventStringLog).hasSize(4)
        assertThat(eventStringLog[0]).isEqualTo(PointerEventPass.Initial.toString())
        assertThat(eventStringLog[1]).isEqualTo(PointerEventPass.Main.toString())
        assertThat(eventStringLog[2]).isEqualTo(PointerEventPass.Final.toString())
        assertThat(eventStringLog[3]).isEqualTo("motionEvent")
    }

    @Test
    fun ui_downDisallowInterceptMove_moveIsDispatchedDuringInitial() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val move =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
            eventStringLog.clear()
            child.requestDisallowInterceptTouchEvent(true)
            root.dispatchTouchEvent(move)
        }

        assertThat(eventStringLog).hasSize(4)
        assertThat(eventStringLog[0]).isEqualTo(PointerEventPass.Initial.toString())
        assertThat(eventStringLog[1]).isEqualTo("motionEvent")
        assertThat(eventStringLog[2]).isEqualTo(PointerEventPass.Main.toString())
        assertThat(eventStringLog[3]).isEqualTo(PointerEventPass.Final.toString())
    }

    @Test
    fun ui_downDisallowInterceptMoveAllowInterceptMove_2ndMoveIsDispatchedDuringFinal() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val move1 =
            MotionEvent(
                10,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )
        val move2 =
            MotionEvent(
                20,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(150f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
            child.requestDisallowInterceptTouchEvent(true)
            root.dispatchTouchEvent(move1)
            eventStringLog.clear()
            child.requestDisallowInterceptTouchEvent(false)
            root.dispatchTouchEvent(move2)
        }

        assertThat(eventStringLog).hasSize(4)
        assertThat(eventStringLog[0]).isEqualTo(PointerEventPass.Initial.toString())
        assertThat(eventStringLog[1]).isEqualTo(PointerEventPass.Main.toString())
        assertThat(eventStringLog[2]).isEqualTo(PointerEventPass.Final.toString())
        assertThat(eventStringLog[3]).isEqualTo("motionEvent")
    }

    @Test
    fun ui_downDisallowInterceptUpDownMove_2ndMoveIsDispatchedDuringFinal() {
        val down =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val up =
            MotionEvent(
                10,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val downB =
            MotionEvent(
                20,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(50f, 50f)),
                root
            )
        val moveB =
            MotionEvent(
                30,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(100f, 50f)),
                root
            )

        rule.runOnIdle {
            root.dispatchTouchEvent(down)
            child.requestDisallowInterceptTouchEvent(true)
            root.dispatchTouchEvent(up)
            root.dispatchTouchEvent(downB)
            eventStringLog.clear()
            root.dispatchTouchEvent(moveB)
        }

        assertThat(eventStringLog).hasSize(4)
        assertThat(eventStringLog[0]).isEqualTo(PointerEventPass.Initial.toString())
        assertThat(eventStringLog[1]).isEqualTo(PointerEventPass.Main.toString())
        assertThat(eventStringLog[2]).isEqualTo(PointerEventPass.Final.toString())
        assertThat(eventStringLog[3]).isEqualTo("motionEvent")
    }

    @Test
    fun disallowNotTriggeredWhenMovementInClickChild() {
        var clicked = false
        rule.runOnUiThread {
            child.setOnClickListener { clicked = true }
        }
        rule.runOnIdle {
            val outOfView = Offset(-50f, -50f)
            root.dispatchTouchEvent(down())
            root.dispatchTouchEvent(move(10, outOfView))
            root.dispatchTouchEvent(up(20, outOfView))
        }

        assertThat(clicked).isFalse()
        assertThat(captureRequestDisallow.disallowIntercept).isFalse()
    }

    @Test
    fun disallowTriggeredWhenMovementInClickChildAfterRequestDisallow() {
        var clicked = false
        rule.runOnUiThread {
            child.setOnClickListener { clicked = true }
        }

        rule.runOnIdle {
            val outOfView = Offset(-50f, -50f)
            root.dispatchTouchEvent(down())
            child.requestDisallowInterceptTouchEvent(true)
            root.dispatchTouchEvent(move(10, outOfView))
            root.dispatchTouchEvent(up(20, outOfView))
        }

        assertThat(clicked).isFalse()
        assertThat(captureRequestDisallow.disallowIntercept).isTrue()
    }

    @Test
    fun overlappingChildAllowsEventsThrough() {
        rule.runOnIdle {
            val start = Offset(50f, 50f)
            val middle = Offset(10f, 10f)
            val end = Offset(51f, 50f)
            root.dispatchTouchEvent(down(0, start))
            root.dispatchTouchEvent(move(10, middle))
            root.dispatchTouchEvent(move(20, end))
            root.dispatchTouchEvent(up(30, end))
        }

        rule.runOnIdle {
            assertThat(siblingEvents).hasSize(4)
            assertThat(siblingEvents).containsExactly(
                PointerEventType.Press,
                PointerEventType.Move,
                PointerEventType.Move,
                PointerEventType.Release
            )
        }
    }

    fun down(eventTime: Int = 0, offset: Offset = Offset(50f, 50f)) =
        MotionEvent(
            eventTime,
            ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(offset.x, offset.y)),
            root
        )

    fun move(eventTime: Int, offset: Offset) =
        MotionEvent(
            eventTime,
            ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(offset.x, offset.y)),
            root
        )

    fun up(eventTime: Int, offset: Offset = Offset(50f, 50f)) =
        MotionEvent(
            eventTime,
            ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(offset.x, offset.y)),
            root
        )
}

private class CustomView2(context: Context, val callBack: (MotionEvent?) -> Unit) : ViewGroup
(context) {
    var retVal = true

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        callBack(event)
        return retVal
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {}
}

private class CaptureRequestDisallow(context: Context) : FrameLayout(context) {
    var disallowIntercept = false
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        this.disallowIntercept = disallowIntercept
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
}