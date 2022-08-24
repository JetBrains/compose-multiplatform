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

package androidx.compose.material.pullrefresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.pow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class PullRefreshStateTest {

    @get:Rule
    val rule = createComposeRule()

    private val pullRefreshNode = rule.onNodeWithTag(PullRefreshTag)

    @Test
    fun pullBeyondThreshold_triggersRefresh() {

        var refreshCount = 0
        var touchSlop = 0f
        val threshold = 400f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            val state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        // Account for PullModifier - pull down twice the threshold value.
        pullRefreshNode.performTouchInput { swipeDown(endY = 2 * threshold + touchSlop + 1f) }

        rule.runOnIdle { assertThat(refreshCount).isEqualTo(1) }
    }

    @Test
    fun pullLessThanOrEqualToThreshold_doesNot_triggerRefresh() {
        var refreshCount = 0
        var touchSlop = 0f
        val threshold = 400f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            val state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        // Account for PullModifier - pull down twice the threshold value.

        // Less than threshold
        pullRefreshNode.performTouchInput { swipeDown(endY = 2 * threshold + touchSlop - 1f) }

        rule.waitForIdle()

        // Equal to threshold
        pullRefreshNode.performTouchInput { swipeDown(endY = 2 * threshold + touchSlop) }

        rule.runOnIdle { assertThat(refreshCount).isEqualTo(0) }
    }

    @Test
    fun progressAndPosition_scaleCorrectly_untilThreshold() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        val threshold = 400f

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = {
                    state.setRefreshing(true)
                    refreshCount++
                    state.setRefreshing(false)
                },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.onPull(threshold)

        rule.runOnIdle {
            val adjustedDistancePulled = threshold / 2 // Account for PullMultiplier.
            assertThat(state.progress).isEqualTo(0.5f)
            assertThat(state.position).isEqualTo(adjustedDistancePulled)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onPull(threshold + 1f)

        rule.runOnIdle {
            val adjustedDistancePulled = (2 * threshold + 1f) / 2 // Account for PullMultiplier.
            assertThat(state.progress).isEqualTo(adjustedDistancePulled / threshold)
            assertThat(state.position).isEqualTo(
                calculateIndicatorPosition(adjustedDistancePulled, threshold)
            )
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onRelease()

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.position).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    @Test
    fun progressAndPosition_scaleCorrectly_beyondThreshold() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        val threshold = 400f

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.onPull(2 * threshold)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(1f)
            assertThat(state.position).isEqualTo(threshold) // Account for PullMultiplier.
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onPull(threshold)

        rule.runOnIdle {
            val adjustedDistancePulled = 3 * threshold / 2 // Account for PullMultiplier.
            assertThat(state.progress).isEqualTo(1.5f)
            assertThat(state.position).isEqualTo(
                calculateIndicatorPosition(adjustedDistancePulled, threshold)
            )
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onRelease()

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    @Test
    fun positionIsCapped() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        val threshold = 400f

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.onPull(10 * threshold)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(5f) // Account for PullMultiplier.
            // Indicator position is capped to 2 times the refresh threshold.
            assertThat(state.position).isEqualTo(2 * threshold)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.onRelease()

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(1)
        }
    }

    @Test
    fun pullInterrupted() {
        lateinit var state: PullRefreshState
        var refreshCount = 0
        val threshold = 400f
        val refreshingOffset = 200f

        rule.setContent {
            state = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { refreshCount++ },
                refreshThreshold = with(LocalDensity.current) { threshold.toDp() },
                refreshingOffset = with(LocalDensity.current) { refreshingOffset.toDp() }
            )

            Box(Modifier.pullRefresh(state).testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }

        state.onPull(threshold)

        rule.runOnIdle {
            val adjustedDistancePulled = threshold / 2 // Account for PullMultiplier.
            assertThat(state.progress).isEqualTo(0.5f)
            assertThat(state.position).isEqualTo(adjustedDistancePulled)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.setRefreshing(true)

        val consumed = state.onPull(threshold)

        rule.runOnIdle {
            assertThat(consumed).isEqualTo(0f)
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.position).isEqualTo(refreshingOffset)
            assertThat(refreshCount).isEqualTo(0)
        }

        state.setRefreshing(false)

        rule.runOnIdle {
            assertThat(state.progress).isEqualTo(0f)
            assertThat(state.position).isEqualTo(0f)
            assertThat(refreshCount).isEqualTo(0)
        }
    }

    /**
     * Taken from the private function of the same name in [PullRefreshState].
     */
    private fun calculateIndicatorPosition(distance: Float, threshold: Float): Float = when {
        distance <= threshold -> distance
        else -> {
            val overshootPercent = abs(distance / threshold) - 1.0f
            val linearTension = overshootPercent.coerceIn(0f, 2f)
            val tensionPercent = linearTension - linearTension.pow(2) / 4
            val extraOffset = threshold * tensionPercent
            threshold + extraOffset
        }
    }
}

private const val PullRefreshTag = "PullRefresh"
