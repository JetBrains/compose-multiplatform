/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertPixelColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(Parameterized::class)
class ElevationOverlayTest(private val elevation: Dp?, overlayAlpha: Float?) {
    private val expectedOverlayAlpha = overlayAlpha!!

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        // Mappings for elevation -> expected alpha for the overlay color in dark theme
        fun initElevation(): Array<Any> = arrayOf(
            arrayOf(0.dp, 0f, null),
            arrayOf(1.dp, 0.05f, null),
            arrayOf(2.dp, 0.07f, null),
            arrayOf(3.dp, 0.08f, null),
            arrayOf(4.dp, 0.09f, null),
            arrayOf(6.dp, 0.11f, null),
            arrayOf(8.dp, 0.12f, null),
            arrayOf(12.dp, 0.14f, null),
            arrayOf(16.dp, 0.15f, null),
            arrayOf(24.dp, 0.16f, null)
        )
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun correctElevationOverlayInDarkTheme() {
        val colors = darkColors()

        rule.setContent {
            TestSurface(elevation!!, colors)
        }

        val expectedSurfaceColor = calculateTestSurfaceColor(
            surfaceColor = colors.surface,
            foregroundColor = colors.onSurface
        )

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .toPixelMap()
            .assertPixelColor(
                expected = expectedSurfaceColor,
                x = SurfaceSize.width / 2,
                y = SurfaceSize.height / 2
            )
    }

    @Test
    fun correctElevationOverlayInDarkTheme_withParentSurface() {
        val colors = darkColors()

        rule.setContent {
            Surface(elevation = 2.dp) {
                // The total overlay should be 2 + 2 = 4.dp
                TestSurface(2.dp, colors)
            }
        }

        val expectedSurfaceColor = colors.onSurface
            .copy(alpha = 0.09f)
            .compositeOver(colors.surface)

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .toPixelMap()
            .assertPixelColor(
                expected = expectedSurfaceColor,
                x = SurfaceSize.width / 2,
                y = SurfaceSize.height / 2
            )
    }

    @Test
    fun correctElevationOverlayWithCustomContentColor() {
        val customContentColor = Color.Blue

        val customColors = darkColors(
            onBackground = customContentColor,
            onSurface = customContentColor
        )

        rule.setContent {
            TestSurface(elevation!!, customColors)
        }

        val expectedSurfaceColor = calculateTestSurfaceColor(
            surfaceColor = customColors.surface,
            foregroundColor = customContentColor
        )

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .toPixelMap()
            .assertPixelColor(
                expected = expectedSurfaceColor,
                x = SurfaceSize.width / 2,
                y = SurfaceSize.height / 2
            )
    }

    @Test
    fun noChangesInLightTheme() {
        val colors = lightColors()

        rule.setContent {
            TestSurface(elevation!!, lightColors())
        }

        // No overlay should be applied in light theme
        val expectedSurfaceColor = colors.surface

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .toPixelMap()
            .assertPixelColor(
                expected = expectedSurfaceColor,
                x = SurfaceSize.width / 2,
                y = SurfaceSize.height / 2
            )
    }

    @Test
    fun noChangesIfNullElevationOverlay() {
        val colors = darkColors()

        rule.setContent {
            // Turn off overlay behavior
            CompositionLocalProvider(LocalElevationOverlay provides null) {
                TestSurface(elevation!!, colors)
            }
        }

        // No overlay should be applied
        val expectedSurfaceColor = colors.surface

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .toPixelMap()
            .assertPixelColor(
                expected = expectedSurfaceColor,
                x = SurfaceSize.width / 2,
                y = SurfaceSize.height / 2
            )
    }

    @Test
    fun customElevationOverlay() {
        val customOverlayColor = Color.Red

        val customOverlay = object : ElevationOverlay {
            @Composable
            override fun apply(color: Color, elevation: Dp): Color = customOverlayColor
        }

        rule.setContent {
            CompositionLocalProvider(LocalElevationOverlay provides customOverlay) {
                TestSurface(elevation!!, lightColors())
            }
        }

        rule.onNodeWithTag(Tag)
            .captureToImage()
            .toPixelMap()
            .assertPixelColor(
                expected = customOverlayColor,
                x = SurfaceSize.width / 2,
                y = SurfaceSize.height / 2
            )
    }

    /**
     * @return the resulting color from compositing [foregroundColor] with [expectedOverlayAlpha]
     * over [surfaceColor].
     */
    private fun calculateTestSurfaceColor(surfaceColor: Color, foregroundColor: Color): Color {
        return foregroundColor.copy(expectedOverlayAlpha).compositeOver(surfaceColor)
    }
}

@Composable
private fun TestSurface(elevation: Dp, colors: Colors) {
    MaterialTheme(colors) {
        Box {
            Surface(elevation = elevation) {
                with(LocalDensity.current) {
                    // Make the surface size small so we compare less pixels
                    Box(
                        Modifier.size(
                            SurfaceSize.width.toDp(),
                            SurfaceSize.height.toDp()
                        ).testTag(Tag)
                    )
                }
            }
        }
    }
}

private const val Tag = "Surface"
private val SurfaceSize = IntSize(10, 10)
