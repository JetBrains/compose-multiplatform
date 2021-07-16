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

package androidx.compose.ui.node

import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.test.TestActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ModelReadsTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()
    private lateinit var activity: TestActivity
    private lateinit var latch: CountDownLatch

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        latch = CountDownLatch(1)
    }

    @Test
    fun useTheSameModelInDrawAndPosition() {
        val offset = mutableStateOf(5)
        var drawLatch = CountDownLatch(1)
        var positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    {},
                    modifier = Modifier.drawBehind {
                        // read from the model
                        offset.value
                        drawLatch.countDown()
                    }
                ) { _, _ ->
                    layout(10, 10) {
                        // read from the model
                        offset.value
                        positionLatch.countDown()
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            offset.value = 7
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            offset.value = 10
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    @MediumTest
    fun useDifferentModelsInDrawAndPosition() {
        val drawModel = mutableStateOf(5)
        val positionModel = mutableStateOf(5)
        var drawLatch = CountDownLatch(1)
        var positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    {},
                    modifier = Modifier.drawBehind {
                        // read from the model
                        drawModel.value
                        drawLatch.countDown()
                    }
                ) { _, _ ->
                    layout(10, 10) {
                        // read from the model
                        positionModel.value
                        positionLatch.countDown()
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))

        drawLatch = CountDownLatch(1)
        positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            drawModel.value = 7
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertFalse(positionLatch.await(200, TimeUnit.MILLISECONDS))

        drawLatch = CountDownLatch(1)
        positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            positionModel.value = 10
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        assertFalse(drawLatch.await(200, TimeUnit.MILLISECONDS))
    }

    @Test
    fun useTheSameModelInMeasureAndDraw() {
        val offset = mutableStateOf(5)
        var measureLatch = CountDownLatch(1)
        var drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    {},
                    modifier = Modifier.drawBehind {
                        // read from the model
                        offset.value
                        drawLatch.countDown()
                    }
                ) { _, _ ->
                    measureLatch.countDown()
                    // read from the model
                    layout(offset.value, 10) {}
                }
            }
        }
        assertTrue(measureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        measureLatch = CountDownLatch(1)
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            offset.value = 10
        }

        assertTrue(measureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        measureLatch = CountDownLatch(1)
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            offset.value = 15
        }

        assertTrue(measureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun useDifferentModelsInMeasureAndPosition() {
        val measureModel = mutableStateOf(5)
        val positionModel = mutableStateOf(5)
        var measureLatch = CountDownLatch(1)
        var positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                Layout({}) { _, _ ->
                    measureLatch.countDown()
                    // read from the model
                    layout(measureModel.value, 10) {
                        // read from the model
                        positionModel.value
                        positionLatch.countDown()
                    }
                }
            }
        }
        assertTrue(measureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))

        measureLatch = CountDownLatch(1)
        positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            measureModel.value = 10
        }

        assertTrue(measureLatch.await(1, TimeUnit.SECONDS))
        // remeasuring automatically triggers relayout
        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))

        measureLatch = CountDownLatch(1)
        positionLatch = CountDownLatch(1)
        rule.runOnUiThread {
            positionModel.value = 15
        }

        assertFalse(measureLatch.await(200, TimeUnit.MILLISECONDS))
        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun drawReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                AtLeastSize(
                    10,
                    modifier = Modifier.drawBehind {
                        if (enabled.value) {
                            // read the model
                            model.value
                        }
                        latch.countDown()
                    }
                ) {
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun measureReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                Layout({}) { _, _ ->
                    if (enabled.value) {
                        // read the model
                        model.value
                    }
                    latch.countDown()
                    layout(10, 10) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertCountDownOnlyWhileEnabled(enabled, model)
    }

    @Test
    fun layoutReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                Layout({}) { _, _ ->
                    layout(10, 10) {
                        if (enabled.value) {
                            // read the model
                            model.value
                        }
                        latch.countDown()
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertCountDownOnlyWhileEnabled(enabled, model)
    }

    @Test
    @MediumTest
    fun drawStopsReactingOnModelsAfterDetaching() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                val modifier = if (enabled.value) {
                    Modifier.drawBehind {
                        // read the model
                        model.value
                        latch.countDown()
                    }
                } else Modifier
                AtLeastSize(10, modifier = modifier) {}
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertCountDownOnlyWhileEnabled(enabled, model, false)
    }

    @Test
    @MediumTest
    fun measureStopsReactingOnModelsAfterDetaching() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                if (enabled.value) {
                    Layout({}) { _, _ ->
                        // read the model
                        model.value
                        latch.countDown()
                        layout(10, 10) {}
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertCountDownOnlyWhileEnabled(enabled, model, false)
    }

    @Test
    @MediumTest
    fun layoutStopsReactingOnModelsAfterDetaching() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                if (enabled.value) {
                    Layout({}) { _, _ ->
                        layout(10, 10) {
                            // read the model
                            model.value
                            latch.countDown()
                        }
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertCountDownOnlyWhileEnabled(enabled, model, false)
    }

    @Test
    fun remeasureRequestForTheNodeBeingMeasured() {
        var latch = CountDownLatch(1)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                Layout({}) { _, _ ->
                    if (model.value == 1) {
                        // this will trigger remeasure request for this node we currently measure
                        model.value = 2
                        Snapshot.sendApplyNotifications()
                    }
                    latch.countDown()
                    layout(100, 100) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)

        rule.runOnUiThread {
            model.value = 1
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun remeasureRequestForTheNodeBeingLaidOut() {
        var remeasureLatch = CountDownLatch(1)
        var relayoutLatch = CountDownLatch(1)
        val remeasureModel = mutableStateOf(0)
        val relayoutModel = mutableStateOf(0)
        var valueReadDuringMeasure = -1
        var modelAlreadyChanged = false
        rule.runOnUiThread {
            activity.setContent {
                Layout({}) { _, _ ->
                    valueReadDuringMeasure = remeasureModel.value
                    remeasureLatch.countDown()
                    layout(100, 100) {
                        if (relayoutModel.value != 0) {
                            if (!modelAlreadyChanged) {
                                // this will trigger remeasure request for this node we layout
                                remeasureModel.value = 1
                                Snapshot.sendApplyNotifications()
                                // the remeasure will also include another relayout and we don't
                                // want to loop and request remeasure again
                                modelAlreadyChanged = true
                            }
                        }
                        relayoutLatch.countDown()
                    }
                }
            }
        }

        assertTrue(remeasureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(relayoutLatch.await(1, TimeUnit.SECONDS))

        remeasureLatch = CountDownLatch(1)
        relayoutLatch = CountDownLatch(1)

        rule.runOnUiThread {
            relayoutModel.value = 1
        }

        assertTrue(remeasureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(relayoutLatch.await(1, TimeUnit.SECONDS))
        assertEquals(1, valueReadDuringMeasure)
    }

    @Test
    fun relayoutRequestForTheNodeBeingMeasured() {
        var remeasureLatch = CountDownLatch(1)
        var relayoutLatch = CountDownLatch(1)
        val remeasureModel = mutableStateOf(0)
        val relayoutModel = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                Layout({}) { _, _ ->
                    if (remeasureModel.value != 0) {
                        // this will trigger relayout request for this node we currently measure
                        relayoutModel.value = 1
                        Snapshot.sendApplyNotifications()
                    }
                    remeasureLatch.countDown()
                    layout(100, 100) {
                        relayoutModel.value // just register the read
                        relayoutLatch.countDown()
                    }
                }
            }
        }

        assertTrue(remeasureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(relayoutLatch.await(1, TimeUnit.SECONDS))

        remeasureLatch = CountDownLatch(1)
        relayoutLatch = CountDownLatch(1)

        rule.runOnUiThread {
            remeasureModel.value = 1
        }

        assertTrue(remeasureLatch.await(1, TimeUnit.SECONDS))
        assertTrue(relayoutLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun relayoutRequestForTheNodeBeingLaidOut() {
        var latch = CountDownLatch(1)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                Layout({}) { _, _ ->
                    layout(100, 100) {
                        if (model.value == 1) {
                            // this will trigger relayout request for this node we currently layout
                            model.value = 2
                            Snapshot.sendApplyNotifications()
                        }
                        latch.countDown()
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)

        rule.runOnUiThread {
            model.value = 1
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun measureModifierReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    {},
                    Modifier.layout(
                        onMeasure = {
                            if (enabled.value) {
                                // read the model
                                model.value
                            }
                            latch.countDown()
                        }
                    )
                ) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertCountDownOnlyWhileEnabled(enabled, model)
    }

    @Test
    fun layoutModifierReactsOnCorrectModelsChanges() {
        val enabled = mutableStateOf(true)
        val model = mutableStateOf(0)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    {},
                    Modifier.layout(
                        onLayout = {
                            if (enabled.value) {
                                // read the model
                                model.value
                            }
                            latch.countDown()
                        }
                    )
                ) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertCountDownOnlyWhileEnabled(enabled, model)
    }

    @Test
    fun parentIsNotRemeasuredOrRelaidOutWhenChildMeasureModifierUsesState() {
        val model = mutableStateOf(0)
        var parentMeasureCount = 0
        var parentLayoutsCount = 0
        rule.runOnUiThread {
            activity.setContent {
                Layout({
                    Layout(
                        {},
                        Modifier.layout(
                            onMeasure = {
                                // read the model
                                model.value
                                latch.countDown()
                            }
                        )
                    ) { _, _ ->
                        layout(10, 10) {}
                    }
                }) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    parentMeasureCount++
                    layout(placeable.width, placeable.height) {
                        parentLayoutsCount++
                        placeable.place(0, 0)
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            assertEquals(1, parentMeasureCount)
            assertEquals(1, parentLayoutsCount)
            model.value++
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            assertEquals(1, parentMeasureCount)
            assertEquals(1, parentLayoutsCount)
        }
    }

    @Test
    fun parentIsNotRelaidOutWhenChildLayoutModifierUsesState() {
        val model = mutableStateOf(0)
        var parentLayoutsCount = 0
        rule.runOnUiThread {
            activity.setContent {
                Layout({
                    Layout(
                        {},
                        Modifier.layout(
                            onLayout = {
                                // read the model
                                model.value
                                latch.countDown()
                            }
                        )
                    ) { _, _ ->
                        layout(10, 10) {}
                    }
                }) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        parentLayoutsCount++
                        placeable.place(0, 0)
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            assertEquals(1, parentLayoutsCount)
            model.value++
        }

        assertTrue(latch.await(1, TimeUnit.HOURS))

        rule.runOnUiThread {
            assertEquals(1, parentLayoutsCount)
        }
    }

    @Test
    fun stateReadForTheIntroducedLaterMeasureModifierIsObserved() {
        val model = mutableStateOf(0)
        var modifier by mutableStateOf(Modifier.layout(onMeasure = { latch.countDown() }))
        rule.runOnUiThread {
            activity.setContent {
                Layout({}, modifier) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            modifier = Modifier.layout(
                onMeasure = {
                    // read the model
                    model.value
                    latch.countDown()
                }
            )
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            model.value++
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun stateReadForTheIntroducedLaterLayoutModifierIsObserved() {
        val model = mutableStateOf(0)
        var modifier by mutableStateOf(Modifier.layout(onLayout = { latch.countDown() }))
        rule.runOnUiThread {
            activity.setContent {
                Layout({}, modifier) { _, _ ->
                    layout(10, 10) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            modifier = Modifier.layout(
                onLayout = {
                    // read the model
                    model.value
                    latch.countDown()
                }
            )
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            model.value++
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    private fun Modifier.layout(onMeasure: () -> Unit = {}, onLayout: () -> Unit = {}) =
        layout { measurable, constraints ->
            onMeasure()
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                onLayout()
                placeable.place(0, 0)
            }
        }

    fun assertCountDownOnlyWhileEnabled(
        enableModel: MutableState<Boolean>,
        valueModel: MutableState<Int>,
        triggeredByEnableSwitch: Boolean = true
    ) {
        latch = CountDownLatch(1)
        rule.runOnUiThread {
            valueModel.value++
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            enableModel.value = false
        }
        if (triggeredByEnableSwitch) {
            assertTrue(latch.await(1, TimeUnit.SECONDS))
        } else {
            assertFalse(latch.await(200, TimeUnit.MILLISECONDS))
        }

        latch = CountDownLatch(1)
        rule.runOnUiThread {
            valueModel.value++
        }
        assertFalse(latch.await(200, TimeUnit.MILLISECONDS))
    }
}
