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

package androidx.compose.foundation.text

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextLayoutTest {
    @Suppress("DEPRECATION")
    @get:Rule
    internal val activityTestRule = androidx.test.rule.ActivityTestRule(
        ComponentActivity::class.java
    )
    private lateinit var activity: ComponentActivity
    private lateinit var density: Density

    @Before
    fun setup() {
        activity = activityTestRule.activity
        density = Density(activity)
    }

    @Test
    fun testTextLayout() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val textSize = Ref<IntSize>()
        val doubleTextSize = Ref<IntSize>()
        show {
            TestingText(
                "aa",
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    textSize.value = coordinates.size
                    layoutLatch.countDown()
                }
            )
            TestingText(
                "aaaa",
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    doubleTextSize.value = coordinates.size
                    layoutLatch.countDown()
                }
            )
        }
        assertThat(layoutLatch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(textSize.value).isNotNull()
        assertThat(doubleTextSize.value).isNotNull()
        assertThat(textSize.value!!.width).isGreaterThan(0)
        assertThat(textSize.value!!.height).isGreaterThan(0)
        assertThat(textSize.value!!.width * 2).isEqualTo(doubleTextSize.value!!.width)
        assertThat(textSize.value!!.height).isEqualTo(doubleTextSize.value!!.height)
    }

    @Test
    fun testTextLayout_intrinsicMeasurements() = with(density) {
        val layoutLatch = CountDownLatch(2)
        val textSize = Ref<IntSize>()
        val doubleTextSize = Ref<IntSize>()
        show {
            TestingText(
                "aa ",
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    textSize.value = coordinates.size
                    layoutLatch.countDown()
                }
            )
            TestingText(
                "aa aa ",
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    doubleTextSize.value = coordinates.size
                    layoutLatch.countDown()
                }
            )
        }
        assertThat(layoutLatch.await(1, TimeUnit.SECONDS)).isTrue()
        val textWidth = textSize.value!!.width
        val textHeight = textSize.value!!.height
        val doubleTextWidth = doubleTextSize.value!!.width

        val intrinsicsLatch = CountDownLatch(1)
        show {
            val text = @Composable {
                TestingText("aa aa ")
            }
            val measurePolicy = remember {
                object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints
                    ): MeasureResult {
                        val textMeasurable = measurables.first()
                        // Min width.
                        assertThat(textWidth).isEqualTo(textMeasurable.minIntrinsicWidth(0))
                        // Min height.
                        assertThat(textMeasurable.minIntrinsicHeight(textWidth))
                            .isGreaterThan(textHeight)
                        assertThat(textHeight)
                            .isEqualTo(textMeasurable.minIntrinsicHeight(doubleTextWidth))
                        assertThat(textHeight)
                            .isEqualTo(textMeasurable.minIntrinsicHeight(Constraints.Infinity))
                        // Max width.
                        assertThat(doubleTextWidth).isEqualTo(textMeasurable.maxIntrinsicWidth(0))
                        // Max height.
                        assertThat(textMeasurable.maxIntrinsicHeight(textWidth))
                            .isGreaterThan(textHeight)
                        assertThat(textHeight)
                            .isEqualTo(textMeasurable.maxIntrinsicHeight(doubleTextWidth))
                        assertThat(textHeight)
                            .isEqualTo(textMeasurable.maxIntrinsicHeight(Constraints.Infinity))

                        intrinsicsLatch.countDown()

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
            }
            Layout(
                content = text,
                measurePolicy = measurePolicy
            )
        }
        assertThat(intrinsicsLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testTextLayout_providesBaselines() = with(density) {
        val layoutLatch = CountDownLatch(2)
        show {
            val text = @Composable {
                TestingText("aa")
            }
            Layout(text) { measurables, _ ->
                val placeable = measurables.first().measure(Constraints())
                assertThat(placeable[FirstBaseline]).isNotNull()
                assertThat(placeable[LastBaseline]).isNotNull()
                assertThat(placeable[FirstBaseline]).isEqualTo(placeable[LastBaseline])
                layoutLatch.countDown()
                layout(0, 0) {}
            }
            Layout(text) { measurables, _ ->
                val placeable = measurables.first().measure(Constraints(maxWidth = 0))
                assertThat(placeable[FirstBaseline]).isNotNull()
                assertThat(placeable[LastBaseline]).isNotNull()
                assertThat(placeable[FirstBaseline])
                    .isLessThan(placeable[LastBaseline])
                layoutLatch.countDown()
                layout(0, 0) {}
            }
        }
        assertThat(layoutLatch.await(1, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testOnTextLayout() = with(density) {
        val layoutLatch = CountDownLatch(1)
        val callback = mock<(TextLayoutResult) -> Unit>()
        show {
            val text = @Composable {
                TestingText("aa", onTextLayout = callback)
            }
            Layout(text) { measurables, _ ->
                measurables.first().measure(Constraints())
                layoutLatch.countDown()
                layout(0, 0) {}
            }
        }
        assertThat(layoutLatch.await(1, TimeUnit.SECONDS)).isTrue()
        verify(callback, times(1)).invoke(any())
    }

    private fun show(composable: @Composable () -> Unit) {
        val runnable = Runnable {
            activity.setContent {
                Layout(composable) { measurables, constraints ->
                    val placeables = measurables.map {
                        it.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    }
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        var top = 0
                        placeables.forEach {
                            it.placeRelative(0, top)
                            top += it.height
                        }
                    }
                }
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }
}

@Composable
private fun TestingText(
    text: String,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val textStyle = remember {
        TextStyle(fontFamily = TEST_FONT_FAMILY)
    }
    BasicText(
        AnnotatedString(text),
        style = textStyle,
        modifier = modifier,
        softWrap = true,
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Clip,
        inlineContent = mapOf(),
        onTextLayout = onTextLayout
    )
}