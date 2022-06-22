/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.tokens.SliderTokens
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f, 0))
        rule.runOnIdle {
            state.value = -123145f
        }
        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f, 0))
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

        rule.setMaterialContent(lightColorScheme()) {
            Slider(
                modifier = Modifier.testTag(tag), value = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f, 0))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.runOnUiThread {
            state.value = 0.5f
        }

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f, 0))

        rule.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.7f) }

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0.7f, 0f..1f, 0))
    }

    @Test
    fun slider_semantics_stepped() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            Slider(
                modifier = Modifier.testTag(tag), value = state.value,
                onValueChange = { state.value = it }, steps = 4
            )
        }

        rule.onNodeWithTag(tag)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f, 4))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.runOnUiThread {
            state.value = 0.6f
        }

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, 0f..1f, 4))

        rule.onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }

        rule.onNodeWithTag(tag).assertRangeInfoEquals(ProgressBarRangeInfo(0.8f, 0f..1f, 4))
    }

    @Test
    fun slider_semantics_focusable() {
        rule.setMaterialContent(lightColorScheme()) {
            Slider(value = 0f, onValueChange = {}, modifier = Modifier.testTag(tag))
        }

        rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
    }

    @Test
    fun slider_semantics_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Slider(
                value = 0f,
                onValueChange = {},
                modifier = Modifier.testTag(tag),
                enabled = false
            )
        }

        rule.onNodeWithTag(tag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
    }

    @Test
    fun slider_drag() {
        val state = mutableStateOf(0f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
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
            .performTouchInput {
                down(center)
                moveBy(Offset(100f, 0f))
                up()
                expected = calculateFraction(left, right, centerX + 100 - slop)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(expected)
        }
    }

    @Test
    fun slider_drag_out_of_bounds() {
        val state = mutableStateOf(0f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
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
            .performTouchInput {
                down(center)
                moveBy(Offset(width.toFloat(), 0f))
                moveBy(Offset(-width.toFloat(), 0f))
                moveBy(Offset(-width.toFloat(), 0f))
                moveBy(Offset(width.toFloat() + 100f, 0f))
                up()
                expected = calculateFraction(left, right, centerX + 100 - slop)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(expected)
        }
    }

    @Test
    fun slider_tap() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
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
            .performTouchInput {
                down(Offset(centerX + 50, centerY))
                up()
                expected = calculateFraction(left, right, centerX + 50)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(expected)
        }
    }

    /**
     * Guarantee slider doesn't move as we scroll, tapping still works
     */
    @Test
    fun slider_scrollableContainer() {
        val state = mutableStateOf(0f)
        val offset = mutableStateOf(0f)

        rule.setContent {
            Column(
                modifier = Modifier
                    .height(2000.dp)
                    .scrollable(
                        orientation = Orientation.Vertical,
                        state = rememberScrollableState { delta ->
                            offset.value += delta
                            delta
                        })
            ) {
                Slider(
                    modifier = Modifier.testTag(tag),
                    value = state.value,
                    onValueChange = { state.value = it }
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(offset.value).isEqualTo(0f)
        }

        // Just scroll
        rule.onNodeWithTag(tag, useUnmergedTree = true)
            .performTouchInput {
                down(Offset(centerX, centerY))
                moveBy(Offset(0f, 500f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(offset.value).isGreaterThan(0f)
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        // Tap
        var expected = 0f
        rule.onNodeWithTag(tag, useUnmergedTree = true)
            .performTouchInput {
                click(Offset(centerX, centerY))
                expected = calculateFraction(left, right, centerX)
            }

        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(expected)
        }
    }

    @Test
    fun slider_tap_rangeChange() {
        val state = mutableStateOf(0f)
        val rangeEnd = mutableStateOf(0.25f)

        rule.setMaterialContent(lightColorScheme()) {
            Slider(
                modifier = Modifier.testTag(tag),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..rangeEnd.value
            )
        }
        // change to 1 since [calculateFraction] coerces between 0..1
        rule.runOnUiThread {
            rangeEnd.value = 1f
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                click(Offset(centerX + 50, centerY))
                expected = calculateFraction(left, right, centerX + 50)
            }

        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.001f).of(expected)
        }
    }

    @Test
    fun slider_drag_rtl() {
        val state = mutableStateOf(0f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                slop = LocalViewConfiguration.current.touchSlop
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
            .performTouchInput {
                down(center)
                moveBy(Offset(100f, 0f))
                up()
                // subtract here as we're in rtl and going in the opposite direction
                expected = calculateFraction(left, right, centerX - 100 + slop)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.002f).of(expected)
        }
    }

    @Test
    fun slider_tap_rtl() {
        val state = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
            .performTouchInput {
                down(Offset(centerX + 50, centerY))
                up()
                expected = calculateFraction(left, right, centerX - 50)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value).isWithin(0.002f).of(expected)
        }
    }

    private fun calculateFraction(left: Float, right: Float, pos: Float) = with(rule.density) {
        val offset = (ThumbWidth / 2).toPx()
        val start = left + offset
        val end = right - offset
        ((pos - start) / (end - start)).coerceIn(0f, 1f)
    }

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

    @Test
    fun slider_min_size() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.requiredSize(0.dp)) {
                Slider(
                    modifier = Modifier.testTag(tag),
                    value = 0f,
                    onValueChange = { }
                )
            }
        }

        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(SliderTokens.HandleWidth)
            .assertHeightIsEqualTo(SliderTokens.HandleHeight)
    }

    @Test
    fun slider_noUnwantedCallbackCalls() {
        val state = mutableStateOf(0f)
        val callCount = mutableStateOf(0f)

        rule.setMaterialContent(lightColorScheme()) {
            Slider(
                modifier = Modifier.testTag(tag),
                value = state.value,
                onValueChange = {
                    callCount.value += 1
                }
            )
        }

        rule.runOnIdle {
            Truth.assertThat(callCount.value).isEqualTo(0f)
        }
    }

    @Test
    fun slider_valueChangeFinished_calledOnce() {
        val state = mutableStateOf(0f)
        val callCount = mutableStateOf(0)

        rule.setMaterialContent(lightColorScheme()) {
            Slider(
                modifier = Modifier.testTag(tag),
                value = state.value,
                onValueChangeFinished = {
                    callCount.value += 1
                },
                onValueChange = { state.value = it }
            )
        }

        rule.runOnIdle {
            Truth.assertThat(callCount.value).isEqualTo(0)
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(center)
            moveBy(Offset(50f, 50f))
            up()
        }

        rule.runOnIdle {
            Truth.assertThat(callCount.value).isEqualTo(1)
        }
    }

    @Test
    fun slider_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitSlider by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitSlider) {
                    Slider(
                        modifier = Modifier.testTag(tag),
                        value = 0.5f,
                        onValueChange = {},
                        interactionSource = interactionSource
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset(100f, 0f))
            }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose
        rule.runOnIdle {
            emitSlider = false
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            Truth.assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            Truth.assertThat((interactions[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun slider_onValueChangedFinish_afterTap() {
        var changedFlag = false
        rule.setContent {
            Slider(
                modifier = Modifier.testTag(tag),
                value = 0.0f,
                onValueChangeFinished = { changedFlag = true },
                onValueChange = {}
            )
        }

        rule.onNodeWithTag(tag)
            .performTouchInput {
                click(center)
            }

        rule.runOnIdle {
            Truth.assertThat(changedFlag).isTrue()
        }
    }

    @Test
    fun slider_zero_width() {
        rule.setMaterialContentForSizeAssertions(
            parentMaxHeight = 0.dp,
            parentMaxWidth = 0.dp
        ) { Slider(value = 1f, onValueChange = {}) }
            .assertHeightIsEqualTo(0.dp)
            .assertWidthIsEqualTo(0.dp)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_dragThumb() {
        val state = mutableStateOf(0f..1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(
                modifier = Modifier.testTag(tag),
                values = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f..1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset(slop, 0f))
                moveBy(Offset(100f, 0f))
                expected = calculateFraction(left, right, centerX + 100)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value.start).isEqualTo(0f)
            Truth.assertThat(state.value.endInclusive).isWithin(0.001f).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_drag_out_of_bounds() {
        val state = mutableStateOf(0f..1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(
                modifier = Modifier.testTag(tag),
                values = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f..1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset(slop, 0f))
                moveBy(Offset(width.toFloat(), 0f))
                moveBy(Offset(-width.toFloat(), 0f))
                moveBy(Offset(-width.toFloat(), 0f))
                moveBy(Offset(width.toFloat() + 100f, 0f))
                up()
                expected = calculateFraction(left, right, centerX + 100)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value.start).isEqualTo(0f)
            Truth.assertThat(state.value.endInclusive).isWithin(0.001f).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_drag_overlap_thumbs() {
        val state = mutableStateOf(0.5f..1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(
                modifier = Modifier.testTag(tag),
                values = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(centerRight)
                moveBy(Offset(-slop, 0f))
                moveBy(Offset(-width.toFloat(), 0f))
                up()
            }
        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(0.5f..0.5f)
        }

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset(-slop, 0f))
                moveBy(Offset(-width.toFloat(), 0f))
                up()
            }
        rule.runOnIdle {
            Truth.assertThat(state.value).isEqualTo(0.0f..0.5f)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_tap() {
        val state = mutableStateOf(0f..1f)

        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(
                modifier = Modifier.testTag(tag),
                values = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f..1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(Offset(centerX + 50, centerY))
                up()
                expected = calculateFraction(left, right, centerX + 50)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value.endInclusive).isWithin(0.001f).of(expected)
            Truth.assertThat(state.value.start).isEqualTo(0f)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_tap_rangeChange() {
        val state = mutableStateOf(0f..25f)
        val rangeEnd = mutableStateOf(.25f)

        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(
                modifier = Modifier.testTag(tag),
                values = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..rangeEnd.value
            )
        }
        // change to 1 since [calculateFraction] coerces between 0..1
        rule.runOnUiThread {
            rangeEnd.value = 1f
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(Offset(centerX + 50, centerY))
                up()
                expected = calculateFraction(left, right, centerX + 50)
            }

        rule.runOnIdle {
            Truth.assertThat(state.value.endInclusive).isWithin(0.001f).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_drag_rtl() {
        val state = mutableStateOf(0f..1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                slop = LocalViewConfiguration.current.touchSlop
                RangeSlider(
                    modifier = Modifier.testTag(tag),
                    values = state.value,
                    onValueChange = { state.value = it }
                )
            }
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f..1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset(slop, 0f))
                moveBy(Offset(100f, 0f))
                up()
                // subtract here as we're in rtl and going in the opposite direction
                expected = calculateFraction(left, right, centerX - 100)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value.start).isEqualTo(0f)
            Truth.assertThat(state.value.endInclusive).isWithin(0.001f).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_drag_out_of_bounds_rtl() {
        val state = mutableStateOf(0f..1f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                slop = LocalViewConfiguration.current.touchSlop
                RangeSlider(
                    modifier = Modifier.testTag(tag),
                    values = state.value,
                    onValueChange = { state.value = it }
                )
            }
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f..1f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset(slop, 0f))
                moveBy(Offset(width.toFloat(), 0f))
                moveBy(Offset(-width.toFloat(), 0f))
                moveBy(Offset(-width.toFloat(), 0f))
                moveBy(Offset(width.toFloat() + 100f, 0f))
                up()
                // subtract here as we're in rtl and going in the opposite direction
                expected = calculateFraction(left, right, centerX - 100)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value.start).isEqualTo(0f)
            Truth.assertThat(state.value.endInclusive).isWithin(0.001f).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_closeThumbs_dragRight() {
        val state = mutableStateOf(0.5f..0.5f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(
                modifier = Modifier.testTag(tag),
                values = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0.5f..0.5f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset(slop, 0f))
                moveBy(Offset(100f, 0f))
                up()
                // subtract here as we're in rtl and going in the opposite direction
                expected = calculateFraction(left, right, centerX + 100)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value.start).isEqualTo(0.5f)
            Truth.assertThat(state.value.endInclusive).isWithin(0.001f).of(expected)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_closeThumbs_dragLeft() {
        val state = mutableStateOf(0.5f..0.5f)
        var slop = 0f

        rule.setMaterialContent(lightColorScheme()) {
            slop = LocalViewConfiguration.current.touchSlop
            RangeSlider(
                modifier = Modifier.testTag(tag),
                values = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0.5f..0.5f)
        }

        var expected = 0f

        rule.onNodeWithTag(tag)
            .performTouchInput {
                down(center)
                moveBy(Offset(-slop - 1, 0f))
                moveBy(Offset(-100f, 0f))
                up()
                // subtract here as we're in rtl and going in the opposite direction
                expected = calculateFraction(left, right, centerX - 100)
            }
        rule.runOnIdle {
            Truth.assertThat(state.value.start).isWithin(0.001f).of(expected)
            Truth.assertThat(state.value.endInclusive).isEqualTo(0.5f)
        }
    }

    /**
     * Regression test for bug: 210289161 where RangeSlider was ignoring some modifiers like weight.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_weightModifier() {
        var sliderBounds = Rect(0f, 0f, 0f, 0f)
        rule.setMaterialContent(lightColorScheme()) {
            with(LocalDensity.current) {
                Row(Modifier.width(500.toDp())) {
                    Spacer(Modifier.requiredSize(100.toDp()))
                    RangeSlider(
                        values = 0f..0.5f,
                        onValueChange = {},
                        modifier = Modifier
                            .testTag(tag)
                            .weight(1f)
                            .onGloballyPositioned {
                                sliderBounds = it.boundsInParent()
                            }
                    )
                    Spacer(Modifier.requiredSize(100.toDp()))
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(sliderBounds.left).isEqualTo(100)
            Truth.assertThat(sliderBounds.right).isEqualTo(400)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_semantics_continuous() {
        val state = mutableStateOf(0f..1f)

        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(
                modifier = Modifier.testTag(tag), values = state.value,
                onValueChange = { state.value = it }
            )
        }

        rule.onAllNodes(isFocusable(), true)[0]
            .assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..1f, 0))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.onAllNodes(isFocusable(), true)[1]
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f, 0))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))

        rule.runOnUiThread {
            state.value = 0.5f..0.75f
        }

        rule.onAllNodes(isFocusable(), true)[0].assertRangeInfoEquals(
            ProgressBarRangeInfo(
                0.5f,
                0f..0.75f,
                0
            )
        )

        rule.onAllNodes(isFocusable(), true)[1].assertRangeInfoEquals(
            ProgressBarRangeInfo(
                0.75f,
                0.5f..1f,
                0
            )
        )

        rule.onAllNodes(isFocusable(), true)[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.6f) }

        rule.onAllNodes(isFocusable(), true)[1]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.8f) }

        rule.onAllNodes(isFocusable(), true)[0]
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, 0f..0.8f, 0))

        rule.onAllNodes(isFocusable(), true)[1]
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.8f, 0.6f..1f, 0))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSlider_semantics_stepped() {
        val state = mutableStateOf(0f..20f)
        // Slider with [0,5,10,15,20] possible values
        rule.setMaterialContent(lightColorScheme()) {
            RangeSlider(
                modifier = Modifier.testTag(tag), values = state.value,
                steps = 3,
                valueRange = 0f..20f,
                onValueChange = { state.value = it },
            )
        }

        rule.runOnUiThread {
            state.value = 5f..10f
        }

        rule.onAllNodes(isFocusable(), true)[0].assertRangeInfoEquals(
            ProgressBarRangeInfo(
                5f,
                0f..10f,
                1
            )
        )

        rule.onAllNodes(isFocusable(), true)[1].assertRangeInfoEquals(
            ProgressBarRangeInfo(
                10f,
                5f..20f,
                2,
            )
        )

        rule.onAllNodes(isFocusable(), true)[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(10f) }

        rule.onAllNodes(isFocusable(), true)[1]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(15f) }

        rule.onAllNodes(isFocusable(), true)[0]
            .assertRangeInfoEquals(ProgressBarRangeInfo(10f, 0f..15f, 2))

        rule.onAllNodes(isFocusable(), true)[1]
            .assertRangeInfoEquals(ProgressBarRangeInfo(15f, 10f..20f, 1))
    }
}