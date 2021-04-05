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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.animatePanBy
import androidx.compose.foundation.gestures.animateRotateBy
import androidx.compose.foundation.gestures.animateZoomBy
import androidx.compose.foundation.gestures.panBy
import androidx.compose.foundation.gestures.rotateBy
import androidx.compose.foundation.gestures.stopTransformation
import androidx.compose.foundation.gestures.zoomBy
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_TAG = "transformableTag"

private const val EDGE_FUZZ_FACTOR = 0.2f

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class TransformableTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun transformable_zoomIn() {
        var cumulativeScale = 1.0f

        setTransformableContent {
            Modifier.transformable(
                state = rememberTransformableState { zoom, _, _ ->
                    cumulativeScale *= zoom
                }
            )
        }

        rule.onNodeWithTag(TEST_TAG).performGesture {
            val leftStartX = center.x - 10
            val leftEndX = visibleSize.toSize().width * EDGE_FUZZ_FACTOR
            val rightStartX = center.x + 10
            val rightEndX = visibleSize.toSize().width * (1 - EDGE_FUZZ_FACTOR)

            pinch(
                Offset(leftStartX, center.y),
                Offset(leftEndX, center.y),
                Offset(rightStartX, center.y),
                Offset(rightEndX, center.y)
            )
        }

        rule.mainClock.advanceTimeBy(milliseconds = 1000)

        rule.runOnIdle {
            assertWithMessage("Should have scaled at least 4x").that(cumulativeScale).isAtLeast(4f)
        }
    }

    @Test
    fun transformable_pan() {
        var cumulativePan = Offset.Zero
        var touchSlop = 0f

        setTransformableContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Modifier.transformable(
                state = rememberTransformableState { _, pan, _ ->
                    cumulativePan += pan
                }
            )
        }

        val expected = Offset(50f + touchSlop, 0f)

        rule.onNodeWithTag(TEST_TAG).performGesture {
            down(1, center)
            down(2, center + Offset(10f, 10f))
            moveBy(1, expected)
            moveBy(2, expected)
            up(1)
            up(2)
        }

        rule.mainClock.advanceTimeBy(milliseconds = 1000)

        rule.runOnIdle {
            assertWithMessage("Should have panned 20/10").that(cumulativePan).isEqualTo(expected)
        }
    }

    @Test
    fun transformable_rotate() {
        var cumulativeRotation = 0f

        setTransformableContent {
            Modifier.transformable(
                state = rememberTransformableState { _, _, rotation ->
                    cumulativeRotation += rotation
                }
            )
        }

        rule.onNodeWithTag(TEST_TAG).performGesture {
            down(1, center)
            down(2, center + Offset(0f, 50f))
            moveBy(2, Offset(50f, -50f))
            up(1)
            up(2)
        }

        rule.mainClock.advanceTimeBy(milliseconds = 1000)

        rule.runOnIdle {
            assertWithMessage("Should have rotated -90").that(cumulativeRotation).isEqualTo(-90f)
        }
    }

    @Test
    fun transformable_rotationLock() {
        var cumulativeRotation = 0f
        val rotationLock = mutableStateOf(false)
        var touchSlop = 0f

        setTransformableContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Modifier.transformable(
                lockRotationOnZoomPan = rotationLock.value,
                state = rememberTransformableState { _, _, rotation ->
                    cumulativeRotation += rotation
                }
            )
        }

        val panShift = Offset(touchSlop, 0f)

        rule.onNodeWithTag(TEST_TAG).performGesture {
            down(1, center)
            down(2, center + Offset(0f, 50f))
            // first pan a bit
            moveBy(1, panShift)
            moveBy(2, panShift)
            // pan a bit more
            moveBy(1, panShift)
            moveBy(2, panShift)
            // then rotate
            moveBy(2, Offset(50f, -50f))
            up(1)
            up(2)
        }

        rule.mainClock.advanceTimeBy(milliseconds = 1000)

        rule.runOnIdle {
            assertWithMessage("Should have rotated -90").that(cumulativeRotation).isEqualTo(-90f)
            cumulativeRotation = 0f
        }
        rotationLock.value = !rotationLock.value
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).performGesture {
            down(1, center)
            down(2, center + Offset(0f, 50f))
            // first pan a bit
            moveBy(1, panShift)
            moveBy(2, panShift)
            // pan a bit more
            moveBy(1, panShift)
            moveBy(2, panShift)
            // then rotate
            moveBy(2, Offset(50f, -50f))
            up(1)
            up(2)
        }

        rule.mainClock.advanceTimeBy(milliseconds = 1000)

        rule.runOnIdle {
            assertWithMessage("Rotation should be locked").that(cumulativeRotation).isEqualTo(0f)
        }
    }

    @Test
    fun transformable_zoomOut() {
        var cumulativeScale = 1.0f

        setTransformableContent {
            Modifier.transformable(
                state = rememberTransformableState { zoom, _, _ ->
                    cumulativeScale *= zoom
                }
            )
        }

        rule.onNodeWithTag(TEST_TAG).performGesture {
            val leftStartX = visibleSize.toSize().width * EDGE_FUZZ_FACTOR
            val leftEndX = center.x - 10
            val rightStartX = visibleSize.toSize().width * (1 - EDGE_FUZZ_FACTOR)
            val rightEndX = center.x + 10

            pinch(
                Offset(leftStartX, center.y),
                Offset(leftEndX, center.y),
                Offset(rightStartX, center.y),
                Offset(rightEndX, center.y)
            )
        }

        rule.mainClock.advanceTimeBy(milliseconds = 1000)

        rule.runOnIdle {
            assertWithMessage("Should have scaled down at least 4x")
                .that(cumulativeScale)
                .isAtMost(0.25f)
        }
    }

    @Test
    fun transformable_startStop_notify() {
        var cumulativeScale = 1.0f
        val state = TransformableState { zoom, _, _ ->
            cumulativeScale *= zoom
        }

        setTransformableContent {
            Modifier.transformable(state = state)
        }

        rule.runOnIdle {
            assertThat(state.isTransformInProgress).isEqualTo(false)
        }

        rule.onNodeWithTag(TEST_TAG).performGesture {
            down(pointerId = 1, center)
            down(pointerId = 2, center + Offset(10f, 10f))
            moveBy(2, Offset(20f, 20f))
        }

        assertThat(state.isTransformInProgress).isEqualTo(true)

        rule.onNodeWithTag(TEST_TAG).performGesture {
            up(pointerId = 1)
            up(pointerId = 2)
        }

        rule.runOnIdle {
            assertThat(state.isTransformInProgress).isEqualTo(false)
        }
    }

    @Test
    fun transformable_disabledWontCallLambda() {
        val enabled = mutableStateOf(true)
        var cumulativeScale = 1.0f

        setTransformableContent {
            Modifier.transformable(
                enabled = enabled.value,
                state = rememberTransformableState { zoom, _, _ ->
                    cumulativeScale *= zoom
                }
            )
        }

        rule.onNodeWithTag(TEST_TAG).performGesture {
            val leftStartX = center.x - 10
            val leftEndX = visibleSize.toSize().width * EDGE_FUZZ_FACTOR
            val rightStartX = center.x + 10
            val rightEndX = visibleSize.toSize().width * (1 - EDGE_FUZZ_FACTOR)

            pinch(
                Offset(leftStartX, center.y),
                Offset(leftEndX, center.y),
                Offset(rightStartX, center.y),
                Offset(rightEndX, center.y)
            )
        }

        rule.mainClock.advanceTimeBy(milliseconds = 1000)

        val prevScale = rule.runOnIdle {
            assertWithMessage("Should have scaled at least 4x").that(cumulativeScale).isAtLeast(4f)
            cumulativeScale
        }
        enabled.value = false
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).performGesture {
            val leftStartX = visibleSize.toSize().width * EDGE_FUZZ_FACTOR
            val leftEndX = center.x - 10
            val rightStartX = visibleSize.toSize().width * (1 - EDGE_FUZZ_FACTOR)
            val rightEndX = center.x + 10

            pinch(
                Offset(leftStartX, center.y),
                Offset(leftEndX, center.y),
                Offset(rightStartX, center.y),
                Offset(rightEndX, center.y)
            )
        }

        rule.runOnIdle {
            assertWithMessage("When enabled = false, scale should stay the same")
                .that(cumulativeScale)
                .isEqualTo(prevScale)
        }
    }

    @Test
    fun transformable_animateTo_zoom() = runBlocking(AutoTestFrameClock()) {
        rule.mainClock.autoAdvance = false
        var cumulativeScale = 1.0f
        var callbackCount = 0
        val state = TransformableState { zoom, _, _ ->
            cumulativeScale *= zoom
            callbackCount += 1
        }
        setTransformableContent {
            Modifier.transformable(state)
        }

        state.animateZoomBy(4f)

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertWithMessage("Zoom should have been smooth").that(callbackCount).isAtLeast(1)
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertWithMessage("Zoom should have been smooth").that(callbackCount).isAtLeast(2)
        }

        rule.mainClock.advanceTimeBy(milliseconds = 100000)

        rule.runOnIdle {
            assertWithMessage("Zoom should have been smooth").that(callbackCount).isAtLeast(3)
            // Include a bit of tolerance for floating point discrepancies.
            assertWithMessage("Should have scaled ~4x").that(cumulativeScale).isAtLeast(3.9f)
        }
    }

    @Test
    fun transformable_animateTo_rotate() = runBlocking(AutoTestFrameClock()) {
        rule.mainClock.autoAdvance = false
        var totalRotation = 0f
        var callbackCount = 0
        val state = TransformableState { _, _, rotation ->
            totalRotation += rotation
            callbackCount += 1
        }
        setTransformableContent {
            Modifier.transformable(state)
        }

        state.animateRotateBy(180f)

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(1)
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(2)
        }

        rule.mainClock.advanceTimeBy(milliseconds = 100000)

        rule.runOnIdle {
            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(3)
            // Include a bit of tolerance for floating point discrepancies.
            assertWithMessage("Should have rotated 180").that(totalRotation).isAtLeast(179f)
        }
    }

    @Test
    fun transformable_animateTo_pan() = runBlocking(AutoTestFrameClock()) {
        rule.mainClock.autoAdvance = false
        var totalPan = Offset.Zero
        var callbackCount = 0
        val state = TransformableState { _, pan, _ ->
            totalPan += pan
            callbackCount += 1
        }
        setTransformableContent {
            Modifier.transformable(state)
        }

        val expected = Offset(100f, 80f)
        state.animatePanBy(expected)

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertWithMessage("Panning should have been smooth").that(callbackCount).isAtLeast(1)
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertWithMessage("Panning should have been smooth").that(callbackCount).isAtLeast(2)
        }

        rule.mainClock.advanceTimeBy(milliseconds = 100000)

        rule.runOnIdle {
            assertWithMessage("Panning should have been smooth").that(callbackCount).isAtLeast(3)
            // Include a bit of tolerance for floating point discrepancies.
            assertWithMessage("Should have panned to 100 / 80").that(totalPan).isEqualTo(expected)
        }
    }

    @Test
    fun transformable_snapTo_zoom() = runBlocking {
        var cumulativeScale = 1.0f
        var callbackCount = 0
        val state = TransformableState { zoom, _, _ ->
            cumulativeScale *= zoom
            callbackCount += 1
        }
        setTransformableContent {
            Modifier.transformable(state)
        }

        state.zoomBy(4f)

        assertWithMessage("Zoom should have been smooth").that(callbackCount).isEqualTo(1)
        // Include a bit of tolerance for floating point discrepancies.
        assertWithMessage("Should have scaled ~4x").that(cumulativeScale).isAtLeast(3.9f)
    }

    @Test
    fun transformable_snapTo_rotate() = runBlocking {
        var totalRotation = 0f
        var callbackCount = 0
        val state = TransformableState { _, _, rotation ->
            totalRotation += rotation
            callbackCount += 1
        }
        setTransformableContent {
            Modifier.transformable(state)
        }

        state.rotateBy(180f)

        assertWithMessage("Rotation should have been smooth").that(callbackCount).isEqualTo(1)
        // Include a bit of tolerance for floating point discrepancies.
        assertWithMessage("Should have rotated 180").that(totalRotation).isAtLeast(179f)
    }

    @Test
    fun transformable_snapTo_pan() = runBlocking {
        var totalPan = Offset.Zero
        var callbackCount = 0
        val state = TransformableState { _, pan, _ ->
            totalPan += pan
            callbackCount += 1
        }
        setTransformableContent {
            Modifier.transformable(state)
        }

        val expected = Offset(100f, 80f)
        state.panBy(expected)

        assertWithMessage("Panning should have been smooth").that(callbackCount).isAtLeast(1)
        // Include a bit of tolerance for floating point discrepancies.
        assertWithMessage("Should have panned to 100 / 80").that(totalPan).isEqualTo(expected)
    }

    @Test
    fun transformable_stopTransformations() = runBlocking(AutoTestFrameClock()) {
        rule.mainClock.autoAdvance = false
        var totalRotation = 0f
        var callbackCount = 0
        val state = TransformableState { _, _, rotation ->
            totalRotation += rotation
            callbackCount += 1
        }
        setTransformableContent {
            Modifier.transformable(state)
        }

        state.animateRotateBy(180f)

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(1)
        }

        rule.mainClock.advanceTimeByFrame()

        val lastCallbackCount = rule.runOnIdle {
            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(2)
            callbackCount
        }

        state.stopTransformation()

        rule.mainClock.advanceTimeBy(milliseconds = 100000)

        rule.runOnIdle {
            assertWithMessage("Rotation should have been stopped").that(callbackCount)
                .isEqualTo(lastCallbackCount)
        }
    }

    @Test
    fun testInspectorValue() {
        rule.setContent {
            val state = rememberTransformableState { _, _, _ -> }
            val modifier = Modifier.transformable(state) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("transformable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "state",
                "lockRotationOnZoomPan",
                "enabled"
            )
        }
    }

    private fun setTransformableContent(getModifier: @Composable () -> Modifier) {
        rule.setContent {
            Box(Modifier.size(600.dp).testTag(TEST_TAG).then(getModifier()))
        }
    }
}
