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

import android.annotation.SuppressLint
import android.os.Build
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.composed
import androidx.compose.ui.draw.assertColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SubcomposeLayoutTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun useSizeOfTheFirstItemInSecondSubcomposition() {
        val firstTag = "first"
        val secondTag = "second"

        rule.setContent {
            SubcomposeLayout { constraints ->
                val first = subcompose(0) {
                    Spacer(Modifier.requiredSize(50.dp).testTag(firstTag))
                }.first().measure(constraints)

                // it is an input for the second subcomposition
                val halfFirstSize = (first.width / 2).toDp()

                val second = subcompose(1) {
                    Spacer(Modifier.requiredSize(halfFirstSize).testTag(secondTag))
                }.first().measure(constraints)

                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(first.width - second.width, first.height - second.height)
                }
            }
        }

        rule.onNodeWithTag(firstTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        rule.onNodeWithTag(secondTag)
            .assertPositionInRootIsEqualTo(25.dp, 25.dp)
            .assertWidthIsEqualTo(25.dp)
            .assertHeightIsEqualTo(25.dp)
    }

    @Test
    fun subcomposeMultipleLayoutsInOneSlot() {
        val firstTag = "first"
        val secondTag = "second"
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val placeables = subcompose(Unit) {
                    Spacer(Modifier.requiredSize(50.dp).testTag(firstTag))
                    Spacer(Modifier.requiredSize(30.dp).testTag(secondTag))
                }.map {
                    it.measure(constraints)
                }

                val maxWidth = placeables.maxByOrNull { it.width }!!.width
                val height = placeables.sumOf { it.height }

                layout(maxWidth, height) {
                    placeables.fold(0) { top, placeable ->
                        placeable.place(0, top)
                        top + placeable.height
                    }
                }
            }
        }

        rule.onNodeWithTag(firstTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        rule.onNodeWithTag(secondTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(30.dp)

        rule.onNodeWithTag(layoutTag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun recompositionDeepInsideTheSlotDoesntRecomposeUnaffectedLayerOrRemeasure() {
        val model = mutableStateOf(0)
        var measuresCount = 0
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        rule.setContent {
            SubcomposeLayout { constraints ->
                measuresCount++
                val placeable = subcompose(Unit) {
                    recompositionsCount1++
                    NonInlineBox(Modifier.requiredSize(20.dp)) {
                        model.value // model read
                        recompositionsCount2++
                    }
                }.first().measure(constraints)

                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle { model.value++ }

        rule.runOnIdle {
            assertEquals(1, measuresCount)
            assertEquals(1, recompositionsCount1)
            assertEquals(2, recompositionsCount2)
        }
    }

    @Composable
    private fun NonInlineBox(modifier: Modifier, content: @Composable () -> Unit) {
        Box(modifier = modifier) { content() }
    }

    @Test
    fun recompositionOfTheFirstSlotDoestAffectTheSecond() {
        val model = mutableStateOf(0)
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        rule.setContent {
            SubcomposeLayout {
                subcompose(1) {
                    recompositionsCount1++
                    model.value // model read
                }
                subcompose(2) {
                    recompositionsCount2++
                }

                layout(100, 100) {
                }
            }
        }

        rule.runOnIdle { model.value++ }

        rule.runOnIdle {
            assertEquals(2, recompositionsCount1)
            assertEquals(1, recompositionsCount2)
        }
    }

    @Test
    fun addLayoutOnlyAfterRecomposition() {
        val addChild = mutableStateOf(false)
        val childTag = "child"
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val placeables = subcompose(Unit) {
                    if (addChild.value) {
                        Spacer(Modifier.requiredSize(20.dp).testTag(childTag))
                    }
                }.map { it.measure(constraints) }

                val size = placeables.firstOrNull()?.width ?: 0
                layout(size, size) {
                    placeables.forEach { it.place(0, 0) }
                }
            }
        }

        rule.onNodeWithTag(layoutTag)
            .assertWidthIsEqualTo(0.dp)
            .assertHeightIsEqualTo(0.dp)

        rule.onNodeWithTag(childTag)
            .assertDoesNotExist()

        rule.runOnIdle {
            addChild.value = true
        }

        rule.onNodeWithTag(layoutTag)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)

        rule.onNodeWithTag(childTag)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun providingNewLambdaCausingRecomposition() {
        val content = mutableStateOf<@Composable () -> Unit>({
            Spacer(Modifier.requiredSize(10.dp))
        })

        rule.setContent {
            MySubcomposeLayout(content.value)
        }

        val updatedTag = "updated"

        rule.runOnIdle {
            content.value = {
                Spacer(Modifier.requiredSize(10.dp).testTag(updatedTag))
            }
        }

        rule.onNodeWithTag(updatedTag)
            .assertIsDisplayed()
    }

    @Composable
    private fun MySubcomposeLayout(content: @Composable () -> Unit) {
        SubcomposeLayout { constraints ->
            val placeables = subcompose(Unit, content).map { it.measure(constraints) }
            val maxWidth = placeables.maxByOrNull { it.width }!!.width
            val height = placeables.sumOf { it.height }
            layout(maxWidth, height) {
                placeables.forEach { it.place(0, 0) }
            }
        }
    }

    @Test
    fun notSubcomposedSlotIsDisposed() {
        val addSlot = mutableStateOf(true)
        var composed = false
        var disposed = false

        rule.setContent {
            SubcomposeLayout {
                if (addSlot.value) {
                    subcompose(Unit) {
                        DisposableEffect(Unit) {
                            composed = true
                            onDispose { }
                        }
                        DisposableEffect(Unit) {
                            onDispose {
                                disposed = true
                            }
                        }
                    }
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(composed).isTrue()
            assertThat(disposed).isFalse()

            addSlot.value = false
        }

        rule.runOnIdle {
            assertThat(disposed).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun slotsAreDrawnInTheOrderTheyComposed() {
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val first = subcompose(Color.Red) {
                    Spacer(Modifier.requiredSize(10.dp).background(Color.Red))
                }.first().measure(constraints)
                val second = subcompose(Color.Green) {
                    Spacer(Modifier.requiredSize(10.dp).background(Color.Green))
                }.first().measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(layoutTag)
            .captureToImage()
            .assertCenterPixelColor(Color.Green)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun slotsCouldBeReordered() {
        val layoutTag = "layout"
        val firstSlotIsRed = mutableStateOf(true)

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val firstColor = if (firstSlotIsRed.value) Color.Red else Color.Green
                val secondColor = if (firstSlotIsRed.value) Color.Green else Color.Red
                val first = subcompose(firstColor) {
                    Spacer(Modifier.requiredSize(10.dp).background(firstColor))
                }.first().measure(constraints)
                val second = subcompose(secondColor) {
                    Spacer(Modifier.requiredSize(10.dp).background(secondColor))
                }.first().measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.onNodeWithTag(layoutTag)
            .captureToImage()
            .assertCenterPixelColor(Color.Green)

        rule.runOnIdle {
            firstSlotIsRed.value = false
        }

        rule.onNodeWithTag(layoutTag)
            .captureToImage()
            .assertCenterPixelColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawingOrderCouldBeChangedUsingZIndex() {
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val first = subcompose(Color.Red) {
                    Spacer(Modifier.requiredSize(10.dp).background(Color.Red).zIndex(1f))
                }.first().measure(constraints)
                val second = subcompose(Color.Green) {
                    Spacer(Modifier.requiredSize(10.dp).background(Color.Green))
                }.first().measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.onNodeWithTag(layoutTag)
            .captureToImage()
            .assertCenterPixelColor(Color.Red)
    }

    @Test
    fun slotsAreDisposedWhenLayoutIsDisposed() {
        val addLayout = mutableStateOf(true)
        var firstDisposed = false
        var secondDisposed = false

        rule.setContent {
            if (addLayout.value) {
                SubcomposeLayout {
                    subcompose(0) {
                        DisposableEffect(Unit) {
                            onDispose {
                                firstDisposed = true
                            }
                        }
                    }
                    subcompose(1) {
                        DisposableEffect(Unit) {
                            onDispose {
                                secondDisposed = true
                            }
                        }
                    }
                    layout(10, 10) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(firstDisposed).isFalse()
            assertThat(secondDisposed).isFalse()

            addLayout.value = false
        }

        rule.runOnIdle {
            assertThat(firstDisposed).isTrue()
            assertThat(secondDisposed).isTrue()
        }
    }

    @Test
    fun propagatesDensity() {
        rule.setContent {
            val size = 50.dp
            val density = Density(3f)
            val sizeIpx = with(density) { size.roundToPx() }
            CompositionLocalProvider(LocalDensity provides density) {
                SubcomposeLayout(
                    Modifier.requiredSize(size).onGloballyPositioned {
                        assertThat(it.size).isEqualTo(IntSize(sizeIpx, sizeIpx))
                    }
                ) { constraints ->
                    layout(constraints.maxWidth, constraints.maxHeight) {}
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawingOrderIsControlledByPlaceCalls() {
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val first = subcompose(Color.Red) {
                    Spacer(Modifier.requiredSize(10.dp).background(Color.Red))
                }.first().measure(constraints)
                val second = subcompose(Color.Green) {
                    Spacer(Modifier.requiredSize(10.dp).background(Color.Green))
                }.first().measure(constraints)

                layout(first.width, first.height) {
                    second.place(0, 0)
                    first.place(0, 0)
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(layoutTag)
            .captureToImage()
            .assertCenterPixelColor(Color.Red)
    }

    @Test
    @LargeTest
    fun viewWithSubcomposeLayoutCanBeDetached() {
        // verifies that the View with composed SubcomposeLayout can be detached at any point of
        // time without runtime crashes and once the view will be attached again the change will
        // be applied

        val scenario = rule.activityRule.scenario

        lateinit var container1: FrameLayout
        lateinit var container2: ComposeView
        val state = mutableStateOf(10.dp)
        var stateUsedLatch = CountDownLatch(1)

        scenario.onActivity {
            container1 = FrameLayout(it)
            container2 = ComposeView(it)
            it.setContentView(container1)
            container1.addView(container2)
            container2.setContent {
                SubcomposeLayout { constraints ->
                    val first = subcompose(Unit) {
                        stateUsedLatch.countDown()
                        Box(Modifier.requiredSize(state.value))
                    }.first().measure(constraints)
                    layout(first.width, first.height) {
                        first.place(0, 0)
                    }
                }
            }
        }

        assertTrue("state was used in setup", stateUsedLatch.await(1, TimeUnit.SECONDS))

        stateUsedLatch = CountDownLatch(1)
        scenario.onActivity {
            state.value = 15.dp
            container1.removeView(container2)
        }

        // The subcomposition is allowed to be active while the View is detached,
        // but it isn't required
        rule.waitForIdle()

        scenario.onActivity {
            container1.addView(container2)
        }

        assertTrue(
            "state was used after reattaching view",
            stateUsedLatch.await(1, TimeUnit.SECONDS)
        )
    }

    @Test
    fun precompose() {
        val addSlot = mutableStateOf(false)
        var composingCounter = 0
        var composedDuringMeasure = false
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit = {
            composingCounter++
        }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot.value) {
                    composedDuringMeasure = true
                    subcompose(Unit, content)
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(0)
            state.precompose(Unit, content)
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(1)

            assertThat(composedDuringMeasure).isFalse()
            addSlot.value = true
        }

        rule.runOnIdle {
            assertThat(composedDuringMeasure).isTrue()
            assertThat(composingCounter).isEqualTo(1)
        }
    }

    @Test
    fun disposePrecomposedItem() {
        var composed = false
        var disposed = false
        val state = SubcomposeLayoutState()

        rule.setContent {
            SubcomposeLayout(state) {
                layout(10, 10) {}
            }
        }

        val slot = rule.runOnIdle {
            state.precompose(Unit) {
                DisposableEffect(Unit) {
                    composed = true
                    onDispose {
                        disposed = true
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(composed).isTrue()
            assertThat(disposed).isFalse()

            slot.dispose()
        }

        rule.runOnIdle {
            assertThat(disposed).isTrue()
        }
    }

    @Test
    fun composeItemRegularlyAfterDisposingPrecomposedItem() {
        val addSlot = mutableStateOf(false)
        var composingCounter = 0
        var enterCounter = 0
        var exitCounter = 0
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit = @Composable {
            composingCounter++
            DisposableEffect(Unit) {
                enterCounter++
                onDispose {
                    exitCounter++
                }
            }
        }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot.value) {
                    subcompose(Unit, content)
                }
                layout(10, 10) {}
            }
        }

        val slot = rule.runOnIdle {
            state.precompose(Unit, content)
        }

        rule.runOnIdle {
            slot.dispose()
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(1)
            assertThat(enterCounter).isEqualTo(1)
            assertThat(exitCounter).isEqualTo(1)

            addSlot.value = true
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(2)
            assertThat(enterCounter).isEqualTo(2)
            assertThat(exitCounter).isEqualTo(1)
        }
    }

    @Test
    fun precomposeTwoItems() {
        val addSlots = mutableStateOf(false)
        var composing1Counter = 0
        var composing2Counter = 0
        val state = SubcomposeLayoutState()
        val content1: @Composable () -> Unit = {
            composing1Counter++
        }
        val content2: @Composable () -> Unit = {
            composing2Counter++
        }

        rule.setContent {
            SubcomposeLayout(state) {
                subcompose(0) { }
                if (addSlots.value) {
                    subcompose(1, content1)
                    subcompose(2, content2)
                }
                subcompose(3) { }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(composing1Counter).isEqualTo(0)
            assertThat(composing2Counter).isEqualTo(0)
            state.precompose(1, content1)
            state.precompose(2, content2)
        }

        rule.runOnIdle {
            assertThat(composing1Counter).isEqualTo(1)
            assertThat(composing2Counter).isEqualTo(1)
            addSlots.value = true
        }

        rule.runOnIdle {
            assertThat(composing1Counter).isEqualTo(1)
            assertThat(composing2Counter).isEqualTo(1)
        }
    }

    @Test
    fun precomposedItemDisposedWhenSubcomposeLayoutIsDisposed() {
        val emitLayout = mutableStateOf(true)
        var enterCounter = 0
        var exitCounter = 0
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit = @Composable {
            DisposableEffect(Unit) {
                enterCounter++
                onDispose {
                    exitCounter++
                }
            }
        }

        rule.setContent {
            if (emitLayout.value) {
                SubcomposeLayout(state) {
                    layout(10, 10) {}
                }
            }
        }

        rule.runOnIdle {
            state.precompose(Unit, content)
        }

        rule.runOnIdle {
            assertThat(enterCounter).isEqualTo(1)
            assertThat(exitCounter).isEqualTo(0)
            emitLayout.value = false
        }

        rule.runOnIdle {
            assertThat(exitCounter).isEqualTo(1)
        }
    }

    @Test
    fun precomposeIsNotTriggeringParentRemeasure() {
        val state = SubcomposeLayoutState()

        var measureCount = 0
        var layoutCount = 0

        rule.setContent {
            SubcomposeLayout(state) {
                measureCount++
                layout(10, 10) {
                    layoutCount++
                }
            }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(layoutCount).isEqualTo(1)
            state.precompose(Unit) {
                Box(Modifier.fillMaxSize())
            }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(layoutCount).isEqualTo(1)
        }
    }

    @Test
    fun precomposedItemDisposalIsNotTriggeringParentRemeasure() {
        val state = SubcomposeLayoutState()

        var measureCount = 0
        var layoutCount = 0

        rule.setContent {
            SubcomposeLayout(state) {
                measureCount++
                layout(10, 10) {
                    layoutCount++
                }
            }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(layoutCount).isEqualTo(1)
            val handle = state.precompose(Unit) {
                Box(Modifier.fillMaxSize())
            }
            handle.dispose()
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(layoutCount).isEqualTo(1)
        }
    }

    @Test
    fun slotsKeptForReuse() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(2))

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf(2, 3)
        }

        assertNodes(
            exists = /*active*/ listOf(2, 3) + /*reusable*/ listOf(0, 1),
            doesNotExist = /*disposed*/ listOf(4)
        )
    }

    @Test
    fun newSlotIsUsingReusedSlot() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(2))

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf(2, 3)
            // 0 and 1 are now in reusable buffer
        }

        rule.runOnIdle {
            items.value = listOf(2, 3, 5)
            // the last reusable slot (1) will be used for composing 5
        }

        assertNodes(
            exists = /*active*/ listOf(2, 3, 5) + /*reusable*/ listOf(0),
            doesNotExist = /*disposed*/ listOf(1, 4)
        )
    }

    @Test
    fun theSameSlotIsUsedWhileItIsInReusableList() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(2))

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf(2, 3)
            // 0 and 1 are now in reusable buffer
        }

        rule.runOnIdle {
            items.value = listOf(2, 3, 1)
            // slot 1 should be taken back from reusable
        }

        assertNodes(
            exists = /*active*/ listOf(2, 3, 1) + /*reusable*/ listOf(0)
        )
    }

    @Test
    fun prefetchIsUsingReusableNodes() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(2))

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf(2, 3)
            // 0 and 1 are now in reusable buffer
        }

        rule.runOnIdle {
            state.precompose(5) {
                ItemContent(5)
            }
            // prefetch should take slot 1 from reuse
        }

        assertNodes(
            exists = /*active*/ listOf(2, 3) + /*prefetch*/ listOf(5) + /*reusable*/ listOf(0)
        )
    }

    @Test
    fun prefetchSlotWhichIsInReusableList() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(3))

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf(2)
            // 0, 1, 3 are now in reusable buffer
        }

        rule.runOnIdle {
            state.precompose(3) {
                ItemContent(3)
            }
            // prefetch should take slot 3 from reuse
        }

        assertNodes(
            exists = /*active*/ listOf(2) + /*prefetch*/ listOf(3) + /*reusable*/ listOf(0, 1),
            doesNotExist = listOf(4)
        )
    }

    @Test
    fun nothingIsReusedWhenMaxSlotsAre0() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(0))

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf(2, 4)
        }

        assertNodes(
            exists = listOf(2, 4),
            doesNotExist = listOf(0, 1, 3)
        )
    }

    @Test
    fun reuse1Node() {
        val items = mutableStateOf(listOf(0, 1, 2, 3))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf(0, 1)
        }

        assertNodes(
            exists = /*active*/ listOf(0, 1) + /*reusable*/ listOf(2),
            doesNotExist = /*disposed*/ listOf(3)
        )
    }

    @SuppressLint("RememberReturnType")
    @Test
    fun reusedCompositionResetsRememberedObject() {
        val slotState = mutableStateOf(0)
        var lastRememberedSlot: Any? = null
        var lastRememberedComposedModifierSlot: Any? = null

        rule.setContent {
            SubcomposeLayout(remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }) {
                val slot = slotState.value
                subcompose(slot) {
                    ReusableContent(slot) {
                        remember {
                            lastRememberedSlot = slot
                        }
                        Box(
                            Modifier.composed {
                                remember {
                                    lastRememberedComposedModifierSlot = slot
                                }
                                Modifier
                            }
                        )
                    }
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(lastRememberedSlot).isEqualTo(0)
            assertThat(lastRememberedComposedModifierSlot).isEqualTo(0)
            slotState.value = 1
        }

        rule.runOnIdle {
            assertThat(lastRememberedSlot).isEqualTo(1)
            assertThat(lastRememberedComposedModifierSlot).isEqualTo(1)
            slotState.value = 2
        }

        rule.runOnIdle {
            assertThat(lastRememberedSlot).isEqualTo(2)
            assertThat(lastRememberedComposedModifierSlot).isEqualTo(2)
        }
    }

    @Test
    fun subcomposeLayoutInsideLayoutUsingAlignmentsIsNotCrashing() {
        // fix for regression from b/189965769
        val emit = mutableStateOf(false)
        rule.setContent {
            LayoutUsingAlignments {
                Box {
                    if (emit.value) {
                        SubcomposeLayout {
                            subcompose(Unit) {}
                            layout(10, 10) {}
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            emit.value = true
        }

        // awaits that the change is applied and no crash happened
        rule.runOnIdle { }
    }

    @Test
    fun compositionLocalChangeInMainCompositionRecomposesSubcomposition() {
        var flag by mutableStateOf(true)
        val compositionLocal = compositionLocalOf<Boolean> { error("") }
        var subcomposionValue: Boolean? = null
        val subcomposeLambda = @Composable {
            // makes sure the recomposition happens only once after the change
            assertThat(compositionLocal.current).isNotEqualTo(subcomposionValue)
            subcomposionValue = compositionLocal.current
        }

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides flag) {
                val mainMovableValue = flag
                SubcomposeLayout(
                    Modifier.drawBehind {
                        // makes sure we never draw inconsistent states
                        assertThat(subcomposionValue).isEqualTo(mainMovableValue)
                    }
                ) {
                    subcompose(Unit, subcomposeLambda)
                    layout(100, 100) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isFalse()
        }
    }

    @Test
    fun compositionLocalChangeInMainCompositionRecomposesSubcomposition_noRemeasure() {
        var flag by mutableStateOf(true)
        val compositionLocal = compositionLocalOf<Boolean> { error("") }
        var subcomposionValue: Boolean? = null
        val subcomposeLambda = @Composable {
            // makes sure the recomposition happens only once after the change
            assertThat(compositionLocal.current).isNotEqualTo(subcomposionValue)
            subcomposionValue = compositionLocal.current
        }
        val measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult = {
            subcompose(Unit, subcomposeLambda)
            layout(100, 100) {}
        }

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides flag) {
                SubcomposeLayout(measurePolicy = measurePolicy)
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isFalse()
        }
    }

    @Test
    fun staticCompositionLocalChangeInMainCompositionRecomposesSubcomposition() {
        var flag by mutableStateOf(true)
        val compositionLocal = staticCompositionLocalOf<Boolean> { error("") }
        var subcomposionValue: Boolean? = null
        val subcomposeLambda = @Composable {
            // makes sure the recomposition happens only once after the change
            assertThat(compositionLocal.current).isNotEqualTo(subcomposionValue)
            subcomposionValue = compositionLocal.current
        }
        val measureBlock: SubcomposeMeasureScope.(Constraints) -> MeasureResult = {
            subcompose(Unit, subcomposeLambda)
            layout(100, 100) {}
        }

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides flag) {
                val mainCompositionValue = flag
                SubcomposeLayout(
                    Modifier.drawBehind {
                        // makes sure we never draw inconsistent states
                        assertThat(subcomposionValue).isEqualTo(mainCompositionValue)
                    },
                    measureBlock
                )
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isFalse()
        }
    }

    @Test
    fun staticCompositionLocalChangeInMainCompositionRecomposesSubcomposition_noRemeasure() {
        var flag by mutableStateOf(true)
        val compositionLocal = staticCompositionLocalOf<Boolean> { error("") }
        var subcomposionValue: Boolean? = null
        val subcomposeLambda = @Composable {
            // makes sure the recomposition happens only once after the change
            assertThat(compositionLocal.current).isNotEqualTo(subcomposionValue)
            subcomposionValue = compositionLocal.current
        }
        val measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult = {
            subcompose(Unit, subcomposeLambda)
            layout(100, 100) {}
        }

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides flag) {
                SubcomposeLayout(measurePolicy = measurePolicy)
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isFalse()
        }
    }

    @Test
    fun derivedStateChangeInMainCompositionRecomposesSubcomposition() {
        var flag by mutableStateOf(true)
        var subcomposionValue: Boolean? = null

        rule.setContent {
            val updatedState = rememberUpdatedState(flag)
            val derivedState = remember { derivedStateOf { updatedState.value } }
            val subcomposeLambda = remember<@Composable () -> Unit> {
                {
                    // makes sure the recomposition happens only once after the change
                    assertThat(derivedState.value).isNotEqualTo(subcomposionValue)
                    subcomposionValue = derivedState.value
                }
            }

            SubcomposeLayout(
                Modifier.drawBehind {
                    // makes sure we never draw inconsistent states
                    assertThat(subcomposionValue).isEqualTo(updatedState.value)
                }
            ) {
                subcompose(Unit, subcomposeLambda)
                layout(100, 100) {}
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isFalse()
        }
    }

    @Test
    fun updatingStateWorks() {
        val tagState = mutableStateOf("box1")

        rule.setContent {
            val tag = tagState.value
            val state = remember(tag) { SubcomposeLayoutState() }

            SubcomposeLayout(state = state) {
                val placeable = subcompose(Unit) {
                    Box(Modifier.size(10.dp).testTag(tag))
                }.first().measure(Constraints())
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }

        rule.onNodeWithTag("box1").assertIsDisplayed()

        rule.runOnIdle { tagState.value = "box2" }

        rule.onNodeWithTag("box2").assertIsDisplayed()
    }

    @Test
    fun nodesKeptAsReusableAreReusedWhenTheStateObjectChanges() {
        val slotState = mutableStateOf(0)
        var remeasuresCount = 0
        val measureModifier = Modifier.layout { _, _ ->
            remeasuresCount++
            layout(10, 10) {}
        }
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))

        rule.setContent {
            val slot = slotState.value
            SubcomposeLayout(layoutState.value) {
                val placeable = subcompose(slot) {
                    ReusableContent(slot) {
                        Box(measureModifier)
                    }
                }.first().measure(Constraints())
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            slotState.value = 1
            // slot 0 is kept for reuse
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(2)
            remeasuresCount = 0
            slotState.value = 2 // slot 0 should be reused
            layoutState.value = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        }

        rule.runOnIdle {
            // there is no remeasure as the node was reused and the modifier didn't change
            assertThat(remeasuresCount).isEqualTo(0)
        }
    }

    @Test
    fun previouslyActiveNodesAreReusedWhenTheStateObjectChanges() {
        val slotState = mutableStateOf(0)
        var remeasuresCount = 0
        val measureModifier = Modifier.layout { _, _ ->
            remeasuresCount++
            layout(10, 10) {}
        }
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))

        rule.setContent {
            val slot = slotState.value
            SubcomposeLayout(layoutState.value) { _ ->
                val placeable = subcompose(slot) {
                    ReusableContent(slot) {
                        Box(measureModifier)
                    }
                }.first().measure(Constraints())
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(1)
            remeasuresCount = 0
            slotState.value = 1 // slot 0 should be reused
            layoutState.value = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        }

        rule.runOnIdle {
            // there is no remeasure as the node was reused and the modifier didn't change
            assertThat(remeasuresCount).isEqualTo(0)
        }
    }

    @Test
    fun reusableNodeIsKeptAsReusableAfterStateUpdate() {
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))
        val needChild = mutableStateOf(true)
        var coordinates: LayoutCoordinates? = null

        rule.setContent {
            SubcomposeLayout(state = layoutState.value) { constraints ->
                val node = if (needChild.value) {
                    subcompose(Unit) {
                        Box(Modifier.onGloballyPositioned { coordinates = it })
                    }.first().measure(constraints)
                } else {
                    null
                }
                layout(10, 10) {
                    node?.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(coordinates!!.isAttached).isTrue()
            needChild.value = false
        }

        rule.runOnIdle {
            // the modifier is still attached
            assertThat(coordinates!!.isAttached).isTrue()
            layoutState.value = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        }

        rule.runOnIdle {
            // the modifier is still attached
            assertThat(coordinates!!.isAttached).isTrue()
        }
    }

    @Test
    fun passingSmallerMaxSlotsToRetainForReuse() {
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))
        val needChild = mutableStateOf(true)
        var coordinates: LayoutCoordinates? = null

        rule.setContent {
            SubcomposeLayout(state = layoutState.value) { constraints ->
                val node = if (needChild.value) {
                    subcompose(Unit) {
                        Box(Modifier.onGloballyPositioned { coordinates = it })
                    }.first().measure(constraints)
                } else {
                    null
                }
                layout(10, 10) {
                    node?.place(0, 0)
                }
            }
        }

        rule.runOnIdle { needChild.value = false }

        rule.runOnIdle {
            // the node  in the reusable pool is still attached
            assertThat(coordinates!!.isAttached).isTrue()
            layoutState.value = SubcomposeLayoutState(SubcomposeSlotReusePolicy(0))
        }

        rule.runOnIdle {
            // detached as the new state has 0 as maxSlotsToRetainForReuse
            assertThat(coordinates!!.isAttached).isFalse()
        }
    }

    @Test
    fun compositionKeptForReuseIsDisposed() {
        val needChild = mutableStateOf(true)
        var disposed = false

        rule.setContent {
            SubcomposeLayout(
                state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
            ) {
                if (needChild.value) {
                    subcompose(Unit) {
                        DisposableEffect(Unit) {
                            onDispose {
                                disposed = true
                            }
                        }
                    }
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle { needChild.value = false }

        rule.runOnIdle {
            // the composition in the reusable pool is disposed
            assertThat(disposed).isTrue()
        }
    }

    @Test
    fun composedModifierOnReusableNodeIsDisposedButAttached() {
        val layoutState = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        val needChild = mutableStateOf(true)
        var composed = false
        var coordinates: LayoutCoordinates? = null

        rule.setContent {
            SubcomposeLayout(state = layoutState) { constraints ->
                val node = if (needChild.value) {
                    subcompose(Unit) {
                        Box(Modifier.composed {
                            DisposableEffect(Unit) {
                                composed = true
                                onDispose {
                                    composed = false
                                }
                            }
                            onGloballyPositioned {
                                coordinates = it
                            }
                        })
                    }.first().measure(constraints)
                } else {
                    null
                }
                layout(10, 10) {
                    node?.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(composed).isTrue()
            assertThat(coordinates!!.isAttached).isTrue()
            needChild.value = false
        }

        rule.runOnIdle {
            assertThat(composed).isFalse()
            assertThat(coordinates!!.isAttached).isTrue()
            needChild.value = true
        }

        rule.runOnIdle {
            assertThat(composed).isTrue()
            assertThat(coordinates!!.isAttached).isTrue()
        }
    }

    @Test
    fun customPolicy_retainingExactItem() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val policy = object : SubcomposeSlotReusePolicy {
            override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
                assertThat(slotIds).containsExactly(1, 2, 4).inOrder()
                slotIds.remove(1)
                slotIds.remove(4)
            }

            override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean {
                assertThat(reusableSlotId).isEqualTo(2)
                return true
            }
        }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf(0, 3)
        }

        assertNodes(
            exists = /*active*/ listOf(0, 3) + /*reusable*/ listOf(2),
            doesNotExist = /*disposed*/ listOf(1, 4)
        )

        rule.runOnIdle {
            items.value = listOf(0, 3, 5)
        }

        assertNodes(
            exists = /*active*/ listOf(0, 3, 5) + /*reusable*/ emptyList(),
            doesNotExist = /*disposed*/ listOf(1, 2, 4)
        )
    }

    @Test
    fun customPolicy_lastUsedItemsAreFirstInSet() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        var expectedReusable = arrayOf<Int>()
        val policy = object : SubcomposeSlotReusePolicy {
            override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
                assertThat(slotIds).containsExactly(*expectedReusable).inOrder()
            }

            override fun areCompatible(slotId: Any?, reusableSlotId: Any?) = true
        }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)

        rule.runOnIdle {
            expectedReusable = arrayOf(1, 2, 3, 4)
            items.value = listOf(0)
        }

        rule.runOnIdle {
            expectedReusable = arrayOf(1, 2, 3)
            items.value = listOf(0, 4)
        }

        rule.runOnIdle {
            expectedReusable = arrayOf(4, 1, 2, 3)
            items.value = listOf(0)
        }
    }

    @Test
    fun customPolicy_disposedPrefetchedItemIsFirstInSet() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        var expectedReusable = arrayOf<Int>()
        var callbackCalled = false
        var expectedSlotId: Any? = null
        var expectedreusableSlotId: Any? = null
        val policy = object : SubcomposeSlotReusePolicy {
            override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
                callbackCalled = true
                assertThat(slotIds).containsExactly(*expectedReusable).inOrder()
            }

            override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean {
                assertThat(slotId).isEqualTo(expectedSlotId)
                assertThat(reusableSlotId).isEqualTo(expectedreusableSlotId)
                return true
            }
        }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)

        rule.runOnIdle {
            expectedReusable = arrayOf(1, 2, 3, 4)
            items.value = listOf(0)
        }

        rule.runOnIdle {
            assertThat(callbackCalled).isTrue()
            callbackCalled = false

            expectedSlotId = 5
            expectedreusableSlotId = 4
            val handle = state.precompose(5, {}) // it should reuse slot 4
            expectedReusable = arrayOf(5, 1, 2, 3)
            handle.dispose()
            assertThat(callbackCalled).isTrue()
        }
    }

    @Test
    fun customPolicy_retainingOddNumbers() {
        fun isOdd(number: Any?): Boolean {
            return (number as Int) % 2 == 1
        }
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6))
        val policy = object : SubcomposeSlotReusePolicy {
            override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
                slotIds.removeAll { !isOdd(it) }
            }

            override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean {
                return isOdd(slotId) && isOdd(reusableSlotId)
            }
        }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf()
        }

        assertNodes(
            exists = /*active*/ emptyList<Int>() + /*reusable*/ listOf(1, 3, 5),
            doesNotExist = /*disposed*/ listOf(0, 2, 4, 6)
        )

        rule.runOnIdle {
            items.value = listOf(8, 9, 10)
            // new slots composed for 8 and 10
            // 5 is reused for 9
        }

        assertNodes(
            exists = /*active*/ listOf(8, 9, 10) + /*reusable*/ listOf(1, 3),
            doesNotExist = /*disposed*/ listOf(5)
        )
    }

    @Test
    fun customPolicy_reusingSecondSlotFromTheEnd() {
        fun isOdd(number: Any?): Boolean {
            return (number as Int) % 2 == 1
        }
        val items = mutableStateOf(listOf(0, 1, 2, 3))
        val policy = object : SubcomposeSlotReusePolicy {
            override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {}

            override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean {
                return isOdd(slotId) == isOdd(reusableSlotId)
            }
        }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)

        rule.runOnIdle {
            items.value = listOf()
        }

        assertNodes(exists = /*active*/ emptyList<Int>() + /*reusable*/ listOf(0, 1, 2, 3))

        rule.runOnIdle {
            items.value = listOf(10) // slot 2 should be reused
        }

        assertNodes(
            exists = /*active*/ listOf(10) + /*reusable*/ listOf(0, 1, 3),
            doesNotExist = /*disposed*/ listOf(2)
        )
    }

    @Test
    fun premeasuringAllowsToSkipMeasureOnceTheSlotIsComposed() {
        val state = SubcomposeLayoutState()
        var remeasuresCount = 0
        var relayoutCount = 0
        var subcomposeLayoutRemeasures = 0
        val modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            remeasuresCount++
            layout(placeable.width, placeable.height) {
                relayoutCount++
                placeable.place(0, 0)
            }
        }.fillMaxSize()
        val content = @Composable { Box(modifier) }
        val constraints = Constraints(maxWidth = 100, minWidth = 100)
        var needContent by mutableStateOf(false)

        rule.setContent {
            SubcomposeLayout(state) {
                subcomposeLayoutRemeasures++
                val placeable = if (needContent) {
                    subcompose(Unit, content).first().measure(constraints)
                } else {
                    null
                }
                layout(10, 10) {
                    placeable?.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
            subcomposeLayoutRemeasures = 0
            val handle = state.precompose(Unit, content)

            assertThat(remeasuresCount).isEqualTo(0)
            assertThat(handle.placeablesCount).isEqualTo(1)
            handle.premeasure(0, constraints)

            assertThat(remeasuresCount).isEqualTo(1)
            assertThat(relayoutCount).isEqualTo(0)
            assertThat(subcomposeLayoutRemeasures).isEqualTo(0)
            remeasuresCount = 0

            needContent = true
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
            assertThat(relayoutCount).isEqualTo(1)
        }
    }

    @Test
    fun premeasuringTwoPlaceables() {
        val state = SubcomposeLayoutState()
        var remeasuresCount = 0
        val modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            remeasuresCount++
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }.fillMaxSize()
        val content = @Composable {
            Box(modifier)
            Box(modifier)
        }
        val constraints0 = Constraints(maxWidth = 100, minWidth = 100)
        val constraints1 = Constraints(maxWidth = 200, minWidth = 200)
        var needContent by mutableStateOf(false)

        rule.setContent {
            SubcomposeLayout(state) {
                val placeables = if (needContent) {
                    val measurables = subcompose(Unit, content)
                    assertThat(measurables.size).isEqualTo(2)
                    measurables.mapIndexed { index, measurable ->
                        measurable.measure(if (index == 0) constraints0 else constraints1)
                    }
                } else {
                    emptyList()
                }
                layout(10, 10) {
                    placeables.forEach { it.place(0, 0) }
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
            val handle = state.precompose(Unit, content)

            assertThat(remeasuresCount).isEqualTo(0)
            assertThat(handle.placeablesCount).isEqualTo(2)
            handle.premeasure(0, constraints0)

            assertThat(remeasuresCount).isEqualTo(1)
            handle.premeasure(1, constraints1)
            assertThat(remeasuresCount).isEqualTo(2)
            remeasuresCount = 0

            needContent = true
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
        }
    }

    @Test
    fun premeasuringIncorrectIndexesCrashes() {
        val state = SubcomposeLayoutState()
        val content = @Composable {
            Box(Modifier.size(10.dp))
            Box(Modifier.size(10.dp))
        }

        rule.setContent {
            SubcomposeLayout(state) {
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            val handle = state.precompose(Unit, content)

            assertThrows(IndexOutOfBoundsException::class.java) {
                handle.premeasure(-1, Constraints())
            }
            assertThrows(IndexOutOfBoundsException::class.java) {
                handle.premeasure(2, Constraints())
            }
        }
    }

    @Test
    fun ifSlotWasUsedDuringMeasurePassHandleHasZeroPlaceables() {
        val state = SubcomposeLayoutState()
        val content = @Composable {
            Box(Modifier.size(10.dp))
        }
        var needContent by mutableStateOf(false)

        rule.setContent {
            SubcomposeLayout(state) {
                val placeable = if (needContent) {
                    subcompose(Unit, content).first().measure(Constraints())
                } else {
                    null
                }
                layout(10, 10) {
                    placeable?.place(0, 0)
                }
            }
        }

        lateinit var handle: SubcomposeLayoutState.PrecomposedSlotHandle

        rule.runOnIdle {
            handle = state.precompose(Unit, content)
            handle.premeasure(0, Constraints())
            needContent = true
        }

        rule.runOnIdle {
            assertThat(handle.placeablesCount).isEqualTo(0)
            // we also make sure that calling dispose on such handle is safe
            handle.dispose()
        }
    }

    @Test
    fun stateIsRestoredWhenGoBackToScreen1WithSubcomposition() {
        val restorationTester = StateRestorationTester(rule)

        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(screen) {
                if (screen == Screens.Screen1) {
                    SubcomposeLayout {
                        subcompose(Unit) {
                            restorableNumberOnScreen1 = rememberSaveable { increment++ }
                        }
                        layout(10, 10) {}
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(0)
            screen = Screens.Screen2
        }

        // wait for the screen switch to apply
        rule.runOnIdle {
            restorableNumberOnScreen1 = -1
            // switch back to screen1
            screen = Screens.Screen1
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(0)
        }
    }

    @Test
    fun disposeSecondPrecomposedItem() {
        // it is a regression from b/218668336. the assertion was incorrectly checking
        // for the ranges so disposing the second active precomposed node was crashing.
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(0))

        composeItems(state, mutableStateOf(emptyList()))

        rule.runOnIdle {
            state.precompose(0) { ItemContent(0) }
            val handle = state.precompose(1) { ItemContent(1) }
            handle.dispose()
        }

        assertNodes(
            exists = /*prefetch*/ listOf(0),
            doesNotExist = /*disposed*/ listOf(1)
        )
    }

    @Test
    fun reusingWithNestedSubcomposeLayoutInside() {
        val slotState = mutableStateOf(0)

        rule.setContent {
            SubcomposeLayout(
                remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }
            ) { constraints ->
                val slot = slotState.value
                val child = subcompose(slot) {
                    ReusableContent(slot) {
                        Box {
                            SubcomposeLayout(Modifier.testTag("$slot")) { constraints ->
                                val placeable = subcompose(0) {
                                    Box(modifier = Modifier.size(10.dp))
                                }.first().measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }
                        }
                    }
                }.first().measure(constraints)
                layout(child.width, child.height) {
                    child.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            slotState.value = 1
        }

        rule.runOnIdle {
            slotState.value = 2
        }

        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun disposingPrecomposedItemInTheNestedSubcomposeLayout() {
        var needSlot by mutableStateOf(true)
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))

        rule.setContent {
            SubcomposeLayout(
                remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }
            ) { constraints ->
                val child = if (needSlot) {
                    subcompose(0) {
                        Box {
                            SubcomposeLayout(state = state, Modifier.testTag("0")) { constraints ->
                                if (needSlot) {
                                    val placeable = subcompose(0) {
                                        Box(modifier = Modifier.size(10.dp))
                                    }.first().measure(constraints)
                                    layout(placeable.width, placeable.height) {
                                        placeable.place(0, 0)
                                    }
                                } else {
                                    layout(100, 100) { }
                                }
                            }
                        }
                    }.first().measure(constraints)
                } else {
                    null
                }
                layout(100, 100) {
                    child?.place(0, 0)
                }
            }
        }

        val handle = rule.runOnIdle {
            state.precompose(1) {
                Box(modifier = Modifier.size(10.dp).testTag("1"))
            }
        }

        rule.runOnIdle {
            needSlot = false
        }

        rule.runOnIdle {
            handle.dispose()
        }

        rule.onNodeWithTag("1").assertDoesNotExist()
        rule.onNodeWithTag("0").assertIsNotDisplayed()
    }

    private fun composeItems(
        state: SubcomposeLayoutState,
        items: MutableState<List<Int>>
    ) {
        rule.setContent {
            SubcomposeLayout(state) { constraints ->
                items.value.forEach {
                    subcompose(it) {
                        ItemContent(it)
                    }.forEach {
                        it.measure(constraints)
                    }
                }
                layout(10, 10) {}
            }
        }
    }

    @Composable
    private fun ItemContent(index: Int) {
        Box(Modifier.fillMaxSize().testTag("$index"))
    }

    private fun assertNodes(exists: List<Int>, doesNotExist: List<Int> = emptyList()) {
        exists.forEach {
            rule.onNodeWithTag("$it")
                .assertExists()
        }
        doesNotExist.forEach {
            rule.onNodeWithTag("$it")
                .assertDoesNotExist()
        }
    }
}

fun ImageBitmap.assertCenterPixelColor(expectedColor: Color) {
    asAndroidBitmap().assertColor(expectedColor, width / 2, height / 2)
}

@Composable
private fun LayoutUsingAlignments(content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        placeable[FirstBaseline]
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

private enum class Screens {
    Screen1,
    Screen2,
}