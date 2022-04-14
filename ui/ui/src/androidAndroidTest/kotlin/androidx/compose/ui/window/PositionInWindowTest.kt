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
package androidx.compose.ui.window

import android.os.Build
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.findAndroidComposeView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class PositionInWindowTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    lateinit var activity: ComponentActivity

    @Before
    fun setup() {
        rule.activityRule.scenario.onActivity { activity = it }
    }

    // Make sure that the position in the window doesn't change when the window position changes.
    @Test
    fun positionInWindow() {
        var coordinates: LayoutCoordinates? = null
        var size by mutableStateOf(10)
        rule.runOnUiThread {
            val window = activity.window
            val layoutParams = window.attributes
            layoutParams.x = 0
            layoutParams.y = 0
            layoutParams.width = 100
            layoutParams.height = 100
            window.attributes = layoutParams
        }
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.requiredSize(size.toDp()).onGloballyPositioned { coordinates = it })
            }
        }

        var position = Offset.Zero
        rule.runOnIdle {
            position = coordinates!!.positionInWindow()
            size = 12
            val window = activity.window
            val layoutParams = window.attributes
            layoutParams.x = 10
            layoutParams.y = 10
            layoutParams.width = 100
            layoutParams.height = 100
            window.attributes = layoutParams
        }

        rule.runOnIdle {
            val newPosition = coordinates!!.positionInWindow()
            assertThat(newPosition).isEqualTo(position)
        }
    }

    // Make sure that the position in the window changes when the decor view's scroll changes.
    @Test
    fun positionInWindowOnScrollDecorView() {
        var coordinates: LayoutCoordinates? = null
        var latch = CountDownLatch(1)

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .requiredSize(10.toDp())
                        .onGloballyPositioned {
                            coordinates = it
                            latch.countDown()
                        }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        latch = CountDownLatch(1)

        var position = Offset.Zero
        rule.runOnIdle {
            position = coordinates!!.positionInWindow()
            activity.window.decorView.scrollY = -100
        }

        assertTrue(
            "OnPositioned is not called when the decorView scroll changed",
            latch.await(1, TimeUnit.SECONDS)
        )

        rule.runOnIdle {
            val newPosition = coordinates!!.positionInWindow()
            assertThat(newPosition.y).isEqualTo(position.y + 100)
        }
    }

    // Make sure that the position in the window changes when the decor view's scroll changes.
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun positionInWindowOnScrollWindow() {
        var coordinates: LayoutCoordinates? = null
        rule.runOnUiThread {
            val window = activity.window
            val layoutParams = window.attributes
            layoutParams.x = 0
            layoutParams.y = 0
            layoutParams.width = 20
            layoutParams.height = 10
            window.attributes = layoutParams

            val composeView = ComposeView(activity)
            val composeViewLayoutParams = ViewGroup.LayoutParams(20, 20)
            activity.setContentView(composeView, composeViewLayoutParams)

            composeView.setContent {
                with(LocalDensity.current) {
                    Box(Modifier.requiredSize(20.toDp()).onGloballyPositioned { coordinates = it })
                }
            }
        }

        var position = Offset.Zero
        rule.runOnIdle {
            position = coordinates!!.positionInWindow()

            // Can't easily scroll the window as if the window insets have changed, so
            // just directly modify the properties of ViewRootImpl
            val viewRootImpl = activity.window.decorView.parent
            val viewRootImplClass = viewRootImpl.javaClass
            val scrollYField = viewRootImplClass.getDeclaredField("mScrollY")
            scrollYField.isAccessible = true
            scrollYField.set(viewRootImpl, -10)
            val curScrollYField = viewRootImplClass.getDeclaredField("mCurScrollY")
            curScrollYField.isAccessible = true
            curScrollYField.set(viewRootImpl, -10)
        }

        rule.runOnIdle {
            val newPosition = coordinates!!.positionInWindow()
            assertThat(newPosition.y).isEqualTo(position.y + 10)
        }
    }

    // Make sure that the position in the window changes when the decor view's position changes.
    @Test
    fun positionInWindowWithViewOffset() {
        var coordinates: LayoutCoordinates? = null
        rule.runOnUiThread {
            val composeView = ComposeView(activity)
            val composeViewLayoutParams = ViewGroup.LayoutParams(20, 20)
            activity.setContentView(composeView, composeViewLayoutParams)

            composeView.setContent {
                with(LocalDensity.current) {
                    Box(Modifier.requiredSize(20.toDp()).onGloballyPositioned { coordinates = it })
                }
            }
        }

        var position = Offset.Zero
        rule.runOnIdle {
            position = coordinates!!.positionInWindow()
        }

        rule.runOnIdle {
            val decorView = activity.window.decorView as ViewGroup
            val androidComposeView = findAndroidComposeView(decorView) as AndroidComposeView
            // Reset this just in case something else has forced a redraw this frame.
            // This should fix test flakes.
            androidComposeView.lastMatrixRecalculationAnimationTime = -1
            decorView.offsetTopAndBottom(10)
            val newPosition = coordinates!!.positionInWindow()
            assertThat(newPosition.y).isEqualTo(position.y + 10)
        }
    }

    @Test
    fun positionInMovingPopup() {
        val smallBoxTag = "smallBox"
        var offset by mutableStateOf(Offset.Zero)
        val endOffsetPx = 200f

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                Popup(offset = offset.round()) {
                    Box(
                        Modifier.size(10.dp, 10.dp)
                            .background(Color.Black)
                            .testTag(smallBoxTag)
                            .pointerInput(Unit) {
                                forEachGesture {
                                    awaitPointerEventScope {
                                        val down = awaitFirstDown()
                                        var previous = down.position
                                        drag(down.id) {
                                            it.consume()
                                            offset += it.position - previous
                                            previous = it.position
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }

        rule.onNodeWithTag(smallBoxTag)
            .performTouchInput {
                swipe(Offset.Zero, Offset(endOffsetPx, endOffsetPx))
            }

        rule.runOnIdle {
            assertThat(offset).isEqualTo(Offset(endOffsetPx, endOffsetPx))
        }
    }
}
