/*
 * Copyright 2023 The Android Open Source Project
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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ResizingComposeViewTest {

    private var drawLatch = CountDownLatch(1)
    private lateinit var composeView: ComposeView

    @Before
    fun setup() {
        composeView = ComposeView(rule.activity)
    }

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule(
        TestActivity::class.java
    )

    @Test
    fun whenParentIsMeasuringTwiceWithDifferentConstraints() {
        var height by mutableStateOf(10)
        rule.runOnUiThread {
            val linearLayout = LinearLayout(rule.activity)
            linearLayout.orientation = LinearLayout.VERTICAL
            rule.activity.setContentView(linearLayout)
            linearLayout.addView(
                composeView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
            linearLayout.addView(
                View(rule.activity),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    10000f
                )
            )
            composeView.setContent {
                ResizingChild(layoutHeight = { height })
            }
        }

        awaitDrawAndAssertSizes()
        rule.runOnUiThread {
            height = 20
            drawLatch = CountDownLatch(1)
        }

        awaitDrawAndAssertSizes()
    }

    @Test
    fun whenMeasuredWithWrapContent() {
        var height by mutableStateOf(10)

        rule.runOnUiThread {
            rule.activity.setContentView(
                composeView, WrapContentLayoutParams
            )
            composeView.setContent {
                ResizingChild(layoutHeight = { height })
            }
        }

        awaitDrawAndAssertSizes()
        rule.runOnUiThread {
            height = 20
            drawLatch = CountDownLatch(1)
        }

        awaitDrawAndAssertSizes()
    }

    @Test
    fun whenMeasuredWithFixedConstraints() {
        var childHeight by mutableStateOf(10)
        val viewSize = 30
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)

        rule.runOnUiThread {
            parent.addView(composeView, ViewGroup.LayoutParams(viewSize, viewSize))
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                ResizingChild(layoutHeight = { childHeight }, viewHeight = { viewSize })
            }
        }

        awaitDrawAndAssertSizes()
        rule.runOnUiThread {
            childHeight = 20
            drawLatch = CountDownLatch(1)
            parent.requestLayoutCalled = false
        }

        awaitDrawAndAssertSizes()
        // as the ComposeView is measured with fixed size parent shouldn't be remeasured
        assertThat(parent.requestLayoutCalled).isFalse()
    }

    @Test
    fun whenInsideComposableParentWithFixedSize() {
        var childHeight by mutableStateOf(10)
        val parentSize = 30
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)

        rule.runOnUiThread {
            parent.addView(composeView, WrapContentLayoutParams)
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                Layout(
                    modifier = Modifier.layout { measurable, _ ->
                        // this modifier sets a fixed size on a parent similarly to how
                        // Modifier.fillMaxSize() or Modifier.size(foo) would do
                        val placeable =
                            measurable.measure(Constraints.fixed(parentSize, parentSize))
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
                    content = {
                        ResizingChild(layoutHeight = { childHeight }, viewHeight = { parentSize })
                    }
                ) { measurables, constraints ->
                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
            }
        }

        awaitDrawAndAssertSizes()
        rule.runOnUiThread {
            childHeight = 20
            drawLatch = CountDownLatch(1)
            parent.requestLayoutCalled = false
        }

        awaitDrawAndAssertSizes()
        // as the child is not affecting size parent view shouldn't be remeasured
        assertThat(parent.requestLayoutCalled).isFalse()
    }

    @Test
    fun whenParentIsMeasuringInLayoutBlock() {
        var childHeight by mutableStateOf(10)
        val parentSize = 30
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)

        rule.runOnUiThread {
            parent.addView(composeView, WrapContentLayoutParams)
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                Layout(
                    content = {
                        ResizingChild(layoutHeight = { childHeight }, viewHeight = { parentSize })
                    }
                ) { measurables, _ ->
                    layout(parentSize, parentSize) {
                        val placeable =
                            measurables[0].measure(Constraints.fixed(parentSize, parentSize))
                        placeable.place(0, 0)
                    }
                }
            }
        }

        awaitDrawAndAssertSizes()
        rule.runOnUiThread {
            childHeight = 20
            drawLatch = CountDownLatch(1)
            parent.requestLayoutCalled = false
        }

        awaitDrawAndAssertSizes()
        // as the child is not affecting size parent view shouldn't be remeasured
        assertThat(parent.requestLayoutCalled).isFalse()
    }

    @Test
    fun whenParentIsSettingFixedIntrinsicsSize() {
        var intrinsicsHeight by mutableStateOf(10)
        val parent = RequestLayoutTrackingFrameLayout(rule.activity)

        rule.runOnUiThread {
            parent.addView(composeView, WrapContentLayoutParams)
            rule.activity.setContentView(parent, WrapContentLayoutParams)
            composeView.setContent {
                Layout(
                    modifier = Modifier.layout { measurable, _ ->
                        val intrinsicsSize = measurable.minIntrinsicHeight(Int.MAX_VALUE)
                        val placeable =
                            measurable.measure(Constraints.fixed(intrinsicsSize, intrinsicsSize))
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
                    content = {
                        IntrinsicsChild(intrinsicsHeight = { intrinsicsHeight })
                    }
                ) { measurables, constraints ->
                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
            }
        }

        awaitDrawAndAssertSizes()
        rule.runOnUiThread {
            intrinsicsHeight = 20
            drawLatch = CountDownLatch(1)
        }

        awaitDrawAndAssertSizes()
    }

    private fun awaitDrawAndAssertSizes() {
        Assert.assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        // size assertion is done inside Modifier.drawBehind() which calls countDown() on the latch
    }

    @Composable
    private fun ResizingChild(
        layoutHeight: () -> Int,
        viewHeight: () -> Int = layoutHeight,
    ) {
        Layout(
            {},
            Modifier.drawBehind {
                val expectedLayoutHeight = Snapshot.withoutReadObservation { layoutHeight() }
                assertWithMessage("Layout size is wrong")
                    .that(size.height.roundToInt()).isEqualTo(expectedLayoutHeight)
                val expectedViewHeight = Snapshot.withoutReadObservation { viewHeight() }
                assertWithMessage("ComposeView size is wrong")
                    .that(composeView.measuredHeight).isEqualTo(expectedViewHeight)
                drawLatch.countDown()
            }
        ) { _, constraints ->
            layout(constraints.maxWidth, layoutHeight()) {}
        }
    }

    @Composable
    private fun IntrinsicsChild(
        intrinsicsHeight: () -> Int
    ) {
        Layout(
            {},
            Modifier.drawBehind {
                val expectedHeight = Snapshot.withoutReadObservation { intrinsicsHeight() }
                assertWithMessage("Layout size is wrong")
                    .that(size.height.roundToInt()).isEqualTo(expectedHeight)
                assertWithMessage("ComposeView size is wrong")
                    .that(composeView.measuredHeight).isEqualTo(expectedHeight)
                drawLatch.countDown()
            },
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints
                ): MeasureResult {
                    return layout(constraints.maxWidth, constraints.maxHeight) {}
                }

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int = intrinsicsHeight()
            }
        )
    }
}

private class RequestLayoutTrackingFrameLayout(context: Context) : FrameLayout(context) {

    var requestLayoutCalled = false

    override fun requestLayout() {
        super.requestLayout()
        requestLayoutCalled = true
    }
}

private val WrapContentLayoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.WRAP_CONTENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
)
