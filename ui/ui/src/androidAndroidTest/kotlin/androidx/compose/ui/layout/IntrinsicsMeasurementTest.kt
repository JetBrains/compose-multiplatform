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

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class IntrinsicsMeasurementTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * When intrinsics are used for child content measurement and the content changes,
     * then the intrinsics measurement should be triggered again.
     */
    @Test
    fun intrinsicsChangeCausesParentRemeasure() {
        lateinit var coords: LayoutCoordinates
        var showContent by mutableStateOf(true)

        rule.setContent {
            val content: @Composable () -> Unit = {
                Column {
                    with(LocalDensity.current) {
                        Box(Modifier.requiredSize(10.toDp()))
                        if (showContent) {
                            Box(Modifier.requiredSize(10.toDp()))
                        }
                    }
                }
            }
            Layout(
                content = content,
                modifier = Modifier.onPlaced { coords = it }
            ) { measurables, _ ->
                val intrinsicHeight = measurables[0].minIntrinsicHeight(Constraints.Infinity)
                val placeable = measurables[0].measure(
                    Constraints.fixedHeight((intrinsicHeight * 1.2).roundToInt())
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
            showContent = false
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(12)
            showContent = true
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
        }
    }

    /**
     * When intrinsics are used in the measure block, and measurement is in the layout block,
     * and the content changes, then the intrinsics measurement should be triggered again.
     */
    @Test
    fun intrinsicsChangeCausesParentRemeasureWhenMeasuredInPlacement() {
        lateinit var coords: LayoutCoordinates
        var showContent by mutableStateOf(true)

        rule.setContent {
            val content: @Composable () -> Unit = {
                Column {
                    with(LocalDensity.current) {
                        Box(Modifier.requiredSize(10.toDp()))
                        if (showContent) {
                            Box(Modifier.requiredSize(10.toDp()))
                        }
                    }
                }
            }
            Layout(
                content = content,
                modifier = Modifier.onPlaced { coords = it }
            ) { measurables, constraints ->
                val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 100
                val height = (measurables[0].minIntrinsicHeight(width) * 1.2).roundToInt()
                layout(width, height) {
                    val placeable = measurables[0].measure(
                        Constraints.fixed(width, height)
                    )
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
            showContent = false
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(12)
            showContent = true
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
        }
    }

    /**
     * When intrinsics are used for child content measurement inside the layout block
     * and the content changes, then the intrinsics measurement should be triggered again.
     */
    @Test
    fun intrinsicsUseInPlacementOnlyRelayout() {
        lateinit var coords: LayoutCoordinates
        var showContent by mutableStateOf(true)
        var measureCount = 0
        var placeCount = 0

        rule.setContent {
            val content: @Composable () -> Unit = {
                Column(Modifier.onPlaced { coords = it }) {
                    with(LocalDensity.current) {
                        Box(Modifier.requiredSize(10.toDp()))
                        if (showContent) {
                            Box(Modifier.requiredSize(10.toDp()))
                        }
                    }
                }
            }
            Layout(
                content = content
            ) { measurables, constraints ->
                val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 100
                val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 100
                measureCount++
                layout(width, height) {
                    placeCount++
                    val adjustedHeight =
                        (measurables[0].minIntrinsicHeight(width) * 1.2).roundToInt()
                    val placeable = measurables[0].measure(
                        Constraints.fixed(width, adjustedHeight)
                    )
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
            measureCount = 0
            placeCount = 0
            showContent = false
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(0)
            assertThat(placeCount).isEqualTo(1)
            assertThat(coords.size.height).isEqualTo(12)
            placeCount = 0
            showContent = true
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
            assertThat(measureCount).isEqualTo(0)
            assertThat(placeCount).isEqualTo(1)
        }
    }

    /**
     * When intrinsics are used for child content measurement and the content changes,
     * then the intrinsics measurement should be triggered again. This should work when the
     * content is not a direct child of the intrinsic measurement.
     */
    @Test
    fun grandchildChangeCausesRemeasure() {
        lateinit var coords: LayoutCoordinates
        var showContent by mutableStateOf(true)

        rule.setContent {
            val content: @Composable () -> Unit = {
                Box {
                    Column {
                        with(LocalDensity.current) {
                            Box(Modifier.requiredSize(10.toDp()))
                            if (showContent) {
                                Box(Modifier.requiredSize(10.toDp()))
                            }
                        }
                    }
                }
            }
            Layout(
                content = content,
                modifier = Modifier.onPlaced { coords = it }
            ) { measurables, _ ->
                val intrinsicHeight = measurables[0].minIntrinsicHeight(Constraints.Infinity)
                val placeable = measurables[0].measure(
                    Constraints.fixedHeight((intrinsicHeight * 1.2).roundToInt())
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
            showContent = false
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(12)
            showContent = true
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
        }
    }

    /**
     * When the intrinsics usage changes, the content should only be measured properly.
     * In this case, the intrinsics is used in the measure block and then not used, so the
     * content changing shouldn't cause measurement.
     */
    @Test
    fun intrinsicsUsageChanges() {
        lateinit var coords: LayoutCoordinates
        var showContent by mutableStateOf(true)
        var useIntrinsics by mutableStateOf(true)
        var measureCount = 0
        var placeCount = 0

        val sometimesIntrinsicsMeasurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                if (useIntrinsics) {
                    val placeable = measurables[0].measure(constraints)
                    return layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                return layout(100, 100) {
                    val placeable = measurables[0].measure(constraints)
                    placeable.place(0, 0)
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ): Int {
                if (useIntrinsics) {
                    return measurables[0].minIntrinsicHeight(width)
                }
                return 100
            }
        }

        rule.setContent {
            val content1: @Composable () -> Unit = {
                Column {
                    with(LocalDensity.current) {
                        Box(Modifier.requiredSize(10.toDp()))
                        if (showContent) {
                            Box(Modifier.requiredSize(10.toDp()))
                        }
                    }
                }
            }
            val content2: @Composable () -> Unit = {
                Box {
                    Layout(content1, Modifier, sometimesIntrinsicsMeasurePolicy)
                }
            }
            Layout(
                content = content2,
                modifier = Modifier.onPlaced { coords = it }
            ) { measurables, _ ->
                measureCount++
                val intrinsicHeight = measurables[0].minIntrinsicHeight(Constraints.Infinity)
                val placeable = measurables[0].measure(
                    Constraints.fixedHeight((intrinsicHeight * 1.2).roundToInt())
                )
                layout(placeable.width, placeable.height) {
                    placeCount++
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            measureCount = 0
            placeCount = 0
            showContent = false
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(placeCount).isEqualTo(1)
            measureCount = 0
            placeCount = 0
            showContent = true
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(placeCount).isEqualTo(1)
            assertThat(coords.size.height).isEqualTo(24)
            useIntrinsics = false
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(120)
            measureCount = 0
            placeCount = 0
            showContent = false
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(0)
            assertThat(placeCount).isEqualTo(0)
            assertThat(coords.size.height).isEqualTo(120)
        }
    }

    /**
     * When the intrinsics usage changes, the content should only be measured properly.
     * In this case, the measure block and intrinsics change from using the child's size
     * to not using the child's size for its own size.
     */
    @Test
    fun intrinsicsUsageChangesInPlacement() {
        var showContent by mutableStateOf(true)
        var useIntrinsics by mutableStateOf(true)
        var measureCount = 0
        var placeCount = 0

        val sometimesIntrinsicsMeasurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                if (useIntrinsics) {
                    val placeable = measurables[0].measure(constraints)
                    return layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                return layout(100, 100) {
                    val placeable = measurables[0].measure(constraints)
                    placeable.place(0, 0)
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ): Int {
                if (useIntrinsics) {
                    return measurables[0].minIntrinsicHeight(width)
                }
                return 100
            }
        }

        rule.setContent {
            val content1: @Composable () -> Unit = {
                Box {
                    Column {
                        with(LocalDensity.current) {
                            Box(Modifier.requiredSize(10.toDp()))
                            if (showContent) {
                                Box(Modifier.requiredSize(10.toDp()))
                            }
                        }
                    }
                }
            }
            val content2: @Composable () -> Unit = {
                Layout(content1, Modifier, sometimesIntrinsicsMeasurePolicy)
            }
            Layout(
                content = content2
            ) { measurables, _ ->
                measureCount++
                layout(100, 100) {
                    placeCount++
                    val intrinsicHeight = measurables[0].minIntrinsicHeight(Constraints.Infinity)
                    val placeable = measurables[0].measure(
                        Constraints.fixedHeight((intrinsicHeight * 1.2).roundToInt())
                    )
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            measureCount = 0
            placeCount = 0
            showContent = false
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(0)
            assertThat(placeCount).isEqualTo(1)
            showContent = true
        }

        rule.runOnIdle {
            measureCount = 0
            placeCount = 0
            useIntrinsics = false
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(0)
            assertThat(placeCount).isEqualTo(1)
            measureCount = 0
            placeCount = 0
            showContent = false
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(0)
            assertThat(placeCount).isEqualTo(0)
        }
    }

    /**
     * The parent starts off using intrinsics in the measure block and then switches to
     * using it in the layout block.
     */
    @Test
    fun changeIntrinsicsUsageFromMeasureToLayout() {
        lateinit var coords: LayoutCoordinates
        var showContent by mutableStateOf(true)
        var intrinsicsInMeasure by mutableStateOf(true)
        var measureCount = 0
        var placeCount = 0

        rule.setContent {
            val content: @Composable () -> Unit = {
                Box(Modifier.onPlaced { coords = it }) {
                    Column {
                        with(LocalDensity.current) {
                            Box(Modifier.requiredSize(10.toDp()))
                            if (showContent) {
                                Box(Modifier.requiredSize(10.toDp()))
                            }
                        }
                    }
                }
            }
            Layout(
                content = content,
            ) { measurables, _ ->
                if (intrinsicsInMeasure) {
                    val intrinsicHeight = measurables[0].minIntrinsicHeight(Constraints.Infinity)
                    val placeable = measurables[0].measure(
                        Constraints.fixedHeight((intrinsicHeight * 1.2).roundToInt())
                    )
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                } else {
                    measureCount++
                    layout(100, 100) {
                        placeCount++
                        val intrinsicHeight =
                            measurables[0].minIntrinsicHeight(Constraints.Infinity)
                        val placeable = measurables[0].measure(
                            Constraints.fixedHeight((intrinsicHeight * 1.2).roundToInt())
                        )
                        placeable.place(0, 0)
                    }
                }
            }
        }

        rule.runOnIdle {
            intrinsicsInMeasure = false
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
            measureCount = 0
            placeCount = 0
            showContent = false
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(12)
            assertThat(measureCount).isEqualTo(0)
            assertThat(placeCount).isEqualTo(1)

            placeCount = 0
            showContent = true
        }

        rule.runOnIdle {
            assertThat(coords.size.height).isEqualTo(24)
            assertThat(measureCount).isEqualTo(0)
            assertThat(placeCount).isEqualTo(1)
        }
    }
}
