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
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.SimpleRow
import androidx.compose.ui.Wrap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.padding
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.min

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
                                    wrap1Position = coordinates.positionInWindow().x
                                    latch.countDown()
                                }
                            )
                        } else {
                            Wrap(
                                minWidth = size,
                                minHeight = size,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    wrap2Position = coordinates.positionInWindow().x
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
                    content = {
                        Wrap(
                            minWidth = 10,
                            minHeight = 10
                        ) {
                            Wrap(
                                minWidth = 10,
                                minHeight = 10,
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    childGlobalPosition = coordinates.positionInRoot()
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
                    content = {
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
                    Modifier.padding(5).then(
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
            while (root.parentLayoutCoordinates != null) {
                root = root.parentLayoutCoordinates!!
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
                    Modifier.padding(5).then(
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
            while (root.parentLayoutCoordinates != null) {
                root = root.parentLayoutCoordinates!!
            }

            assertEquals(Rect(0f, 0f, 20f, 20f), root.boundsInParent)
        }
    }

    @Test
    fun onPositionedIsCalledWhenComposeContainerIsScrolled() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null
        var scrollView: ScrollView? = null
        var view: ComposeView? = null

        rule.runOnUiThread {
            scrollView = ScrollView(rule.activity)
            activity.setContentView(scrollView, ViewGroup.LayoutParams(100, 100))
            view = ComposeView(rule.activity)
            scrollView!!.addView(view)
            view?.setContent {
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
            view?.getLocationOnScreen(position)
        }
        assertEquals(position[1].toFloat(), coordinates!!.positionInWindow().y)
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
            val view = ComposeView(rule.activity)
            linearLayout.addView(view, ViewGroup.LayoutParams(100, 100))
            view.setContent {
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
        val startY = coordinates!!.positionInWindow().y
        positionedLatch = CountDownLatch(1)

        rule.runOnUiThread {
            topView!!.visibility = View.GONE
        }

        assertTrue(
            "OnPositioned is not called when the container moved",
            positionedLatch.await(1, TimeUnit.SECONDS)
        )
        assertEquals(startY - 100f, coordinates!!.positionInWindow().y)
    }

    @Test
    fun onPositionedCalledInDifferentPartsOfHierarchy() {
        var positionedLatch = CountDownLatch(2)
        var coordinates1: LayoutCoordinates? = null
        var coordinates2: LayoutCoordinates? = null
        var size by mutableStateOf(10f)

        rule.runOnUiThread {
            activity.setContent {
                with(AmbientDensity.current) {
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

    @Test
    fun simplePadding() {
        val paddingLeftPx = 100.0f
        val paddingTopPx = 120.0f
        var realLeft: Float? = null
        var realTop: Float? = null

        val positionedLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                with(AmbientDensity.current) {
                    Box(
                        Modifier.fillMaxSize()
                            .padding(start = paddingLeftPx.toDp(), top = paddingTopPx.toDp())
                            .onGloballyPositioned {
                                realLeft = it.positionInParent.x
                                realTop = it.positionInParent.y
                                positionedLatch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertThat(paddingLeftPx).isEqualTo(realLeft)
        assertThat(paddingTopPx).isEqualTo(realTop)
    }
    @Test
    fun nestedLayoutCoordinates() {
        val firstPaddingPx = 10f
        val secondPaddingPx = 20f
        val thirdPaddingPx = 30f
        var gpCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null

        val positionedLatch = CountDownLatch(2)
        rule.runOnUiThread {
            activity.setContent {
                with(AmbientDensity.current) {
                    Box(
                        Modifier.padding(start = firstPaddingPx.toDp()).then(
                            Modifier.onGloballyPositioned {
                                gpCoordinates = it
                                positionedLatch.countDown()
                            }
                        )
                    ) {
                        Box(Modifier.padding(start = secondPaddingPx.toDp())) {
                            Box(
                                Modifier.fillMaxSize()
                                    .padding(start = thirdPaddingPx.toDp())
                                    .onGloballyPositioned {
                                        childCoordinates = it
                                        positionedLatch.countDown()
                                    }
                            )
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        // global position
        val gPos = childCoordinates!!.localToWindow(Offset.Zero).x
        assertThat(gPos).isEqualTo((firstPaddingPx + secondPaddingPx + thirdPaddingPx))
        // Position in grandparent Px(value=50.0)
        val gpPos = gpCoordinates!!.localPositionOf(childCoordinates!!, Offset.Zero).x
        assertThat(gpPos).isEqualTo((secondPaddingPx + thirdPaddingPx))
        // local position
        assertThat(childCoordinates!!.positionInParent.x).isEqualTo(thirdPaddingPx)
    }

    @Test
    fun globalCoordinatesAreInActivityCoordinates() {
        val padding = 30
        val localPosition = androidx.compose.ui.geometry.Offset.Zero
        val framePadding = Offset(padding.toFloat(), padding.toFloat())
        var realGlobalPosition: Offset? = null
        var realLocalPosition: Offset? = null
        var frameGlobalPosition: Offset? = null

        val positionedLatch = CountDownLatch(1)
        rule.runOnUiThread {
            val composeView = ComposeView(activity)
            composeView.setPadding(padding, padding, padding, padding)
            activity.setContentView(composeView)

            val position = IntArray(2)
            composeView.getLocationOnScreen(position)
            frameGlobalPosition = Offset(position[0].toFloat(), position[1].toFloat())

            composeView.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        realGlobalPosition = it.localToWindow(localPosition)
                        realLocalPosition = it.windowToLocal(
                            framePadding + frameGlobalPosition!!
                        )
                        positionedLatch.countDown()
                    }
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertThat(realGlobalPosition).isEqualTo(frameGlobalPosition!! + framePadding)
        assertThat(realLocalPosition).isEqualTo(localPosition)
    }

    @Test
    fun justAddedOnPositionedCallbackFiredWithoutLayoutChanges() {
        val needCallback = mutableStateOf(false)

        val positionedLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                val modifier = if (needCallback.value) {
                    Modifier.onGloballyPositioned { positionedLatch.countDown() }
                } else {
                    Modifier
                }
                Box(modifier.fillMaxSize())
            }
        }

        rule.runOnUiThread { needCallback.value = true }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRepositionTriggersCallback() {
        val left = mutableStateOf(30)
        var realLeft: Float? = null

        var positionedLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                with(AmbientDensity.current) {
                    Box {
                        Box(
                            Modifier.onGloballyPositioned {
                                realLeft = it.positionInParent.x
                                positionedLatch.countDown()
                            }
                                .fillMaxSize()
                                .padding(start = left.value.toDp()),
                        )
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        rule.runOnUiThread { left.value = 40 }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        assertThat(realLeft).isEqualTo(40)
    }

    @Test
    fun testGrandParentRepositionTriggersChildrenCallback() {
        // when we reposition any parent layout is causes the change in global
        // position of all the children down the tree(for example during the scrolling).
        // children should be able to react on this change.
        val left = mutableStateOf(20)
        var realLeft: Float? = null
        var positionedLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                with(AmbientDensity.current) {
                    Box {
                        Offset(left) {
                            Box(Modifier.size(10.toDp())) {
                                Box(Modifier.size(10.toDp())) {
                                    Box(
                                        Modifier.onGloballyPositioned {
                                            realLeft = it.positionInRoot().x
                                            positionedLatch.countDown()
                                        }.size(10.toDp())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        rule.runOnUiThread { left.value = 40 }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        assertThat(realLeft).isEqualTo(40)
    }

    @Test
    fun testAlignmentLinesArePresent() {
        val latch = CountDownLatch(1)
        val line = VerticalAlignmentLine(::min)
        val lineValue = 10
        rule.runOnUiThread {
            activity.setContent {
                val onPositioned = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                    assertEquals(1, coordinates.providedAlignmentLines.size)
                    assertEquals(lineValue, coordinates[line])
                    latch.countDown()
                }
                Layout(modifier = onPositioned, content = { }) { _, _ ->
                    layout(0, 0, mapOf(line to lineValue)) { }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }
}

@Composable
fun DelayedMeasure(
    size: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = emptyContent()
) {
    Layout(content = content, modifier = modifier) { measurables, _ ->
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

@Composable
private fun Offset(sizeModel: State<Int>, content: @Composable () -> Unit) {
    // simple copy of Padding which doesn't recompose when the size changes
    Layout(content) { measurables, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            measurables.first().measure(constraints).placeRelative(sizeModel.value, 0)
        }
    }
}
