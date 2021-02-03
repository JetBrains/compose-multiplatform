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

package androidx.compose.ui.layout

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class RootNodeLayoutTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    @Test
    fun rootMeasuresWithZeroMinConstraints() {
        var realConstraints: Constraints? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                Layout({}) { _, constraints ->
                    realConstraints = constraints
                    latch.countDown()
                    layout(10, 10) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(realConstraints)
        assertEquals(0, realConstraints!!.minWidth)
        assertEquals(0, realConstraints!!.minHeight)
    }

    @Test
    fun rootPositionsInTheTopLeftCorner() {
        var coordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    {},
                    Modifier.onGloballyPositioned {
                        coordinates = it
                        latch.countDown()
                    }
                ) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(coordinates)
        assertEquals(
            Rect(left = 0f, top = 0f, right = 10f, bottom = 10f),
            coordinates!!.boundsInRoot()
        )
    }

    @Test
    fun viewMeasuredCorrectlyWithWrapContent() {
        val latch = CountDownLatch(1)
        val child = ComposeView(activity)
        rule.runOnUiThread {
            val parent = FrameLayout(activity)
            parent.addView(
                child,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            activity.setContentView(parent)
            child.setContent {
                Layout(
                    {},
                    Modifier.onGloballyPositioned {
                        latch.countDown()
                    }
                ) { _, _ ->
                    layout(10, 15) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(10, child.measuredWidth)
        assertEquals(15, child.measuredHeight)
    }

    @Test
    fun viewMeasuredCorrectlyWithMatchParent() {
        val latch = CountDownLatch(1)
        val child = ComposeView(activity)
        val parent = FrameLayout(activity)
        rule.runOnUiThread {
            parent.addView(
                child,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            activity.setContentView(parent)
            child.setContent {
                Layout(
                    {},
                    Modifier.fillMaxSize().onGloballyPositioned {
                        latch.countDown()
                    }
                ) { _, _ ->
                    layout(10, 15) {}
                }
            }
        }

        val composeView = child.getChildAt(0)
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotEquals(10, composeView.measuredWidth)
        assertNotEquals(15, composeView.measuredHeight)
        assertEquals(parent.measuredWidth, composeView.measuredWidth)
        assertEquals(parent.measuredHeight, composeView.measuredHeight)
    }
}