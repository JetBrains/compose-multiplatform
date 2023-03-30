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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.Ref
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun textLayout() {
        val textSize = Ref<IntSize>()
        val doubleTextSize = Ref<IntSize>()
        rule.setContent {
            TestingText(
                "aa",
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    textSize.value = coordinates.size
                }
            )
            TestingText(
                "aaaa",
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    doubleTextSize.value = coordinates.size
                }
            )
        }

        rule.runOnIdle {
            assertThat(textSize.value).isNotNull()
            assertThat(doubleTextSize.value).isNotNull()
            assertThat(textSize.value!!.width).isGreaterThan(0)
            assertThat(textSize.value!!.height).isGreaterThan(0)
            assertThat(textSize.value!!.width * 2).isEqualTo(doubleTextSize.value!!.width)
            assertThat(textSize.value!!.height).isEqualTo(doubleTextSize.value!!.height)
        }
    }

    @Test
    fun textLayout_intrinsicMeasurements() {
        val textSize = Ref<IntSize>()
        val doubleTextSize = Ref<IntSize>()
        var textMeasurable by mutableStateOf<Measurable?>(null)

        rule.setContent {
            TestingText(
                "aa ",
                modifier = Modifier.onSizeChanged { textSize.value = it }
            )
            TestingText(
                "aa aa ",
                modifier = Modifier.onSizeChanged { doubleTextSize.value = it }
            )

            Layout(
                content = {
                    TestingText("aa aa ")
                },
                measurePolicy = object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints
                    ): MeasureResult {
                        textMeasurable = measurables.first()
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
            )
        }

        rule.runOnIdle {
            val textWidth = textSize.value!!.width
            val textHeight = textSize.value!!.height
            val doubleTextWidth = doubleTextSize.value!!.width

            textMeasurable!!.let { textMeasurable ->
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
            }
        }
    }

    @Test
    fun textLayout_providesBaselines_whenUnconstrained() {
        var firstBaseline by mutableStateOf(-1)
        var lastBaseline by mutableStateOf(-1)

        rule.setContent {
            Layout({
                TestingText("aa")
            }) { measurables, _ ->
                val placeable = measurables.first().measure(Constraints())
                firstBaseline = placeable[FirstBaseline]
                lastBaseline = placeable[LastBaseline]
                layout(0, 0) {}
            }
        }

        rule.runOnIdle {
            assertThat(firstBaseline).isGreaterThan(-1)
            assertThat(lastBaseline).isGreaterThan(-1)
            assertThat(firstBaseline).isEqualTo(lastBaseline)
        }
    }

    @Test
    fun textLayout_providesBaselines_whenZeroMaxWidth() {
        var firstBaseline by mutableStateOf(-1)
        var lastBaseline by mutableStateOf(-1)

        rule.setContent {
            Layout({
                TestingText("aa")
            }) { measurables, _ ->
                val placeable = measurables.first().measure(Constraints(maxWidth = 0))
                firstBaseline = placeable[FirstBaseline]
                lastBaseline = placeable[LastBaseline]
                layout(0, 0) {}
            }
        }

        rule.runOnIdle {
            assertThat(firstBaseline).isGreaterThan(-1)
            assertThat(lastBaseline).isGreaterThan(-1)
            assertThat(firstBaseline).isLessThan(lastBaseline)
        }
    }

    @Test
    fun textLayout_OnTextLayoutCallback() {
        val resultsFromCallback = mutableListOf<TextLayoutResult>()
        rule.setContent {
            TestingText("aa", onTextLayout = { resultsFromCallback += it })
        }

        rule.runOnIdle {
            assertThat(resultsFromCallback).hasSize(1)
        }
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