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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class PullRefreshIndicatorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun indicatorDisplayed_refreshingInitially() {
        rule.setContent {
            val state = rememberPullRefreshState(true, {})
            Box(Modifier.fillMaxSize()) {
                PullRefreshIndicator(true, state, Modifier.testTag(IndicatorTag))
            }
        }
        indicatorNode.assertIsDisplayed()
    }

    @Test
    fun indicatorDisplayed_setRefreshing() {
        var refreshing by mutableStateOf(false)

        rule.setContent {
            val state = rememberPullRefreshState(refreshing, {})
            Box(Modifier.fillMaxSize()) {
                PullRefreshIndicator(refreshing, state, Modifier.testTag(IndicatorTag))
            }
        }

        indicatorNode.assertIsNotDisplayed()

        refreshing = true

        rule.waitForIdle()
        indicatorNode.assertIsDisplayed()
    }

    @Test
    fun indicatorDisplayed_pullRefresh() {
        var refreshCount = 0

        var refreshing by mutableStateOf(false)

        rule.setContent {
            val state = rememberPullRefreshState(refreshing, { refreshing = true; refreshCount++ })

            Box(
                Modifier
                    .pullRefresh(state)
                    .testTag(PullRefreshTag)) {
                LazyColumn {
                    items(30) {
                        ListItem {
                            Text("Item: $it")
                        }
                    }
                }
                PullRefreshIndicator(refreshing, state, Modifier.testTag(IndicatorTag))
            }
        }

        indicatorNode.assertIsNotDisplayed()
        pullRefreshNode.performTouchInput { swipeDown() }

        rule.waitForIdle()
        assertThat(refreshCount).isEqualTo(1)
        assertThat(refreshing).isTrue()
        indicatorNode.assertIsDisplayed()

        refreshing = false

        rule.waitForIdle()
        indicatorNode.assertIsNotDisplayed()
    }

    @Test
    fun refreshingIndicator_returnsToRest() {
        var refreshing by mutableStateOf(false)

        rule.setContent {
            val state = rememberPullRefreshState(refreshing, {})

            Box(
                Modifier
                    .pullRefresh(state)
                    .testTag(PullRefreshTag)) {
                LazyColumn {
                    items(30) {
                        ListItem {
                            Text("Item: $it")
                        }
                    }
                }
                PullRefreshIndicator(refreshing, state, Modifier.testTag(IndicatorTag))
            }
        }

        indicatorNode.assertIsNotDisplayed()
        val restingBounds = indicatorNode.getUnclippedBoundsInRoot()

        refreshing = true

        rule.waitForIdle()
        indicatorNode.assertIsDisplayed()
        val refreshingBounds = indicatorNode.getUnclippedBoundsInRoot()

        pullRefreshNode.performTouchInput { swipeDown() }

        rule.waitForIdle()
        indicatorNode.assertIsDisplayed()
        assertThat(indicatorNode.getUnclippedBoundsInRoot()).isEqualTo(refreshingBounds)

        refreshing = false

        rule.waitForIdle()
        indicatorNode.assertIsNotDisplayed()
        assertThat(indicatorNode.getUnclippedBoundsInRoot()).isEqualTo(restingBounds)
    }

    private val pullRefreshNode get() = rule.onNodeWithTag(PullRefreshTag)
    private val indicatorNode get() = rule.onNodeWithTag(IndicatorTag).onChild()
}

private const val PullRefreshTag = "pull-refresh"
private const val IndicatorTag = "pull-refresh-indicator"