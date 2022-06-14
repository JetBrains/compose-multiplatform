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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.SimpleRow
import androidx.compose.ui.Wrap
import androidx.compose.ui.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.padding
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.sqrt

@MediumTest
@RunWith(AndroidJUnit4::class)
class OnGloballyPositionedTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun handlesChildrenNodeMoveCorrectly() {
        val size = 50
        var index by mutableStateOf(0)
        var wrap1Position = 0f
        var wrap2Position = 0f
        rule.setContent {
            SimpleRow {
                for (i in 0 until 2) {
                    if (index == i) {
                        Wrap(
                            minWidth = size,
                            minHeight = size,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                wrap1Position = coordinates.positionInWindow().x
                            }
                        )
                    } else {
                        Wrap(
                            minWidth = size,
                            minHeight = size,
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                wrap2Position = coordinates.positionInWindow().x
                            }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0f, wrap1Position)
            assertEquals(size.toFloat(), wrap2Position)
            index = 1
        }

        rule.runOnIdle {
            assertEquals(size.toFloat(), wrap1Position)
            assertEquals(0f, wrap2Position)
        }
    }

    @Test
    fun callbacksAreCalledWhenChildResized() {
        var size by mutableStateOf(10)
        var realChildSize = 0
        rule.setContent {
            AtLeastSize(size = 20) {
                Wrap(
                    minWidth = size, minHeight = size,
                    modifier = Modifier.onGloballyPositioned {
                        realChildSize = it.size.width
                    }
                )
            }
        }

        rule.runOnIdle {
            assertEquals(10, realChildSize)
            size = 15
        }

        rule.runOnIdle {
            assertEquals(15, realChildSize)
        }
    }

    @Test
    fun callbackCalledForChildWhenParentMoved() {
        var position by mutableStateOf(0)
        var childGlobalPosition = Offset(0f, 0f)
        var latch = CountDownLatch(1)
        rule.setContent {
            Layout(
                measurePolicy = { measurables, constraints ->
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
        rule.setContent {
            Layout(
                measurePolicy = { measurables, constraints ->
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

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertFalse(wrap1OnPositionedCalled)
        assertTrue(wrap2OnPositionedCalled)
    }

    @Test
    fun testPositionInParent() {
        val positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null

        rule.setContent {
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
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            assertEquals(Offset(5f, 5f), coordinates!!.positionInParent())

            var root = coordinates!!
            while (root.parentLayoutCoordinates != null) {
                root = root.parentLayoutCoordinates!!
            }

            assertEquals(Offset.Zero, root.positionInParent())
        }
    }

    @Test
    fun testBoundsInParent() {
        val positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null

        rule.setContent {
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
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            assertEquals(Rect(5f, 5f, 15f, 15f), coordinates!!.boundsInParent())

            var root = coordinates!!
            while (root.parentLayoutCoordinates != null) {
                root = root.parentLayoutCoordinates!!
            }

            assertEquals(Rect(0f, 0f, 20f, 20f), root.boundsInParent())
        }
    }

    @Test
    fun onPositionedIsCalledWhenComposeContainerIsScrolled() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null
        var scrollView: ScrollView? = null
        lateinit var view: ComposeView

        rule.runOnUiThread {
            scrollView = ScrollView(rule.activity)
            rule.activity.setContentView(scrollView, ViewGroup.LayoutParams(100, 100))
            view = ComposeView(rule.activity)
            scrollView!!.addView(view)
            view.setContent {
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

        rule.waitForIdle()

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        positionedLatch = CountDownLatch(1)

        rule.runOnIdle {
            coordinates = null
            scrollView!!.scrollBy(0, 50)
        }

        assertTrue(
            "OnPositioned is not called when the container scrolled",
            positionedLatch.await(1, TimeUnit.SECONDS)
        )

        rule.runOnIdle {
            assertEquals(view.getYInWindow(), coordinates!!.positionInWindow().y)
        }
    }

    @Test
    fun onPositionedCalledWhenLayerChanged() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null
        var offsetX by mutableStateOf(0f)

        rule.setContent {
            Layout(
                {},
                modifier = Modifier.graphicsLayer {
                    translationX = offsetX
                }.onGloballyPositioned {
                    coordinates = it
                    positionedLatch.countDown()
                }
            ) { _, _ ->
                layout(100, 200) {}
            }
        }

        rule.waitForIdle()

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        positionedLatch = CountDownLatch(1)

        rule.runOnIdle {
            coordinates = null
            offsetX = 5f
        }

        assertTrue(
            "OnPositioned is not called when the container scrolled",
            positionedLatch.await(1, TimeUnit.SECONDS)
        )

        rule.runOnIdle {
            assertEquals(5f, coordinates!!.positionInRoot().x)
        }
    }

    private fun View.getYInWindow(): Float {
        var offset = 0f
        val parentView = parent
        if (parentView is View) {
            offset += parentView.getYInWindow()
            offset -= scrollY.toFloat()
            offset += top.toFloat()
        }
        return offset
    }

    @Test
    fun onPositionedIsCalledWhenComposeContainerPositionChanged() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: LayoutCoordinates? = null
        var topView: View? = null

        rule.runOnUiThread {
            val linearLayout = LinearLayout(rule.activity)
            linearLayout.orientation = LinearLayout.VERTICAL
            rule.activity.setContentView(linearLayout, ViewGroup.LayoutParams(100, 200))
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

        rule.waitForIdle()

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        val startY = coordinates!!.positionInWindow().y
        positionedLatch = CountDownLatch(1)

        rule.runOnIdle {
            topView!!.visibility = View.GONE
        }

        assertTrue(
            "OnPositioned is not called when the container moved",
            positionedLatch.await(1, TimeUnit.SECONDS)
        )

        rule.runOnIdle {
            assertEquals(startY - 100f, coordinates!!.positionInWindow().y)
        }
    }

    @Test
    fun onPositionedCalledInDifferentPartsOfHierarchy() {
        var coordinates1: LayoutCoordinates? = null
        var coordinates2: LayoutCoordinates? = null
        var size by mutableStateOf(10f)

        rule.setContent {
            with(LocalDensity.current) {
                DelayedMeasure(50) {
                    Box(Modifier.requiredSize(25.toDp())) {
                        Box(
                            Modifier.requiredSize(size.toDp())
                                .onGloballyPositioned {
                                    coordinates1 = it
                                }
                        )
                    }
                    Box(Modifier.requiredSize(25.toDp())) {
                        Box(
                            Modifier.requiredSize(size.toDp())
                                .onGloballyPositioned {
                                    coordinates2 = it
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(coordinates1)
            assertNotNull(coordinates2)
            coordinates1 = null
            coordinates2 = null
            size = 15f
        }

        rule.runOnIdle {
            assertNotNull(coordinates1)
            assertNotNull(coordinates2)
        }
    }

    @Test
    fun simplePadding() {
        val paddingLeftPx = 100.0f
        val paddingTopPx = 120.0f
        var realLeft: Float? = null
        var realTop: Float? = null

        val positionedLatch = CountDownLatch(1)
        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.fillMaxSize()
                        .padding(start = paddingLeftPx.toDp(), top = paddingTopPx.toDp())
                        .onGloballyPositioned {
                            realLeft = it.positionInParent().x
                            realTop = it.positionInParent().y
                            positionedLatch.countDown()
                        }
                )
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
        rule.setContent {
            with(LocalDensity.current) {
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
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        // global position
        val gPos = childCoordinates!!.localToWindow(Offset.Zero).x
        assertThat(gPos).isEqualTo((firstPaddingPx + secondPaddingPx + thirdPaddingPx))
        // Position in grandparent Px(value=50.0)
        val gpPos = gpCoordinates!!.localPositionOf(childCoordinates!!, Offset.Zero).x
        assertThat(gpPos).isEqualTo((secondPaddingPx + thirdPaddingPx))
        // local position
        assertThat(childCoordinates!!.positionInParent().x).isEqualTo(thirdPaddingPx)
    }

    @FlakyTest(bugId = 213889751)
    @Test
    fun globalCoordinatesAreInActivityCoordinates() {
        val padding = 30
        val localPosition = Offset.Zero
        val framePadding = Offset(padding.toFloat(), padding.toFloat())
        var realGlobalPosition: Offset? = null
        var realLocalPosition: Offset? = null
        var frameGlobalPosition: Offset? = null

        val positionedLatch = CountDownLatch(1)
        rule.runOnUiThread {
            val composeView = ComposeView(rule.activity)
            composeView.setPadding(padding, padding, padding, padding)
            rule.activity.setContentView(composeView)

            composeView.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        val position = IntArray(2)
                        composeView.getLocationInWindow(position)
                        frameGlobalPosition = Offset(position[0].toFloat(), position[1].toFloat())

                        realGlobalPosition = it.localToWindow(localPosition)
                        realLocalPosition = it.windowToLocal(
                            framePadding + frameGlobalPosition!!
                        )

                        positionedLatch.countDown()
                    }
                )
            }
        }

        rule.waitForIdle()

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnIdle {
            assertThat(realGlobalPosition).isEqualTo(frameGlobalPosition!! + framePadding)
            assertThat(realLocalPosition).isEqualTo(localPosition)
        }
    }

    @Test
    fun justAddedOnPositionedCallbackFiredWithoutLayoutChanges() {
        val needCallback = mutableStateOf(false)

        var positionedCalled = false
        rule.setContent {
            val modifier = if (needCallback.value) {
                Modifier.onGloballyPositioned { positionedCalled = true }
            } else {
                Modifier
            }
            Box(modifier.fillMaxSize())
        }

        rule.runOnIdle { needCallback.value = true }

        rule.runOnIdle {
            assertThat(positionedCalled).isTrue()
        }
    }

    @Test
    fun testRepositionTriggersCallback() {
        val left = mutableStateOf(30)
        var realLeft: Float? = null

        rule.setContent {
            with(LocalDensity.current) {
                Box {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(start = left.value.toDp())
                            .onGloballyPositioned {
                                realLeft = it.positionInParent().x
                            }
                    )
                }
            }
        }

        rule.runOnIdle { left.value = 40 }

        rule.runOnIdle {
            assertThat(realLeft).isEqualTo(40)
        }
    }

    @Test
    fun testGrandParentRepositionTriggersChildrenCallback() {
        // when we reposition any parent layout is causes the change in global
        // position of all the children down the tree(for example during the scrolling).
        // children should be able to react on this change.
        val left = mutableStateOf(20)
        var realLeft: Float? = null
        var positionedLatch = CountDownLatch(1)
        rule.setContent {
            with(LocalDensity.current) {
                Box {
                    Offset(left) {
                        Box(Modifier.requiredSize(10.toDp())) {
                            Box(Modifier.requiredSize(10.toDp())) {
                                Box(
                                    Modifier.onGloballyPositioned {
                                        realLeft = it.positionInRoot().x
                                        positionedLatch.countDown()
                                    }.requiredSize(10.toDp())
                                )
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
        val line1 = VerticalAlignmentLine(::min)
        val line2 = HorizontalAlignmentLine(::min)
        val lineValue = 10
        rule.setContent {
            val onPositioned = Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                assertEquals(2, coordinates.providedAlignmentLines.size)
                assertEquals(lineValue, coordinates[line1])
                assertEquals(lineValue, coordinates[line2])
                latch.countDown()
            }
            val lineProvider = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(0, 0, mapOf(line2 to lineValue)) {
                    placeable.place(0, 0)
                }
            }
            Layout(modifier = onPositioned.then(lineProvider), content = { }) { _, _ ->
                layout(0, 0, mapOf(line1 to lineValue)) { }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testLayerBoundsPositionInRotatedView() {
        var coords: LayoutCoordinates? = null
        var view: View? = null
        rule.setContent {
            view = LocalView.current
            FixedSize(
                30,
                Modifier.padding(10).onGloballyPositioned {
                    coords = it
                }
            ) { /* no-op */ }
        }

        val composeView = view as AndroidComposeView
        rule.runOnUiThread {
            // rotate the view so that it no longer aligns squarely
            composeView.rotation = 45f
            composeView.pivotX = 0f
            composeView.pivotY = 0f
        }

        rule.runOnIdle { } // wait for redraw

        rule.onRoot().apply {
            val layoutCoordinates = coords!!
            assertEquals(Offset(10f, 10f), layoutCoordinates.positionInRoot())
            assertEquals(Rect(10f, 10f, 40f, 40f), layoutCoordinates.boundsInRoot())

            val topLeftInWindow = layoutCoordinates.localToWindow(Offset.Zero)
            assertEquals(0f, topLeftInWindow.x, 0.1f)
            assertEquals(10f * sqrt(2f), topLeftInWindow.y, 0.1f)

            val topLeftInLayout = layoutCoordinates.windowToLocal(topLeftInWindow)
            assertEquals(0f, topLeftInLayout.x, 0.1f)
            assertEquals(0f, topLeftInLayout.y, 0.1f)

            val bottomRightInWindow = layoutCoordinates.localToWindow(Offset(30f, 30f))
            assertEquals(0f, bottomRightInWindow.x, 0.1f)
            assertEquals(40f * sqrt(2f), bottomRightInWindow.y, 0.1f)

            val bottomRightInLayout = layoutCoordinates.windowToLocal(bottomRightInWindow)
            assertEquals(30f, bottomRightInLayout.x, 0.1f)
            assertEquals(30f, bottomRightInLayout.y, 0.1f)

            val boundsInWindow = layoutCoordinates.boundsInWindow()

            assertEquals(10f * sqrt(2f), boundsInWindow.top, 0.1f)
            assertEquals(30f * sqrt(2f) / 2f, boundsInWindow.right, 0.1f)
            assertEquals(-30f * sqrt(2f) / 2f, boundsInWindow.left, 0.1f)
            assertEquals(40f * sqrt(2f), boundsInWindow.bottom, 0.1f)
        }
    }

    @Test
    fun testLayerBoundsPositionInMovedWindow() {
        var coords: LayoutCoordinates? = null
        var alignment by mutableStateOf(Alignment.Center)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Popup(alignment = alignment) {
                    FixedSize(
                        30,
                        Modifier.padding(10).background(Color.Red).onGloballyPositioned {
                            coords = it
                        }
                    ) { /* no-op */ }
                }
            }
        }

        rule.runOnIdle {
            val inWindow = coords!!.positionInWindow()
            assertEquals(10f, inWindow.x)
            assertEquals(10f, inWindow.y)
            alignment = Alignment.BottomEnd
        }

        rule.runOnIdle {
            val inWindow = coords!!.positionInWindow()
            assertEquals(10f, inWindow.x)
            assertEquals(10f, inWindow.y)
        }
    }

    @Test
    fun coordinatesOfTheModifierAreReported() {
        var coords1: LayoutCoordinates? = null
        var coords2: LayoutCoordinates? = null
        var coords3: LayoutCoordinates? = null
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        coords1 = it
                    }
                    .padding(2.dp)
                    .onGloballyPositioned {
                        coords2 = it
                    }
                    .padding(3.dp)
                    .onGloballyPositioned {
                        coords3 = it
                    }
            )
        }

        rule.runOnIdle {
            assertEquals(0f, coords1!!.positionInWindow().x)
            val padding1 = with(rule.density) { 2.dp.roundToPx() }
            assertEquals(padding1.toFloat(), coords2!!.positionInWindow().x)
            val padding2 = padding1 + with(rule.density) { 3.dp.roundToPx() }
            assertEquals(padding2.toFloat(), coords3!!.positionInWindow().x)
        }
    }

    @Test
    @SmallTest
    fun modifierIsReturningEqualObjectForTheSameLambda() {
        val lambda: (LayoutCoordinates) -> Unit = { }
        assertEquals(Modifier.onGloballyPositioned(lambda), Modifier.onGloballyPositioned(lambda))
    }

    @Test
    @SmallTest
    fun modifierIsReturningNotEqualObjectForDifferentLambdas() {
        val lambda1: (LayoutCoordinates) -> Unit = {
            it.isAttached
        }
        val lambda2: (LayoutCoordinates) -> Unit = {
            !it.isAttached
        }
        Assert.assertNotEquals(
            Modifier.onGloballyPositioned(lambda1),
            Modifier.onGloballyPositioned(lambda2)
        )
    }

    @Test
    fun forceRemeasureTriggersCallbacks() {
        var coords: LayoutCoordinates? = null
        var remeasurementObj: Remeasurement? = null
        rule.setContent {
            Box(
                Modifier
                    .then(object : RemeasurementModifier {
                        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                            remeasurementObj = remeasurement
                        }
                    })
                    .onGloballyPositioned {
                        coords = it
                    }
                    .size(100.dp)
            )
        }

        rule.runOnIdle {
            assertNotNull(coords)
            coords = null
            assertNotNull(remeasurementObj)
            remeasurementObj!!.forceRemeasure()

            assertNotNull(coords)
        }
    }
}

@Composable
fun DelayedMeasure(
    size: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
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
