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

@file:Suppress("Deprecation")

package androidx.compose.ui.layout

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Layout
import androidx.compose.ui.LayoutModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureBlock
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Padding
import androidx.compose.ui.Scroller
import androidx.compose.ui.SquareModel
import androidx.compose.ui.WithConstraints
import androidx.compose.ui.assertRect
import androidx.compose.ui.draw.paint
import androidx.compose.ui.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.node.Ref
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.waitAndScreenShot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class WithConstraintsTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    @Test
    fun withConstraintsTest() {
        val size = 20

        val countDownLatch = CountDownLatch(1)
        val topConstraints = Ref<Constraints>()
        val paddedConstraints = Ref<Constraints>()
        val firstChildConstraints = Ref<Constraints>()
        val secondChildConstraints = Ref<Constraints>()
        rule.runOnUiThreadIR {
            activity.setContent {
                WithConstraints {
                    topConstraints.value = constraints
                    Padding(size = size) {
                        val drawModifier = Modifier.drawBehind {
                            countDownLatch.countDown()
                        }
                        WithConstraints(drawModifier) {
                            paddedConstraints.value = constraints
                            Layout(
                                measureBlock = { _, childConstraints ->
                                    firstChildConstraints.value = childConstraints
                                    layout(size, size) { }
                                },
                                children = { }
                            )
                            Layout(
                                measureBlock = { _, chilConstraints ->
                                    secondChildConstraints.value = chilConstraints
                                    layout(size, size) { }
                                },
                                children = { }
                            )
                        }
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

        rule.runOnUiThreadIR {
            activity.setContent {
                WithConstraints {
                    val outerModifier = Modifier.drawBehind {
                        drawRect(model.outerColor)
                    }
                    Layout(
                        children = {
                            val innerModifier = Modifier.drawBehind {
                                drawLatch.countDown()
                                drawRect(model.innerColor)
                            }
                            Layout(
                                children = {},
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
        }
        takeScreenShot(60).apply {
            assertRect(color = white, size = 20)
            assertRect(color = green, holeSize = 20)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
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
        rule.runOnUiThreadIR {
            activity.setContent {
                Scroller(
                    modifier = countdownLatchBackgroundModifier(Color.Yellow),
                    onScrollPositionChanged = { position, _ ->
                        offset.value = position
                    },
                    offset = offset
                ) {
                    // Need to pass some param here to a separate function or else it works fine
                    TestLayout(5)
                }
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

        rule.runOnUiThreadIR {
            activity.setContent {
                WithConstraints {
                    // this block is called as a subcomposition from LayoutNode.measure()
                    // VectorPainter introduces additional subcomposition which is closing the
                    // current frame and opens a new one. our model reads during measure()
                    // wasn't possible to survide Frames swicth previously so the model read
                    // within the child Layout wasn't recorded
                    val background = Modifier.paint(
                        rememberVectorPainter(
                            name = "testPainter",
                            defaultWidth = 10.dp,
                            defaultHeight = 10.dp
                        ) { _, _ ->
                            /* intentionally empty */
                        }
                    )
                    Layout(modifier = background, children = {}) { _, _ ->
                        // read the model
                        model.value
                        latch.countDown()
                        layout(10, 10) {}
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread { model.value++ }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun withConstraintCallbackIsNotExecutedWithInnerRecompositions() {
        val model = mutableStateOf(0)
        var latch = CountDownLatch(1)
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        rule.runOnUiThreadIR {
            activity.setContent {
                WithConstraints {
                    recompositionsCount1++
                    Container(100, 100) {
                        model.value // model read
                        recompositionsCount2++
                        latch.countDown()
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread { model.value++ }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(1, recompositionsCount1)
        assertEquals(2, recompositionsCount2)
    }

    @Test
    fun updateConstraintsRecomposingWithConstraints() {
        val model = mutableStateOf(50)
        var latch = CountDownLatch(1)
        var actualConstraints: Constraints? = null

        rule.runOnUiThreadIR {
            activity.setContent {
                ChangingConstraintsLayout(model) {
                    WithConstraints {
                        actualConstraints = constraints
                        assertEquals(1, latch.count)
                        latch.countDown()
                        Container(width = 100, height = 100, children = emptyContent())
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(Constraints.fixed(50, 50), actualConstraints)

        latch = CountDownLatch(1)
        rule.runOnUiThread { model.value = 100 }

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

        rule.runOnUiThreadIR {
            activity.setContent {
                Container(width = 200, height = 200) {
                    WithConstraints(
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
        rule.runOnUiThread { size.value = 100 }

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

        rule.runOnUiThreadIR {
            activity.setContent {
                Container(100, 100, backgroundModifier(Color.Red)) {
                    ChangingConstraintsLayout(model) {
                        WithConstraints {
                            val receivedConstraints = constraints
                            Container(100, 100, infiniteConstraints) {
                                Container(100, 100) {
                                    Layout(
                                        {},
                                        countdownLatchBackgroundModifier(Color.Yellow)
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
        }
        takeScreenShot(100).apply {
            assertRect(color = Color.Yellow)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
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
        rule.runOnUiThread {
            activity.setContent {
                Container(width = 100, height = 100) {
                    WithConstraints {
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
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun removeLayoutNodeFromWithConstraintsDuringOnMeasure() {
        val model = mutableStateOf(100)
        drawLatch = CountDownLatch(2)

        rule.runOnUiThreadIR {
            activity.setContent {
                Container(
                    100, 100,
                    modifier = countdownLatchBackgroundModifier(Color.Red)
                ) {
                    // this component changes the constraints which triggers subcomposition
                    // within onMeasure block
                    ChangingConstraintsLayout(model) {
                        WithConstraints {
                            if (constraints.maxWidth == 100) {
                                // we will stop emmitting this layouts after constraints change
                                // Additional Container is needed so the Layout will be
                                // marked as not affecting parent size which means the Layout
                                // will be added into relayoutNodes List separately
                                Container(100, 100) {
                                    Layout(
                                        children = {},
                                        modifier = countdownLatchBackgroundModifier(Color.Yellow)
                                    ) { _, _ ->
                                        layout(model.value, model.value) {}
                                    }
                                }
                            }
                        }
                        Container(100, 100, Modifier, emptyContent())
                    }
                }
            }
        }
        takeScreenShot(100).apply {
            assertRect(color = Color.Yellow)
        }

        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            model.value = 50
        }

        takeScreenShot(100).apply {
            assertRect(color = Color.Red)
        }
    }

    @Test
    fun withConstraintsSiblingWhichIsChangingTheModelInsideMeasureBlock() {
        // WithConstraints is calling FrameManager.nextFrame() after composition
        // so this code was causing an issue as the model value change is triggering
        // remeasuring while our parent is measuring right now and this child was
        // already measured
        val drawlatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                val state = remember { mutableStateOf(false) }
                var lastLayoutValue: Boolean = false
                val drawModifier = Modifier.drawBehind {
                    // this verifies the layout was remeasured before being drawn
                    assertTrue(lastLayoutValue)
                    drawlatch.countDown()
                }
                Layout(children = {}, modifier = drawModifier) { _, _ ->
                    lastLayoutValue = state.value
                    // this registers the value read
                    if (!state.value) {
                        // change the value right inside the measure block
                        // it will cause one more remeasure pass as we also read this value
                        state.value = true
                    }
                    layout(100, 100) {}
                }
                WithConstraints {}
            }
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
        rule.runOnUiThread {
            activity.setContent {
                assertEquals(1, outerComposeLatch.count)
                outerComposeLatch.countDown()
                val children = @Composable {
                    Layout(
                        children = {
                            WithConstraints {
                                assertEquals(1, innerComposeLatch.count)
                                innerComposeLatch.countDown()
                                Layout(children = emptyContent()) { _, _ ->
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

                Layout(children) { measurables, _ ->
                    layout(100, 100) {
                        // we fix the constraints used by children so if the constraints given
                        // by the android view will change it would not affect the test
                        val constraints = Constraints(maxWidth = 100, maxHeight = 100)
                        measurables.first().measure(constraints).place(0, 0)
                    }
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
        rule.runOnUiThread {
            activity.setContent {
                val state = remember { mutableStateOf(0) }
                ContainerChildrenAffectsParentSize(100, 100) {
                    WithConstraints {
                        Layout(
                            children = {},
                            modifier = countdownLatchBackgroundModifier(Color.Transparent)
                        ) { _, _ ->
                            // read and write once inside measureBlock
                            if (state.value == 0) {
                                state.value = 1
                            }
                            layout(100, 100) {}
                        }
                    }
                    Container(100, 100) {
                        WithConstraints {}
                    }
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
        rule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    measureBlock = { measurables, _ ->
                        layout(0, 0) {
                            // there was a bug when the child of WithConstraints wasn't marking
                            // needsRemeasure and it was only measured because the constraints
                            // have been changed. to verify needRemeasure is true we measure the
                            // children with the default zero constraints so it will be equals to the
                            // initial constraints
                            measurables.first().measure(zeroConstraints).place(0, 0)
                        }
                    },
                    children = {
                        WithConstraints {
                            compositionLatch.countDown()
                            Layout(children = {}) { _, _ ->
                                childMeasureLatch.countDown()
                                layout(0, 0) {}
                            }
                        }
                    }
                )
            }
        }

        assertTrue(compositionLatch.await(1, TimeUnit.SECONDS))
        assertTrue(childMeasureLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun onDisposeInsideWithConstraintsCalled() {
        var emit by mutableStateOf(true)
        val composedLatch = CountDownLatch(1)
        val disposedLatch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                if (emit) {
                    WithConstraints {
                        composedLatch.countDown()
                        onDispose {
                            disposedLatch.countDown()
                        }
                    }
                }
            }
        }

        assertTrue(composedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            emit = false
        }
        assertTrue(disposedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun dpOverloadsHaveCorrectValues() {
        val latch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                val minWidthConstraint = 5.dp
                val maxWidthConstraint = 7.dp
                val minHeightConstraint = 9.dp
                val maxHeightConstraint = 12.dp
                Layout(
                    children = @Composable {
                        WithConstraints {
                            with(DensityAmbient.current) {
                                assertEquals(minWidthConstraint.toIntPx(), minWidth.toIntPx())
                                assertEquals(maxWidthConstraint.toIntPx(), maxWidth.toIntPx())
                                assertEquals(minHeightConstraint.toIntPx(), minHeight.toIntPx())
                                assertEquals(maxHeightConstraint.toIntPx(), maxHeight.toIntPx())
                            }
                            latch.countDown()
                        }
                    }
                ) { m, _ ->
                    layout(0, 0) {
                        m.first().measure(
                            Constraints(
                                minWidth = minWidthConstraint.toIntPx(),
                                maxWidth = maxWidthConstraint.toIntPx(),
                                minHeight = minHeightConstraint.toIntPx(),
                                maxHeight = maxHeightConstraint.toIntPx()
                            )
                        ).place(Offset.Zero)
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    private fun takeScreenShot(size: Int): Bitmap {
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        val bitmap = rule.waitAndScreenShot()
        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
        return bitmap
    }

    private fun countdownLatchBackgroundModifier(color: Color) =
        Modifier.drawBehind {
            drawRect(color)
            drawLatch.countDown()
        }
}

@Composable
private fun TestLayout(@Suppress("UNUSED_PARAMETER") someInput: Int) {
    Layout(
        children = {
            WithConstraints {
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
        children = {},
        modifier = backgroundModifier(Color.Red)
    ) { _, _ ->
        layout(foo, foo) { }
    }
}

@Composable
fun Container(
    width: Int,
    height: Int,
    modifier: Modifier = Modifier,
    children: @Composable () ->
    Unit
) {
    Layout(
        children = children,
        modifier = modifier,
        measureBlock = remember<MeasureBlock>(width, height) {
            { measurables, _ ->
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
    children: @Composable () -> Unit
) {
    Layout(
        children = children,
        measureBlock = remember<MeasureBlock>(width, height) {
            { measurables, _ ->
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
private fun ChangingConstraintsLayout(size: State<Int>, children: @Composable () -> Unit) {
    Layout(children) { measurables, _ ->
        layout(100, 100) {
            val constraints = Constraints.fixed(size.value, size.value)
            measurables.first().measure(constraints).place(0, 0)
        }
    }
}

fun backgroundModifier(color: Color) = Modifier.drawBehind {
    drawRect(color)
}

val infiniteConstraints = object : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(Constraints())
        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0, 0)
        }
    }
}
