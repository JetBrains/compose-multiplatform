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

package androidx.compose.ui.graphics.vector

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimatedImageVectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val pathStart: PathBuilder.() -> Unit = {
        horizontalLineTo(50f)
        verticalLineTo(50f)
        horizontalLineTo(0f)
        close()
    }

    private val pathEnd: PathBuilder.() -> Unit = {
        horizontalLineTo(10f)
        verticalLineTo(10f)
        horizontalLineTo(0f)
        close()
    }

    private val topLeftRed = ImageVector.Builder(
        name = "image",
        defaultWidth = 100.dp,
        defaultHeight = 100.dp,
        viewportWidth = 100f,
        viewportHeight = 100f
    ).apply {
        group(
            name = "group",
            pivotX = 50f,
            pivotY = 50f
        ) {
            // Top left is red.
            path(name = "path", fill = SolidColor(Color.Red), pathBuilder = pathStart)
        }
    }.build()

    private val groupRotation = AnimatedVectorTarget(
        name = "group",
        animator = ObjectAnimator(
            duration = 1000,
            startDelay = 0,
            repeatCount = 0,
            repeatMode = RepeatMode.Restart,
            holders = listOf(
                PropertyValuesHolderFloat(
                    propertyName = "rotation",
                    animatorKeyframes = listOf(
                        Keyframe(0f, 0f, LinearEasing),
                        Keyframe(1f, 360f, LinearEasing)
                    )
                )
            )
        )
    )

    private val pathData = AnimatedVectorTarget(
        name = "path",
        animator = ObjectAnimator(
            duration = 1000,
            startDelay = 0,
            repeatCount = 0,
            repeatMode = RepeatMode.Restart,
            holders = listOf(
                PropertyValuesHolderPath(
                    propertyName = "pathData",
                    animatorKeyframes = listOf(
                        Keyframe(0f, PathData(pathStart), LinearEasing),
                        Keyframe(1f, PathData(pathEnd), LinearEasing)
                    )
                )
            )
        )
    )

    private val delta = 0.001f

    @Test
    fun rotation() {
        composeTestRule.mainClock.autoAdvance = false
        val image = AnimatedImageVector(
            imageVector = topLeftRed,
            targets = listOf(groupRotation)
        )
        var override: VectorOverride? = null
        var state by mutableStateOf(false)
        composeTestRule.setContent {
            image.painterFor(state) { _, map ->
                override = map["group"]
            }
        }
        val impossibleValue = 1234f
        assertThat(override!!.obtainRotation(impossibleValue)).isWithin(delta).of(0f)
        assertThat(override!!.obtainTranslateX(impossibleValue)).isWithin(delta).of(impossibleValue)
        composeTestRule.runOnUiThread { state = true }
        // TODO(yaraki): Use deterministic animation testing framework to test intermediate values.
        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.waitForIdle()
        assertThat(override!!.obtainRotation(impossibleValue)).isWithin(delta).of(360f)
    }

    @Test
    fun pathData() {
        composeTestRule.mainClock.autoAdvance = false
        val image = AnimatedImageVector(
            imageVector = topLeftRed,
            targets = listOf(pathData)
        )
        var override: VectorOverride? = null
        var state by mutableStateOf(false)
        composeTestRule.setContent {
            image.painterFor(state) { _, map ->
                override = map["path"]
            }
        }
        assertThat(override!!.obtainPathData(emptyList())).isEqualTo(PathData(pathStart))
        composeTestRule.runOnUiThread { state = true }
        // TODO(yaraki): Use deterministic animation testing framework to test intermediate values.
        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.waitForIdle()
        assertThat(override!!.obtainPathData(emptyList())).isEqualTo(PathData(pathEnd))
    }
}
