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

@file:Suppress("Deprecation")

package androidx.compose.ui.platform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.emit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Align
import androidx.compose.ui.AlignmentLine
import androidx.compose.ui.Layout
import androidx.compose.ui.LayoutModifier
import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.drawLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.globalPosition
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.LayoutEmitHelper
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.Ref
import androidx.compose.ui.node.isAttached
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.emitView
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.test.createAndroidComposeRule
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.onNodeWithTag
import junit.framework.TestCase.assertNotNull
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.roundToInt

/**
 * Testing the support for Android Views in Compose UI.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidViewCompatTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun simpleLayoutTest() {
        val squareRef = Ref<ColoredSquareView>()
        val squareSize = mutableStateOf(100)
        var expectedSize = 100
        rule.setContent {
            Align {
                Layout(
                    modifier = Modifier.testTag("content"),
                    children = @Composable {
                        AndroidView(::ColoredSquareView) {
                            it.size = squareSize.value
                            it.ref = squareRef
                        }
                    }
                ) { measurables, constraints ->
                    assertEquals(1, measurables.size)
                    val placeable = measurables.first().measure(
                        constraints.copy(minWidth = 0, minHeight = 0)
                    )
                    assertEquals(placeable.width, expectedSize)
                    assertEquals(placeable.height, expectedSize)
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                }
            }
        }
        rule.onNodeWithTag("content").assertIsDisplayed()
        val squareView = squareRef.value
        assertNotNull(squareView)
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))

        rule.runOnUiThread {
            // Change view attribute using recomposition.
            squareSize.value = 200
            expectedSize = 200
        }
        rule.onNodeWithTag("content").assertIsDisplayed()
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))

        rule.runOnUiThread {
            // Change view attribute using the View reference.
            squareView!!.size = 300
            expectedSize = 300
        }
        rule.onNodeWithTag("content").assertIsDisplayed()
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun simpleDrawTest() {
        val squareRef = Ref<ColoredSquareView>()
        val colorModel = mutableStateOf(Color.Blue)
        val squareSize = 100
        var expectedColor = Color.Blue
        rule.setContent {
            Align {
                Container(Modifier.testTag("content").drawLayer()) {
                    AndroidView(::ColoredSquareView) {
                        it.color = colorModel.value
                        it.ref = squareRef
                    }
                }
            }
        }
        val squareView = squareRef.value
        assertNotNull(squareView)
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))
        val expectedPixelColor = { position: IntOffset ->
            if (position.x < squareSize && position.y < squareSize) {
                expectedColor
            } else {
                Color.White
            }
        }
        rule.onNodeWithTag("content")
            .assertIsDisplayed()
            .captureToBitmap()
            .assertPixels(expectedColorProvider = expectedPixelColor)

        rule.runOnUiThread {
            // Change view attribute using recomposition.
            colorModel.value = Color.Green
            expectedColor = Color.Green
        }
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))
        rule.onNodeWithTag("content")
            .assertIsDisplayed()
            .captureToBitmap()
            .assertPixels(expectedColorProvider = expectedPixelColor)

        rule.runOnUiThread {
            // Change view attribute using the View reference.
            colorModel.value = Color.Red
            expectedColor = Color.Red
        }
        Espresso
            .onView(instanceOf(ColoredSquareView::class.java))
            .check(matches(isDescendantOfA(instanceOf(Owner::class.java))))
            .check(matches(`is`(squareView)))
        rule.onNodeWithTag("content")
            .assertIsDisplayed()
            .captureToBitmap()
            .assertPixels(expectedColorProvider = expectedPixelColor)
    }

    // When incoming constraints are fixed.

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_1() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20, 30),
            ViewGroup.LayoutParams(40, 50)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_2() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20, 30),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_3() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            Constraints.fixed(20, 30),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
    }

    // When incoming constraints are finite.

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_4() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(25, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20, maxWidth = 30, minHeight = 35, maxHeight = 45
            ),
            ViewGroup.LayoutParams(25, 35)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_5() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(20, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20, maxWidth = 30, minHeight = 35, maxHeight = 45
            ),
            ViewGroup.LayoutParams(15, 25)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_6() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(30, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(45, MeasureSpec.EXACTLY),
            Constraints(
                minWidth = 20, maxWidth = 30, minHeight = 35, maxHeight = 45
            ),
            ViewGroup.LayoutParams(35, 50)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_7() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(40, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(50, MeasureSpec.AT_MOST),
            Constraints(maxWidth = 40, maxHeight = 50),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_8() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(40, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(50, MeasureSpec.EXACTLY),
            Constraints(maxWidth = 40, maxHeight = 50),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
    }

    // When incoming constraints are infinite.

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_9() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(25, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(35, MeasureSpec.EXACTLY),
            Constraints(),
            ViewGroup.LayoutParams(25, 35)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_10() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            Constraints(),
            ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMeasureSpecs_11() {
        testMeasurement_isDoneWithCorrectMeasureSpecs(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            Constraints(),
            ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
    }

    private fun testMeasurement_isDoneWithCorrectMeasureSpecs(
        expectedWidthSpec: Int,
        expectedHeightSpec: Int,
        constraints: Constraints,
        layoutParams: ViewGroup.LayoutParams
    ) {
        val viewRef = Ref<MeasureSpecSaverView>()
        val widthMeasureSpecRef = Ref<Int>()
        val heightMeasureSpecRef = Ref<Int>()
        // Unique starting constraints so that new constraints are different and thus recomp is
        // guaranteed.
        val constraintsHolder = mutableStateOf(Constraints.fixed(1234, 5678))

        rule.setContent {
            Container(LayoutConstraints(constraintsHolder.value)) {
                AndroidView(::MeasureSpecSaverView) {
                    it.ref = viewRef
                    it.widthMeasureSpecRef = widthMeasureSpecRef
                    it.heightMeasureSpecRef = heightMeasureSpecRef
                }
            }
        }

        rule.runOnUiThread {
            constraintsHolder.value = constraints
            viewRef.value?.layoutParams = layoutParams
        }

        rule.runOnIdle {
            assertEquals(expectedWidthSpec, widthMeasureSpecRef.value)
            assertEquals(expectedHeightSpec, heightMeasureSpecRef.value)
        }
    }

    @Test
    fun testMeasurement_isDoneWithCorrectMinimumDimensionsSetOnView() {
        val viewRef = Ref<MeasureSpecSaverView>()
        val constraintsHolder = mutableStateOf(Constraints())
        rule.setContent {
            Container(LayoutConstraints(constraintsHolder.value)) {
                AndroidView(::MeasureSpecSaverView) { it.ref = viewRef }
            }
        }
        rule.runOnUiThread {
            constraintsHolder.value = Constraints(minWidth = 20, minHeight = 30)
        }

        rule.runOnIdle {
            assertEquals(20, viewRef.value!!.minimumWidth)
            assertEquals(30, viewRef.value!!.minimumHeight)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testRedrawing_onSubsequentRemeasuring() {
        var size by mutableStateOf(20)
        rule.setContent {
            Box(Modifier.drawLayer().fillMaxSize()) {
                val context = ContextAmbient.current
                val view = remember { View(context) }
                AndroidView({ view }, Modifier.testTag("view"))
                view.layoutParams = ViewGroup.LayoutParams(size, size)
                view.setBackgroundColor(android.graphics.Color.BLUE)
            }
        }
        rule.onNodeWithTag("view").captureToBitmap()
            .assertPixels(IntSize(size, size)) { Color.Blue }

        rule.runOnIdle { size += 20 }
        rule.onNodeWithTag("view").captureToBitmap()
            .assertPixels(IntSize(size, size)) { Color.Blue }

        rule.runOnIdle { size += 20 }
        rule.onNodeWithTag("view").captureToBitmap()
            .assertPixels(IntSize(size, size)) { Color.Blue }
    }

    @Test
    fun testCoordinates_acrossMultipleViewAndComposeSwitches() {
        val padding = 100

        var outer: Offset = Offset.Zero
        var inner: Offset = Offset.Zero

        rule.setContent {
            Box(Modifier.onGloballyPositioned { outer = it.globalPosition }) {
                val paddingDp = with(DensityAmbient.current) { padding.toDp() }
                Box(Modifier.padding(paddingDp)) {
                    AndroidView(::FrameLayout) {
                        it.setContent {
                            Box(
                                Modifier.padding(paddingDp)
                                    .onGloballyPositioned { inner = it.globalPosition }
                            )
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(outer.x + padding * 2, inner.x)
            assertEquals(outer.y + padding * 2, inner.y)
        }
    }

    @Test
    fun testCoordinates_acrossMultipleViewAndComposeSwitches_whenContainerMoves() {
        val size = 100
        val padding = 100

        lateinit var topView: View
        lateinit var coordinates: LayoutCoordinates
        var startX = 0

        rule.activityRule.scenario.onActivity {
            val root = LinearLayout(it)
            it.setContentView(root)

            topView = View(it)
            root.addView(topView, size, size)
            val frameLayout = FrameLayout(it)
            root.addView(frameLayout)

            frameLayout.setContent {
                Box {
                    val paddingDp = with(DensityAmbient.current) { padding.toDp() }
                    Box(Modifier.padding(paddingDp)) {
                        AndroidView(::FrameLayout) {
                            it.setContent {
                                Box(
                                    Modifier.padding(paddingDp)
                                        .onGloballyPositioned { coordinates = it }
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle { startX = coordinates.globalPosition.x.roundToInt() }

        rule.runOnIdle { topView.visibility = View.GONE }

        rule.runOnIdle {
            assertEquals(100, startX - coordinates.globalPosition.x.roundToInt())
        }
    }

    @Test
    @Suppress("Deprecation")
    fun testComposeInsideView_simpleLayout() {
        val padding = 10f
        val paddingDp = with(rule.density) { padding.toDp() }
        val size = 20f
        val sizeDp = with(rule.density) { size.toDp() }

        var outer: Offset = Offset.Zero
        var inner1: Offset = Offset.Zero
        var inner2: Offset = Offset.Zero
        rule.setContent {
            Box(Modifier.onGloballyPositioned { outer = it.globalPosition }) {
                Box(Modifier.padding(start = paddingDp, top = paddingDp)) {
                    emitView(::LinearLayout, {}) {
                        Box(
                            Modifier.size(sizeDp).background(Color.Blue).onGloballyPositioned {
                                inner1 = it.globalPosition
                            }
                        )
                        Box(
                            Modifier.size(sizeDp).background(Color.Gray).onGloballyPositioned {
                                inner2 = it.globalPosition
                            }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(Offset(padding, padding), inner1 - outer)
            assertEquals(Offset(padding + size, padding), inner2 - outer)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Suppress("Deprecation")
    fun testComposeInsideView_simpleDraw() {
        val padding = 10f
        val paddingDp = with(rule.density) { padding.toDp() }
        val size = 20f
        val sizeDp = with(rule.density) { size.toDp() }

        rule.setContent {
            Box(Modifier.testTag("box")) {
                Box(Modifier.background(Color.Blue).padding(paddingDp)) {
                    emitView(::LinearLayout, {}) {
                        Box(Modifier.size(sizeDp).background(Color.White))
                        Box(Modifier.size(sizeDp).background(Color.Gray))
                    }
                }
            }
        }

        rule.onNodeWithTag("box").captureToBitmap().assertPixels(
            IntSize((padding * 2 + size * 2).roundToInt(), (padding * 2 + size).roundToInt())
        ) { offset ->
            if (offset.y < padding || offset.y >= padding + size || offset.x < padding ||
                offset.x >= padding + size * 2
            ) {
                Color.Blue
            } else if (offset.x >= padding && offset.x < padding + size) {
                Color.White
            } else {
                Color.Gray
            }
        }
    }

    @Test
    @OptIn(ExperimentalLayoutNodeApi::class, ExperimentalComposeApi::class)
    fun testComposeInsideView_attachingAndDetaching() {
        var composeContent by mutableStateOf(true)
        var node: LayoutNode? = null
        rule.setContent {
            if (composeContent) {
                Box {
                    AndroidView(::LinearLayout) {
                        it.setContent {
                            emit<LayoutNode, Applier<Any>>(
                                ctor = LayoutEmitHelper.constructor,
                                update = {
                                    node = this.node
                                    set(noOpMeasureBlocks, LayoutEmitHelper.setMeasureBlocks)
                                }
                            )
                        }
                    }
                }
            }
        }

        Espresso.onView(
            allOf(
                withClassName(endsWith("AndroidComposeView")),
                not(isDescendantOfA(withClassName(endsWith("AndroidComposeView"))))
            )
        ).check { view, exception ->
            view as AndroidComposeView
            // The root layout node should have one child, the Box.
            if (view.root.children.size != 1) throw exception
        }
        var innerAndroidComposeView: AndroidComposeView? = null
        Espresso.onView(
            allOf(
                withClassName(endsWith("AndroidComposeView")),
                isDescendantOfA(withClassName(endsWith("AndroidComposeView")))
            )
        ).check { view, exception ->
            innerAndroidComposeView = view as AndroidComposeView
            // It should have one layout node child, the inner emitted LayoutNode.
            if (view.root.children.size != 1) throw exception
        }
        // The layout node and its AndroidComposeView should be attached.
        assertNotNull(innerAndroidComposeView)
        assertTrue(innerAndroidComposeView!!.isAttachedToWindow)
        assertNotNull(node)
        assertTrue(node!!.isAttached())

        rule.runOnIdle { composeContent = false }

        // The composition has been disposed.
        rule.runOnIdle {
            assertFalse(innerAndroidComposeView!!.isAttachedToWindow)
            // the node stays attached after the compose view is detached
            assertTrue(node!!.isAttached())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Suppress("Deprecation")
    fun testComposeInsideView_remove() {
        val size = 40.dp
        val sizePx = with(rule.density) { size.toIntPx() }

        var first by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.testTag("view")) {
                emitView(::LinearLayout, {}) {
                    if (first) {
                        Box(Modifier.size(size).background(Color.Green))
                    } else {
                        Box(Modifier.size(size).background(Color.Blue))
                    }
                }
            }
        }

        rule.onNodeWithTag("view")
            .captureToBitmap().assertPixels(IntSize(sizePx, sizePx)) { Color.Green }

        rule.runOnIdle { first = false }

        rule.onNodeWithTag("view")
            .captureToBitmap().assertPixels(IntSize(sizePx, sizePx)) { Color.Blue }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Suppress("Deprecation")
    fun testComposeInsideView_move() {
        val size = 40.dp
        val sizePx = with(rule.density) { size.toIntPx() }

        var first by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.testTag("view")) {
                emitView(::LinearLayout, {}) {
                    if (first) {
                        key("green") {
                            Box(Modifier.size(size).background(Color.Green))
                        }
                        key("blue") {
                            Box(Modifier.size(size).background(Color.Blue))
                        }
                    } else {
                        key("blue") {
                            Box(Modifier.size(size).background(Color.Blue))
                        }
                        key("green") {
                            Box(Modifier.size(size).background(Color.Green))
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag("view").captureToBitmap()
            .assertPixels(IntSize(sizePx * 2, sizePx)) {
                if (it.x < sizePx) Color.Green else Color.Blue
            }

        rule.runOnIdle { first = false }

        rule.onNodeWithTag("view").captureToBitmap()
            .assertPixels(IntSize(sizePx * 2, sizePx)) {
                if (it.x < sizePx) Color.Blue else Color.Green
            }
    }

    class ColoredSquareView(context: Context) : View(context) {
        var size: Int = 100
            set(value) {
                if (value != field) {
                    field = value
                    requestLayout()
                }
            }

        var color: Color = Color.Blue
            set(value) {
                if (value != field) {
                    field = value
                    invalidate()
                }
            }

        var ref: Ref<ColoredSquareView>? = null
            set(value) {
                field = value
                value?.value = this
            }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(size, size)
        }

        override fun draw(canvas: Canvas?) {
            super.draw(canvas)
            canvas!!.drawRect(
                Rect(0, 0, size, size),
                Paint().apply { color = this@ColoredSquareView.color.toArgb() }
            )
        }
    }

    class MeasureSpecSaverView(context: Context) : View(context) {
        var ref: Ref<MeasureSpecSaverView>? = null
            set(value) {
                field = value
                value?.value = this
            }
        var widthMeasureSpecRef: Ref<Int>? = null
        var heightMeasureSpecRef: Ref<Int>? = null

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            widthMeasureSpecRef?.value = widthMeasureSpec
            heightMeasureSpecRef?.value = heightMeasureSpec
            setMeasuredDimension(0, 0)
        }
    }

    fun LayoutConstraints(childConstraints: Constraints) = object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureScope.MeasureResult {
            val placeable = measurable.measure(childConstraints)
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }

    @Composable
    fun Container(
        modifier: Modifier = Modifier,
        children: @Composable () -> Unit
    ) {
        Layout(children, modifier) { measurables, constraints ->
            val placeable = measurables[0].measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }

    @OptIn(ExperimentalLayoutNodeApi::class)
    private val noOpMeasureBlocks = object : LayoutNode.NoIntrinsicsMeasureBlocks("") {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureScope.MeasureResult {
            return object : MeasureScope.MeasureResult {
                override val width = 0
                override val height = 0
                override val alignmentLines: Map<AlignmentLine, Int> get() = mapOf()
                override fun placeChildren() {}
            }
        }
    }
}