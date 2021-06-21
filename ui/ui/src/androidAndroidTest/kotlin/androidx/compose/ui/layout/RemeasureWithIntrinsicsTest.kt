/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RemeasureWithIntrinsicsTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun remeasuringChildWhenParentUsedIntrinsicSizes() {
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)

        rule.setContent {
            LayoutUsingIntrinsics {
                LayoutWithIntrinsics(
                    intrinsicWidth,
                    intrinsicHeight,
                    Modifier.testTag("child")
                )
            }
        }

        rule.onNodeWithTag("child")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(50.dp)

        rule.runOnIdle {
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.onNodeWithTag("child")
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun updatingChildIntrinsicsViaModifierWhenParentUsedIntrinsicSizes() {
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)

        rule.setContent {
            LayoutUsingIntrinsics {
                Box(
                    Modifier
                        .testTag("child")
                        .withIntrinsics(intrinsicWidth, intrinsicHeight)
                )
            }
        }

        rule.onNodeWithTag("child")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(50.dp)

        rule.runOnIdle {
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.onNodeWithTag("child")
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun remeasuringGrandChildWhenGrandParentUsedIntrinsicSizes() {
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)

        rule.setContent {
            LayoutUsingIntrinsics {
                Box(propagateMinConstraints = true) {
                    LayoutWithIntrinsics(
                        intrinsicWidth,
                        intrinsicHeight,
                        Modifier.testTag("child")
                    )
                }
            }
        }

        rule.onNodeWithTag("child")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(50.dp)

        rule.runOnIdle {
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.onNodeWithTag("child")
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun updatingGrandChildIntrinsicsViaModifierWhenGrandParentUsedIntrinsicSizes() {
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)

        rule.setContent {
            LayoutUsingIntrinsics {
                Box(propagateMinConstraints = true) {
                    Box(
                        Modifier
                            .testTag("child")
                            .withIntrinsics(intrinsicWidth, intrinsicHeight)
                    )
                }
            }
        }

        rule.onNodeWithTag("child")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(50.dp)

        rule.runOnIdle {
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.onNodeWithTag("child")
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun nodeDoesNotCauseRemeasureOfAncestor_whenItsIntrinsicsAreUnused() {
        var measures = 0
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)
        rule.setContent {
            LayoutUsingIntrinsics(
                onMeasure = { ++measures },
                modifier = Modifier.testTag("parent")
            ) {
                LayoutWithIntrinsics(20.dp, 20.dp) {
                    LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                }
            }
        }

        rule.runOnIdle {
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.runOnIdle {
            assertEquals(1, measures)
        }
        rule.onNodeWithTag("parent")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun causesRemeasureOfAllDependantAncestors() {
        var measures1 = 0
        var measures2 = 0
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)
        rule.setContent {
            LayoutUsingIntrinsics(
                modifier = Modifier.testTag("parent1"),
                onMeasure = { ++measures1 }
            ) {
                Box {
                    LayoutUsingIntrinsics(
                        modifier = Modifier.testTag("parent2"),
                        onMeasure = { ++measures2 }
                    ) {
                        Box {
                            LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.runOnIdle {
            assertEquals(2, measures1)
            assertEquals(2, measures2)
        }
    }

    @Test
    fun whenConnectionFromOwnerDoesNotQueryAnymore() {
        var measures = 0
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)
        var connectionModifier by mutableStateOf(Modifier as Modifier)

        val parentLayoutPolicy = MeasurePolicy { measurables, constraints ->
            ++measures
            val measurable = measurables.first()
            // Query intrinsics but do not size child to them, to make sure we are
            // remeasured when the connectionModifier is added.
            measurable.maxIntrinsicWidth(constraints.maxHeight)
            measurable.maxIntrinsicHeight(constraints.maxWidth)
            val placeable = measurable.measure(constraints)
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(0, 0)
            }
        }

        rule.setContent {
            Layout(
                {
                    Box(modifier = connectionModifier) {
                        LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                    }
                },
                measurePolicy = parentLayoutPolicy
            )
        }

        rule.runOnIdle {
            assertEquals(1, measures)
            connectionModifier = Modifier.size(10.dp)
        }

        rule.runOnIdle {
            assertEquals(2, measures)
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.runOnIdle {
            assertEquals(2, measures)
        }
    }

    @Test
    fun whenQueriedFromModifier() {
        var parentMeasures = 0
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)
        rule.setContent {
            LayoutMaybeUsingIntrinsics({ false }, onMeasure = { ++parentMeasures }) {
                // Box used to fast return intrinsics and do not remeasure when the size
                // of the inner Box is changing after the intrinsics change.
                Box(Modifier.requiredSize(100.dp)) {
                    Box(Modifier.testTag("box").then(ModifierUsingIntrinsics)) {
                        LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                    }
                }
            }
        }

        rule.runOnIdle {
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
        }

        rule.onNodeWithTag("box")
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun whenQueriedFromModifier_andAParentQueriesAbove() {
        var parentMeasures = 0
        var intrinsicWidth by mutableStateOf(40.dp)
        var intrinsicHeight by mutableStateOf(50.dp)
        rule.setContent {
            LayoutUsingIntrinsics(onMeasure = { ++parentMeasures }) {
                // Box used to fast return intrinsics and do not remeasure when the size
                // of the inner Box is changing after the intrinsics change.
                Box(Modifier.requiredSize(100.dp)) {
                    Box(Modifier.testTag("box").then(ModifierUsingIntrinsics)) {
                        LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                    }
                }
            }
        }

        rule.runOnIdle {
            intrinsicWidth = 30.dp
            intrinsicHeight = 20.dp
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
        }

        rule.onNodeWithTag("box")
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun childWithStatefulMeasureBlock() {
        val offsetPx = 20f
        val offsetDp = with(rule.density) { offsetPx.toDp() }
        val remeasureState = mutableStateOf(Unit, neverEqualPolicy())
        rule.setContent {
            Layout(
                content = {
                    var rect by remember { mutableStateOf(Rect.Zero) }
                    Box(
                        Modifier
                            .testTag("box")
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                rect = Rect(
                                    offsetPx, offsetPx,
                                    placeable.width - offsetPx, placeable.height - offsetPx
                                )
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            }.drawBehind {
                                drawRect(Color.Black)
                                drawRect(Color.Red, topLeft = rect.topLeft, rect.size)
                            }
                    )
                }
            ) { measurables, _ ->
                remeasureState.value
                val measurable = measurables.first()
                measurable.minIntrinsicHeight(50)
                val placeable = measurable.measure(Constraints.fixed(100, 100))
                layout(100, 100) {
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            remeasureState.value = Unit
        }

        rule.onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = offsetDp,
                verticalPadding = offsetDp,
                backgroundColor = Color.Black,
                shapeColor = Color.Red
            )
    }
}

@Composable
private fun LayoutWithIntrinsics(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    onMeasure: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                onMeasure()
                return layout(constraints.minWidth, constraints.minHeight) {}
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ): Int = width.roundToPx()

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ): Int = height.roundToPx()
        }
    )
}

@Composable
private fun LayoutMaybeUsingIntrinsics(
    useIntrinsics: () -> Boolean,
    modifier: Modifier = Modifier,
    onMeasure: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val measurePolicy = remember {
        object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                require(measurables.size == 1)
                onMeasure()
                val childConstraints = if (useIntrinsics()) {
                    val width = measurables.first().maxIntrinsicWidth(constraints.maxHeight)
                    val height = measurables.first().maxIntrinsicHeight(constraints.maxWidth)
                    Constraints.fixed(width, height)
                } else {
                    constraints
                }
                val placeable = measurables.first().measure(childConstraints)
                return layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ) = measurables.first().minIntrinsicWidth(height)

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ) = measurables.first().minIntrinsicHeight(width)

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ) = measurables.first().maxIntrinsicWidth(height)

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ) = measurables.first().maxIntrinsicHeight(width)
        }
    }
    Layout(content, modifier, measurePolicy)
}

@Composable
private fun LayoutUsingIntrinsics(
    modifier: Modifier = Modifier,
    onMeasure: () -> Unit = {},
    content: @Composable () -> Unit
) = LayoutMaybeUsingIntrinsics({ true }, modifier, onMeasure, content)

private val ModifierUsingIntrinsics = object : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val width = measurable.maxIntrinsicWidth(constraints.maxHeight)
        val height = measurable.maxIntrinsicHeight(constraints.maxWidth)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)
}

private fun Modifier.withIntrinsics(width: Dp, height: Dp): Modifier {
    return this.then(object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurable: IntrinsicMeasurable,
            height: Int
        ): Int = width.roundToPx()

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurable: IntrinsicMeasurable,
            width: Int
        ): Int = height.roundToPx()
    })
}