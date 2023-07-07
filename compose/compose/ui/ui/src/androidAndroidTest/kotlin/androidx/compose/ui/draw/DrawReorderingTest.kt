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

package androidx.compose.ui.draw

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.padding
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.validateSquareColors
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class DrawReorderingTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testDrawingOrderWhenWePlaceItemsInTheNaturalOrder() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(
                            10,
                            Modifier.padding(10)
                                .background(Color.White)
                        )
                        FixedSize(
                            30,
                            Modifier.graphicsLayer()
                                .background(Color.Red)
                                .drawLatchModifier()
                        )
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables.forEach { child ->
                            child.placeRelative(0, 0)
                        }
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Red,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testDrawingOrderWhenWePlaceItemsInTheReverseOrder() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(
                            10,
                            Modifier.padding(10)
                                .background(Color.White)
                        )
                        FixedSize(
                            30,
                            Modifier.graphicsLayer()
                                .background(Color.Red)
                                .drawLatchModifier()
                        )
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables.reversed().forEach { child ->
                            child.placeRelative(0, 0)
                        }
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testDrawingOrderIsOverriddenWithZIndexModifierWhenWePlaceItemsInTheReverseOrder() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(
                            10,
                            Modifier.padding(10)
                                .background(Color.White)
                        )
                        FixedSize(
                            30,
                            Modifier.graphicsLayer()
                                .background(Color.Red)
                                .zIndex(1f)
                                .drawLatchModifier()
                        )
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables.reversed().forEach { child ->
                            child.placeRelative(0, 0)
                        }
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Red,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testDrawingOrderIsOverriddenWithZIndexWhenWePlaceItemsInTheReverseOrder() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(
                            10,
                            Modifier.padding(10)
                                .background(Color.White)
                        )
                        FixedSize(
                            30,
                            Modifier.graphicsLayer()
                                .background(Color.Red)
                                .zIndex(1f)
                                .drawLatchModifier()
                        )
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables.reversed().forEach { child ->
                            child.place(0, 0, zIndex = placeables.indexOf(child).toFloat())
                        }
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Red,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testCustomDrawingOrderForThreeItems() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(
                            30,
                            Modifier.graphicsLayer()
                                .background(Color.Red)
                                .drawLatchModifier()
                        )
                        FixedSize(
                            10,
                            Modifier.padding(10)
                                .background(Color.White)
                        )
                        FixedSize(
                            30,
                            Modifier.graphicsLayer()
                                .background(Color.Blue)
                                .drawLatchModifier()
                        )
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables[2].place(0, 0)
                        placeables[0].place(0, 0)
                        placeables[1].place(0, 0)
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    fun placingTheSameItemTwiceIsNotAllowedAsItBreaksTheDrawingOrder() {
        var exception: Throwable? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(30)
                    }
                ) { measurables, constraints ->
                    val placeables = measurables.first().measure(constraints)
                    layout(30, 30) {
                        placeables.place(0, 0)
                        try {
                            placeables.place(0, 0)
                        } catch (e: Throwable) {
                            exception = e
                        }
                        latch.countDown()
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(exception)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSiblingZOrder() {
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30
                ) {
                    FixedSize(
                        10,
                        Modifier.padding(10)
                            .zIndex(1f)
                            .background(Color.White)
                    )
                    FixedSize(
                        30,
                        Modifier.graphicsLayer()
                            .background(Color.Red)
                            .drawLatchModifier()
                    )
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testUncleZOrder() {
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30
                ) {
                    FixedSize(
                        10,
                        Modifier.padding(10)
                            .zIndex(1f)
                            .background(Color.White)
                    )
                    FixedSize(
                        30,
                        Modifier.background(Color.Red)
                            .drawLatchModifier()
                    )
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testCousinZOrder() {
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30
                ) {
                    FixedSize(10, Modifier.padding(10)) {
                        FixedSize(
                            10,
                            Modifier.zIndex(1f)
                                .background(Color.Green)
                        )
                    }
                    FixedSize(30, Modifier.background(Color.Red))
                    FixedSize(10, Modifier.padding(10)) {
                        FixedSize(
                            10,
                            Modifier.background(Color.White)
                                .drawLatchModifier()
                        )
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testCousinZOrder2() {
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30
                ) {
                    FixedSize(10, Modifier.padding(10)) {
                        FixedSize(
                            10,
                            Modifier.zIndex(1f)
                                .background(Color.Green)
                        )
                    }
                    FixedSize(
                        30,
                        Modifier.background(Color.Red)
                            .drawLatchModifier()
                    )
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Red,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testChangingZOrder() {
        val state = mutableStateOf(0f)
        val view = View(activity)
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30
                ) {
                    FixedSize(
                        10,
                        Modifier.padding(10)
                            .zIndex(state.value)
                            .background(Color.Black)
                    )
                    FixedSize(
                        30,
                        Modifier.background(Color.Red)
                            .drawLatchModifier()
                    )
                    FixedSize(
                        10,
                        Modifier.padding(10).background(Color.White)
                    )
                }
            }
            activity.addContentView(view, ViewGroup.LayoutParams(1, 1))
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )

        val onDrawListener = object :
            ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                drawLatch.countDown()
            }
        }
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            view.viewTreeObserver.addOnDrawListener(onDrawListener)
            state.value = 1f
            view.invalidate()
        }

        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Black,
            size = 10,
            drawLatch = drawLatch
        )
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            state.value = 0f
            view.invalidate()
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testChangingZOrderReusingModifiers() {
        val state = mutableStateOf(0f)
        val zIndex = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0, zIndex = state.value)
            }
        }
        val modifier1 = Modifier.padding(10)
            .then(zIndex)
            .background(Color.White)
        val modifier2 = Modifier.background(Color.Red)
            .drawLatchModifier()
        val view = View(activity)
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(size = 30) {
                    FixedSize(10, modifier1)
                    FixedSize(30, modifier2)
                }
            }
            activity.addContentView(view, ViewGroup.LayoutParams(1, 1))
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Red,
            size = 10,
            drawLatch = drawLatch
        )

        val onDrawListener = object :
            ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                drawLatch.countDown()
            }
        }
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            view.viewTreeObserver.addOnDrawListener(onDrawListener)
            state.value = 1f
            view.invalidate()
        }

        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )

        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            state.value = 0f
            view.invalidate()
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Red,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testChangingZOrderUncle() {
        val state = mutableStateOf(0f)
        val elevation = Modifier.graphicsLayer {
            shadowElevation = state.value
        }
        val view = View(activity)
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30
                ) {
                    FixedSize(30) {
                        FixedSize(
                            10,
                            Modifier.padding(10).then(elevation).background(Color.Black)
                        )
                    }
                    FixedSize(
                        30,
                        Modifier.background(Color.Red)
                            .drawLatchModifier()
                    )
                    FixedSize(
                        10,
                        Modifier.padding(10)
                            .background(Color.White)
                    )
                }
            }
            activity.addContentView(view, ViewGroup.LayoutParams(1, 1))
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
        val onDrawListener = object :
            ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                drawLatch.countDown()
            }
        }
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            view.viewTreeObserver.addOnDrawListener(onDrawListener)
            state.value = 1f
            view.invalidate()
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testChangingReorderedChildSize() {
        val size = mutableStateOf(10)
        val view = View(activity)
        rule.runOnUiThread {
            activity.setContent {
                AtLeastSize(
                    size = 30,
                    modifier = Modifier.background(Color.Red)
                ) {
                    FixedSize(
                        size,
                        Modifier.padding(10)
                            .zIndex(1f)
                            .background(Color.White)
                    )
                    FixedSize(
                        30,
                        Modifier.background(Color.Red)
                            .drawLatchModifier()
                    )
                }
            }
            activity.addContentView(view, ViewGroup.LayoutParams(1, 1))
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
        val onDrawListener = object :
            ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                drawLatch.countDown()
            }
        }
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            view.viewTreeObserver.addOnDrawListener(onDrawListener)
            size.value = 20
            view.invalidate()
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 20,
            totalSize = 40,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testInvalidateReorderedChild() {
        val color = mutableStateOf(Color.Red)
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(size = 30) {
                    FixedSize(
                        10,
                        Modifier.padding(10)
                            .zIndex(1f)
                            .background(Color.White)
                    )
                    FixedSize(
                        30,
                        Modifier.background(color.value)
                            .drawLatchModifier()
                    )
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            color.value = Color.Blue
        }
        rule.validateSquareColors(
            outerColor = Color.Blue,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun sumOfAllZIndexesIsUsed() {
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30
                ) {
                    FixedSize(
                        10,
                        Modifier.padding(10)
                            .zIndex(2f)
                            .zIndex(2f)
                            .background(Color.White)
                    )
                    FixedSize(
                        30,
                        Modifier.zIndex(4f)
                            .zIndex(-1f)
                            .background(Color.Red)
                            .drawLatchModifier()
                    )
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testInvalidateParentOfReorderedChild() {
        val color = mutableStateOf(Color.Red)
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(size = 30) {
                    FixedSize(
                        10,
                        Modifier.padding(10)
                            .zIndex(1f)
                            .background(Color.White)
                    )
                    FixedSize(30, Modifier.background(color.value).drawLatchModifier())
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
        drawLatch = CountDownLatch(1)
        rule.runOnUiThread {
            color.value = Color.Blue
        }
        rule.validateSquareColors(
            outerColor = Color.Blue,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testShadowSizeIsNotCausingReorder() {
        rule.runOnUiThread {
            activity.setContent {
                FixedSize(
                    size = 30
                ) {
                    FixedSize(
                        10,
                        Modifier.padding(10)
                            .graphicsLayer(shadowElevation = 1f)
                            .background(Color.White)
                    )
                    FixedSize(
                        30,
                        Modifier.graphicsLayer()
                            .background(Color.Red)
                            .drawLatchModifier()
                    )
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Red,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun placeOrderIsUsedWhenParentProvidedSameZIndex() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(30) {
                            FixedSize(
                                10,
                                Modifier.padding(10)
                                    .background(Color.White)
                            )
                        }
                        FixedSize(30) {
                            FixedSize(
                                30,
                                Modifier.background(Color.Red)
                                    .drawLatchModifier()
                            )
                        }
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables[0].place(0, 0, zIndex = 1f)
                        placeables[1].place(0, 0, zIndex = 1f)
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.Red,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun placeOrderIsUsedWhenParentProvidedSameZIndex_reversePlaceOrder() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(30) {
                            FixedSize(
                                10,
                                Modifier.padding(10)
                                    .background(Color.White)
                            )
                        }
                        FixedSize(30) {
                            FixedSize(
                                30,
                                Modifier.background(Color.Red)
                                    .drawLatchModifier()
                            )
                        }
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables[1].place(0, 0, zIndex = 1f)
                        placeables[0].place(0, 0, zIndex = 1f)
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun parentProvidedZIndexSummedWithTheOneFromModifier() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(30, Modifier.zIndex(2f)) {
                            FixedSize(
                                10,
                                Modifier.padding(10)
                                    .background(Color.White)
                            )
                        }
                        FixedSize(30) {
                            FixedSize(
                                30,
                                Modifier.background(Color.Red)
                                    .drawLatchModifier()
                            )
                        }
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables[0].place(0, 0, zIndex = 1f)
                        placeables[1].place(0, 0, zIndex = 2f)
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun placeRelativePassesZIndex() {
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(30) {
                            FixedSize(
                                10,
                                Modifier.padding(10)
                                    .background(Color.White)
                            )
                        }
                        FixedSize(30) {
                            FixedSize(
                                30,
                                Modifier.background(Color.Red)
                                    .drawLatchModifier()
                            )
                        }
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables[0].placeRelative(0, 0, zIndex = 1f)
                        placeables[1].placeRelative(0, 0, zIndex = -1f)
                    }
                }
            }
        }
        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun whenSecondChildAddedLaterDrawingOrderIsStillCorrect() {
        var needSecondChild by mutableStateOf(false)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(30) {
                            FixedSize(
                                10,
                                Modifier.padding(10)
                                    .background(Color.White)
                                    .drawLatchModifier()
                            )
                        }
                        if (needSecondChild) {
                            FixedSize(30) {
                                FixedSize(
                                    30,
                                    Modifier.background(Color.Red)
                                )
                            }
                        }
                    }
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        placeables[0].placeRelative(0, 0, zIndex = 1f)
                        placeables.getOrNull(1)?.placeRelative(0, 0)
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        rule.runOnUiThread {
            drawLatch = CountDownLatch(1)
            needSecondChild = true
        }

        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun placingInDifferentOrderTriggersRedraw() {
        var reverseOrder by mutableStateOf(false)
        rule.runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        FixedSize(30) {
                            FixedSize(
                                10,
                                Modifier.padding(10)
                                    .background(Color.White)
                            )
                        }
                        FixedSize(30) {
                            FixedSize(
                                30,
                                Modifier.background(Color.Red)
                            )
                        }
                    },
                    modifier = Modifier.drawLatchModifier()
                ) { measurables, _ ->
                    val newConstraints = Constraints.fixed(30, 30)
                    val placeables = measurables.map { m ->
                        m.measure(newConstraints)
                    }
                    layout(newConstraints.maxWidth, newConstraints.maxWidth) {
                        if (!reverseOrder) {
                            placeables[0].place(0, 0)
                            placeables[1].place(0, 0)
                        } else {
                            placeables[1].place(0, 0)
                            placeables[0].place(0, 0)
                        }
                    }
                }
            }
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            drawLatch = CountDownLatch(1)
            reverseOrder = true
        }

        rule.validateSquareColors(
            outerColor = Color.Red,
            innerColor = Color.White,
            size = 10,
            drawLatch = drawLatch
        )
    }

    fun Modifier.drawLatchModifier() = drawBehind { drawLatch.countDown() }
}

@Composable
private fun FixedSize(
    size: State<Int>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Layout(content = content, modifier = modifier) { measurables, _ ->
        val newConstraints = Constraints.fixed(size.value, size.value)
        val placeables = measurables.map { m ->
            m.measure(newConstraints)
        }
        layout(size.value, size.value) {
            placeables.forEach { child ->
                child.placeRelative(0, 0)
            }
        }
    }
}
