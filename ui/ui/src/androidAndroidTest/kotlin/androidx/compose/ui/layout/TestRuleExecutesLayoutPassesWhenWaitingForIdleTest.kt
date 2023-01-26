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

import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("UNUSED_EXPRESSION")
@MediumTest
@RunWith(AndroidJUnit4::class)
class TestRuleExecutesLayoutPassesWhenWaitingForIdleTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun measure_animation() {
        val numUpdates = 5
        var state by mutableStateOf(0)
        var measures = 0
        var placements = 0
        rule.setContent {
            Layout { _, _ ->
                measures++
                state
                layout(1, 1) {
                    placements++
                }
            }

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        state++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(measures).isEqualTo(numUpdates + 1)
            assertThat(placements).isEqualTo(numUpdates + 1)
        }
    }

    @Test
    fun placement_animation() {
        val numUpdates = 5
        var state by mutableStateOf(0)
        var measures = 0
        var placements = 0
        rule.setContent {
            Layout { _, _ ->
                measures++
                layout(1, 1) {
                    placements++
                    state
                }
            }

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        state++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(measures).isEqualTo(1)
            assertThat(placements).isEqualTo(numUpdates + 1)
        }
    }

    @Ignore("b/265281787")
    @Test
    fun child_AndroidView() {
        val numUpdates = 5
        var size by mutableStateOf(10)
        var viewMeasures = 0
        var viewLayouts = 0
        rule.setContent {
            AndroidView(
                // Animate the constraints of the View.
                modifier = Modifier.layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints.fixed(size, size))
                    layout(placeable.width, placeable.height) {
                        placeable.place(IntOffset.Zero)
                    }
                },
                factory = {
                    object : View(it) {
                        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                            viewMeasures++
                            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                        }

                        override fun onLayout(
                            changed: Boolean,
                            left: Int,
                            top: Int,
                            right: Int,
                            bottom: Int
                        ) {
                            viewLayouts++
                            super.onLayout(changed, left, top, right, bottom)
                        }
                    }
                }
            )

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        size++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(viewMeasures).isEqualTo(numUpdates + 2)
            assertThat(viewLayouts).isEqualTo(numUpdates + 1)
        }
    }

    @Test
    fun child_AndroidView_child_ComposeView_measure_animation() {
        val numUpdates = 5
        var state by mutableStateOf(0)
        var nestedMeasures = 0
        var nestedPlacements = 0
        rule.setContent {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        val leaf = ComposeView(context)
                        addView(leaf)
                        leaf.setContent {
                            Layout { _, _ ->
                                nestedMeasures++
                                state
                                layout(1, 1) {
                                    nestedPlacements++
                                }
                            }
                        }
                    }
                }
            )

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        state++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(nestedMeasures).isEqualTo(numUpdates + 1)
            assertThat(nestedPlacements).isEqualTo(numUpdates + 1)
        }
    }

    @Test
    fun child_AndroidView_child_ComposeView_placement_animation() {
        val numUpdates = 5
        var state by mutableStateOf(0)
        var nestedMeasures = 0
        var nestedPlacements = 0
        rule.setContent {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        val leaf = ComposeView(context)
                        addView(leaf)
                        leaf.setContent {
                            Layout { _, _ ->
                                nestedMeasures++
                                layout(1, 1) {
                                    nestedPlacements++
                                    state
                                }
                            }
                        }
                    }
                }
            )

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        state++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(nestedMeasures).isEqualTo(1)
            assertThat(nestedPlacements).isEqualTo(numUpdates + 1)
        }
    }

    @Test
    fun SubcomposeLayout_compose_in_measure_animate_measure() {
        val numUpdates = 5
        var state by mutableStateOf(0)
        var measures = 0
        var placements = 0
        rule.setContent {
            SubcomposeLayout { constraints ->
                val measurables = subcompose(null) {
                    Layout { _, _ ->
                        measures++
                        state
                        layout(1, 1) {
                            placements++
                        }
                    }
                }
                val placeable = measurables.single().measure(constraints)

                layout(1, 1) {
                    placeable.place(IntOffset.Zero)
                }
            }

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        state++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(measures).isEqualTo(numUpdates + 1)
            assertThat(placements).isEqualTo(numUpdates + 1)
        }
    }

    @Test
    fun SubcomposeLayout_compose_in_measure_animate_placement() {
        val numUpdates = 5
        var state by mutableStateOf(0)
        var measures = 0
        var placements = 0
        rule.setContent {
            SubcomposeLayout { constraints ->
                val measurables = subcompose(null) {
                    Layout { _, _ ->
                        measures++
                        layout(1, 1) {
                            placements++
                            state
                        }
                    }
                }
                val placeable = measurables.single().measure(constraints)

                layout(1, 1) {
                    placeable.place(IntOffset.Zero)
                }
            }

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        state++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(measures).isEqualTo(1)
            assertThat(placements).isEqualTo(numUpdates + 1)
        }
    }

    @Test
    fun SubcomposeLayout_compose_in_placement_animate_measure() {
        val numUpdates = 5
        var state by mutableStateOf(0)
        var measures = 0
        var placements = 0
        rule.setContent {
            SubcomposeLayout { constraints ->
                layout(1, 1) {
                    val measurables = subcompose(null) {
                        Layout { _, _ ->
                            measures++
                            state
                            layout(1, 1) {
                                placements++
                            }
                        }
                    }
                    val placeable = measurables.single().measure(constraints)
                    placeable.place(IntOffset.Zero)
                }
            }

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        state++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(measures).isEqualTo(numUpdates + 1)
            assertThat(placements).isEqualTo(numUpdates + 1)
        }
    }

    @Test
    fun SubcomposeLayout_compose_in_placement_animate_placement() {
        val numUpdates = 5
        var state by mutableStateOf(0)
        var measures = 0
        var placements = 0
        rule.setContent {
            SubcomposeLayout { constraints ->
                layout(1, 1) {
                    val measurables = subcompose(null) {
                        Layout { _, _ ->
                            measures++
                            layout(1, 1) {
                                placements++
                                state
                            }
                        }
                    }
                    val placeable = measurables.single().measure(constraints)
                    placeable.place(IntOffset.Zero)
                }
            }

            LaunchedEffect(Unit) {
                for (i in 0 until numUpdates) {
                    withFrameNanos {
                        state++
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(measures).isEqualTo(1)
            assertThat(placements).isEqualTo(numUpdates + 1)
        }
    }
}