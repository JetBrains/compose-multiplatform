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

package androidx.compose.material

import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.AccessibilityRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.assert
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertValueEquals
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.center
import androidx.ui.test.centerX
import androidx.ui.test.centerY
import androidx.ui.test.createComposeRule
import androidx.ui.test.down
import androidx.ui.test.left
import androidx.ui.test.moveBy
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.performSemanticsAction
import androidx.ui.test.right
import androidx.ui.test.up
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs

@MediumTest
@RunWith(AndroidJUnit4::class)
class SliderTest {
    private val tag = "slider"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun sliderPosition_valueCoercion() {
        val state = mutableStateOf(0f)
        rule.setContent {
            Slider(
                modifier = Modifier.testTag(tag),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..1f
            )
        }
        rule.runOnIdle {
            state.value = 2f
        }
        rule.onNodeWithTag(tag).assertValueEquals("100 percent")
        rule.runOnIdle {
            state.value = -123145f
        }
        rule.onNodeWithTag(tag).assertValueEquals("0 percent")
    }

    @Test(expected = IllegalArgumentException::class)
    fun sliderPosition_stepsThrowWhenLessThanZero() {
        rule.setContent {
            Slider(value = 0f, onValueChange = {}, steps = -1)
        }
    }

    @Test
    fun slider_semantics_continuous() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent {
            Slider(
                modifier = Modifier.testTag(tag), value = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals("0 percent")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.AccessibilityRangeInfo,
                    AccessibilityRangeInfo(0f, 0f..1f, 0)
                )
            )
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.runOnUiThread {
            state.value = 0.5f
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals("50 percent")

        rule.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.7f) }

        rule.onNodeWithTag(tag)
            .assertValueEquals("70 percent")
    }

    @Test
    fun slider_semantics_stepped() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent {
            Slider(
                modifier = Modifier.testTag(tag), value = state.value,
                onValueChange = { state.value = it }, steps = 4
            )
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals("0 percent")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.AccessibilityRangeInfo,
                    AccessibilityRangeInfo(0f, 0f..1f, 4)
                )
            )
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.runOnUiThread {
            state.value = 0.6f
        }

        rule.onNodeWithTag(tag)
            .assertValueEquals("60 percent")

        rule.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }

        rule.onNodeWithTag(tag)
            .assertValueEquals("80 percent")
    }

    @Test
    fun slider_drag() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent {
            Slider(
                modifier = Modifier.testTag(tag),
                value = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performGesture {
                down(center)
                moveBy(Offset(100f, 0f))
                up()
                expected = calculateFraction(left, right, centerX + 100)
            }
        rule.runOnIdle {
            Truth.assertThat(abs(state.value - expected)).isLessThan(0.001f)
        }
    }

    @Test
    fun slider_tap() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent {
            Slider(
                modifier = Modifier.testTag(tag),
                value = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performGesture {
                down(Offset(centerX + 50, centerY))
                up()
                expected = calculateFraction(left, right, centerX + 50)
            }
        rule.runOnIdle {
            Truth.assertThat(abs(state.value - expected)).isLessThan(0.001f)
        }
    }

    @Test
    fun slider_drag_rtl() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                Slider(
                    modifier = Modifier.testTag(tag),
                    value = state.value,
                    onValueChange = { state.value = it }
                )
            }
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performGesture {
                down(center)
                moveBy(Offset(100f, 0f))
                up()
                // subtract here as we're in rtl and going in the opposite direction
                expected = calculateFraction(left, right, centerX - 100)
            }
        rule.runOnIdle {
            Truth.assertThat(abs(state.value - expected)).isLessThan(0.001f)
        }
    }

    @Test
    fun slider_tap_rtl() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                Slider(
                    modifier = Modifier.testTag(tag),
                    value = state.value,
                    onValueChange = { state.value = it }
                )
            }
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performGesture {
                down(Offset(centerX + 50, centerY))
                up()
                expected = calculateFraction(left, right, centerX - 50)
            }
        rule.runOnIdle {
            Truth.assertThat(abs(state.value - expected)).isLessThan(0.001f)
        }
    }

    private fun calculateFraction(a: Float, b: Float, pos: Float) =
        ((pos - a) / (b - a)).coerceIn(0f, 1f)

    @Test
    fun slider_sizes() {
        val state = mutableStateOf(0f)
        rule
            .setMaterialContentForSizeAssertions(
                parentMaxWidth = 100.dp,
                parentMaxHeight = 100.dp
            ) { Slider(value = state.value, onValueChange = { state.value = it }) }
            .assertHeightIsEqualTo(48.dp)
            .assertWidthIsEqualTo(100.dp)
    }
}
