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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
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
class PullRefreshTest {

    @get:Rule
    val rule = createComposeRule()

    private val pullRefreshNode = rule.onNodeWithTag(PullRefreshTag)

    @Test
    fun pullDownFromTop_producesCorrect_pullDelta() {
        var distancePulled = 0f

        fun onPull(pullDelta: Float): Float {
            distancePulled += pullDelta
            return pullDelta // Consume whole delta.
        }

        val touchSlop = setPullRefreshAndReturnSlop(::onPull) { 0f }
        val pullAmount = 400f

        pullRefreshNode.performTouchInput {
            swipeDown(endY = pullAmount + touchSlop)
        }

        rule.runOnIdle { assertThat(distancePulled).isEqualTo(pullAmount) }
    }

    @Test
    fun onRelease_calledWhen_pullEnds() {
        var releaseCount = 0

        setPullRefreshAndReturnSlop({ it }) { releaseCount++; 0f }

        pullRefreshNode.performTouchInput {
            down(Offset.Zero)
            moveBy(Offset(0f, 400f))
        }

        rule.runOnIdle { assertThat(releaseCount).isEqualTo(0) }

        pullRefreshNode.performTouchInput { up() }

        rule.runOnIdle { assertThat(releaseCount).isEqualTo(1) }
    }

    @Test
    fun swipeUp_pullDown() {
        var distancePulled = 0f
        var deltaGiven = 0f
        var releaseCount = 0

        fun onPull(pullDelta: Float): Float {
            deltaGiven += pullDelta
            val newDist = (distancePulled + pullDelta).coerceAtLeast(0f)
            val consumed = newDist - distancePulled
            distancePulled = newDist
            return consumed
        }

        val touchSlop = setPullRefreshAndReturnSlop(::onPull) { releaseCount++; 0f }

        pullRefreshNode.performTouchInput {
            down(Offset(0f, 800f))
            moveBy(Offset(0f, -(400f + touchSlop)))
        }

        rule.runOnIdle {
            assertThat(deltaGiven).isEqualTo(-400f)
            assertThat(distancePulled).isEqualTo(0f)
            assertThat(releaseCount).isEqualTo(0)
        }

        pullRefreshNode.performTouchInput {
            moveBy(Offset(0f, 200f))
        }

        rule.runOnIdle {
            assertThat(deltaGiven).isEqualTo(-400f)
            assertThat(distancePulled).isEqualTo(0f)
            assertThat(releaseCount).isEqualTo(0)
        }

        pullRefreshNode.performTouchInput {
            moveBy(Offset(0f, 200f))
        }

        rule.runOnIdle {
            assertThat(deltaGiven).isEqualTo(-400f)
            assertThat(distancePulled).isEqualTo(0f)
            assertThat(releaseCount).isEqualTo(0)
        }

        pullRefreshNode.performTouchInput {
            moveBy(Offset(0f, 200f))
            up()
        }

        rule.runOnIdle {
            assertThat(deltaGiven).isEqualTo(-200f)
            assertThat(distancePulled).isEqualTo(200f)
            assertThat(releaseCount).isEqualTo(1)
        }
    }

    @Test
    fun pullDown_swipeUp() {
        var distancePulled = 0f
        var deltaGiven = 0f
        var releaseCount = 0

        fun onPull(pullDelta: Float): Float {
            deltaGiven += pullDelta
            val newDist = (distancePulled + pullDelta).coerceAtLeast(0f)
            val consumed = newDist - distancePulled
            distancePulled = newDist
            return consumed
        }

        val touchSlop = setPullRefreshAndReturnSlop(::onPull) { releaseCount++; 0f }

        pullRefreshNode.performTouchInput {
            down(Offset.Zero)
            moveBy(Offset(0f, 400f + touchSlop))
        }

        rule.runOnIdle {
            assertThat(deltaGiven).isEqualTo(400f)
            assertThat(distancePulled).isEqualTo(400f)
            assertThat(releaseCount).isEqualTo(0)
        }

        pullRefreshNode.performTouchInput {
            moveBy(Offset(0f, -200f))
        }

        rule.runOnIdle {
            assertThat(deltaGiven).isEqualTo(200f)
            assertThat(distancePulled).isEqualTo(200f)
            assertThat(releaseCount).isEqualTo(0)
        }

        pullRefreshNode.performTouchInput {
            moveBy(Offset(0f, -200f))
        }

        rule.runOnIdle {
            assertThat(deltaGiven).isEqualTo(0f)
            assertThat(distancePulled).isEqualTo(0f)
            assertThat(releaseCount).isEqualTo(0)
        }

        pullRefreshNode.performTouchInput {
            moveBy(Offset(0f, -200f))
            up()
        }

        rule.runOnIdle {
            assertThat(deltaGiven).isEqualTo(-200f)
            assertThat(distancePulled).isEqualTo(0f)
            assertThat(releaseCount).isEqualTo(1)
        }
    }

    private fun setPullRefreshAndReturnSlop(
        onPull: (pullDelta: Float) -> Float,
        onRelease: (flingVelocity: Float) -> Float,
    ): Float {
        var slop = 0f
        rule.setContent {
            slop = LocalViewConfiguration.current.touchSlop
            Box(
                Modifier
                    .pullRefresh({ onPull(it) }, { onRelease(it) })
                    .testTag(PullRefreshTag)) {
                LazyColumn {
                    items(100) {
                        Text("item $it")
                    }
                }
            }
        }
        return slop
    }
}

private const val PullRefreshTag = "PullRefresh"
