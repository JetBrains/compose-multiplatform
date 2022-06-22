/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("Deprecation")

package androidx.compose.foundation.layout

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@MediumTest
@RunWith(AndroidJUnit4::class)
class BoxWithConstraintsTest : LayoutTest() {
    var drawLatch = CountDownLatch(1)

    @Test
    fun withConstraintsTest() {
        val size = 20

        val countDownLatch = CountDownLatch(1)
        val topConstraints = Ref<Constraints>()
        val paddedConstraints = Ref<Constraints>()
        val firstChildConstraints = Ref<Constraints>()
        val secondChildConstraints = Ref<Constraints>()
        show {
            BoxWithConstraints {
                topConstraints.value = constraints
                Padding(size = size) {
                    val drawModifier = Modifier.drawBehind {
                        countDownLatch.countDown()
                    }
                    BoxWithConstraints(drawModifier) {
                        paddedConstraints.value = constraints
                        Layout(
                            measurePolicy = { _, childConstraints ->
                                firstChildConstraints.value = childConstraints
                                layout(size, size) { }
                            },
                            content = { }
                        )
                        Layout(
                            measurePolicy = { _, chilConstraints ->
                                secondChildConstraints.value = chilConstraints
                                layout(size, size) { }
                            },
                            content = { }
                        )
                    }
                }
            }
        }
        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))

        val expectedPaddedConstraints = Constraints(
            0,
            topConstraints.value!!.maxWidth - size * 2,
            0,
            topConstraints.value!!.maxHeight - size * 2
        )
        assertEquals(expectedPaddedConstraints, paddedConstraints.value)
        assertEquals(paddedConstraints.value, firstChildConstraints.value)
        assertEquals(paddedConstraints.value, secondChildConstraints.value)
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun withConstraints_layoutListener() {
        val green = Color.Green
        val white = Color.White
        val model = SquareModel(size = 20, outerColor = green, innerColor = white)

        show {
            BoxWithConstraints {
                val outerModifier = Modifier.drawBehind {
                    drawRect(model.outerColor)
                }
                Layout(
                    content = {
                        val innerModifier = Modifier.drawBehind {
                            drawLatch.countDown()
                            drawRect(model.innerColor)
                        }
                        Layout(
                            content = {},
                            modifier = innerModifier
                        ) { measurables, constraints2 ->
                            layout(model.size, model.size) {}
                        }
                    },
                    modifier = outerModifier
                ) { measurables, constraints3 ->
                    val placeable = measurables[0].measure(
                        Constraints.fixed(
                            model.size,
                            model.size
                        )
                    )
                    layout(model.size * 3, model.size * 3) {
                        placeable.place(model.size, model.size)
                    }
                }
            }
        }
        takeScreenShot(60).apply {
            assertRect(color = white, size = 20)
            assertRect(color = green, holeSize = 20)
        }

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            model.size = 10
        }

        takeScreenShot(30).apply {
            assertRect(color = white, size = 10)
            assertRect(color = green, holeSize = 10)
        }
    }

    /**
     * WithConstraints will cause a requestLayout during layout in some circumstances.
     * The test here is the minimal example from a bug.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun requestLayoutDuringLayout() {
        val offset = mutableStateOf(0)
        show {
            Scroller(
                modifier = Modifier.countdownLatchBackground(Color.Yellow),
                onScrollPositionChanged = { position, _ ->
                    offset.value = position
                },
                offset = offset
            ) {
                // Need to pass some param here to a separate function or else it works fine
                TestLayout(5)
            }
        }

        takeScreenShot(30).apply {
            assertRect(color = Color.Red, size = 10)
            assertRect(color = Color.Yellow, holeSize = 10)
        }
    }

    @Test
    fun subcomposionInsideWithConstraintsDoesntAffectModelReadsObserving() {
        val model = mutableStateOf(0)
        var latch = CountDownLatch(1)

        show {
            BoxWithConstraints {
                // this block is called as a subcomposition from LayoutNode.measure()
                // VectorPainter introduces additional subcomposition which is closing the
                // current frame and opens a new one. our model reads during measure()
                // wasn't possible to survide Frames swicth previously so the model read
                // within the child Layout wasn't recorded
                val background = Modifier.paint(
                    rememberVectorPainter(
                        name = "testPainter",
                        defaultWidth = 10.dp,
                        defaultHeight = 10.dp,
                        autoMirror = false
                    ) { _, _ ->
                        /* intentionally empty */
                    }
                )
                Layout(modifier = background, content = {}) { _, _ ->
                    // read the model
                    model.value
                    latch.countDown()
                    layout(10, 10) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        activityTestRule.runOnUiThread { model.value++ }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun withConstraintCallbackIsNotExecutedWithInnerRecompositions() {
        val model = mutableStateOf(0)
        var latch = CountDownLatch(1)
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        show {
            BoxWithConstraints {
                recompositionsCount1++
                Container(100, 100) {
                    model.value // model read
                    recompositionsCount2++
                    latch.countDown()
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        activityTestRule.runOnUiThread { model.value++ }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(1, recompositionsCount1)
        assertEquals(2, recompositionsCount2)
    }

    @Test
    fun updateConstraintsRecomposingWithConstraints() {
        val model = mutableStateOf(50)
        var latch = CountDownLatch(1)
        var actualConstraints: Constraints? = null

        show {
            ChangingConstraintsLayout(model) {
                BoxWithConstraints {
                    actualConstraints = constraints
                    assertEquals(1, latch.count)
                    latch.countDown()
                    Container(width = 100, height = 100) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(Constraints.fixed(50, 50), actualConstraints)

        latch = CountDownLatch(1)
        activityTestRule.runOnUiThread { model.value = 100 }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(Constraints.fixed(100, 100), actualConstraints)
    }

    @Test
    fun withConstsraintsBehavesAsWrap() {
        val size = mutableStateOf(50)
        var withConstLatch = CountDownLatch(1)
        var childLatch = CountDownLatch(1)
        var withConstSize: IntSize? = null
        var childSize: IntSize? = null

        show {
            Container(width = 200, height = 200) {
                BoxWithConstraints(
                    modifier = Modifier.onGloballyPositioned {
                        // OnPositioned can be fired multiple times with the same value
                        // for example when requestLayout() was triggered on ComposeView.
                        // if we called twice, let's make sure we got the correct values.
                        assertTrue(withConstSize == null || withConstSize == it.size)
                        withConstSize = it.size
                        withConstLatch.countDown()
                    }
                ) {
                    Container(
                        width = size.value, height = size.value,
                        modifier = Modifier.onGloballyPositioned {
                            // OnPositioned can be fired multiple times with the same value
                            // for example when requestLayout() was triggered on ComposeView.
                            // if we called twice, let's make sure we got the correct values.
                            assertTrue(childSize == null || childSize == it.size)
                            childSize = it.size
                            childLatch.countDown()
                        }
                    ) {
                    }
                }
            }
        }
        assertTrue(withConstLatch.await(1, TimeUnit.SECONDS))
        assertTrue(childLatch.await(1, TimeUnit.SECONDS))
        var expectedSize = IntSize(50, 50)
        assertEquals(expectedSize, withConstSize)
        assertEquals(expectedSize, childSize)

        withConstSize = null
        childSize = null
        withConstLatch = CountDownLatch(1)
        childLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { size.value = 100 }

        assertTrue(withConstLatch.await(1, TimeUnit.SECONDS))
        assertTrue(childLatch.await(1, TimeUnit.SECONDS))
        expectedSize = IntSize(100, 100)
        assertEquals(expectedSize, withConstSize)
        assertEquals(expectedSize, childSize)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun withConstraintsIsNotSwallowingInnerRemeasureRequest() {
        val model = mutableStateOf(100)

        show {
            Container(100, 100, Modifier.background(Color.Red)) {
                ChangingConstraintsLayout(model) {
                    BoxWithConstraints {
                        val receivedConstraints = constraints
                        Container(100, 100, infiniteConstraints) {
                            Container(100, 100) {
                                Layout(
                                    {},
                                    Modifier.countdownLatchBackground(Color.Yellow)
                                ) { _, _ ->
                                    // the same as the value inside ValueModel
                                    val size = receivedConstraints.maxWidth
                                    layout(size, size) {}
                                }
                            }
                        }
                    }
                }
            }
        }
        takeScreenShot(100).apply {
            assertRect(color = Color.Yellow)
        }

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            model.value = 50
        }
        takeScreenShot(100).apply {
            assertRect(color = Color.Red, holeSize = 50)
            assertRect(color = Color.Yellow, size = 50)
        }
    }

    @Test
    fun updateModelInMeasuringAndReadItInCompositionWorksInsideWithConstraints() {
        val latch = CountDownLatch(1)
        show {
            Container(width = 100, height = 100) {
                BoxWithConstraints {
                    // this replicates the popular pattern we currently use
                    // where we save some data calculated in the measuring block
                    // and then use it in the next composition frame
                    var model by remember { mutableStateOf(false) }
                    Layout({
                        if (model) {
                            latch.countDown()
                        }
                    }) { _, _ ->
                        if (!model) {
                            model = true
                        }
                        layout(100, 100) {}
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun removeLayoutNodeFromWithConstraintsDuringOnMeasure() {
        val model = mutableStateOf(100)
        drawLatch = CountDownLatch(2)

        show {
            Container(
                100, 100,
                modifier = Modifier.countdownLatchBackground(Color.Red)
            ) {
                // this component changes the constraints which triggers subcomposition
                // within onMeasure block
                ChangingConstraintsLayout(model) {
                    BoxWithConstraints {
                        if (constraints.maxWidth == 100) {
                            // we will stop emitting this layouts after constraints change
                            // Additional Container is needed so the Layout will be
                            // marked as not affecting parent size which means the Layout
                            // will be added into relayoutNodes List separately
                            Container(100, 100) {
                                Layout(
                                    content = {},
                                    modifier = Modifier.countdownLatchBackground(Color.Yellow)
                                ) { _, _ ->
                                    layout(model.value, model.value) {}
                                }
                            }
                        }
                    }
                    Container(100, 100, Modifier) {}
                }
            }
        }
        takeScreenShot(100).apply {
            assertRect(color = Color.Yellow)
        }

        drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread {
            model.value = 50
        }

        takeScreenShot(100).apply {
            assertRect(color = Color.Red)
        }
    }

    @Test
    fun withConstraintsSiblingWhichIsChangingTheModelInsideMeasureBlock() {
        // WithConstraints used to call FrameManager.nextFrame() after composition
        // so this code was causing an issue as the model value change is triggering
        // remeasuring while our parent is measuring right now and this child was
        // already measured
        val drawlatch = CountDownLatch(1)
        show {
            val state = remember { mutableStateOf(false) }
            var lastLayoutValue: Boolean = false
            val drawModifier = Modifier.drawBehind {
                // this verifies the layout was remeasured before being drawn
                assertTrue(lastLayoutValue)
                drawlatch.countDown()
            }
            Layout(content = {}, modifier = drawModifier) { _, _ ->
                lastLayoutValue = state.value
                // this registers the value read
                if (!state.value) {
                    // change the value right inside the measure block
                    // it will cause one more remeasure pass as we also read this value
                    state.value = true
                }
                layout(100, 100) {}
            }
            BoxWithConstraints {}
        }
        assertTrue(drawlatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun allTheStepsCalledExactlyOnce() {
        val outerComposeLatch = CountDownLatch(1)
        val outerMeasureLatch = CountDownLatch(1)
        val outerLayoutLatch = CountDownLatch(1)
        val innerComposeLatch = CountDownLatch(1)
        val innerMeasureLatch = CountDownLatch(1)
        val innerLayoutLatch = CountDownLatch(1)
        show {
            assertEquals(1, outerComposeLatch.count)
            outerComposeLatch.countDown()
            val content = @Composable {
                Layout(
                    content = {
                        BoxWithConstraints {
                            assertEquals(1, innerComposeLatch.count)
                            innerComposeLatch.countDown()
                            Layout(content = {}) { _, _ ->
                                assertEquals(1, innerMeasureLatch.count)
                                innerMeasureLatch.countDown()
                                layout(100, 100) {
                                    assertEquals(1, innerLayoutLatch.count)
                                    innerLayoutLatch.countDown()
                                }
                            }
                        }
                    }
                ) { measurables, constraints ->
                    assertEquals(1, outerMeasureLatch.count)
                    outerMeasureLatch.countDown()
                    layout(100, 100) {
                        assertEquals(1, outerLayoutLatch.count)
                        outerLayoutLatch.countDown()
                        measurables.forEach { it.measure(constraints).place(0, 0) }
                    }
                }
            }

            Layout(content) { measurables, _ ->
                layout(100, 100) {
                    // we fix the constraints used by children so if the constraints given
                    // by the android view will change it would not affect the test
                    val constraints = Constraints(maxWidth = 100, maxHeight = 100)
                    measurables.first().measure(constraints).place(0, 0)
                }
            }
        }
        assertTrue(outerComposeLatch.await(1, TimeUnit.SECONDS))
        assertTrue(outerMeasureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(outerLayoutLatch.await(1, TimeUnit.SECONDS))
        assertTrue(innerComposeLatch.await(1, TimeUnit.SECONDS))
        assertTrue(innerMeasureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(innerLayoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun triggerRootRemeasureWhileRootIsLayouting() {
        show {
            val state = remember { mutableStateOf(0) }
            ContainerChildrenAffectsParentSize(100, 100) {
                BoxWithConstraints {
                    Layout(
                        content = {},
                        modifier = Modifier.countdownLatchBackground(Color.Transparent)
                    ) { _, _ ->
                        // read and write once inside measureBlock
                        if (state.value == 0) {
                            state.value = 1
                        }
                        layout(100, 100) {}
                    }
                }
                Container(100, 100) {
                    BoxWithConstraints {}
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        // before the fix this was failing our internal assertions in AndroidOwner
        // so nothing else to assert, apart from not crashing
    }

    @Test
    fun withConstraintsChildIsMeasuredEvenWithDefaultConstraints() {
        val compositionLatch = CountDownLatch(1)
        val childMeasureLatch = CountDownLatch(1)
        val zeroConstraints = Constraints.fixed(0, 0)
        show {
            Layout(
                measurePolicy = { measurables, _ ->
                    layout(0, 0) {
                        // there was a bug when the child of WithConstraints wasn't marking
                        // needsRemeasure and it was only measured because the constraints
                        // have been changed. to verify needRemeasure is true we measure the
                        // children with the default zero constraints so it will be equals to the
                        // initial constraints
                        measurables.first().measure(zeroConstraints).place(0, 0)
                    }
                },
                content = {
                    BoxWithConstraints {
                        compositionLatch.countDown()
                        Layout(content = {}) { _, _ ->
                            childMeasureLatch.countDown()
                            layout(0, 0) {}
                        }
                    }
                }
            )
        }

        assertTrue(compositionLatch.await(1, TimeUnit.SECONDS))
        assertTrue(childMeasureLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun onDisposeInsideWithConstraintsCalled() {
        var emit by mutableStateOf(true)
        val composedLatch = CountDownLatch(1)
        val disposedLatch = CountDownLatch(1)
        show {
            if (emit) {
                BoxWithConstraints {
                    composedLatch.countDown()
                    DisposableEffect(Unit) {
                        onDispose {
                            disposedLatch.countDown()
                        }
                    }
                }
            }
        }

        assertTrue(composedLatch.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
            emit = false
        }
        assertTrue(disposedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun dpOverloadsHaveCorrectValues() {
        val latch = CountDownLatch(1)
        show {
            val minWidthConstraint = 5.dp
            val maxWidthConstraint = 7.dp
            val minHeightConstraint = 9.dp
            val maxHeightConstraint = 12.dp
            Layout(
                content = @Composable {
                    BoxWithConstraints {
                        with(LocalDensity.current) {
                            assertEquals(minWidthConstraint.roundToPx(), minWidth.roundToPx())
                            assertEquals(maxWidthConstraint.roundToPx(), maxWidth.roundToPx())
                            assertEquals(minHeightConstraint.roundToPx(), minHeight.roundToPx())
                            assertEquals(maxHeightConstraint.roundToPx(), maxHeight.roundToPx())
                        }
                        latch.countDown()
                    }
                }
            ) { m, _ ->
                layout(0, 0) {
                    m.first().measure(
                        Constraints(
                            minWidth = minWidthConstraint.roundToPx(),
                            maxWidth = maxWidthConstraint.roundToPx(),
                            minHeight = minHeightConstraint.roundToPx(),
                            maxHeight = maxHeightConstraint.roundToPx()
                        )
                    ).place(IntOffset.Zero)
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun preservesInfinity() {
        val latch = CountDownLatch(1)
        show {
            BoxWithConstraints(Modifier.wrapContentSize(unbounded = true)) {
                assertEquals(Dp.Infinity, maxWidth)
                assertEquals(Dp.Infinity, maxHeight)
                latch.countDown()
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    // waitAndScreenShot() requires API level 26
    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot(size: Int): Bitmap {
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = activityTestRule.waitAndScreenShot()
        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
        return bitmap
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.O)
    fun ActivityTestRule<*>.waitAndScreenShot(
        forceInvalidate: Boolean = true
    ): Bitmap = waitAndScreenShot(findComposeView(), forceInvalidate)

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.O)
    fun ActivityTestRule<*>.waitAndScreenShot(
        view: View,
        forceInvalidate: Boolean = true
    ): Bitmap {
        val flushListener = DrawCounterListener(view)
        val offset = intArrayOf(0, 0)
        var handler: Handler? = null
        runOnUiThread {
            view.getLocationInWindow(offset)
            if (forceInvalidate) {
                view.viewTreeObserver.addOnPreDrawListener(flushListener)
                view.invalidate()
            }
            handler = Handler(Looper.getMainLooper())
        }

        if (forceInvalidate) {
            assertTrue("Drawing latch timed out", flushListener.latch.await(1, TimeUnit.SECONDS))
        }
        val width = view.width
        val height = view.height

        val dest =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val srcRect = android.graphics.Rect(0, 0, width, height)
        srcRect.offset(offset[0], offset[1])
        val latch = CountDownLatch(1)
        var copyResult = 0
        val onCopyFinished = object : PixelCopy.OnPixelCopyFinishedListener {
            override fun onPixelCopyFinished(result: Int) {
                copyResult = result
                latch.countDown()
            }
        }
        PixelCopy.request(activity.window, srcRect, dest, onCopyFinished, handler!!)
        assertTrue("Pixel copy latch timed out", latch.await(1, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, copyResult)
        return dest
    }

    private fun Modifier.countdownLatchBackground(color: Color): Modifier = drawBehind {
        drawRect(color)
        drawLatch.countDown()
    }
}

@Composable
private fun TestLayout(@Suppress("UNUSED_PARAMETER") someInput: Int) {
    Layout(
        content = {
            BoxWithConstraints {
                NeedsOtherMeasurementComposable(10)
            }
        }
    ) { measurables, constraints ->
        val withConstraintsPlaceable = measurables[0].measure(constraints)

        layout(30, 30) {
            withConstraintsPlaceable.place(10, 10)
        }
    }
}

@Composable
private fun NeedsOtherMeasurementComposable(foo: Int) {
    Layout(
        content = {},
        modifier = Modifier.background(Color.Red)
    ) { _, _ ->
        layout(foo, foo) { }
    }
}

@Composable
fun Container(
    width: Int,
    height: Int,
    modifier: Modifier = Modifier,
    content: @Composable () ->
    Unit
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = remember(width, height) {
            MeasurePolicy { measurables, _ ->
                val constraint = Constraints(maxWidth = width, maxHeight = height)
                layout(width, height) {
                    measurables.forEach {
                        val placeable = it.measure(constraint)
                        placeable.place(
                            (width - placeable.width) / 2,
                            (height - placeable.height) / 2
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ContainerChildrenAffectsParentSize(
    width: Int,
    height: Int,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        measurePolicy = remember(width, height) {
            MeasurePolicy { measurables, _ ->
                val constraint = Constraints(maxWidth = width, maxHeight = height)
                val placeables = measurables.map { it.measure(constraint) }
                layout(width, height) {
                    placeables.forEach {
                        it.place((width - width) / 2, (height - height) / 2)
                    }
                }
            }
        }
    )
}

@Composable
private fun ChangingConstraintsLayout(size: State<Int>, content: @Composable () -> Unit) {
    Layout(content) { measurables, _ ->
        layout(100, 100) {
            val constraints = Constraints.fixed(size.value, size.value)
            measurables.first().measure(constraints).place(0, 0)
        }
    }
}

fun Modifier.background(color: Color): Modifier = drawBehind {
    drawRect(color)
}

val infiniteConstraints = object : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(Constraints())
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0, 0)
        }
    }
}

@Composable
internal fun Padding(
    size: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            val totalDiff = size * 2
            val targetMinWidth = constraints.minWidth - totalDiff
            val targetMaxWidth = if (constraints.hasBoundedWidth) {
                constraints.maxWidth - totalDiff
            } else {
                Constraints.Infinity
            }
            val targetMinHeight = constraints.minHeight - totalDiff
            val targetMaxHeight = if (constraints.hasBoundedHeight) {
                constraints.maxHeight - totalDiff
            } else {
                Constraints.Infinity
            }
            val newConstraints = Constraints(
                minWidth = targetMinWidth.coerceAtLeast(0),
                maxWidth = targetMaxWidth.coerceAtLeast(0),
                minHeight = targetMinHeight.coerceAtLeast(0),
                maxHeight = targetMaxHeight.coerceAtLeast(0)
            )
            val placeables = measurables.map { m ->
                m.measure(newConstraints)
            }
            var maxWidth = size
            var maxHeight = size
            placeables.forEach { child ->
                maxHeight = max(child.height + totalDiff, maxHeight)
                maxWidth = max(child.width + totalDiff, maxWidth)
            }
            layout(maxWidth, maxHeight) {
                placeables.forEach { child ->
                    child.placeRelative(size, size)
                }
            }
        },
        content = content
    )
}

fun Bitmap.assertRect(
    color: Color,
    holeSize: Int = 0,
    size: Int = width,
    centerX: Int = width / 2,
    centerY: Int = height / 2
) {
    assertTrue(centerX + size / 2 <= width)
    assertTrue(centerX - size / 2 >= 0)
    assertTrue(centerY + size / 2 <= height)
    assertTrue(centerY - size / 2 >= 0)
    val halfHoleSize = holeSize / 2
    for (x in centerX - size / 2 until centerX + size / 2) {
        for (y in centerY - size / 2 until centerY + size / 2) {
            if (abs(x - centerX) > halfHoleSize &&
                abs(y - centerY) > halfHoleSize
            ) {
                val currentColor = Color(getPixel(x, y))
                assertColorsEqual(color, currentColor)
            }
        }
    }
}

@Composable
fun Scroller(
    modifier: Modifier = Modifier,
    onScrollPositionChanged: (position: Int, maxPosition: Int) -> Unit,
    offset: State<Int>,
    content: @Composable () -> Unit
) {
    val maxPosition = remember { mutableStateOf(Constraints.Infinity) }
    ScrollerLayout(
        modifier = modifier,
        maxPosition = maxPosition.value,
        onMaxPositionChanged = {
            maxPosition.value = 0
            onScrollPositionChanged(offset.value, 0)
        },
        content = content
    )
}

@Composable
private fun ScrollerLayout(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") maxPosition: Int,
    onMaxPositionChanged: () -> Unit,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val childConstraints = constraints.copy(
            maxHeight = constraints.maxHeight,
            maxWidth = Constraints.Infinity
        )
        val childMeasurable = measurables.first()
        val placeable = childMeasurable.measure(childConstraints)
        val width = min(placeable.width, constraints.maxWidth)
        layout(width, placeable.height) {
            onMaxPositionChanged()
            placeable.placeRelative(0, 0)
        }
    }
}

class DrawCounterListener(private val view: View) :
    ViewTreeObserver.OnPreDrawListener {
    val latch = CountDownLatch(5)

    override fun onPreDraw(): Boolean {
        latch.countDown()
        if (latch.count > 0) {
            view.postInvalidate()
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(this)
        }
        return true
    }
}

fun assertColorsEqual(
    expected: Color,
    color: Color,
    error: () -> String = { "$expected and $color are not similar!" }
) {
    val errorString = error()
    assertEquals(errorString, expected.red, color.red, 0.01f)
    assertEquals(errorString, expected.green, color.green, 0.01f)
    assertEquals(errorString, expected.blue, color.blue, 0.01f)
    assertEquals(errorString, expected.alpha, color.alpha, 0.01f)
}

@Stable
class SquareModel(
    size: Int = 10,
    outerColor: Color = Color(0xFF000080),
    innerColor: Color = Color(0xFFFFFFFF)
) {
    var size: Int by mutableStateOf(size)
    var outerColor: Color by mutableStateOf(outerColor)
    var innerColor: Color by mutableStateOf(innerColor)
}