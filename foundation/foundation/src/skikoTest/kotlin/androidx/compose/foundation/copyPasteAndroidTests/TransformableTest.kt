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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.animation.core.tween
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.animateZoomBy
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isAtLeast
import androidx.compose.foundation.isAtMost
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.isNull
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TEST_TAG = "transformableTag"

private const val EDGE_FUZZ_FACTOR = 0.2f

@OptIn(ExperimentalTestApi::class)
@Ignore
// TODO: These tests use multiple pointers touch, which is currently not supported in skiko targets
class TransformableTest {

    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    private fun SkikoComposeUiTest.setContentAndGetScope(content: @Composable () -> Unit) {
        setContent {
            val actualScope = rememberCoroutineScope()
            SideEffect { scope = actualScope }
            content()
        }
    }

    @Test
    fun transformable_zoomIn() = runSkikoComposeUiTest {
        var cumulativeScale = 1.0f

        setTransformableContent {
            Modifier.transformable(
                state = rememberTransformableState { zoom, _, _ ->
                    cumulativeScale *= zoom
                }
            )
        }

        onNodeWithTag(TEST_TAG).performTouchInput {
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

        mainClock.advanceTimeBy(milliseconds = 1000)

        runOnIdle {
            assertThat(cumulativeScale, message = "Should have scaled at least 4x, but was $cumulativeScale").isAtLeast(
                4f
            )
        }
    }

    @Test
    fun transformable_pan() = runSkikoComposeUiTest {
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

        onNodeWithTag(TEST_TAG).performTouchInput {
            down(1, center)
            down(2, center + Offset(10f, 10f))
            moveBy(1, expected)
            moveBy(2, expected)
            up(1)
            up(2)
        }

        mainClock.advanceTimeBy(milliseconds = 1000)

        runOnIdle {
            assertThat(cumulativePan, message = "Should have panned 20/10").isEqualTo(expected)
        }
    }

    @Test
    fun transformable_rotate() = runSkikoComposeUiTest {
        var cumulativeRotation = 0f

        setTransformableContent {
            Modifier.transformable(
                state = rememberTransformableState { _, _, rotation ->
                    cumulativeRotation += rotation
                }
            )
        }

        onNodeWithTag(TEST_TAG).performTouchInput {
            down(1, center)
            down(2, center + Offset(0f, 50f))
            moveBy(2, Offset(50f, -50f))
            up(1)
            up(2)
        }

        mainClock.advanceTimeBy(milliseconds = 1000)

        runOnIdle {
            assertThat(cumulativeRotation, message = "Should have rotated -90").isEqualTo(-90f)
        }
    }

    @Test
    fun transformable_rotationLock() = runSkikoComposeUiTest {
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

        onNodeWithTag(TEST_TAG).performTouchInput {
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

        mainClock.advanceTimeBy(milliseconds = 1000)

        runOnIdle {
            assertThat(cumulativeRotation, message = "Should have rotated -90").isEqualTo(-90f)
            cumulativeRotation = 0f
        }
        rotationLock.value = !rotationLock.value
        waitForIdle()

        onNodeWithTag(TEST_TAG).performTouchInput {
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

        mainClock.advanceTimeBy(milliseconds = 1000)

        runOnIdle {
            assertThat(cumulativeRotation, message = "Rotation should be locked").isEqualTo(0f)
        }
    }

    @Test
    fun transformable_zoomOut() = runSkikoComposeUiTest {
        var cumulativeScale = 1.0f

        setTransformableContent {
            Modifier.transformable(
                state = rememberTransformableState { zoom, _, _ ->
                    cumulativeScale *= zoom
                }
            )
        }

        onNodeWithTag(TEST_TAG).performTouchInput {
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

        mainClock.advanceTimeBy(milliseconds = 1000)

        runOnIdle {
            assertThat(cumulativeScale, message = "Should have scaled down at least 4x")
                .isAtMost(0.25f)
        }
    }

    @Test
    fun transformable_startStop_notify() = runSkikoComposeUiTest {
        var cumulativeScale = 1.0f
        val state = TransformableState { zoom, _, _ ->
            cumulativeScale *= zoom
        }

        setTransformableContent {
            Modifier.transformable(state = state)
        }

        runOnIdle {
            assertThat(state.isTransformInProgress).isEqualTo(false)
        }

        onNodeWithTag(TEST_TAG).performTouchInput {
            down(pointerId = 1, center)
            down(pointerId = 2, center + Offset(10f, 10f))
            moveBy(2, Offset(20f, 20f))
        }

        assertThat(state.isTransformInProgress).isEqualTo(true)

        onNodeWithTag(TEST_TAG).performTouchInput {
            up(pointerId = 1)
            up(pointerId = 2)
        }

        runOnIdle {
            assertThat(state.isTransformInProgress).isEqualTo(false)
        }
    }

    @Test
    fun transformable_disabledWontCallLambda() = runSkikoComposeUiTest {
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

        onNodeWithTag(TEST_TAG).performTouchInput {
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

        mainClock.advanceTimeBy(milliseconds = 1000)

        val prevScale = runOnIdle {
            assertThat(cumulativeScale, message = "Should have scaled at least 4x").isAtLeast(4f)
            cumulativeScale
        }
        enabled.value = false
        waitForIdle()

        onNodeWithTag(TEST_TAG).performTouchInput {
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

        runOnIdle {
            assertThat(cumulativeScale, message = "When enabled = false, scale should stay the same")
                .isEqualTo(prevScale)
        }
    }

//    @Test
//    fun transformable_animateTo_zoom() = runBlocking(AutoTestFrameClock()) {
//       mainClock.autoAdvance = false
//        var cumulativeScale = 1.0f
//        var callbackCount = 0
//        val state = TransformableState { zoom, _, _ ->
//            cumulativeScale *= zoom
//            callbackCount += 1
//        }
//        setTransformableContent {
//            Modifier.transformable(state)
//        }
//
//        state.animateZoomBy(4f)
//
//       mainClock.advanceTimeByFrame()
//
//       runOnIdle {
//            assertWithMessage("Zoom should have been smooth").that(callbackCount).isAtLeast(1)
//        }
//
//       mainClock.advanceTimeByFrame()
//
//       runOnIdle {
//            assertWithMessage("Zoom should have been smooth").that(callbackCount).isAtLeast(2)
//        }
//
//       mainClock.advanceTimeBy(milliseconds = 100000)
//
//       runOnIdle {
//            assertWithMessage("Zoom should have been smooth").that(callbackCount).isAtLeast(3)
//            // Include a bit of tolerance for floating point discrepancies.
//            assertWithMessage("Should have scaled ~4x").that(cumulativeScale).isAtLeast(3.9f)
//        }
//    }
//
//    @Test
//    fun transformable_animateTo_rotate() = runBlocking(AutoTestFrameClock()) {
//       mainClock.autoAdvance = false
//        var totalRotation = 0f
//        var callbackCount = 0
//        val state = TransformableState { _, _, rotation ->
//            totalRotation += rotation
//            callbackCount += 1
//        }
//        setTransformableContent {
//            Modifier.transformable(state)
//        }
//
//        state.animateRotateBy(180f)
//
//       mainClock.advanceTimeByFrame()
//
//       runOnIdle {
//            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(1)
//        }
//
//       mainClock.advanceTimeByFrame()
//
//       runOnIdle {
//            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(2)
//        }
//
//       mainClock.advanceTimeBy(milliseconds = 100000)
//
//       runOnIdle {
//            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(3)
//            // Include a bit of tolerance for floating point discrepancies.
//            assertWithMessage("Should have rotated 180").that(totalRotation).isAtLeast(179f)
//        }
//    }
//
//    @Test
//    fun transformable_animateTo_pan() = runBlocking(AutoTestFrameClock()) {
//       mainClock.autoAdvance = false
//        var totalPan = Offset.Zero
//        var callbackCount = 0
//        val state = TransformableState { _, pan, _ ->
//            totalPan += pan
//            callbackCount += 1
//        }
//        setTransformableContent {
//            Modifier.transformable(state)
//        }
//
//        val expected = Offset(100f, 80f)
//        state.animatePanBy(expected)
//
//       mainClock.advanceTimeByFrame()
//
//       runOnIdle {
//            assertWithMessage("Panning should have been smooth").that(callbackCount).isAtLeast(1)
//        }
//
//       mainClock.advanceTimeByFrame()
//
//       runOnIdle {
//            assertWithMessage("Panning should have been smooth").that(callbackCount).isAtLeast(2)
//        }
//
//       mainClock.advanceTimeBy(milliseconds = 100000)
//
//       runOnIdle {
//            assertWithMessage("Panning should have been smooth").that(callbackCount).isAtLeast(3)
//            // Include a bit of tolerance for floating point discrepancies.
//            assertWithMessage("Should have panned to 100 / 80").that(totalPan).isEqualTo(expected)
//        }
//    }
//
//    @Test
//    fun transformable_snapTo_zoom() = runBlocking {
//        var cumulativeScale = 1.0f
//        var callbackCount = 0
//        val state = TransformableState { zoom, _, _ ->
//            cumulativeScale *= zoom
//            callbackCount += 1
//        }
//        setTransformableContent {
//            Modifier.transformable(state)
//        }
//
//        state.zoomBy(4f)
//
//        assertWithMessage("Zoom should have been smooth").that(callbackCount).isEqualTo(1)
//        // Include a bit of tolerance for floating point discrepancies.
//        assertWithMessage("Should have scaled ~4x").that(cumulativeScale).isAtLeast(3.9f)
//    }
//
//    @Test
//    fun transformable_snapTo_rotate() = runBlocking {
//        var totalRotation = 0f
//        var callbackCount = 0
//        val state = TransformableState { _, _, rotation ->
//            totalRotation += rotation
//            callbackCount += 1
//        }
//        setTransformableContent {
//            Modifier.transformable(state)
//        }
//
//        state.rotateBy(180f)
//
//        assertWithMessage("Rotation should have been smooth").that(callbackCount).isEqualTo(1)
//        // Include a bit of tolerance for floating point discrepancies.
//        assertWithMessage("Should have rotated 180").that(totalRotation).isAtLeast(179f)
//    }
//
//    @Test
//    fun transformable_snapTo_pan() = runBlocking {
//        var totalPan = Offset.Zero
//        var callbackCount = 0
//        val state = TransformableState { _, pan, _ ->
//            totalPan += pan
//            callbackCount += 1
//        }
//        setTransformableContent {
//            Modifier.transformable(state)
//        }
//
//        val expected = Offset(100f, 80f)
//        state.panBy(expected)
//
//        assertWithMessage("Panning should have been smooth").that(callbackCount).isAtLeast(1)
//        // Include a bit of tolerance for floating point discrepancies.
//        assertWithMessage("Should have panned to 100 / 80").that(totalPan).isEqualTo(expected)
//    }
//
//    @Test
//    fun transformable_stopTransformations() = runBlocking(AutoTestFrameClock()) {
//       mainClock.autoAdvance = false
//        var totalRotation = 0f
//        var callbackCount = 0
//        val state = TransformableState { _, _, rotation ->
//            totalRotation += rotation
//            callbackCount += 1
//        }
//        setTransformableContent {
//            Modifier.transformable(state)
//        }
//
//        state.animateRotateBy(180f)
//
//       mainClock.advanceTimeByFrame()
//
//       runOnIdle {
//            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(1)
//        }
//
//       mainClock.advanceTimeByFrame()
//
//        val lastCallbackCount =runOnIdle {
//            assertWithMessage("Rotation should have been smooth").that(callbackCount).isAtLeast(2)
//            callbackCount
//        }
//
//        state.stopTransformation()
//
//       mainClock.advanceTimeBy(milliseconds = 100000)
//
//       runOnIdle {
//            assertWithMessage("Rotation should have been stopped").that(callbackCount)
//                .isEqualTo(lastCallbackCount)
//        }
//    }

    @Test
    fun transformable_animateCancelledUpdatesIsTransformInProgress() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        val state = TransformableState { _, _, _ -> }
        setTransformableContent {
            Modifier.transformable(state)
        }

        lateinit var animateJob: Job

        runOnIdle {
            assertThat(state.isTransformInProgress).isFalse()
            animateJob = scope.launch {
                state.animateZoomBy(4f, tween(1000))
            }
        }

        mainClock.advanceTimeBy(500)

        runOnIdle {
            assertThat(state.isTransformInProgress).isTrue()
        }

        animateJob.cancel()

        runOnIdle {
            assertThat(state.isTransformInProgress).isFalse()
        }
    }

    @Test
    fun testInspectorValue() = runSkikoComposeUiTest {
        setContent {
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

    private fun SkikoComposeUiTest.setTransformableContent(getModifier: @Composable () -> Modifier) {
        setContentAndGetScope {
            Box(Modifier.size(600.dp).testTag(TEST_TAG).then(getModifier()))
        }
    }
}
