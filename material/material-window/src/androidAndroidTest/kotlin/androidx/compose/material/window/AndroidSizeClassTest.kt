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

package androidx.compose.material.window

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
@OptIn(ExperimentalMaterialWindowApi::class)
class AndroidSizeClassTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun widthSizeClass_correctCalculation() {
        var actualWidthSizeClass: WidthSizeClass? = null
        rule.setContent {
            actualWidthSizeClass = rule.activity.rememberWidthSizeClass()
        }

        rule.runOnIdle {
            assertThat(actualWidthSizeClass).isNotNull()

            val width = getWindowBounds().width()
            val expectedWidthSizeClass = with(rule.density) {
                WidthSizeClass.fromWidth(width.toDp())
            }
            assertThat(expectedWidthSizeClass).isEqualTo(actualWidthSizeClass)
        }
    }

    @Test
    fun heightSizeClass_correctCalculation() {
        var actualHeightSizeClass: HeightSizeClass? = null
        rule.setContent {
            actualHeightSizeClass = rule.activity.rememberHeightSizeClass()
        }

        rule.runOnIdle {
            assertThat(actualHeightSizeClass).isNotNull()

            val height = getWindowBounds().height()
            val expectedHeightSizeClass = with(rule.density) {
                HeightSizeClass.fromHeight(height.toDp())
            }
            assertThat(expectedHeightSizeClass).isEqualTo(actualHeightSizeClass)
        }
    }

    @Test
    fun sizeClass_recalculated_onDensityUpdate() {
        lateinit var actualSizeClass: SizeClass
        var firstSize: DpSize? = null
        var secondSize: DpSize
        val density = mutableStateOf(Density(1f))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density.value) {
                actualSizeClass = rule.activity.rememberSizeClass()
            }
        }

        rule.runOnIdle {
            val expectedSizeClass = with(density.value) {
                firstSize = getWindowDpSize()
                SizeClass.calculateFromSize(firstSize!!)
            }
            assertThat(actualSizeClass).isEqualTo(expectedSizeClass)
        }

        // change density
        density.value = Density(10f)

        rule.runOnIdle {
            val expectedSizeClass = with(density.value) {
                secondSize = getWindowDpSize()
                SizeClass.calculateFromSize(secondSize)
            }
            assertThat(actualSizeClass).isEqualTo(expectedSizeClass)

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