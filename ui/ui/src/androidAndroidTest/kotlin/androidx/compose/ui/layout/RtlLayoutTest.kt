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

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class RtlLayoutTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule =
        androidx.test.rule.ActivityTestRule<TestActivity>(
            TestActivity::class.java
        )
    private lateinit var activity: TestActivity
    internal lateinit var density: Density
    internal lateinit var countDownLatch: CountDownLatch
    internal lateinit var position: Array<Ref<Offset>>
    private val size = 100

    @Before
    fun setup() {
        activity = activityTestRule.activity
        density = Density(activity)
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        position = Array(3) { Ref<Offset>() }
        countDownLatch = CountDownLatch(3)
    }

    @Test
    fun customLayout_absolutePositioning() = with(density) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                CustomLayout(true, LayoutDirection.Ltr)
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        assertEquals(Offset(0f, 0f), position[0].value)
        assertEquals(Offset(size.toFloat(), size.toFloat()), position[1].value)
        assertEquals(
            Offset(
                (size * 2).toFloat(),
                (size * 2).toFloat()
            ),
            position[2].value
        )
    }

    @Test
    fun customLayout_absolutePositioning_rtl() = with(density) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                CustomLayout(true, LayoutDirection.Rtl)
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        assertEquals(
            Offset(0f, 0f),
            position[0].value
        )
        assertEquals(
            Offset(
                size.toFloat(),
                size.toFloat()
            ),
            position[1].value
        )
        assertEquals(
            Offset(
                (size * 2).toFloat(),
                (size * 2).toFloat()
            ),
            position[2].value
        )
    }

    @Test
    fun customLayout_positioning() = with(density) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                CustomLayout(false, LayoutDirection.Ltr)
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)
        assertEquals(Offset(0f, 0f), position[0].value)
        assertEquals(Offset(size.toFloat(), size.toFloat()), position[1].value)
        assertEquals(
            Offset(
                (size * 2).toFloat(),
                (size * 2).toFloat()
            ),
            position[2].value
        )
    }

    @Test
    fun customLayout_positioning_rtl() = with(density) {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                CustomLayout(false, LayoutDirection.Rtl)
            }
        }

        countDownLatch.await(1, TimeUnit.SECONDS)

        countDownLatch.await(1, TimeUnit.SECONDS)
        assertEquals(
            Offset(
                (size * 2).toFloat(),
                0f
            ),
            position[0].value
        )
        assertEquals(
            Offset(size.toFloat(), size.toFloat()),
            position[1].value
        )
        assertEquals(Offset(0f, (size * 2).toFloat()), position[2].value)
    }

    @Test
    fun customLayout_updatingDirectionCausesRemeasure() {
        val direction = mutableStateOf(LayoutDirection.Rtl)
        var latch = CountDownLatch(1)
        var actualDirection: LayoutDirection? = null

        activityTestRule.runOnUiThread {
            activity.setContent {
                val children = @Composable {
                    Layout({}) { _, _ ->
                        actualDirection = layoutDirection
                        latch.countDown()
                        layout(100, 100) {}
                    }
                }
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    Layout(children) { measurables, constraints ->
                        layout(100, 100) {
                            measurables.first().measure(constraints).placeRelative(0, 0)
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Rtl, actualDirection)

        latch = CountDownLatch(1)
        activityTestRule.runOnUiThread { direction.value = LayoutDirection.Ltr }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Ltr, actualDirection)
    }
    @Test
    fun testModifiedLayoutDirection_inMeasureScope() {
        val latch = CountDownLatch(1)
        val resultLayoutDirection = Ref<LayoutDirection>()

        activityTestRule.runOnUiThread {
            activity.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Layout(content = {}) { _, _ ->
                        resultLayoutDirection.value = layoutDirection
                        latch.countDown()
                        layout(0, 0) {}
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(LayoutDirection.Rtl == resultLayoutDirection.value)
    }

    @Test
    fun testModifiedLayoutDirection_inIntrinsicsMeasure() {
        val latch = CountDownLatch(1)
        var resultLayoutDirection: LayoutDirection? = null

        activityTestRule.runOnUiThread {
            activity.setContent {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val measurePolicy = object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints
                        ) = layout(0, 0) {}

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
                        ): Int {
                            resultLayoutDirection = this.layoutDirection
                            latch.countDown()
                            return 0
                        }

                        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                            measurables: List<IntrinsicMeasurable>,
                            width: Int
                        ) = 0
                    }
                    Layout(
                        content = {},
                        modifier = Modifier.width(IntrinsicSize.Max),
                        measurePolicy = measurePolicy
                    )
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        Assert.assertNotNull(resultLayoutDirection)
        assertTrue(LayoutDirection.Rtl == resultLayoutDirection)
    }

    @Test
    fun testRestoreLocaleLayoutDirection() {
        val latch = CountDownLatch(1)
        val resultLayoutDirection = Ref<LayoutDirection>()

        activityTestRule.runOnUiThread {
            activity.setContent {
                val initialLayoutDirection = LocalLayoutDirection.current
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides initialLayoutDirection
                        ) {
                            Layout({}) { _, _ ->
                                resultLayoutDirection.value = layoutDirection
                                latch.countDown()
                                layout(0, 0) {}
                            }
                        }
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Ltr, resultLayoutDirection.value)
    }

    @Composable
    private fun CustomLayout(
        absolutePositioning: Boolean,
        testLayoutDirection: LayoutDirection
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides testLayoutDirection) {
            Layout(
                content = {
                    FixedSize(size, modifier = Modifier.saveLayoutInfo(position[0], countDownLatch))
                    FixedSize(size, modifier = Modifier.saveLayoutInfo(position[1], countDownLatch))
                    FixedSize(size, modifier = Modifier.saveLayoutInfo(position[2], countDownLatch))
                }
            ) { measurables, constraints ->
                val placeables = measurables.map { it.measure(constraints) }
                val width = placeables.fold(0) { sum, p -> sum + p.width }
                val height = placeables.fold(0) { sum, p -> sum + p.height }
                layout(width, height) {
                    var x = 0
                    var y = 0
                    for (placeable in placeables) {
                        if (absolutePositioning) {
                            placeable.place(x, y)
                        } else {
                            placeable.placeRelative(x, y)
                        }
                        x += placeable.width
                        y += placeable.height
                    }
                }
            }
        }
    }

    private fun Modifier.saveLayoutInfo(
        position: Ref<Offset>,
        countDownLatch: CountDownLatch
    ): Modifier = onGloballyPositioned {
        position.value = it.localToRoot(Offset(0f, 0f))
        countDownLatch.countDown()
    }
}
