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

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.PaddingModifier
import androidx.compose.ui.SimpleRow
import androidx.compose.ui.Wrap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnGloballyPositionedTest {

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
    fun handlesChildrenNodeMoveCorrectly() {
        val size = 50
        var index by mutableStateOf(0)
        var latch = CountDownLatch(2)
        var wrap1Position = 0f
        var wrap2Position = 0f
        rule.runOnUiThread {
            activity.setContent {
                SimpleRow {
                    for (i in 0 until 2) {
                        if (index == i) {
                            Wrap(
                                minWidth = size,
                                minHeight = size,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    wrap1Position = coordinates.globalPosition.x
                                    latch.countDown()
                                }
                            )
                        } else {
                            Wrap(
                                minWidth = size,
                                minHeight = size,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    wrap2Position = coordinates.globalPosition.x
                                    latch.countDown()
                                }
                            )
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(0f, wrap1Position)
        assertEquals(size.toFloat(), wrap2Position)
        latch = CountDownLatch(2)
        rule.runOnUiThread {
            index = 1
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(size.toFloat(), wrap1Position)
        assertEquals(0f, wrap2Position)
    }

    @Test
    fun callbacksAreCalledWhenChildResized() {
        var size by mutableStateOf(10)
        var realChildSize = 0
        var childLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                AtLeastSize(size = 20) {
                    Wrap(
                        minWidth = size, minHeight = size,
                        modifier = Modifier.onGloballyPositioned {
                            realChildSize = it.size.width
                            childLatch.countDown()
                        }
                    )
                }
            }
        }

        assertTrue(childLatch.await(1, TimeUnit.SECONDS))
        assertEquals(10, realChildSize)

        childLatch = CountDownLatch(1)
        rule.runOnUiThread {
            size = 15
        }

        assertTrue(childLatch.await(1, TimeUnit.SECONDS))
        assertEquals(15, realChildSize)
    }

    @Test
    fun callbackCalledForChildWhenParentMoved() {
        var position by mutableStateOf(0)
        var childGlobalPosition = Offset(0f, 0f)
        var latch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    measureBlock = { measurables, constraints ->
                        layout(10, 10) {
                            measurables[0].measure(constraints).place(position, 0)
                        }
                    },
                    children = {
                        Wrap(
                            minWidth = 10,
                            minHeight = 10
                        ) {
                            Wrap(
                                minWidth = 10,
                                minHeight = 10,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    childGlobalPosition = coordinates.positionInRoot
                                    latch.countDown()
                                }
                            )
                        }
                    }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            position = 10
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(Offset(10f, 0f), childGlobalPosition)
    }

    @Test
    fun callbacksAreCalledOnlyForPositionedChildren() {
        val latch = CountDownLatch(1)
        var wrap1OnPositionedCalled = false
        var wrap2OnPositionedCalled = false
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    measureBlock = { measurables, constraints ->
                        layout(10, 10) {
                            measurables[1].measure(constraints).place(0, 0)
                        }
                    },
                    children = {
                        Wrap(
                            minWidth = 10,
                            minHeight = 10,
                            modifier = Modifier.onGloballyPositioned {
                                wrap1OnPositionedCalled = true
                            }
                        )
                        Wrap(
                            minWidth = 10,
                            minHeight = 10,
                            modifier = Modifier.onGloballyPositioned {
                                wrap2OnPositionedCalled = true
                            }
                        ) {
                            Wrap(
                                minWidth = 10,
                                minHeight = 10,
                                modifier = Modifier.onGloballyPositioned {
                                    latch.countDown()
                                }
                            )
                        }
                    }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertFalse(wrap1OnPositionedCalled)
        assertTrue(wrap2OnPositionedCalled)
    }

    @Test
    fun testPositionInParent() {
        val positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null

        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    10,
                    PaddingModifier(5).then(
                        Modifier.onGloballyPositioned {
                            coordinates = it
                            positionedLatch.countDown()
                        }
                    )
                ) {
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            assertEquals(Offset(5f, 5f), coordinates!!.positionInParent)

            var root = coordinates!!
            while (root.parentCoordinates != null) {
                root = root.parentCoordinates!!
            }

            assertEquals(Offset.Zero, root.positionInParent)
        }
    }

    @Test
    fun testBoundsInParent() {
        val positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null

        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    10,
                    PaddingModifier(5).then(
                        Modifier.onGloballyPositioned {
                            coordinates = it
                            positionedLatch.countDown()
                        }
                    )
                ) {
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            assertEquals(Rect(5f, 5f, 15f, 15f), coordinates!!.boundsInParent)

            var root = coordinates!!
            while (root.parentCoordinates != null) {
                root = root.parentCoordinates!!
            }

            assertEquals(Rect(0f, 0f, 20f, 20f), root.boundsInParent)
        }
    }

    @Test
    fun onPositionedIsCalledWhenComposeContainerIsScrolled() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null
        var scrollView: ScrollView? = null
        var frameLayout: FrameLayout? = null

        rule.runOnUiThread {
            scrollView = ScrollView(rule.activity)
            activity.setContentView(scrollView, ViewGroup.LayoutParams(100, 100))
            frameLayout = FrameLayout(rule.activity)
            scrollView!!.addView(frameLayout)
            frameLayout?.setContent(Recomposer.current()) {
                Layout(
                    {},
                    modifier = Modifier.onGloballyPositioned {
                        coordinates = it
                        positionedLatch.countDown()
                    }
                ) { _, _ ->
                    layout(100, 200) {}
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        positionedLatch = CountDownLatch(1)

        rule.runOnUiThread {
            scrollView!!.scrollBy(0, 50)
        }

        assertTrue(
            "OnPositioned is not called when the container scrolled",
            positionedLatch.await(1, TimeUnit.SECONDS)
        )
        // There is a bug on older devices where the location isn't exactly 50
        // pixels off of the start position, even though we've scrolled by 50 pixels.
        val position = intArrayOf(0, 0)
        rule.runOnUiThread {
            frameLayout?.getLocationOnScreen(position)
        }
        assertEquals(position[1].toFloat(), coordinates!!.globalPosition.y)
    }

    @Test
    fun onPositionedIsCalledWhenComposeContainerPositionChanged() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null
        var topView: View? = null

        rule.runOnUiThread {
            val linearLayout = LinearLayout(rule.activity)
            linearLayout.orientation = LinearLayout.VERTICAL
            activity.setContentView(linearLayout, ViewGroup.LayoutParams(100, 200))
            topView = View(rule.activity)
            linearLayout.addView(topView!!, ViewGroup.LayoutParams(100, 100))
            val frameLayout = FrameLayout(rule.activity)
            linearLayout.addView(frameLayout, ViewGroup.LayoutParams(100, 100))
            frameLayout.setContent(Recomposer.current()) {
                Layout(
                    {},
                    modifier = Modifier.onGloballyPositioned {
                        coordinates = it
                        positionedLatch.countDown()
                    }
                ) { _, constraints ->
                    layout(constraints.maxWidth, constraints.maxHeight) {}
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        val startY = coordinates!!.globalPosition.y
        positionedLatch = CountDownLatch(1)

        rule.runOnUiThread {
            topView!!.visibility = View.GONE
        }

        assertTrue(
            "OnPositioned is not called when the container moved",
            positionedLatch.await(1, TimeUnit.SECONDS)
        )
        assertEquals(startY - 100f, coordinates!!.globalPosition.y)
    }

    @Test
    fun onPositionedCalledInDifferentPartsOfHierarchy() {
        var positionedLatch = CountDownLatch(2)
        var coordinates1: LayoutCoordinates? = null
        var coordinates2: LayoutCoordinates? = null
        var size by mutableStateOf(10f)

        rule.runOnUiThread {
            activity.setContent {
                with(DensityAmbient.current) {
                    DelayedMeasure(50) {
                        Box(Modifier.size(25.toDp())) {
                            Box(
                                Modifier.size(size.toDp())
                                    .onGloballyPositioned {
                                        coordinates1 = it
                                        positionedLatch.countDown()
                                    }
                            )
                        }
                        Box(Modifier.size(25.toDp())) {
                            Box(
                                Modifier.size(size.toDp())
                                    .onGloballyPositioned {
                                        coordinates2 = it
                                        positionedLatch.countDown()
                                    }
                            )
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        assertNotNull(coordinates1)
        assertNotNull(coordinates2)
        positionedLatch = CountDownLatch(2)

        rule.runOnUiThread {
            size = 15f
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }
}

@Composable
fun DelayedMeasure(
    size: Int,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit = emptyContent()
) {
    Layout(children = children, modifier = modifier) { measurables, _ ->
        layout(size, size) {
            val newConstraints = Constraints(maxWidth = size, maxHeight = size)
            val placeables = measurables.map { m ->
                m.measure(newConstraints)
            }
            placeables.forEach { child ->
                child.place(0, 0)
            }
        }
    }
}