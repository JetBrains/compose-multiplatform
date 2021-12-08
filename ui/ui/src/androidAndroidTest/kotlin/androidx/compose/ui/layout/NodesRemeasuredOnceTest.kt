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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NodesRemeasuredOnceTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun remeasuringDirectChild() {
        val height = mutableStateOf(10)
        var remeasurements = 0

        rule.setContent {
            WrapChild(onMeasured = { actualHeight ->
                assertThat(actualHeight).isEqualTo(height.value)
                remeasurements++
            }) {
                Child(height)
            }
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(1)
            height.value = 20
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(2)
        }
    }

    @Test
    fun remeasuringChildWithExtraLayer() {
        val height = mutableStateOf(10)
        var remeasurements = 0

        rule.setContent {
            WrapChild(onMeasured = { actualHeight ->
                assertThat(actualHeight).isEqualTo(height.value)
                remeasurements++
            }) {
                WrapChild {
                    Child(height)
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(1)
            height.value = 20
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(2)
        }
    }

    @Test
    fun remeasuringChildWith3ExtraLayers() {
        val height = mutableStateOf(10)
        var remeasurements = 0

        rule.setContent {
            WrapChild(onMeasured = { actualHeight ->
                assertThat(actualHeight).isEqualTo(height.value)
                remeasurements++
            }) {
                WrapChild {
                    WrapChild {
                        WrapChild {
                            Child(height)
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(1)
            height.value = 20
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(2)
        }
    }

    @Test
    fun remeasuringChildWithExtraLayer_notPlacedChild() {
        val height = mutableStateOf(10)
        var remeasurements = 0

        rule.setContent {
            WrapChild(onMeasured = { actualHeight ->
                assertThat(actualHeight).isEqualTo(height.value)
                remeasurements++
            }) {
                NotPlaceChild(height) {
                    WrapChild {
                        Child(height)
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(1)
            height.value = 20
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(2)
        }
    }

    @Test
    fun remeasuringChildDuringLayoutWithExtraLayer() {
        val height = mutableStateOf(10)
        var remeasurements = 0

        rule.setContent {
            WrapChildMeasureDuringLayout(onMeasured = { actualHeight ->
                assertThat(actualHeight).isEqualTo(height.value)
                remeasurements++
            }) {
                WrapChild {
                    Child(height)
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(1)
            height.value = 20
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(2)
        }
    }

    @Test
    fun remeasuringChildDuringLayout() {
        val height = mutableStateOf(10)
        var expectedHeight = 10
        var remeasurements = 0

        rule.setContent {
            WrapChildMeasureDuringLayout(onMeasured = { actualHeight ->
                assertThat(actualHeight).isEqualTo(expectedHeight)
                remeasurements++
            }) {
                Child(height)
            }
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(1)
            expectedHeight = 20
            height.value = 20
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(2)
        }
    }

    @Test
    fun remeasuringChildDuringLayoutWithExtraLayerUsingIntrinsics() {
        val height = mutableStateOf(10)
        var remeasurements = 0

        rule.setContent {
            IntrinsicSizeAndMeasureDuringLayout(onMeasured = { actualHeight ->
                assertThat(actualHeight).isEqualTo(height.value)
                remeasurements++
            }) {
                WrapChild {
                    Child(height)
                }
            }
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(1)
            height.value = 20
        }

        rule.runOnIdle {
            assertThat(remeasurements).isEqualTo(2)
        }
    }
}

@Composable
private fun WrapChild(onMeasured: (Int) -> Unit = {}, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val placeable = measurables.first()
            .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        onMeasured(placeable.height)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

@Composable
private fun WrapChildMeasureDuringLayout(
    onMeasured: (Int) -> Unit = {},
    content: @Composable () -> Unit
) {
    Layout(content = content) { measurables, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            val placeable = measurables.first()
                .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            onMeasured(placeable.height)
            placeable.place(0, 0)
        }
    }
}

@Composable
private fun IntrinsicSizeAndMeasureDuringLayout(
    onMeasured: (Int) -> Unit = {},
    content: @Composable () -> Unit
) {
    Layout(content = content) { measurables, constraints ->
        val width = measurables.first().maxIntrinsicWidth(constraints.maxWidth)
        val height = measurables.first().maxIntrinsicHeight(constraints.maxHeight)
        layout(width, height) {
            val placeable = measurables.first()
                .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            onMeasured(placeable.height)
            placeable.place(0, 0)
        }
    }
}

@Composable
private fun NotPlaceChild(height: State<Int>, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        layout(constraints.maxWidth, height.value) {
            measurables.first()
                .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        }
    }
}

@Composable
private fun Child(height: State<Int>) {
    Layout { _, constraints ->
        layout(constraints.maxWidth, height.value) {}
    }
}
