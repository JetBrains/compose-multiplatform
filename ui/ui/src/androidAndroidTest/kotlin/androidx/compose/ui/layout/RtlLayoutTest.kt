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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.Ref
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
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
                Providers(LayoutDirectionAmbient provides direction.value) {
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

    @Composable
    private fun CustomLayout(
        absolutePositioning: Boolean,
        testLayoutDirection: LayoutDirection
    ) {
        Providers(LayoutDirectionAmbient provides testLayoutDirection) {
            Layout(
                children = @Composable {
                    FixedSize(size, modifier = saveLayoutInfo(position[0], countDownLatch)) {
                    }
                    FixedSize(size, modifier = saveLayoutInfo(position[1], countDownLatch)) {
                    }
                    FixedSize(size, modifier = saveLayoutInfo(position[2], countDownLatch)) {
                    }
                }
            ) { measurables, constraints ->
                val placeables = measurables.map { it.measure(constraints) }
                val width = placeables.fold(0) { sum, p -> sum + p.width }
                val height = placeables.fold(0) { sum, p -> sum + p.height }
                layout(width, height) {
                    var x = 0f
                    var y = 0f
                    for (placeable in placeables) {
                        if (absolutePositioning) {
                            placeable.place(Offset(x, y))
                        } else {
                            placeable.placeRelative(Offset(x, y))
                        }
                        x += placeable.width.toFloat()
                        y += placeable.height.toFloat()
                    }
                }
            }
        }
    }

    @Composable
    private fun saveLayoutInfo(
        position: Ref<Offset>,
        countDownLatch: CountDownLatch
    ): Modifier = Modifier.onGloballyPositioned {
        position.value = it.localToRoot(Offset(0f, 0f))
        countDownLatch.countDown()
    }
}
