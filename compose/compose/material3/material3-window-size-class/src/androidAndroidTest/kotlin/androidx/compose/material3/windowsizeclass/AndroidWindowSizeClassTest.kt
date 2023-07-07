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

package androidx.compose.material3.windowsizeclass

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.window.layout.WindowMetricsCalculator
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class AndroidWindowSizeClassTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun widthSizeClass_correctCalculation() {
        var actualWindowWidthSizeClass: WindowWidthSizeClass? = null
        rule.setContent {
            actualWindowWidthSizeClass = calculateWindowSizeClass(rule.activity).widthSizeClass
        }

        rule.runOnIdle {
            assertThat(actualWindowWidthSizeClass).isNotNull()

            val width = getWindowBounds().width()
            val expectedWindowWidthSizeClass = with(rule.density) {
                WindowWidthSizeClass.fromWidth(width.toDp())
            }
            assertThat(expectedWindowWidthSizeClass).isEqualTo(actualWindowWidthSizeClass)
        }
    }

    @Test
    fun heightSizeClass_correctCalculation() {
        var actualWindowHeightSizeClass: WindowHeightSizeClass? = null
        rule.setContent {
            actualWindowHeightSizeClass = calculateWindowSizeClass(rule.activity).heightSizeClass
        }

        rule.runOnIdle {
            assertThat(actualWindowHeightSizeClass).isNotNull()

            val height = getWindowBounds().height()
            val expectedWindowHeightSizeClass = with(rule.density) {
                WindowHeightSizeClass.fromHeight(height.toDp())
            }
            assertThat(expectedWindowHeightSizeClass).isEqualTo(actualWindowHeightSizeClass)
        }
    }

    @Test
    fun sizeClass_recalculated_onDensityUpdate() {
        lateinit var actualWindowSizeClass: WindowSizeClass
        var firstSize: DpSize? = null
        var secondSize: DpSize
        val density = mutableStateOf(Density(1f))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density.value) {
                actualWindowSizeClass = calculateWindowSizeClass(rule.activity)
            }
        }

        rule.runOnIdle {
            val expectedWindowSizeClass = with(density.value) {
                firstSize = getWindowDpSize()
                WindowSizeClass.calculateFromSize(firstSize!!)
            }
            assertThat(actualWindowSizeClass).isEqualTo(expectedWindowSizeClass)
        }

        // change density
        density.value = Density(10f)

        rule.runOnIdle {
            val expectedWindowSizeClass = with(density.value) {
                secondSize = getWindowDpSize()
                WindowSizeClass.calculateFromSize(secondSize)
            }
            assertThat(actualWindowSizeClass).isEqualTo(expectedWindowSizeClass)

            assertThat(firstSize!! / 10f).isEqualTo(secondSize)
        }
    }

    private fun getWindowBounds() = with(rule.activity) {
        WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds
    }

    private fun Density.getWindowDpSize(): DpSize {
        val bounds = getWindowBounds()
        return DpSize(bounds.width().toDp(), bounds.height().toDp())
    }
}