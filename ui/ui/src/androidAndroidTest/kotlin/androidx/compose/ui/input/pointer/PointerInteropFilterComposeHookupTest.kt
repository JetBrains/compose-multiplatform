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

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
@OptIn(ExperimentalComposeUiApi::class)
class PointerInteropFilterComposeHookupTest {

    private lateinit var root: View
    private val motionEventLog = mutableListOf<MotionEvent?>()
    private val eventStringLog = mutableListOf<String>()
    private var retVal = true
    private val motionEventCallback: (MotionEvent?) -> Boolean = {
        motionEventLog.add(it)
        eventStringLog.add("motionEvent")
        retVal
    }
    private val disallowInterceptRequester = RequestDisallowInterceptTouchEvent()

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setup() {
        rule.activityRule.scenario.onActivity { activity ->

            val parent = ComposeView(activity).apply {
                setContent {
                    with(LocalDensity.current) {
                        Box(
                            modifier = Modifier
                                .spyGestureFilter {
                                    eventStringLog.add(it.name)
                                }
                                .pointerInteropFilter(
                                    disallowInterceptRequester,
                                    motionEventCallback
                                )
                                .requiredSize(100f.toDp(), 100f.toDp())
                        )
                    }
                }
            }

            activity.setContentView(
                parent,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            root = activity.findViewById(android.R.id.content)
        }
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
        retVal = false

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
        retVal = false

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
        retVal = false

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
            disallowInterceptRequester.invoke(true)
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
            disallowInterceptRequester.invoke(true)
            root.dispatchTouchEvent(move1)
            eventStringLog.clear()
            disallowInterceptRequester.invoke(false)
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
            disallowInterceptRequester.invoke(true)
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
}