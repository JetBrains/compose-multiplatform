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

import android.graphics.Bitmap
import android.os.Build
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.draw.assertColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertPositionInRootIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createAndroidComposeRule
import androidx.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalSubcomposeLayoutApi::class)
class SubcomposeLayoutTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun useSizeOfTheFirstItemInSecondSubcomposition() {
        val firstTag = "first"
        val secondTag = "second"

        rule.setContent {
            SubcomposeLayout<Int> { constraints ->
                val first = subcompose(0) {
                    Spacer(Modifier.size(50.dp).testTag(firstTag))
                }.first().measure(constraints)

                // it is an input for the second subcomposition
                val halfFirstSize = (first.width / 2).toDp()

                val second = subcompose(1) {
                    Spacer(Modifier.size(halfFirstSize).testTag(secondTag))
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
            SubcomposeLayout<Unit>(Modifier.testTag(layoutTag)) { constraints ->
                val placeables = subcompose(Unit) {
                    Spacer(Modifier.size(50.dp).testTag(firstTag))
                    Spacer(Modifier.size(30.dp).testTag(secondTag))
                }.map {
                    it.measure(constraints)
                }

                val maxWidth = placeables.maxByOrNull { it.width }!!.width
                val height = placeables.sumBy { it.height }

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
            SubcomposeLayout<Unit> { constraints ->
                measuresCount++
                val placeable = subcompose(Unit) {
                    recompositionsCount1++
                    NonInlineBox(Modifier.size(20.dp)) {
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
    private fun NonInlineBox(modifier: Modifier, children: @Composable () -> Unit) {
        Box(modifier = modifier) { children() }
    }

    @Test
    fun recompositionOfTheFirstSlotDoestAffectTheSecond() {
        val model = mutableStateOf(0)
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        rule.setContent {
            SubcomposeLayout<Int> {
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
            SubcomposeLayout<Unit>(Modifier.testTag(layoutTag)) { constraints ->
                val placeables = subcompose(Unit) {
                    if (addChild.value) {
                        Spacer(Modifier.size(20.dp).testTag(childTag))
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
            Spacer(Modifier.size(10.dp))
        })

        rule.setContent {
            MySubcomposeLayout(content.value)
        }

        val updatedTag = "updated"

        rule.runOnIdle {
            content.value = {
                Spacer(Modifier.size(10.dp).testTag(updatedTag))
            }
        }

        rule.onNodeWithTag(updatedTag)
            .assertIsDisplayed()
    }

    @Composable
    private fun MySubcomposeLayout(slotContent: @Composable () -> Unit) {
        SubcomposeLayout<Unit> { constraints ->
            val placeables = subcompose(Unit, slotContent).map { it.measure(constraints) }
            val maxWidth = placeables.maxByOrNull { it.width }!!.width
            val height = placeables.sumBy { it.height }
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
            SubcomposeLayout<Unit> {
                if (addSlot.value) {
                    subcompose(Unit) {
                        onActive {
                            composed = true
                        }
                        onDispose {
                            disposed = true
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
            SubcomposeLayout<Color>(Modifier.testTag(layoutTag)) { constraints ->
                val first = subcompose(Color.Red) {
                    Spacer(Modifier.size(10.dp).background(Color.Red))
                }.first().measure(constraints)
                val second = subcompose(Color.Green) {
                    Spacer(Modifier.size(10.dp).background(Color.Green))
                }.first().measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(layoutTag)
            .captureToBitmap()
            .assertCenterPixelColor(Color.Green)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun slotsCouldBeReordered() {
        val layoutTag = "layout"
        val firstSlotIsRed = mutableStateOf(true)

        rule.setContent {
            SubcomposeLayout<Color>(Modifier.testTag(layoutTag)) { constraints ->
                val firstColor = if (firstSlotIsRed.value) Color.Red else Color.Green
                val secondColor = if (firstSlotIsRed.value) Color.Green else Color.Red
                val first = subcompose(firstColor) {
                    Spacer(Modifier.size(10.dp).background(firstColor))
                }.first().measure(constraints)
                val second = subcompose(secondColor) {
                    Spacer(Modifier.size(10.dp).background(secondColor))
                }.first().measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.onNodeWithTag(layoutTag)
            .captureToBitmap()
            .assertCenterPixelColor(Color.Green)

        rule.runOnIdle {
            firstSlotIsRed.value = false
        }

        rule.onNodeWithTag(layoutTag)
            .captureToBitmap()
            .assertCenterPixelColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawingOrderCouldBeChangedUsingZIndex() {
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout<Color>(Modifier.testTag(layoutTag)) { constraints ->
                val first = subcompose(Color.Red) {
                    Spacer(Modifier.size(10.dp).background(Color.Red).zIndex(1f))
                }.first().measure(constraints)
                val second = subcompose(Color.Green) {
                    Spacer(Modifier.size(10.dp).background(Color.Green))
                }.first().measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.onNodeWithTag(layoutTag)
            .captureToBitmap()
            .assertCenterPixelColor(Color.Red)
    }

    @Test
    fun slotsAreDisposedWhenLayoutIsDisposed() {
        val addLayout = mutableStateOf(true)
        var firstDisposed = false
        var secondDisposed = false

        rule.setContent {
            if (addLayout.value) {
                SubcomposeLayout<Int> {
                    subcompose(0) {
                        onDispose {
                            firstDisposed = true
                        }
                    }
                    subcompose(1) {
                        onDispose {
                            secondDisposed = true
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
            val sizeIpx = with(density) { size.toIntPx() }
            Providers(DensityAmbient provides density) {
                SubcomposeLayout<Unit>(
                    Modifier.size(size).onGloballyPositioned {
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
            SubcomposeLayout<Color>(Modifier.testTag(layoutTag)) { constraints ->
                val first = subcompose(Color.Red) {
                    Spacer(Modifier.size(10.dp).background(Color.Red))
                }.first().measure(constraints)
                val second = subcompose(Color.Green) {
                    Spacer(Modifier.size(10.dp).background(Color.Green))
                }.first().measure(constraints)

                layout(first.width, first.height) {
                    second.place(0, 0)
                    first.place(0, 0)
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(layoutTag)
            .captureToBitmap()
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
        lateinit var container2: FrameLayout
        val state = mutableStateOf(10.dp)
        var stateUsedLatch = CountDownLatch(1)

        scenario.onActivity {
            container1 = FrameLayout(it)
            container2 = FrameLayout(it)
            it.setContentView(container1)
            container1.addView(container2)
            container2.setContent(Recomposer.current()) {
                SubcomposeLayout<Unit> { constraints ->
                    val first = subcompose(Unit) {
                        stateUsedLatch.countDown()
                        Box(Modifier.size(state.value))
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
}

fun Bitmap.assertCenterPixelColor(expectedColor: Color) {
    assertColor(expectedColor, width / 2, height / 2)
}
