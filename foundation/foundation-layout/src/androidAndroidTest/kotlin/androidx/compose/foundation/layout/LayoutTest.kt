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

package androidx.compose.foundation.layout

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.offset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max

open class LayoutTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    lateinit var activity: TestActivity
    lateinit var handler: Handler
    internal lateinit var density: Density

    @Before
    fun setup() {
        activity = activityTestRule.activity
        density = Density(activity)
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)

        // Kotlin IR compiler doesn't seem too happy with auto-conversion from
        // lambda to Runnable, so separate it here
        val runnable: Runnable = object : Runnable {
            override fun run() {
                handler = Handler(Looper.getMainLooper())
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }

    internal fun show(composable: @Composable () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                activity.setContent(content = composable)
            }
        }
        activityTestRule.runOnUiThread(runnable)
        // Wait for the frame to complete before continuing
        runBlocking {
            Recomposer.runningRecomposers.value.forEach { recomposer ->
                recomposer.state.first { it <= Recomposer.State.Idle }
            }
        }
    }

    internal fun findComposeView(): View {
        return findViewRootForTest(activity).view
    }

    internal fun findViewRootForTest(activity: Activity): ViewRootForTest {
        val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
        return findViewRootForTest(contentViewGroup)!!
    }

    internal fun findViewRootForTest(parent: ViewGroup): ViewRootForTest? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is ViewRootForTest) {
                return child
            } else if (child is ViewGroup) {
                val owner = findViewRootForTest(child)
                if (owner != null) {
                    return owner
                }
            }
        }
        return null
    }

    internal fun waitForDraw(view: View) {
        val viewDrawLatch = CountDownLatch(1)
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                viewDrawLatch.countDown()
            }
        }
        view.post(object : Runnable {
            override fun run() {
                view.viewTreeObserver.addOnDrawListener(listener)
                view.invalidate()
            }
        })
        assertTrue(viewDrawLatch.await(1, TimeUnit.SECONDS))
    }

    internal fun Modifier.saveLayoutInfo(
        size: Ref<IntSize>,
        position: Ref<Offset>,
        positionedLatch: CountDownLatch
    ): Modifier = this.onGloballyPositioned { coordinates ->
        size.value = IntSize(coordinates.size.width, coordinates.size.height)
        position.value = coordinates.localToRoot(Offset(0f, 0f))
        positionedLatch.countDown()
    }

    internal fun testIntrinsics(
        vararg layouts: @Composable () -> Unit,
        test: ((Int) -> Int, (Int) -> Int, (Int) -> Int, (Int) -> Int) -> Unit
    ) {
        layouts.forEach { layout ->
            val layoutLatch = CountDownLatch(1)
            show {
                val measurePolicy = object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints
                    ): MeasureResult {
                        val measurable = measurables.first()
                        test(
                            { h -> measurable.minIntrinsicWidth(h) },
                            { w -> measurable.minIntrinsicHeight(w) },
                            { h -> measurable.maxIntrinsicWidth(h) },
                            { w -> measurable.maxIntrinsicHeight(w) }
                        )
                        layoutLatch.countDown()

                        return layout(0, 0) {}
                    }

                    override fun IntrinsicMeasureScope.minIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int
                    ) = 0

                    override fun IntrinsicMeasureScope.minIntrinsicHeight(
                        measurables: List<IntrinsicMeasurable>,
                        width: Int
                    ) = 0

                    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int
                    ) = 0

                    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                        measurables: List<IntrinsicMeasurable>,
                        width: Int
                    ) = 0
                }
                Layout(
                    content = layout,
                    measurePolicy = measurePolicy
                )
            }
            assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        }
    }

    @Composable
    internal fun FixedSizeLayout(
        width: Int,
        height: Int,
        alignmentLines: Map<AlignmentLine, Int>
    ) {
        Layout({}) { _, constraints ->
            layout(
                constraints.constrainWidth(width),
                constraints.constrainHeight(height),
                alignmentLines
            ) {}
        }
    }

    @Composable
    internal fun WithInfiniteConstraints(content: @Composable () -> Unit) {
        Layout(content) { measurables, _ ->
            val placeables = measurables.map { it.measure(Constraints()) }
            layout(0, 0) {
                placeables.forEach { it.placeRelative(0, 0) }
            }
        }
    }

    @Composable
    internal fun ConstrainedBox(
        constraints: DpConstraints,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        with(LocalDensity.current) {
            val pxConstraints = Constraints(constraints)
            val measurePolicy = object : MeasurePolicy {
                @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    incomingConstraints: Constraints
                ): MeasureResult {
                    val measurable = measurables.firstOrNull()
                    val childConstraints = incomingConstraints.constrain(Constraints(constraints))
                    val placeable = measurable?.measure(childConstraints)

                    val layoutWidth = placeable?.width ?: childConstraints.minWidth
                    val layoutHeight = placeable?.height ?: childConstraints.minHeight
                    return layout(layoutWidth, layoutHeight) {
                        placeable?.placeRelative(0, 0)
                    }
                }

                override fun IntrinsicMeasureScope.minIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int
                ): Int {
                    val width = measurables.firstOrNull()?.minIntrinsicWidth(height) ?: 0
                    return pxConstraints.constrainWidth(width)
                }

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int {
                    val height = measurables.firstOrNull()?.minIntrinsicHeight(width) ?: 0
                    return pxConstraints.constrainHeight(height)
                }

                override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int
                ): Int {
                    val width = measurables.firstOrNull()?.maxIntrinsicWidth(height) ?: 0
                    return pxConstraints.constrainWidth(width)
                }

                override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int {
                    val height = measurables.firstOrNull()?.maxIntrinsicHeight(width) ?: 0
                    return pxConstraints.constrainHeight(height)
                }
            }
            Layout(
                content = content,
                modifier = modifier,
                measurePolicy = measurePolicy
            )
        }
    }

    /**
     * Similar to [Constraints], but with constraint values expressed in [Dp].
     */
    @Immutable
    data class DpConstraints(
        @Stable
        val minWidth: Dp = 0.dp,
        @Stable
        val maxWidth: Dp = Dp.Infinity,
        @Stable
        val minHeight: Dp = 0.dp,
        @Stable
        val maxHeight: Dp = Dp.Infinity
    ) {
        init {
            require(minWidth.isFinite) { "Constraints#minWidth should be finite" }
            require(minHeight.isFinite) { "Constraints#minHeight should be finite" }
            require(!minWidth.value.isNaN()) { "Constraints#minWidth should not be NaN" }
            require(!maxWidth.value.isNaN()) { "Constraints#maxWidth should not be NaN" }
            require(!minHeight.value.isNaN()) { "Constraints#minHeight should not be NaN" }
            require(!maxHeight.value.isNaN()) { "Constraints#maxHeight should not be NaN" }
            require(minWidth <= maxWidth) {
                "Constraints should be satisfiable, but minWidth > maxWidth"
            }
            require(minHeight <= maxHeight) {
                "Constraints should be satisfiable, but minHeight > maxHeight"
            }
            require(minWidth >= 0.dp) { "Constraints#minWidth should be non-negative" }
            require(maxWidth >= 0.dp) { "Constraints#maxWidth should be non-negative" }
            require(minHeight >= 0.dp) { "Constraints#minHeight should be non-negative" }
            require(maxHeight >= 0.dp) { "Constraints#maxHeight should be non-negative" }
        }

        companion object {
            /**
             * Creates constraints tight in both dimensions.
             */
            @Stable
            fun fixed(width: Dp, height: Dp) = DpConstraints(width, width, height, height)
        }
    }

    /**
     * Creates the [Constraints] corresponding to the current [DpConstraints].
     */
    @Stable
    fun Density.Constraints(dpConstraints: DpConstraints) = Constraints(
        minWidth = dpConstraints.minWidth.roundToPx(),
        maxWidth = dpConstraints.maxWidth.roundToPx(),
        minHeight = dpConstraints.minHeight.roundToPx(),
        maxHeight = dpConstraints.maxHeight.roundToPx()
    )

    internal fun assertEquals(expected: Size?, actual: Size?) {
        assertNotNull("Null expected size", expected)
        expected as Size
        assertNotNull("Null actual size", actual)
        actual as Size

        assertEquals(
            "Expected width ${expected.width} but obtained ${actual.width}",
            expected.width,
            actual.width,
            0f
        )
        assertEquals(
            "Expected height ${expected.height} but obtained ${actual.height}",
            expected.height,
            actual.height,
            0f
        )
        if (actual.width != actual.width.toInt().toFloat()) {
            fail("Expected integer width")
        }
        if (actual.height != actual.height.toInt().toFloat()) {
            fail("Expected integer height")
        }
    }

    internal fun assertEquals(expected: Offset?, actual: Offset?) {
        assertNotNull("Null expected position", expected)
        expected as Offset
        assertNotNull("Null actual position", actual)
        actual as Offset

        assertEquals(
            "Expected x ${expected.x} but obtained ${actual.x}",
            expected.x,
            actual.x,
            0f
        )
        assertEquals(
            "Expected y ${expected.y} but obtained ${actual.y}",
            expected.y,
            actual.y,
            0f
        )
        if (actual.x != actual.x.toInt().toFloat()) {
            fail("Expected integer x coordinate")
        }
        if (actual.y != actual.y.toInt().toFloat()) {
            fail("Expected integer y coordinate")
        }
    }

    internal fun assertEquals(expected: Int, actual: Int) {
        assertEquals(
            "Expected $expected but obtained $actual",
            expected.toFloat(),
            actual.toFloat(),
            0f
        )
    }

    @Composable
    internal fun Container(
        modifier: Modifier = Modifier,
        padding: PaddingValues = PaddingValues(0.dp),
        alignment: Alignment = Alignment.Center,
        expanded: Boolean = false,
        constraints: DpConstraints = DpConstraints(),
        width: Dp? = null,
        height: Dp? = null,
        content: @Composable () -> Unit
    ) {
        Layout(content, modifier) { measurables, incomingConstraints ->
            val containerConstraints = incomingConstraints.constrain(
                Constraints(constraints)
                    .copy(
                        width?.roundToPx() ?: constraints.minWidth.roundToPx(),
                        width?.roundToPx() ?: constraints.maxWidth.roundToPx(),
                        height?.roundToPx() ?: constraints.minHeight.roundToPx(),
                        height?.roundToPx() ?: constraints.maxHeight.roundToPx()
                    )
            )
            val totalHorizontal = padding.calculateLeftPadding(layoutDirection).roundToPx() +
                padding.calculateRightPadding(layoutDirection).roundToPx()
            val totalVertical = padding.calculateTopPadding().roundToPx() +
                padding.calculateBottomPadding().roundToPx()
            val childConstraints = containerConstraints
                .copy(minWidth = 0, minHeight = 0)
                .offset(-totalHorizontal, -totalVertical)
            var placeable: Placeable? = null
            val containerWidth = if ((containerConstraints.hasFixedWidth || expanded) &&
                containerConstraints.hasBoundedWidth
            ) {
                containerConstraints.maxWidth
            } else {
                placeable = measurables.firstOrNull()?.measure(childConstraints)
                max((placeable?.width ?: 0) + totalHorizontal, containerConstraints.minWidth)
            }
            val containerHeight = if ((containerConstraints.hasFixedHeight || expanded) &&
                containerConstraints.hasBoundedHeight
            ) {
                containerConstraints.maxHeight
            } else {
                if (placeable == null) {
                    placeable = measurables.firstOrNull()?.measure(childConstraints)
                }
                max((placeable?.height ?: 0) + totalVertical, containerConstraints.minHeight)
            }
            layout(containerWidth, containerHeight) {
                val p = placeable ?: measurables.firstOrNull()?.measure(childConstraints)
                p?.let {
                    val position = alignment.align(
                        IntSize(it.width + totalHorizontal, it.height + totalVertical),
                        IntSize(containerWidth, containerHeight),
                        layoutDirection
                    )
                    it.place(
                        padding.calculateLeftPadding(layoutDirection).roundToPx() + position.x,
                        padding.calculateTopPadding().roundToPx() + position.y
                    )
                }
            }
        }
    }
}