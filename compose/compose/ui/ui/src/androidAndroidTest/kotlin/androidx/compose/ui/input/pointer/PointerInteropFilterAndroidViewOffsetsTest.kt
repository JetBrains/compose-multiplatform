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
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests that pointer offsets are correct when a pointer is dispatched from Android through
// Compose and back into Android and each layer offsets the pointer during dispatch.
@MediumTest
@RunWith(AndroidJUnit4::class)
class PointerInteropFilterAndroidViewOffsetsTest {

    private lateinit var five: View
    private val theHitListener: () -> Unit = mock()

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setup() {
        rule.activityRule.scenario.onActivity { activity ->

            // one: Android View that is the touch target, inside
            // two: Android View with 1x2 padding, inside
            // three: Compose Box with 2x12 padding, inside
            // four: Android View with 3x13 padding, inside
            // five: Android View with 4x14 padding
            //
            // With all of the padding, "one" is at 10 x 50 relative to "five" and the tests
            // dispatch MotionEvents to "five".

            val one = CustomView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(1, 1)
                hitListener = theHitListener
            }

            val two = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(1, 11, 0, 0)
                addView(one)
            }

            val four = ComposeView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(3, 13, 0, 0)
                setContent {
                    with(LocalDensity.current) {
                        // Box is "three"
                        Box(
                            Modifier.padding(start = (2f / density).dp, top = (12f / density).dp)
                        ) {
                            AndroidView({ two })
                        }
                    }
                }
            }

            five = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setPadding(4, 14, 0, 0)
                addView(four)
            }

            activity.setContentView(five)
        }
    }

    @Test
    fun uiClick_inside_hits() {
        uiClick(10, 50, true)
    }

    @Test
    fun uiClick_justOutside_misses() {
        uiClick(9, 50, false)
        uiClick(10, 49, false)
        uiClick(11, 50, false)
        uiClick(10, 51, false)
    }

    // Gets reused to should always clean up state.
    private fun uiClick(x: Int, y: Int, hits: Boolean) {
        clearInvocations(theHitListener)

        rule.activityRule.scenario.onActivity {
            val down =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(1)),
                    arrayOf(PointerCoords(x.toFloat(), y.toFloat())),
                    five
                )
            val up =
                MotionEvent(
                    10,
                    ACTION_UP,
                    1,
                    0,
                    arrayOf(PointerProperties(1)),
                    arrayOf(PointerCoords(x.toFloat(), y.toFloat())),
                    five
                )

            five.dispatchTouchEvent(down)
            five.dispatchTouchEvent(up)
        }

        if (hits) {
            verify(theHitListener, times(2)).invoke()
        } else {
            verify(theHitListener, never()).invoke()
        }
    }
}

private class CustomView(context: Context) : View(context) {
    lateinit var hitListener: () -> Unit

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        hitListener()
        return true
    }
}