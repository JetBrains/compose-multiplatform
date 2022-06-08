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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.SpaceAround
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import java.lang.Integer.max
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val Debug = false

@MediumTest
@RunWith(AndroidJUnit4::class)
class LookaheadLayoutTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun lookaheadLayoutAnimation() {
        var isLarge by mutableStateOf(true)
        var size1 = IntSize.Zero
        var size2 = IntSize.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadLayout(
                    measurePolicy = { measurables, constraints ->
                        val placeables = measurables.map { it.measure(constraints) }
                        val maxWidth: Int = placeables.maxOf { it.width }
                        val maxHeight = placeables.maxOf { it.height }
                        // Position the children.
                        layout(maxWidth, maxHeight) {
                            placeables.forEach {
                                it.place(0, 0)
                            }
                        }
                    },
                    content = {
                        Row(if (isLarge) Modifier.size(200.dp) else Modifier.size(50.dp, 100.dp)) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(2f)
                                    .onSizeChanged {
                                        size1 = it
                                    }
                                    .animateSize(this@LookaheadLayout))
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(3f)
                                    .onSizeChanged {
                                        size2 = it
                                    }
                                    .animateSize(this@LookaheadLayout))
                        }
                    }
                )
            }
        }
        // Check that:
        // 1) size changes happen when parent constraints change,
        // 2) animations finish and actual measurements get updated by animation,
        // 3) during the animation the tree is consistent.
        rule.runOnIdle {
            assertEquals(IntSize(80, 200), size1)
            assertEquals(IntSize(120, 200), size2)
            isLarge = false
        }
        rule.runOnIdle {
            assertEquals(IntSize(20, 100), size1)
            assertEquals(IntSize(30, 100), size2)
            isLarge = true
        }
        rule.runOnIdle {
            assertEquals(IntSize(80, 200), size1)
            assertEquals(IntSize(120, 200), size2)
        }
    }

    private fun Modifier.animateSize(scope: LookaheadLayoutScope): Modifier = composed {
        val cScope = rememberCoroutineScope()
        var anim: Animatable<IntSize, AnimationVector2D>? by remember { mutableStateOf(null) }
        with(scope) {
            this@composed.intermediateLayout(
                measure = { measurable, _, size ->
                    anim = anim?.apply {
                        cScope.launch { animateTo(size, tween(200)) }
                    } ?: Animatable(size, IntSize.VectorConverter)
                    val (width, height) = anim!!.value
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
            )
        }
    }

    @Test
    fun nestedLookaheadLayoutTest() {
        var parentLookaheadMeasure = 0
        var childLookaheadMeasure = 0
        var parentLookaheadPlace = 0
        var childLookaheadPlace = 0
        var parentMeasure = 0
        var childMeasure = 0
        var parentPlace = 0
        var childPlace = 0

        var rootPreMeasure = 0
        var rootPrePlace = 0
        var rootPostMeasure = 0
        var rootPostPlace = 0

        var counter = 0

        rule.setContent {
            // The right sequence for this nested lookahead layout setup:
            // parentLookaheadMeasure -> childLookaheadMeasure -> parentMeasure -> childMeasure
            // -> parentLookaheadPlace -> childLookaheadPlace -> -> parentPlace -> childPlace
            // Each event should happen exactly once in the end.
            Box(Modifier.layout(
                measureWithLambdas(
                    preMeasure = { rootPreMeasure = ++counter },
                    postMeasure = { rootPostMeasure = ++counter },
                    prePlacement = { rootPrePlace = ++counter },
                    postPlacement = { rootPostPlace = ++counter }
                )
            )) {
                MyLookaheadLayout {
                    Box(
                        Modifier
                            .padding(top = 100.dp)
                            .fillMaxSize()
                            .intermediateLayout(
                                measure = { measurable, constraints, _ ->
                                    measureWithLambdas(
                                        preMeasure = { parentMeasure = ++counter },
                                        prePlacement = { parentPlace = ++counter }
                                    ).invoke(this, measurable, constraints)
                                }
                            )
                            .layout(
                                measureWithLambdas(
                                    preMeasure = {
                                        if (parentLookaheadMeasure == 0) {
                                            // Only the first invocation is for lookahead
                                            parentLookaheadMeasure = ++counter
                                        }
                                    },
                                    prePlacement = {
                                        if (parentLookaheadPlace == 0) {
                                            // Only the first invocation is for lookahead
                                            parentLookaheadPlace = ++counter
                                        }
                                    }
                                )
                            )
                    ) {
                        MyLookaheadLayout {
                            Column {
                                Box(
                                    Modifier
                                        .size(100.dp)
                                        .background(Color.Red)
                                        .intermediateLayout { measurable, constraints, _ ->
                                            measureWithLambdas(
                                                preMeasure = { childMeasure = ++counter },
                                                prePlacement = { childPlace = ++counter }
                                            ).invoke(this, measurable, constraints)
                                        }
                                        .layout(
                                            measure = measureWithLambdas(
                                                preMeasure = {
                                                    if (childLookaheadMeasure == 0) {
                                                        childLookaheadMeasure = ++counter
                                                    }
                                                },
                                                prePlacement = {
                                                    if (childLookaheadPlace == 0) {
                                                        childLookaheadPlace = ++counter
                                                    }
                                                }
                                            )
                                        )
                                )
                                Box(
                                    Modifier
                                        .size(100.dp)
                                        .background(Color.Green)
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(1, rootPreMeasure)
            assertEquals(2, parentLookaheadMeasure)
            assertEquals(3, childLookaheadMeasure)
            assertEquals(4, parentMeasure)
            assertEquals(5, childMeasure)
            assertEquals(6, rootPostMeasure)

            // Measure finished. Then placement.
            assertEquals(7, rootPrePlace)
            assertEquals(8, parentLookaheadPlace)
            assertEquals(9, childLookaheadPlace)
            assertEquals(10, parentPlace)
            assertEquals(11, childPlace)
            assertEquals(12, rootPostPlace)
        }
    }

    @Test
    fun parentObserveActualMeasurementTest() {
        val width = 200
        val height = 120
        var scaleFactor by mutableStateOf(0.1f)
        var parentSize = IntSize.Zero
        var grandParentSize = IntSize.Zero
        var greatGrandParentSize = IntSize.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Column(
                    Modifier.layout(measureWithLambdas(postMeasure = { greatGrandParentSize = it }))
                ) {
                    Row(
                        Modifier.layout(measureWithLambdas(postMeasure = { grandParentSize = it }))
                    ) {
                        Box(
                            Modifier.layout(measureWithLambdas(postMeasure = { parentSize = it }))
                        ) {
                            MyLookaheadLayout {
                                Box(modifier = Modifier
                                    .intermediateLayout { measurable, constraints, lookaheadSize ->
                                        assertEquals(width, lookaheadSize.width)
                                        assertEquals(height, lookaheadSize.height)
                                        val placeable = measurable.measure(constraints)
                                        layout(
                                            (scaleFactor * width).roundToInt(),
                                            (scaleFactor * height).roundToInt()
                                        ) {
                                            placeable.place(0, 0)
                                        }
                                    }
                                    .size(width.dp, height.dp))
                            }
                        }
                        Spacer(modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.size(50.dp))
                }
            }
        }

        val size = IntSize(width, height)
        repeat(20) {
            rule.runOnIdle {
                assertEquals(size * scaleFactor, parentSize)
                assertEquals((size * scaleFactor).width + 20, grandParentSize.width)
                assertEquals(max((size * scaleFactor).height, 20), grandParentSize.height)
                assertEquals(max(grandParentSize.width, 50), greatGrandParentSize.width)
                assertEquals(grandParentSize.height + 50, greatGrandParentSize.height)
                scaleFactor += 0.1f
            }
        }
    }

    private operator fun IntSize.times(multiplier: Float): IntSize =
        IntSize((width * multiplier).roundToInt(), (height * multiplier).roundToInt())

    @Test
    fun noExtraLookaheadTest() {
        var parentMeasure = 0
        var parentPlace = 0
        var measurePlusLookahead = 0
        var placePlusLookahead = 0
        var measure = 0
        var place = 0

        var isSmall by mutableStateOf(true)
        var controlGroupEnabled by mutableStateOf(true)

        var controlGroupParentMeasure = 0
        var controlGroupParentPlace = 0
        var controlGroupMeasure = 0
        var controlGroupPlace = 0

        rule.setContent {
            if (controlGroupEnabled) {
                Box(
                    Modifier.layout(
                        measureWithLambdas(
                            postMeasure = { controlGroupParentMeasure++ },
                            postPlacement = { controlGroupParentPlace++ }
                        )
                    )
                ) {
                    Layout(measurePolicy = defaultMeasurePolicy, content = {
                        Box(
                            Modifier
                                .size(if (isSmall) 100.dp else 200.dp)
                                .layout(
                                    measureWithLambdas(
                                        postMeasure = { controlGroupMeasure++ },
                                        postPlacement = { controlGroupPlace++ },
                                    )
                                )
                        )
                    })
                }
            } else {
                Box(
                    Modifier.layout(
                        measureWithLambdas(
                            postMeasure = { parentMeasure++ },
                            postPlacement = { parentPlace++ }
                        )
                    )
                ) {
                    MyLookaheadLayout {
                        Box(
                            Modifier
                                .size(if (isSmall) 100.dp else 200.dp)
                                .animateSize(this)
                                .layout(
                                    measureWithLambdas(
                                        postMeasure = { measurePlusLookahead++ },
                                        postPlacement = { placePlusLookahead++ },
                                    )
                                )
                                .intermediateLayout { measurable, constraints, _ ->
                                    measureWithLambdas(
                                        postMeasure = { measure++ },
                                        postPlacement = { place++ }
                                    ).invoke(this, measurable, constraints)
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(1, controlGroupParentMeasure)
            assertEquals(1, controlGroupParentPlace)
            assertEquals(1, controlGroupMeasure)
            assertEquals(1, controlGroupPlace)
            isSmall = !isSmall
        }

        rule.runOnIdle {
            // Check the starting condition before switching over from control group
            assertEquals(0, parentMeasure)
            assertEquals(0, parentPlace)
            assertEquals(0, measurePlusLookahead)
            assertEquals(0, placePlusLookahead)
            assertEquals(0, measure)
            assertEquals(0, place)

            // Switch to LookaheadLayout
            controlGroupEnabled = !controlGroupEnabled
        }

        rule.runOnIdle {
            // Expects 1
            assertEquals(1, parentMeasure)
            assertEquals(1, parentPlace)
            val lookaheadMeasure = measurePlusLookahead - measure
            val lookaheadPlace = placePlusLookahead - place
            assertEquals(1, lookaheadMeasure)
            assertEquals(1, lookaheadPlace)
        }

        // Pump frames so that animation triggered measurements are not completely dependent on
        // system timing.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            isSmall = !isSmall
        }
        repeat(10) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        rule.mainClock.autoAdvance = true
        rule.runOnIdle {
            // Compare number of lookahead measurements & placements with control group.
            assertEquals(controlGroupParentMeasure, parentMeasure)
            assertEquals(controlGroupParentPlace, parentPlace)
            val lookaheadMeasure = measurePlusLookahead - measure
            val lookaheadPlace = placePlusLookahead - place
            assertEquals(controlGroupMeasure, lookaheadMeasure)
            assertEquals(controlGroupPlace, lookaheadPlace)
            assertTrue(lookaheadMeasure < measure)
            assertTrue(lookaheadPlace < place)
        }
    }

    @Test
    fun lookaheadStaysTheSameDuringAnimationTest() {
        var isLarge by mutableStateOf(true)
        var parentLookaheadSize = IntSize.Zero
        var child1LookaheadSize = IntSize.Zero
        var child2LookaheadSize = IntSize.Zero
        rule.setContent {
            LookaheadLayout(
                measurePolicy = { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    val maxWidth: Int = placeables.maxOf { it.width }
                    val maxHeight = placeables.maxOf { it.height }
                    // Position the children.
                    layout(maxWidth, maxHeight) {
                        placeables.forEach {
                            it.place(0, 0)
                        }
                    }
                },
                content = {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Row(
                            (if (isLarge) Modifier.size(200.dp) else Modifier.size(50.dp, 100.dp))
                                .intermediateLayout { measurable, constraints, lookaheadSize ->
                                    parentLookaheadSize = lookaheadSize
                                    measureWithLambdas().invoke(this, measurable, constraints)
                                }
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(2f)
                                    .intermediateLayout { measurable, constraints, lookaheadSize ->
                                        child1LookaheadSize = lookaheadSize
                                        measureWithLambdas().invoke(this, measurable, constraints)
                                    }
                                    .animateSize(this@LookaheadLayout)
                            )
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(3f)
                                    .intermediateLayout { measurable, constraints, lookaheadSize ->
                                        child2LookaheadSize = lookaheadSize
                                        measureWithLambdas().invoke(this, measurable, constraints)
                                    }
                                    .animateSize(this@LookaheadLayout)
                            )
                        }
                    }
                }
            )
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(IntSize(200, 200), parentLookaheadSize)
            assertEquals(IntSize(80, 200), child1LookaheadSize)
            assertEquals(IntSize(120, 200), child2LookaheadSize)
            rule.mainClock.autoAdvance = false
            isLarge = false
        }

        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        repeat(10) {
            rule.runOnIdle {
                assertEquals(IntSize(50, 100), parentLookaheadSize)
                assertEquals(IntSize(20, 100), child1LookaheadSize)
                assertEquals(IntSize(30, 100), child2LookaheadSize)
            }
            rule.mainClock.advanceTimeByFrame()
        }
        rule.runOnIdle {
            isLarge = true
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        repeat(10) {
            rule.runOnIdle {
                assertEquals(IntSize(200, 200), parentLookaheadSize)
                assertEquals(IntSize(80, 200), child1LookaheadSize)
                assertEquals(IntSize(120, 200), child2LookaheadSize)
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun skipPlacementOnlyPostLookahead() {
        var child1TotalPlacement = 0
        var child1Placement = 0
        var child2TotalPlacement = 0
        var child2Placement = 0

        rule.setContent {
            MyLookaheadLayout {
                Row(Modifier.widthIn(100.dp, 200.dp)) {
                    Box(
                        modifier = Modifier
                            .intermediateLayout { measurable, constraints, _ ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    // skip placement in the post-lookahead placement pass
                                }
                            }
                            .weight(1f)
                            .layout { measurable, constraints ->
                                measureWithLambdas(
                                    prePlacement = { child1TotalPlacement++ }
                                ).invoke(this, measurable, constraints)
                            }
                            .intermediateLayout { measurable, constraints, _ ->
                                measureWithLambdas(prePlacement = { child1Placement++ })
                                    .invoke(this, measurable, constraints)
                            }
                    )
                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                measureWithLambdas(
                                    prePlacement = { child2TotalPlacement++ }
                                ).invoke(this, measurable, constraints)
                            }
                            .intermediateLayout { measurable, constraints, _ ->
                                measureWithLambdas(prePlacement = { child2Placement++ })
                                    .invoke(this, measurable, constraints)
                            }
                            .weight(3f)
                    )
                    Box(modifier = Modifier.sizeIn(50.dp))
                }
            }
        }

        rule.runOnIdle {
            // Child1 skips post-lookahead placement
            assertEquals(0, child1Placement)
            // Child2 is placed in post-lookahead placement
            assertEquals(1, child2Placement)
            val child1LookaheadPlacement = child1TotalPlacement - child1Placement
            val child2LookaheadPlacement = child2TotalPlacement - child2Placement
            // Both child1 & child2 should be placed in lookahead, since the skipping only
            // applies to regular placement pass, as per API contract in `intermediateLayout`
            assertEquals(1, child1LookaheadPlacement)
            assertEquals(1, child2LookaheadPlacement)
        }
    }

    @Composable
    private fun MyLookaheadLayout(
        postMeasure: () -> Unit = {},
        postPlacement: () -> Unit = {},
        content: @Composable LookaheadLayoutScope.() -> Unit
    ) {
        LookaheadLayout(
            measurePolicy = { measurables, constraints ->
                val placeables = measurables.map { it.measure(constraints) }
                val maxWidth: Int = placeables.maxOf { it.width }
                val maxHeight = placeables.maxOf { it.height }
                postMeasure()
                // Position the children.
                layout(maxWidth, maxHeight) {
                    placeables.forEach {
                        it.place(0, 0)
                    }
                    postPlacement()
                }
            },
            content = { content() }
        )
    }

    @Test
    fun alterPlacementTest() {
        var placementCount = 0
        var totalPlacementCount = 0
        var shouldPlace by mutableStateOf(false)
        rule.setContent {
            MyLookaheadLayout {
                Layout(
                    content = {
                        Box(Modifier
                            .intermediateLayout { measurable, constraints, _ ->
                                measureWithLambdas(prePlacement = {
                                    placementCount++
                                }).invoke(this, measurable, constraints)
                            }
                            .layout { measurable, constraints ->
                                measureWithLambdas(prePlacement = {
                                    totalPlacementCount++
                                }).invoke(this, measurable, constraints)
                            })
                    }
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    val maxWidth: Int = placeables.maxOf { it.width }
                    val maxHeight = placeables.maxOf { it.height }
                    // Position the children.
                    layout(maxWidth, maxHeight) {
                        if (shouldPlace) {
                            placeables.forEach {
                                it.place(0, 0)
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(0, totalPlacementCount)
            assertEquals(0, placementCount)
            shouldPlace = true
        }
        rule.runOnIdle {
            val lookaheadPlacementCount = totalPlacementCount - placementCount
            assertEquals(1, lookaheadPlacementCount)
            assertEquals(1, placementCount)
        }
    }

    @Test
    fun localLookaheadPositionOfFromDisjointedLookaheadLayoutsTest() {
        var firstCoordinates: LookaheadLayoutCoordinates? = null
        var secondCoordinates: LookaheadLayoutCoordinates? = null
        rule.setContent {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MyLookaheadLayout {
                    Box(
                        Modifier
                            .size(200.dp)
                            .onPlaced { _, it -> firstCoordinates = it })
                }
                Box(
                    Modifier
                        .padding(top = 30.dp, start = 70.dp)
                        .offset(40.dp, 60.dp)
                ) {
                    MyLookaheadLayout {
                        Box(
                            Modifier
                                .size(100.dp, 50.dp)
                                .onPlaced { _, it -> secondCoordinates = it })
                    }
                }
            }
        }
        rule.runOnIdle {
            val offset = secondCoordinates!!.localPositionOf(firstCoordinates!!, Offset.Zero)
            val lookaheadOffset = secondCoordinates!!.localLookaheadPositionOf(firstCoordinates!!)
            assertEquals(offset, lookaheadOffset)
        }
    }

    @Test
    fun localLookaheadPositionOfFromNestedLookaheadLayoutsTest() {
        var firstCoordinates: LookaheadLayoutCoordinates? = null
        var secondCoordinates: LookaheadLayoutCoordinates? = null
        rule.setContent {
            MyLookaheadLayout {
                Row(
                    Modifier
                        .fillMaxSize()
                        .onPlaced { _, it -> firstCoordinates = it },
                    horizontalArrangement = SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(200.dp))
                    Box(
                        Modifier
                            .padding(top = 30.dp, start = 70.dp)
                            .offset(40.dp, 60.dp)
                    ) {
                        MyLookaheadLayout {
                            Box(
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .onPlaced { _, it -> secondCoordinates = it })
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            val offset = secondCoordinates!!.localPositionOf(firstCoordinates!!, Offset.Zero)
            val lookaheadOffset = secondCoordinates!!.localLookaheadPositionOf(firstCoordinates!!)
            assertEquals(offset, lookaheadOffset)
        }
    }

    @Test
    fun lookaheadMaxHeightIntrinsicsTest() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Box(modifier) {
                Row(modifier.height(IntrinsicSize.Max)) {
                    Box(
                        modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .aspectRatio(2f)
                            .background(Color.Gray)
                    )
                    Box(
                        modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .width(1.dp)
                            .background(Color.Black)
                    )
                    Box(
                        modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(Color.Blue)
                    )
                }
            }
        }
    }

    @Test
    fun lookaheadMinHeightIntrinsicsTest() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Box {
                Row(modifier.height(IntrinsicSize.Min)) {
                    Text(
                        text = "This is a really short text",
                        modifier = modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.Black)
                    )
                    Text(
                        text = "This is a much much much much much much much much much much" +
                            " much much much much much much longer text",
                        modifier = modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Test
    fun lookaheadMinWidthIntrinsicsTest() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Column(
                modifier
                    .width(IntrinsicSize.Min)
                    .wrapContentHeight()
            ) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .size(20.dp, 10.dp)
                        .background(Color.Gray)
                )
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .size(30.dp, 10.dp)
                        .background(Color.Blue)
                )
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .size(10.dp, 10.dp)
                        .background(Color.Magenta)
                )
            }
        }
    }

    @Test
    fun lookaheadMaxWidthIntrinsicsTest() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Box {
                Column(
                    modifier
                        .width(IntrinsicSize.Max)
                        .wrapContentHeight()
                ) {
                    Box(
                        modifier
                            .fillMaxWidth()
                            .background(Color.Gray)
                    ) {
                        Text("Short text")
                    }
                    Box(
                        modifier
                            .fillMaxWidth()
                            .background(Color.Blue)
                    ) {
                        Text("Extremely long text giving the width of its siblings")
                    }
                    Box(
                        modifier
                            .fillMaxWidth()
                            .background(Color.Magenta)
                    ) {
                        Text("Medium length text")
                    }
                }
            }
        }
    }

    @Test
    fun firstBaselineAlignmentInLookaheadLayout() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Box(modifier.fillMaxWidth()) {
                Row {
                    Text("Short", modifier.alignByBaseline())
                    Text("3\nline\n\text", modifier.alignByBaseline())
                    Text(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do" +
                            " eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim" +
                            " ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut" +
                            " aliquip ex ea commodo consequat. Duis aute irure dolor in" +
                            " reprehenderit in voluptate velit esse cillum dolore eu fugiat" +
                            " nulla pariatur. Excepteur sint occaecat cupidatat non proident," +
                            " sunt in culpa qui officia deserunt mollit anim id est laborum.",
                        modifier.alignByBaseline()
                    )
                }
            }
        }
    }

    @Test
    fun grandparentQueryBaseline() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Layout(modifier = modifier, content = {
                Row(
                    modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color(0xffb4c8ea)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "First",
                        fontSize = 80.sp,
                        color = Color.White,
                        modifier = modifier
                            .alignByBaseline()
                            .background(color = Color(0xfff3722c), RoundedCornerShape(10))
                    )
                    Spacer(modifier.size(10.dp))
                    Text(
                        text = "Second",
                        color = Color.White,
                        fontSize = 30.sp,
                        modifier = modifier
                            .alignByBaseline()
                            .background(color = Color(0xff90be6d), RoundedCornerShape(10))
                    )
                    Spacer(modifier.size(10.dp))
                    Text(
                        text = "Text",
                        fontSize = 50.sp,
                        color = Color.White,
                        modifier = modifier
                            .alignByBaseline()
                            .background(color = Color(0xffffb900), RoundedCornerShape(10))
                    )
                }
                Spacer(
                    modifier
                        .fillMaxWidth()
                        .requiredHeight(1.dp)
                        .background(Color.Black)
                )
            }) { measurables, constraints ->
                val placeables = measurables.map {
                    it.measure(constraints)
                }
                val row = placeables.first()
                val position = row[FirstBaseline]
                layout(row.width, row.height) {
                    row.place(0, 0)
                    placeables[1].place(0, position)
                }
            }
        }
    }

    private fun assertSameLayoutWithAndWithoutLookahead(
        content: @Composable (
            modifier: Modifier
        ) -> Unit
    ) {
        val controlGroupSizes = mutableVectorOf<IntSize>()
        val controlGroupPositions = mutableVectorOf<Offset>()
        val sizes = mutableVectorOf<IntSize>()
        val positions = mutableVectorOf<Offset>()
        var enableControlGroup by mutableStateOf(true)
        rule.setContent {
            if (enableControlGroup) {
                Layout(measurePolicy = defaultMeasurePolicy, content = {
                    content(
                        modifier = Modifier.trackSizeAndPosition(
                            controlGroupSizes,
                            controlGroupPositions,
                        )
                    )
                })
            } else {
                MyLookaheadLayout {
                    content(
                        modifier = Modifier
                            .trackSizeAndPosition(sizes, positions)
                            .assertSameSizeAndPosition(this)
                    )
                }
            }
        }
        rule.runOnIdle {
            enableControlGroup = !enableControlGroup
        }
        rule.runOnIdle {
            if (Debug) {
                controlGroupPositions.debugPrint("Lookahead")
                controlGroupSizes.debugPrint("Lookahead")
                positions.debugPrint("Lookahead")
                sizes.debugPrint("Lookahead")
            }
            assertEquals(controlGroupPositions.size, positions.size)
            controlGroupPositions.forEachIndexed { i, position ->
                assertEquals(position, positions[i])
            }
            assertEquals(controlGroupSizes.size, sizes.size)
            controlGroupSizes.forEachIndexed { i, size ->
                assertEquals(size, sizes[i])
            }
        }
    }

    private fun Modifier.assertSameSizeAndPosition(scope: LookaheadLayoutScope) = composed {
        var lookaheadSize by remember {
            mutableStateOf(IntSize.Zero)
        }
        var lookaheadLayoutCoordinates: LookaheadLayoutCoordinates? by remember {
            mutableStateOf(
                null
            )
        }
        var onPlacedCoordinates: LookaheadLayoutCoordinates? by remember { mutableStateOf(null) }
        with(scope) {
            this@composed
                .intermediateLayout { measurable, constraints, targetSize ->
                    lookaheadSize = targetSize
                    measureWithLambdas().invoke(this, measurable, constraints)
                }
                .onPlaced { scopeRoot, it ->
                    lookaheadLayoutCoordinates = scopeRoot
                    onPlacedCoordinates = it
                }
                .onGloballyPositioned {
                    assertEquals(lookaheadSize, it.size)
                    assertEquals(
                        lookaheadLayoutCoordinates!!.localLookaheadPositionOf(
                            onPlacedCoordinates!!
                        ),
                        lookaheadLayoutCoordinates!!.localPositionOf(
                            onPlacedCoordinates!!,
                            Offset.Zero
                        )
                    )
                }
        }
    }

    private fun Modifier.trackSizeAndPosition(
        sizes: MutableVector<IntSize>,
        positions: MutableVector<Offset>
    ) = this
        .onGloballyPositioned {
            positions.add(it.positionInRoot())
            sizes.add(it.size)
        }

    private fun <T> MutableVector<T>.debugPrint(tag: String) {
        print("$tag: [")
        forEach { print("$it, ") }
        print("]")
        println()
    }

    private val defaultMeasurePolicy: MeasurePolicy = MeasurePolicy { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val maxWidth: Int = placeables.maxOf { it.width }
        val maxHeight = placeables.maxOf { it.height }
        // Position the children.
        layout(maxWidth, maxHeight) {
            placeables.forEach {
                it.place(0, 0)
            }
        }
    }

    private fun measureWithLambdas(
        preMeasure: () -> Unit = {},
        postMeasure: (IntSize) -> Unit = {},
        prePlacement: () -> Unit = {},
        postPlacement: () -> Unit = {}
    ): MeasureScope.(Measurable, Constraints) -> MeasureResult = { measurable, constraints ->
        preMeasure()
        val placeable = measurable.measure(constraints)
        postMeasure(IntSize(placeable.width, placeable.height))
        layout(placeable.width, placeable.height) {
            prePlacement()
            placeable.place(0, 0)
            postPlacement()
        }
    }
}