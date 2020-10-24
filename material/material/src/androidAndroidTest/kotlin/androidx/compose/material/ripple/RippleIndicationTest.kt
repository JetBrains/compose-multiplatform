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

package androidx.compose.material.ripple

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.GOLDEN_MATERIAL
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.ui.test.ComposeTestRuleJUnit
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterialApi::class)
class RippleIndicationTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun bounded_lightTheme_highLuminance_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.White

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = true,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_bounded_light_highluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.24f)
        )
    }

    @Test
    fun bounded_lightTheme_highLuminance_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.White

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = true,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_bounded_light_highluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.16f)
        )
    }

    @Test
    fun bounded_lightTheme_lowLuminance_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = true,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_bounded_light_lowluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun bounded_lightTheme_lowLuminance_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = true,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_bounded_light_lowluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun bounded_darkTheme_highLuminance_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.White

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = true,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_bounded_dark_highluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun bounded_darkTheme_highLuminance_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.White

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = true,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_bounded_dark_highluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun bounded_darkTheme_lowLuminance_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = true,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_bounded_dark_lowluminance_pressed",
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun bounded_darkTheme_lowLuminance_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = true,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_bounded_dark_lowluminance_dragged",
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_lightTheme_highLuminance_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.White

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = false,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_unbounded_light_highluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.24f)
        )
    }

    @Test
    fun unbounded_lightTheme_highLuminance_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.White

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = false,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_unbounded_light_highluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.16f)
        )
    }

    @Test
    fun unbounded_lightTheme_lowLuminance_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = false,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_unbounded_light_lowluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun unbounded_lightTheme_lowLuminance_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = false,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_unbounded_light_lowluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_darkTheme_highLuminance_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.White

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = false,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_unbounded_dark_highluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun unbounded_darkTheme_highLuminance_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.White

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = false,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_unbounded_dark_highluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_darkTheme_lowLuminance_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = false,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_unbounded_dark_lowluminance_pressed",
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun unbounded_darkTheme_lowLuminance_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        rule.setRippleContent(
            interactionState = interactionState,
            bounded = false,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_unbounded_dark_lowluminance_dragged",
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun customRippleTheme_pressed() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        val rippleColor = Color.Red
        val rippleAlpha = 0.5f

        val rippleTheme = object : RippleTheme {
            @Composable
            override fun defaultColor() = rippleColor

            @Composable
            override fun rippleOpacity() = object : RippleOpacity {
                override fun opacityForInteraction(interaction: Interaction) = rippleAlpha
            }
        }

        rule.setContent {
            Providers(AmbientRippleTheme provides rippleTheme) {
                MaterialTheme {
                    Surface(contentColor = contentColor) {
                        Box(Modifier.fillMaxSize(), alignment = Alignment.Center) {
                            RippleBox(interactionState, RippleIndication())
                        }
                    }
                }
            }
        }

        val expectedColor = calculateResultingRippleColor(rippleColor, rippleOpacity = rippleAlpha)

        assertRippleMatches(
            interactionState,
            Interaction.Pressed,
            "rippleindication_customtheme_pressed",
            expectedColor
        )
    }

    @Test
    fun customRippleTheme_dragged() {
        val interactionState = InteractionState()

        val contentColor = Color.Black

        val rippleColor = Color.Red
        val rippleAlpha = 0.5f

        val rippleTheme = object : RippleTheme {
            @Composable
            override fun defaultColor() = rippleColor

            @Composable
            override fun rippleOpacity() = object : RippleOpacity {
                override fun opacityForInteraction(interaction: Interaction) = rippleAlpha
            }
        }

        rule.setContent {
            Providers(AmbientRippleTheme provides rippleTheme) {
                MaterialTheme {
                    Surface(contentColor = contentColor) {
                        Box(Modifier.fillMaxSize(), alignment = Alignment.Center) {
                            RippleBox(interactionState, RippleIndication())
                        }
                    }
                }
            }
        }

        val expectedColor = calculateResultingRippleColor(rippleColor, rippleOpacity = rippleAlpha)

        assertRippleMatches(
            interactionState,
            Interaction.Dragged,
            "rippleindication_customtheme_dragged",
            expectedColor
        )
    }

    @Test
    fun themeChangeDuringRipple() {
        val interactionState = InteractionState()

        fun createRippleTheme(color: Color, alpha: Float) = object : RippleTheme {
            @Composable
            override fun defaultColor() = color

            @Composable
            override fun rippleOpacity() = object : RippleOpacity {
                override fun opacityForInteraction(interaction: Interaction) = alpha
            }
        }

        val initialColor = Color.Red
        val initialAlpha = 0.5f

        var rippleTheme by mutableStateOf(createRippleTheme(initialColor, initialAlpha))

        rule.setContent {
            Providers(AmbientRippleTheme provides rippleTheme) {
                MaterialTheme {
                    Surface(contentColor = Color.Black) {
                        Box(Modifier.fillMaxSize(), alignment = Alignment.Center) {
                            RippleBox(interactionState, RippleIndication())
                        }
                    }
                }
            }
        }

        rule.runOnUiThread {
            interactionState.addInteraction(Interaction.Pressed, Offset(10f, 10f))
        }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel = captureToBitmap()
                .run {
                    getPixel(width / 2, height / 2)
                }

            val expectedColor =
                calculateResultingRippleColor(initialColor, rippleOpacity = initialAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        val newColor = Color.Green
        val newAlpha = 0.2f

        rule.runOnUiThread {
            rippleTheme = createRippleTheme(newColor, newAlpha)
        }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel = captureToBitmap()
                .run {
                    getPixel(width / 2, height / 2)
                }

            val expectedColor =
                calculateResultingRippleColor(newColor, rippleOpacity = newAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    /**
     * Asserts that the ripple matches the screenshot with identifier [goldenIdentifier], and
     * that the resultant color of the ripple on screen matches [expectedCenterPixelColor].
     *
     * @param interactionState the [InteractionState] driving the ripple
     * @param interaction the [Interaction] to assert for
     * @param goldenIdentifier the identifier for the corresponding screenshot
     * @param expectedCenterPixelColor the expected color for the pixel at the center of the
     * [RippleBox]
     */
    private fun assertRippleMatches(
        interactionState: InteractionState,
        interaction: Interaction,
        goldenIdentifier: String,
        expectedCenterPixelColor: Color
    ) {
        rule.clockTestRule.pauseClock()

        // Start ripple
        rule.runOnUiThread {
            if (interaction is Interaction.Pressed) {
                interactionState.addInteraction(interaction, Offset(10f, 10f))
            } else {
                interactionState.addInteraction(interaction)
            }
        }

        // Advance to somewhere in the middle of the animation for a ripple, or at the end of a
        // state layer transition
        rule.waitForIdle()
        rule.clockTestRule.advanceClock(50)

        // Capture and compare screenshots
        rule.onNodeWithTag(Tag)
            .captureToBitmap()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)

        // Advance until after the end of the ripple animation, so we have a stable final opacity
        rule.waitForIdle()
        rule.clockTestRule.advanceClock(50)
        rule.waitForIdle()

        // Compare expected and actual pixel color
        val centerPixel = rule.onNodeWithTag(Tag)
            .captureToBitmap()
            .run {
                getPixel(width / 2, height / 2)
            }

        Truth.assertThat(Color(centerPixel)).isEqualTo(expectedCenterPixelColor)
    }
}

/**
 * Generic Button like component that allows injecting a [RippleIndication] and also includes
 * padding around the rippled surface, so screenshots will include some dead space for clarity.
 *
 * @param interactionState the [InteractionState] that is used to drive the ripple state
 * @param rippleIndication [RippleIndication] placed inside the surface
 */
@Composable
private fun RippleBox(interactionState: InteractionState, rippleIndication: RippleIndication) {
    Box(Modifier.semantics(mergeAllDescendants = true) {}.testTag(Tag)) {
        Surface(
            Modifier.padding(25.dp),
            color = RippleBoxBackgroundColor, shape = RoundedCornerShape(20)
        ) {
            Box(
                Modifier.preferredWidth(80.dp).preferredHeight(50.dp).indication(
                    interactionState = interactionState,
                    indication = rippleIndication
                )
            )
        }
    }
}

/**
 * Sets the content to a [RippleBox] with a [MaterialTheme] and surrounding [Surface]
 *
 * @param interactionState [InteractionState] used to drive the ripple inside the [RippleBox]
 * @param bounded whether the ripple inside the [RippleBox] is bounded
 * @param lightTheme whether the theme is light or dark
 * @param contentColor the contentColor that will be used for the ripple color
 */
private fun ComposeTestRuleJUnit.setRippleContent(
    interactionState: InteractionState,
    bounded: Boolean,
    lightTheme: Boolean,
    contentColor: Color
) {
    setContent {
        val colors = if (lightTheme) lightColors() else darkColors()

        MaterialTheme(colors) {
            Surface(contentColor = contentColor) {
                Box(Modifier.fillMaxSize(), alignment = Alignment.Center) {
                    RippleBox(interactionState, RippleIndication(bounded))
                }
            }
        }
    }
}

/**
 * Blends ([contentColor] with [rippleOpacity]) on top of [RippleBoxBackgroundColor] to provide
 * the resulting RGB color that can be used for pixel comparison.
 */
private fun calculateResultingRippleColor(
    contentColor: Color,
    rippleOpacity: Float
) = contentColor.copy(alpha = rippleOpacity).compositeOver(RippleBoxBackgroundColor)

private val RippleBoxBackgroundColor = Color.Blue

private const val Tag = "Ripple"