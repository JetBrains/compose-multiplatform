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

package androidx.compose.ui.layout

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Copies of most of the tests in [RemeasureWithIntrinsicsTest] but without using
 * TestMonotonicFrameClock, since it does layout passes slightly differently than in production
 * and this has bitten us in the past (see b/222093277).
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RemeasureWithIntrinsicsRealClockTest {

    @get:Rule
    val rule = ActivityScenarioRule(TestActivity::class.java)

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    private val testLatch = CountDownLatch(1)

    @Test
    fun remeasuringChildWhenParentUsedIntrinsicSizes() {
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)
        var childSize: IntSize? = null

        setTestContent(
            content = {
                LayoutUsingIntrinsics {
                    LayoutWithIntrinsics(
                        intrinsicWidth,
                        intrinsicHeight,
                        Modifier.onSizeChanged { childSize = it }
                    )
                }
            },
            test = {
                assertThat(childSize).isEqualTo(IntSize(40, 50))

                intrinsicWidth = 30
                intrinsicHeight = 20
                withFrameNanos {}

                assertThat(childSize).isEqualTo(IntSize(30, 20))
            }
        )
    }

    @Test
    fun updatingChildIntrinsicsViaModifierWhenParentUsedIntrinsicSizes() {
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)
        var childSize: IntSize? = null

        setTestContent(
            content = {
                LayoutUsingIntrinsics {
                    Box(
                        Modifier
                            .onSizeChanged { childSize = it }
                            .withIntrinsics(intrinsicWidth, intrinsicHeight)
                    )
                }
            },
            test = {
                assertThat(childSize).isEqualTo(IntSize(40, 50))

                intrinsicWidth = 30
                intrinsicHeight = 20
                withFrameNanos {}

                assertThat(childSize).isEqualTo(IntSize(30, 20))
            }
        )
    }

    @Test
    fun remeasuringGrandChildWhenGrandParentUsedIntrinsicSizes() {
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)
        var childSize: IntSize? = null

        setTestContent(
            content = {
                LayoutUsingIntrinsics {
                    Box(propagateMinConstraints = true) {
                        LayoutWithIntrinsics(
                            intrinsicWidth,
                            intrinsicHeight,
                            Modifier.onSizeChanged { childSize = it }
                        )
                    }
                }
            },
            test = {
                assertThat(childSize).isEqualTo(IntSize(40, 50))

                intrinsicWidth = 30
                intrinsicHeight = 20
                withFrameNanos {}

                assertThat(childSize).isEqualTo(IntSize(30, 20))
            }
        )
    }

    @Test
    fun updatingGrandChildIntrinsicsViaModifierWhenGrandParentUsedIntrinsicSizes() {
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)
        var childSize: IntSize? = null

        setTestContent(
            content = {
                LayoutUsingIntrinsics {
                    Box(propagateMinConstraints = true) {
                        Box(
                            Modifier
                                .onSizeChanged { childSize = it }
                                .withIntrinsics(intrinsicWidth, intrinsicHeight)
                        )
                    }
                }
            },
            test = {
                assertThat(childSize).isEqualTo(IntSize(40, 50))

                intrinsicWidth = 30
                intrinsicHeight = 20
                withFrameNanos {}

                assertThat(childSize).isEqualTo(IntSize(30, 20))
            }
        )
    }

    @Test
    fun nodeDoesNotCauseRemeasureOfAncestor_whenItsIntrinsicsAreUnused() {
        var measures = 0
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)
        var parentSize: IntSize? = null

        setTestContent(
            content = {
                LayoutUsingIntrinsics(
                    onMeasure = { ++measures },
                    modifier = Modifier.onSizeChanged { parentSize = it }
                ) {
                    LayoutWithIntrinsics(20, 20) {
                        LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                    }
                }
            },
            test = {
                intrinsicWidth = 30
                intrinsicHeight = 20

                withFrameNanos {}
                assertThat(measures).isEqualTo(1)
                assertThat(parentSize).isEqualTo(IntSize(20, 20))
            }
        )
    }

    @Test
    fun causesRemeasureOfAllDependantAncestors() {
        var measures1 = 0
        var measures2 = 0
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)

        setTestContent(
            content = {
                LayoutUsingIntrinsics(
                    onMeasure = { ++measures1 }
                ) {
                    Box {
                        LayoutUsingIntrinsics(
                            onMeasure = { ++measures2 }
                        ) {
                            Box {
                                LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                            }
                        }
                    }
                }
            },
            test = {
                intrinsicWidth = 30
                intrinsicHeight = 20

                withFrameNanos {}
                assertThat(measures1).isEqualTo(2)
                assertThat(measures2).isEqualTo(2)

                // Shouldn't remeasure any more.
                withFrameNanos {}
                assertThat(measures1).isEqualTo(2)
                assertThat(measures2).isEqualTo(2)
            }
        )
    }

    @Test
    fun whenConnectionFromOwnerDoesNotQueryAnymore() {
        var measures = 0
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)
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

        setTestContent(
            content = {
                Layout(
                    {
                        Box(modifier = connectionModifier) {
                            LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                        }
                    },
                    measurePolicy = parentLayoutPolicy
                )
            },
            test = {
                assertThat(measures).isEqualTo(1)
                connectionModifier = Modifier.size(10.toDp())

                withFrameNanos {}
                assertThat(measures).isEqualTo(2)
                intrinsicWidth = 30
                intrinsicHeight = 20

                withFrameNanos {}
                assertThat(measures).isEqualTo(2)
            }
        )
    }

    @Test
    fun whenQueriedFromModifier() {
        var parentMeasures = 0
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)
        var boxSize: IntSize? = null

        setTestContent(
            content = {
                LayoutMaybeUsingIntrinsics({ false }, onMeasure = { ++parentMeasures }) {
                    // Box used to fast return intrinsics and do not remeasure when the size
                    // of the inner Box is changing after the intrinsics change.
                    Box(Modifier.requiredSize(100.toDp())) {
                        Box(
                            Modifier
                                .onSizeChanged { boxSize = it }
                                .then(ModifierUsingIntrinsics)
                        ) {
                            LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                        }
                    }
                }
            },
            test = {
                intrinsicWidth = 30
                intrinsicHeight = 20

                withFrameNanos {}
                assertThat(parentMeasures).isEqualTo(1)
                assertThat(boxSize).isEqualTo(IntSize(30, 20))
            }
        )
    }

    @Test
    fun whenQueriedFromModifier_andAParentQueriesAbove() {
        var parentMeasures = 0
        var intrinsicWidth by mutableStateOf(40)
        var intrinsicHeight by mutableStateOf(50)
        var boxSize: IntSize? = null

        setTestContent(
            content = {
                LayoutUsingIntrinsics(onMeasure = { ++parentMeasures }) {
                    // Box used to fast return intrinsics and do not remeasure when the size
                    // of the inner Box is changing after the intrinsics change.
                    Box(Modifier.requiredSize(100.toDp())) {
                        Box(
                            Modifier
                                .onSizeChanged { boxSize = it }
                                .then(ModifierUsingIntrinsics)
                        ) {
                            LayoutWithIntrinsics(intrinsicWidth, intrinsicHeight)
                        }
                    }
                }
            },
            test = {
                intrinsicWidth = 30
                intrinsicHeight = 20

                withFrameNanos {}
                assertThat(parentMeasures).isEqualTo(1)
                assertThat(boxSize).isEqualTo(IntSize(30, 20))
            }
        )
    }

    @Test
    fun introducingChildIntrinsicsViaModifierWhenParentUsedIntrinsicSizes() {
        var childModifier by mutableStateOf(Modifier as Modifier)
        var childSize: IntSize? = null

        setTestContent(
            content = {
                LayoutUsingIntrinsics {
                    Box(
                        Modifier
                            .onSizeChanged { childSize = it }
                            .then(childModifier)
                    )
                }
            },
            test = {
                assertThat(childSize).isEqualTo(IntSize.Zero)

                childModifier = Modifier.withIntrinsics(30, 20)

                withFrameNanos {}
                assertThat(childSize).isEqualTo(IntSize(30, 20))
            }
        )
    }

    private fun setTestContent(
        content: @Composable Density.() -> Unit,
        test: suspend Density.() -> Unit
    ) {
        rule.withActivity {
            setContent {
                val density = LocalDensity.current
                content(density)
                LaunchedEffect(Unit) {
                    // Wait for the first layout pass to finish.
                    withFrameNanos {}
                    test(density)
                    testLatch.countDown()
                }
            }
        }
        testLatch.await(3, TimeUnit.SECONDS)
    }

    @Composable
    private fun LayoutWithIntrinsics(
        width: Int,
        height: Int,
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
                ): Int = width

                override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int = height
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

    private fun Modifier.withIntrinsics(width: Int, height: Int): Modifier {
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
            ): Int = width

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurable: IntrinsicMeasurable,
                width: Int
            ): Int = height
        })
    }
}