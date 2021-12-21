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

package androidx.compose.animation

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.junit.After
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalTestApi::class)
class AnimationModifierTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun animateContentSizeTest() {
        val startWidth = 100
        val endWidth = 150
        val startHeight = 400
        val endHeight = 200
        var width by mutableStateOf(startWidth)
        var height by mutableStateOf(startHeight)

        var density = 0f
        val testModifier by mutableStateOf(TestModifier())
        var animationStartSize: IntSize? = null
        var animationEndSize: IntSize? = null

        val frameDuration = 16
        val animDuration = 10 * frameDuration

        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(
                testModifier
                    .animateContentSize(
                        tween(
                            animDuration,
                            easing = LinearOutSlowInEasing
                        )
                    ) { startSize, endSize ->
                        animationStartSize = startSize
                        animationEndSize = endSize
                    }
                    .requiredSize(width.dp, height.dp)
            )
            density = LocalDensity.current.density
        }

        rule.runOnUiThread {
            width = endWidth
            height = endHeight
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        for (i in 0..animDuration step frameDuration) {
            val fraction = LinearOutSlowInEasing.transform(i / animDuration.toFloat())
            assertEquals(
                density * (startWidth * (1 - fraction) + endWidth * fraction),
                testModifier.width.toFloat(), 1f
            )

            assertEquals(
                density * (startHeight * (1 - fraction) + endHeight * fraction),
                testModifier.height.toFloat(), 1f
            )

            if (i == animDuration) {
                assertNotNull(animationStartSize)
                assertEquals(
                    animationStartSize!!.width.toFloat(),
                    startWidth * density, 1f
                )
                assertEquals(
                    animationStartSize!!.height.toFloat(),
                    startHeight * density, 1f
                )
            } else {
                assertNull(animationEndSize)
            }

            rule.mainClock.advanceTimeBy(frameDuration.toLong())
            rule.waitForIdle()
        }
    }

    @Test
    fun testInspectorValue() {
        rule.setContent {
            val modifier = Modifier.animateContentSize() as InspectableValue
            assertThat(modifier.nameFallback, `is`("animateContentSize"))
            assertThat(modifier.valueOverride, nullValue())
            assertThat(
                modifier.inspectableElements.map { it.name }.toList(),
                `is`(listOf("animationSpec", "finishedListener"))
            )
        }
    }
}

internal class TestModifier : LayoutModifier {
    var width: Int = 0
    var height: Int = 0
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        width = placeable.width
        height = placeable.height
        return layout(width, height) {
            placeable.place(0, 0)
        }
    }
}
