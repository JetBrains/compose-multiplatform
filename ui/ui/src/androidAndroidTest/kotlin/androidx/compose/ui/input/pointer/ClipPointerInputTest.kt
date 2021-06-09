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

package androidx.compose.ui.input.pointer

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.gesture.MotionEvent
import androidx.compose.ui.gesture.PointerCoords
import androidx.compose.ui.gesture.PointerProperties
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class ClipPointerInputTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity
    private lateinit var view: View

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    /**
     * This test creates a layout of this shape.
     *
     *     0   1   2   3   4
     *   .........   .........
     * 0 .     t .   . t     .
     *   .   |---|---|---|   .
     * 1 . t | t |   | t | t .
     *   ....|---|   |---|....
     * 2     |           |
     *   ....|---|   |---|....
     * 3 . t | t |   | t | t .
     *   .   |---|---|---|   .
     * 4 .     t .   . t     .
     *   .........   .........
     *
     * 4 LayoutNodes with PointerInputModifiers that are positioned by offset modifiers and where
     * pointer input is clipped by a modifier on the parent. 4 touches touch just inside the
     * parent LayoutNode and inside the child LayoutNodes. 8 touches touch just outside the
     * parent LayoutNode but inside the child LayoutNodes.
     *
     * Because clipToBounds is being used on the parent LayoutNode, only the 4 touches inside the
     * parent LayoutNode should hit.
     */
    @Test
    fun clipToBounds_childrenOffsetViaLayout_onlyCorrectPointersHit() {

        val setupLatch = CountDownLatch(2)

        val loggingPim1 = LoggingPim()
        val loggingPim2 = LoggingPim()
        val loggingPim3 = LoggingPim()
        val loggingPim4 = LoggingPim()

        rule.runOnUiThreadIR {
            activity.setContent {

                val children = @Composable {
                    Child(loggingPim1)
                    Child(loggingPim2)
                    Child(loggingPim3)
                    Child(loggingPim4)
                }

                val middle = @Composable {
                    Layout(
                        content = children,
                        modifier = Modifier.clipToBounds()
                    ) { measurables, constraints ->
                        val placeables = measurables.map { m ->
                            m.measure(constraints)
                        }
                        layout(3, 3) {
                            placeables[0].place((-1), (-1))
                            placeables[1].place(2, (-1))
                            placeables[2].place((-1), 2)
                            placeables[3].place(2, 2)
                        }
                    }
                }

                Layout(content = middle) { measurables, constraints ->
                    val placeables = measurables.map { m ->
                        m.measure(constraints)
                    }
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeables[0].place(1, 1)
                        setupLatch.countDown()
                    }
                }
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }

        assertThat(setupLatch.await(2, TimeUnit.SECONDS)).isTrue()

        val offsetsThatHit =
            listOf(
                Offset(1f, 1f),
                Offset(3f, 1f),
                Offset(1f, 3f),
                Offset(3f, 3f)
            )
        val offsetsThatMiss =
            listOf(
                Offset(1f, 0f),
                Offset(3f, 0f),
                Offset(0f, 1f),
                Offset(4f, 1f),
                Offset(0f, 3f),
                Offset(4f, 3f),
                Offset(1f, 4f),
                Offset(3f, 4f)
            )

        val downEvents = mutableListOf<MotionEvent>()
        (offsetsThatHit + offsetsThatMiss).forEachIndexed { index, value ->
            downEvents.add(
                MotionEvent(
                    index,
                    MotionEvent.ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(value.x, value.y)),
                    view
                )
            )
        }

        // Act
        rule.runOnUiThreadIR {
            downEvents.forEach {
                view.dispatchTouchEvent(it)
            }
        }

        // Assert

        assertThat(loggingPim1.log).isEqualTo(listOf(Offset(1f, 1f)))
        assertThat(loggingPim2.log).isEqualTo(listOf(Offset(0f, 1f)))
        assertThat(loggingPim3.log).isEqualTo(listOf(Offset(1f, 0f)))
        assertThat(loggingPim4.log).isEqualTo(listOf(Offset(0f, 0f)))
    }

    /**
     * This test creates a layout of this shape.
     *
     *     0   1   2   3   4
     *   .........   .........
     * 0 .     t .   . t     .
     *   .   |---|---|---|   .
     * 1 . t | t |   | t | t .
     *   ....|---|   |---|....
     * 2     |           |
     *   ....|---|   |---|....
     * 3 . t | t |   | t | t .
     *   .   |---|---|---|   .
     * 4 .     t .   . t     .
     *   .........   .........
     *
     * 4 LayoutNodes with PointerInputModifiers that are positioned by offset modifiers and where
     * pointer input is clipped by a modifier on the parent. 4 touches touch just inside the
     * parent LayoutNode and inside the child LayoutNodes. 8 touches touch just outside the
     * parent LayoutNode but inside the child LayoutNodes.
     *
     * Because clipToBounds is being used on the parent LayoutNode, only the 4 touches inside the
     * parent LayoutNode should hit.
     */
    @Test
    fun clipToBounds_childrenOffsetViaModifier_onlyCorrectPointersHit() {

        val setupLatch = CountDownLatch(2)

        val loggingPim1 = LoggingPim()
        val loggingPim2 = LoggingPim()
        val loggingPim3 = LoggingPim()
        val loggingPim4 = LoggingPim()

        rule.runOnUiThreadIR {
            activity.setContent {

                with(LocalDensity.current) {

                    val children = @Composable {
                        Child(Modifier.offset((-1f).toDp(), (-1f).toDp()).then(loggingPim1))
                        Child(Modifier.offset(2f.toDp(), (-1f).toDp()).then(loggingPim2))
                        Child(Modifier.offset((-1f).toDp(), 2f.toDp()).then(loggingPim3))
                        Child(Modifier.offset(2f.toDp(), 2f.toDp()).then(loggingPim4))
                    }

                    val middle = @Composable {
                        Layout(
                            content = children,
                            modifier = Modifier.clipToBounds()
                        ) { measurables, constraints ->
                            val placeables = measurables.map { m ->
                                m.measure(constraints)
                            }
                            layout(3, 3) {
                                placeables.forEach { it.place(0, 0) }
                            }
                        }
                    }

                    Layout(content = middle) { measurables, constraints ->
                        val placeables = measurables.map { m ->
                            m.measure(constraints)
                        }
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeables[0].place(1, 1)
                            setupLatch.countDown()
                        }
                    }
                }
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }

        assertThat(setupLatch.await(2, TimeUnit.SECONDS)).isTrue()

        val offsetsThatHit =
            listOf(
                Offset(1f, 1f),
                Offset(3f, 1f),
                Offset(1f, 3f),
                Offset(3f, 3f)
            )
        val offsetsThatMiss =
            listOf(
                Offset(1f, 0f),
                Offset(3f, 0f),
                Offset(0f, 1f),
                Offset(4f, 1f),
                Offset(0f, 3f),
                Offset(4f, 3f),
                Offset(1f, 4f),
                Offset(3f, 4f)
            )

        val downEvents = mutableListOf<MotionEvent>()
        (offsetsThatHit + offsetsThatMiss).forEachIndexed { index, value ->
            downEvents.add(
                MotionEvent(
                    index,
                    MotionEvent.ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(value.x, value.y)),
                    view
                )
            )
        }

        // Act
        rule.runOnUiThreadIR {
            downEvents.forEach {
                view.dispatchTouchEvent(it)
            }
        }

        // Assert

        assertThat(loggingPim1.log).isEqualTo(listOf(Offset(1f, 1f)))
        assertThat(loggingPim2.log).isEqualTo(listOf(Offset(0f, 1f)))
        assertThat(loggingPim3.log).isEqualTo(listOf(Offset(1f, 0f)))
        assertThat(loggingPim4.log).isEqualTo(listOf(Offset(0f, 0f)))
    }

    /**
     * This test creates a layout clipped to a rounded rectangle shape (circle).
     * We'll touch in and out of the rounded area.
     */
    @Test
    fun clip_roundedRect() {
        pokeAroundCircle(RoundedCornerShape(50))
    }

    /**
     * This test creates a layout clipped to a rounded rectangle shape (circle), but the
     * corners are defined as larger than the side length
     * We'll touch in and out of the rounded area.
     */
    @Test
    fun clip_roundedRectLargeCorner() {
        pokeAroundCircle(RoundedCornerShape(1.1f))
    }

    /**
     * This test creates a layout clipped to a generic shape (circle).
     * We'll touch in and out of the rounded area.
     */
    @Test
    fun clip_genericShape() {
        pokeAroundCircle(
            GenericShape { size, _ ->
                addOval(Rect(0f, 0f, size.width, size.height))
            }
        )
    }

    /**
     * This test creates a layout clipped to a circle shape.
     * We'll touch in and out of the rounded area.
     */
    fun pokeAroundCircle(shape: Shape) {

        val setupLatch = CountDownLatch(1)

        val loggingPim = LoggingPim()

        rule.runOnUiThreadIR {
            activity.setContent {
                Child(
                    Modifier.clip(shape)
                        .then(loggingPim)
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                p.place(0, 0)
                                setupLatch.countDown()
                            }
                        }
                )
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }

        assertThat(setupLatch.await(2, TimeUnit.SECONDS)).isTrue()

        val offset = 1f / 128f
        val above0 = offset
        val below2 = 2f - offset
        val above2 = 2f + offset
        val below0 = -offset

        val offsetsThatHit =
            listOf(
                Offset(above0, 1f),
                Offset(1f, above0),
                Offset(1f, below2),
                Offset(below2, 1f),
                Offset(1f, 1f),
                Offset(0.5f, 0.5f),
                Offset(0.5f, 1.5f),
                Offset(1.5f, 0.5f),
                Offset(1.5f, 1.5f),
            )
        val offsetsThatMiss =
            listOf(
                Offset(above0, above0),
                Offset(above0, below2),
                Offset(below2, above0),
                Offset(below2, below2),
                Offset(1f, below0),
                Offset(above2, 1f),
                Offset(below0, 1f),
                Offset(1f, above2),
            )

        val downEvents = mutableListOf<MotionEvent>()
        (offsetsThatHit + offsetsThatMiss).forEachIndexed { index, value ->
            downEvents.add(
                MotionEvent(
                    index,
                    MotionEvent.ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(value.x, value.y)),
                    view
                )
            )
        }

        // Act
        rule.runOnUiThreadIR {
            downEvents.forEach {
                view.dispatchTouchEvent(it)
            }
        }

        // Assert
        assertThat(loggingPim.log).isEqualTo(offsetsThatHit)
    }

    /**
     * This creates a clipped rectangle that is smaller than the bounds and ensures that only the
     * clipped area receives touches
     */
    @Test
    fun clip_smallRect() {
        val rectangleShape: Shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ) =
                Outline.Rectangle(
                    Rect(
                        size.width * 0.25f,
                        size.height * 0.25f,
                        size.width * 0.75f,
                        size.height * 0.75f,
                    )
                )
        }

        val setupLatch = CountDownLatch(1)

        val loggingPim = LoggingPim()

        rule.runOnUiThreadIR {
            activity.setContent {
                Child(
                    Modifier.clip(rectangleShape)
                        .then(loggingPim)
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                p.place(0, 0)
                                setupLatch.countDown()
                            }
                        }
                )
            }

            view = activity.findViewById<ViewGroup>(android.R.id.content)
            setupLatch.countDown()
        }

        assertThat(setupLatch.await(2, TimeUnit.SECONDS)).isTrue()
        val offset = 1f / 128f
        val justIn = 1.5f - offset
        val justOut = 0.5f - offset

        val offsetsThatHit =
            listOf(
                Offset(0.5f, 0.5f),
                Offset(justIn, 0.5f),
                Offset(0.5f, justIn),
                Offset(justIn, justIn),

                Offset(1f, 0.5f),
                Offset(0.5f, 1f),
                Offset(1f, justIn),
                Offset(justIn, 1f),

                Offset(1f, 1f),
            )
        val offsetsThatMiss =
            listOf(
                Offset(justOut, 0.5f),
                Offset(0.5f, justOut),
                Offset(1.5f, 0.5f),
                Offset(justIn, justOut),
                Offset(0.5f, 1.5f),
                Offset(justOut, justIn),
                Offset(justIn, 1.5f),
                Offset(1.5f, justIn),
            )

        val downEvents = mutableListOf<MotionEvent>()
        (offsetsThatHit + offsetsThatMiss).forEachIndexed { index, value ->
            downEvents.add(
                MotionEvent(
                    index,
                    MotionEvent.ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(value.x, value.y)),
                    view
                )
            )
        }

        // Act
        rule.runOnUiThreadIR {
            downEvents.forEach {
                view.dispatchTouchEvent(it)
            }
        }

        // Assert
        assertThat(loggingPim.log).isEqualTo(offsetsThatHit)
    }

    @Composable
    fun Child(modifier: Modifier) {
        Layout(content = {}, modifier = modifier) { _, _ ->
            layout(2, 2) {}
        }
    }

    class LoggingPim : PointerInputModifier {
        val log = mutableListOf<Offset>()

        override val pointerInputFilter = object : PointerInputFilter() {
            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize
            ) {
                if (pass == PointerEventPass.Initial) {
                    pointerEvent.changes.forEach {
                        println("testtest, bounds: $bounds")
                        log.add(it.position)
                    }
                }
            }

            override fun onCancel() {
                // Nothing
            }
        }
    }
}
