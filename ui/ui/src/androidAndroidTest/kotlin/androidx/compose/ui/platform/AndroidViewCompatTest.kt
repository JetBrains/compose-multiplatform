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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Align
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.Ref
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
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
import kotlin.math.roundToInt

/**
 * Testing the support for Android Views in Compose UI.
 */
@MediumTest
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
                    content = @Composable {
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
                Container(Modifier.testTag("content").graphicsLayer()) {
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
            .captureToImage()
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
            .captureToImage()
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
            .captureToImage()
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
            Container(Modifier.layoutConstraints(constraintsHolder.value)) {
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
            Container(Modifier.layoutConstraints(constraintsHolder.value)) {
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
            Box(Modifier.graphicsLayer().fillMaxSize()) {
                val context = LocalContext.current
                val view = remember { View(context) }
                AndroidView({ view }, Modifier.testTag("view"))
                view.layoutParams = ViewGroup.LayoutParams(size, size)
                view.setBackgroundColor(android.graphics.Color.BLUE)
            }
        }
        rule.onNodeWithTag("view").captureToImage()
            .assertPixels(IntSize(size, size)) { Color.Blue }

        rule.runOnIdle { size += 20 }
        rule.runOnIdle { } // just wait for composition to finish
        rule.onNodeWithTag("view").captureToImage()
            .assertPixels(IntSize(size, size)) { Color.Blue }

        rule.runOnIdle { size += 20 }
        rule.runOnIdle { } // just wait for composition to finish
        rule.onNodeWithTag("view").captureToImage()
            .assertPixels(IntSize(size, size)) { Color.Blue }
    }

    @Test
    fun testCoordinates_acrossMultipleViewAndComposeSwitches() {
        val padding = 100

        var outer: Offset = Offset.Zero
        var inner: Offset = Offset.Zero

        rule.setContent {
            Box(Modifier.onGloballyPositioned { outer = it.positionInWindow() }) {
                val paddingDp = with(LocalDensity.current) { padding.toDp() }
                Box(Modifier.padding(paddingDp)) {
                    AndroidView(::ComposeView) {
                        it.setContent {
                            Box(
                                Modifier.padding(paddingDp)
                                    .onGloballyPositioned { inner = it.positionInWindow() }
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
            val view = ComposeView(it)
            root.addView(view)

            view.setContent {
                Box {
                    val paddingDp = with(LocalDensity.current) { padding.toDp() }
                    Box(Modifier.padding(paddingDp)) {
                        AndroidView(::ComposeView) {
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
        rule.runOnIdle { startX = coordinates.positionInWindow().x.roundToInt() }

        rule.runOnIdle { topView.visibility = View.GONE }

        rule.runOnIdle {
            assertEquals(100, startX - coordinates.positionInWindow().x.roundToInt())
        }
    }

    @Test
    fun testComposeInsideView_attachingAndDetaching() {
        var composeContent by mutableStateOf(true)
        var node: LayoutNode? = null
        rule.setContent {
            if (composeContent) {
                Box {
                    AndroidView(
                        {
                            ComposeView(it).apply {
                                setViewCompositionStrategy(
                                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                                )
                            }
                        }
                    ) {
                        it.setContent {
                            ComposeNode<LayoutNode, Applier<Any>>(
                                factory = LayoutNode.Constructor,
                                update = {
                                    init { node = this }
                                    set(noOpMeasurePolicy, ComposeUiNode.SetMeasurePolicy)
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
        assertTrue(node!!.isAttached)

        rule.runOnIdle { composeContent = false }

        // The composition has been disposed.
        rule.runOnIdle {
            assertFalse(innerAndroidComposeView!!.isAttachedToWindow)
            // the node stays attached after the compose view is detached
            assertTrue(node!!.isAttached)
        }
    }

    @Test
    fun testAndroidViewHolder_size() {
        val size = 100

        rule.runOnUiThread {
            val root = FrameLayout(rule.activity)
            val composeView = ComposeView(rule.activity)
            composeView.layoutParams = FrameLayout.LayoutParams(size, size)
            root.addView(composeView)
            rule.activity.setContentView(root)
            composeView.setContent {
                AndroidView(::View, Modifier.size(10.dp))
            }
        }

        Espresso.onView(withClassName(endsWith("AndroidViewsHandler"))).check { view, exception ->
            view as AndroidViewsHandler
            // The views handler should match the size of the ComposeView.
            if (view.width != size || view.height != size) throw exception
        }
    }

    @Test
    fun testRedraw_withoutSizeChangeOrStateRead() {
        val squareRef = Ref<ColoredSquareView>()
        var expectedColor = Color.Blue
        rule.setContent {
            AndroidView(::ColoredSquareView) {
                it.color = expectedColor
                it.ref = squareRef
            }
        }
        val squareView = squareRef.value
        assertNotNull(squareView)

        rule.runOnUiThread {
            assertTrue(squareView!!.drawnAfterLastColorChange)
            // Change view attribute using recomposition.
            squareView.color = Color.Green
            expectedColor = Color.Green
        }

        rule.runOnUiThread {
            assertTrue(squareView!!.drawnAfterLastColorChange)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testMove_withoutRedraw() {
        var offset by mutableStateOf(0)
        rule.setContent {
            Box(Modifier.testTag("box").fillMaxSize()) {
                val offsetDp = with(rule.density) { offset.toDp() }
                Box(Modifier.offset(offsetDp, offsetDp)) {
                    AndroidView(::ColoredSquareView, Modifier.graphicsLayer())
                }
            }
        }
        val offsetColorProvider: (IntOffset) -> Color? = {
            if (it.x >= offset && it.x < offset + 100 && it.y >= offset && it.y < offset + 100) {
                Color.Blue
            } else {
                null
            }
        }
        rule.onNodeWithTag("box").captureToImage()
            .assertPixels(expectedColorProvider = offsetColorProvider)
        rule.runOnUiThread {
            offset = 100
        }
        rule.onNodeWithTag("box").captureToImage()
            .assertPixels(expectedColorProvider = offsetColorProvider)
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
                    drawnAfterLastColorChange = false
                    invalidate()
                }
            }

        var drawnAfterLastColorChange = false

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
            drawnAfterLastColorChange = true
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

    fun Modifier.layoutConstraints(childConstraints: Constraints): Modifier =
        this.then(object : LayoutModifier {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                val placeable = measurable.measure(childConstraints)
                return layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        })

    @Composable
    fun Container(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Layout(content, modifier) { measurables, constraints ->
            val placeable = measurables[0].measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }

    private val noOpMeasurePolicy = object : LayoutNode.NoIntrinsicsMeasurePolicy("") {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            return object : MeasureResult {
                override val width = 0
                override val height = 0
                override val alignmentLines: Map<AlignmentLine, Int> get() = mapOf()
                override fun placeChildren() {}
            }
        }
    }
}