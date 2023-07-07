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

package androidx.compose.ui.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LayoutNodeDensityTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun compositionLocalDensityChangeTriggersRemeasure() {
        var localDensity by mutableStateOf(5f)
        var localFontScale by mutableStateOf(7f)

        var measureScopeDensity = 0f
        var measureScopeFontScale = 0f
        val measurePolicy = MeasurePolicy { _, _ ->
            measureScopeDensity = density
            measureScopeFontScale = fontScale
            layout(0, 0) {}
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(localDensity, localFontScale)) {
                Layout({}, measurePolicy = measurePolicy)
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(localDensity, measureScopeDensity)
            Assert.assertEquals(localFontScale, measureScopeFontScale)
            localDensity = 9f
            localFontScale = 11f
        }

        rule.runOnIdle {
            Assert.assertEquals(localDensity, measureScopeDensity)
            Assert.assertEquals(localFontScale, measureScopeFontScale)
        }
    }

    @Test
    fun compositionLocalDensityChangeTriggersRedraw() {
        var localDensity by mutableStateOf(5f)
        var localFontScale by mutableStateOf(7f)

        var drawScopeDensity = 0f
        var drawScopeFontScale = 0f
        val drawBlock: DrawScope.() -> Unit = {
            drawScopeDensity = density
            drawScopeFontScale = fontScale
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(localDensity, localFontScale)) {
                Canvas(Modifier.fillMaxSize(), onDraw = drawBlock)
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(localDensity, drawScopeDensity)
            Assert.assertEquals(localFontScale, drawScopeFontScale)
            localDensity = 9f
            localFontScale = 11f
        }

        rule.runOnIdle {
            Assert.assertEquals(localDensity, drawScopeDensity)
            Assert.assertEquals(localFontScale, drawScopeFontScale)
        }
    }

    @Test
    fun compositionLocalDensityChangeTriggersRedrawLayerBeforeDraw() {
        var localDensity by mutableStateOf(5f)
        var localFontScale by mutableStateOf(7f)

        var drawScopeDensity = 0f
        var drawScopeFontScale = 0f
        val drawBlock: DrawScope.() -> Unit = {
            drawScopeDensity = density
            drawScopeFontScale = fontScale
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(localDensity, localFontScale)) {
                Spacer(Modifier.fillMaxSize().graphicsLayer().drawBehind(drawBlock))
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(localDensity, drawScopeDensity)
            Assert.assertEquals(localFontScale, drawScopeFontScale)
            localDensity = 9f
            localFontScale = 11f
        }

        rule.runOnIdle {
            Assert.assertEquals(localDensity, drawScopeDensity)
            Assert.assertEquals(localFontScale, drawScopeFontScale)
        }
    }

    @Test
    fun compositionLocalDensityChangeTriggersRedrawLayerAfterDraw() {
        var localDensity by mutableStateOf(5f)
        var localFontScale by mutableStateOf(7f)

        var drawScopeDensity = 0f
        var drawScopeFontScale = 0f
        val drawBlock: DrawScope.() -> Unit = {
            drawScopeDensity = density
            drawScopeFontScale = fontScale
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(localDensity, localFontScale)) {
                Spacer(Modifier.fillMaxSize().drawBehind(drawBlock).graphicsLayer())
            }
        }

        rule.runOnIdle {
            Assert.assertEquals(localDensity, drawScopeDensity)
            Assert.assertEquals(localFontScale, drawScopeFontScale)
            localDensity = 9f
            localFontScale = 11f
        }

        rule.runOnIdle {
            Assert.assertEquals(localDensity, drawScopeDensity)
            Assert.assertEquals(localFontScale, drawScopeFontScale)
        }
    }

    @Test
    fun densityChangeRequestsLayerOutlineUpdate() {
        var density by mutableStateOf(Density(2f))
        var lastOutlineDensity: Density? = null
        val shape = object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                lastOutlineDensity = density
                return Outline.Rectangle(size.toRect())
            }
        }
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides density
            ) {
                Box(
                    Modifier
                        .layout { measurable, _ ->
                            val placeable = measurable.measure(Constraints.fixed(100, 100))
                            layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                        .graphicsLayer(shape = shape, clip = true)
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(lastOutlineDensity).isEqualTo(density)
            density = Density(2.5f)
        }

        rule.runOnIdle {
            Truth.assertThat(lastOutlineDensity).isEqualTo(density)
        }
    }
}
