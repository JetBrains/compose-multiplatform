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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for the [RippleTheme] provided by [MaterialTheme], to verify colors and opacity in
 * different configurations.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class MaterialRippleThemeTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun bounded_lightTheme_highLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = true,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_bounded_light_highluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.24f)
        )
    }

    @Test
    fun bounded_lightTheme_highLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = true,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            "ripple_bounded_light_highluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.16f)
        )
    }

    @Test
    fun bounded_lightTheme_lowLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = true,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_bounded_light_lowluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun bounded_lightTheme_lowLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = true,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            "ripple_bounded_light_lowluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun bounded_darkTheme_highLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = true,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_bounded_dark_highluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun bounded_darkTheme_highLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = true,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            "ripple_bounded_dark_highluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun bounded_darkTheme_lowLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = true,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_bounded_dark_lowluminance_pressed",
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun bounded_darkTheme_lowLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = true,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            "ripple_bounded_dark_lowluminance_dragged",
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_lightTheme_highLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = false,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_unbounded_light_highluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.24f)
        )
    }

    @Test
    fun unbounded_lightTheme_highLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = false,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            "ripple_unbounded_light_highluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.16f)
        )
    }

    @Test
    fun unbounded_lightTheme_lowLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = false,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_unbounded_light_lowluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun unbounded_lightTheme_lowLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = false,
            lightTheme = true,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            "ripple_unbounded_light_lowluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_darkTheme_highLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = false,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_unbounded_dark_highluminance_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun unbounded_darkTheme_highLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.White

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = false,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            "ripple_unbounded_dark_highluminance_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_darkTheme_lowLuminance_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = false,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_unbounded_dark_lowluminance_pressed",
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.10f)
        )
    }

    @Test
    fun unbounded_darkTheme_lowLuminance_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val scope = rule.setRippleContent(
            interactionSource = interactionSource,
            bounded = false,
            lightTheme = false,
            contentColor = contentColor
        )

        assertRippleMatches(
            scope,
            interactionSource,
            DragInteraction.Start(),
            "ripple_unbounded_dark_lowluminance_dragged",
            // Low luminance content in dark theme should use a white ripple by default
            calculateResultingRippleColor(Color.White, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun customRippleTheme_pressed() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val rippleColor = Color.Red
        val expectedAlpha = 0.5f
        val rippleAlpha = RippleAlpha(expectedAlpha, expectedAlpha, expectedAlpha, expectedAlpha)

        val rippleTheme = object : RippleTheme {
            @Composable
            override fun defaultColor() = rippleColor

            @Composable
            override fun rippleAlpha() = rippleAlpha
        }

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                CompositionLocalProvider(LocalRippleTheme provides rippleTheme) {
                    Surface(contentColor = contentColor) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBox(interactionSource, rememberRipple())
                        }
                    }
                }
            }
        }

        val expectedColor = calculateResultingRippleColor(
            rippleColor,
            rippleOpacity = expectedAlpha
        )

        assertRippleMatches(
            scope!!,
            interactionSource,
            PressInteraction.Press(Offset(10f, 10f)),
            "ripple_customtheme_pressed",
            expectedColor
        )
    }

    @Test
    fun customRippleTheme_dragged() {
        val interactionSource = MutableInteractionSource()

        val contentColor = Color.Black

        val rippleColor = Color.Red
        val expectedAlpha = 0.5f
        val rippleAlpha = RippleAlpha(expectedAlpha, expectedAlpha, expectedAlpha, expectedAlpha)

        val rippleTheme = object : RippleTheme {
            @Composable
            override fun defaultColor() = rippleColor
            @Composable
            override fun rippleAlpha() = rippleAlpha
        }

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                CompositionLocalProvider(LocalRippleTheme provides rippleTheme) {
                    Surface(contentColor = contentColor) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBox(interactionSource, rememberRipple())
                        }
                    }
                }
            }
        }

        val expectedColor = calculateResultingRippleColor(
            rippleColor,
            rippleOpacity = expectedAlpha
        )

        assertRippleMatches(
            scope!!,
            interactionSource,
            DragInteraction.Start(),
            "ripple_customtheme_dragged",
            expectedColor
        )
    }

    @Test
    fun themeChangeDuringRipple_dragged() {
        val interactionSource = MutableInteractionSource()

        fun createRippleTheme(color: Color, alpha: Float) = object : RippleTheme {
            val rippleAlpha = RippleAlpha(alpha, alpha, alpha, alpha)
            @Composable
            override fun defaultColor() = color

            @Composable
            override fun rippleAlpha() = rippleAlpha
        }

        val initialColor = Color.Red
        val initialAlpha = 0.5f

        var rippleTheme by mutableStateOf(createRippleTheme(initialColor, initialAlpha))

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                CompositionLocalProvider(LocalRippleTheme provides rippleTheme) {
                    Surface(contentColor = Color.Black) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBox(interactionSource, rememberRipple())
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            scope!!.launch {
                interactionSource.emit(DragInteraction.Start())
            }
        }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel = captureToImage().asAndroidBitmap()
                .run {
                    getPixel(width / 2, height / 2)
                }

            val expectedColor =
                calculateResultingRippleColor(initialColor, rippleOpacity = initialAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }

        val newColor = Color.Green
        // TODO: changing alpha for existing state layers is not currently supported
        val newAlpha = 0.5f

        rule.runOnUiThread {
            rippleTheme = createRippleTheme(newColor, newAlpha)
        }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel = captureToImage().asAndroidBitmap()
                .run {
                    getPixel(width / 2, height / 2)
                }

            val expectedColor =
                calculateResultingRippleColor(newColor, rippleOpacity = newAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    @Test
    fun themeChangeDuringRipple_pressed() {
        val interactionSource = MutableInteractionSource()

        fun createRippleTheme(color: Color, alpha: Float) = object : RippleTheme {
            val rippleAlpha = RippleAlpha(alpha, alpha, alpha, alpha)
            @Composable
            override fun defaultColor() = color

            @Composable
            override fun rippleAlpha() = rippleAlpha
        }

        val initialColor = Color.Red
        val initialAlpha = 0.5f

        var rippleTheme by mutableStateOf(createRippleTheme(initialColor, initialAlpha))

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                CompositionLocalProvider(LocalRippleTheme provides rippleTheme) {
                    Surface(contentColor = Color.Black) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RippleBox(interactionSource, rememberRipple())
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            scope!!.launch {
                interactionSource.emit(PressInteraction.Press(Offset.Zero))
            }
        }
        rule.waitForIdle()

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel = captureToImage().asAndroidBitmap()
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
            val centerPixel = captureToImage().asAndroidBitmap()
                .run {
                    getPixel(width / 2, height / 2)
                }

            val expectedColor =
                calculateResultingRippleColor(newColor, rippleOpacity = newAlpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    @Test
    fun contentColorProvidedAfterRememberRipple() {
        val interactionSource = MutableInteractionSource()

        val alpha = 0.5f
        val rippleAlpha = RippleAlpha(alpha, alpha, alpha, alpha)
        val expectedRippleColor = Color.Red

        val theme = object : RippleTheme {
            @Composable
            override fun defaultColor() = LocalContentColor.current

            @Composable
            override fun rippleAlpha() = rippleAlpha
        }

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            MaterialTheme {
                CompositionLocalProvider(LocalRippleTheme provides theme) {
                    Surface(contentColor = Color.Black) {
                        // Create ripple where contentColor is black
                        val ripple = rememberRipple()
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(contentColor = expectedRippleColor) {
                                // Ripple is used where contentColor is red, so the instance
                                // should get the red color when it is created
                                RippleBox(interactionSource, ripple)
                            }
                        }
                    }
                }
            }
        }

        rule.runOnUiThread {
            scope!!.launch {
                interactionSource.emit(PressInteraction.Press(Offset(10f, 10f)))
            }
        }

        with(rule.onNodeWithTag(Tag)) {
            val centerPixel = captureToImage().asAndroidBitmap()
                .run {
                    getPixel(width / 2, height / 2)
                }

            val expectedColor =
                calculateResultingRippleColor(expectedRippleColor, rippleOpacity = alpha)

            Truth.assertThat(Color(centerPixel)).isEqualTo(expectedColor)
        }
    }

    /**
     * Asserts that the ripple matches the screenshot with identifier [goldenIdentifier], and
     * that the resultant color of the ripple on screen matches [expectedCenterPixelColor].
     *
     * @param interactionSource the [MutableInteractionSource] driving the ripple
     * @param interaction the [Interaction] to assert for
     * @param goldenIdentifier the identifier for the corresponding screenshot
     * @param expectedCenterPixelColor the expected color for the pixel at the center of the
     * [RippleBox]
     */
    private fun assertRippleMatches(
        scope: CoroutineScope,
        interactionSource: MutableInteractionSource,
        interaction: Interaction,
        goldenIdentifier: String,
        expectedCenterPixelColor: Color
    ) {
        rule.mainClock.autoAdvance = false

        // Start ripple
        scope.launch {
            interactionSource.emit(interaction)
        }

        // Advance to somewhere in the middle of the animation for a ripple, or at the end of a
        // state layer transition
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(milliseconds = 50)

        // Capture and compare screenshots
        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)

        // Advance until after the end of the ripple animation, so we have a stable final opacity
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(milliseconds = 50)

        // Compare expected and actual pixel color
        val centerPixel = rule.onNodeWithTag(Tag)
            .captureToImage().asAndroidBitmap()
            .run {
                getPixel(width / 2, height / 2)
            }

        Truth.assertThat(Color(centerPixel)).isEqualTo(expectedCenterPixelColor)
    }
}

/**
 * Generic Button like component that allows injecting an [Indication] and also includes
 * padding around the rippled surface, so screenshots will include some dead space for clarity.
 *
 * @param interactionSource the [MutableInteractionSource] that is used to drive the ripple state
 * @param ripple ripple [Indication] placed inside the surface
 */
@Composable
private fun RippleBox(interactionSource: MutableInteractionSource, ripple: Indication) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        Surface(
            Modifier.padding(25.dp),
            color = RippleBoxBackgroundColor, shape = RoundedCornerShape(20)
        ) {
            Box(
                Modifier.width(80.dp).height(50.dp).indication(
                    interactionSource = interactionSource,
                    indication = ripple
                )
            )
        }
    }
}

/**
 * Sets the content to a [RippleBox] with a [MaterialTheme] and surrounding [Surface]
 *
 * @param interactionSource [MutableInteractionSource] used to drive the ripple inside the [RippleBox]
 * @param bounded whether the ripple inside the [RippleBox] is bounded
 * @param lightTheme whether the theme is light or dark
 * @param contentColor the contentColor that will be used for the ripple color
 */
private fun ComposeContentTestRule.setRippleContent(
    interactionSource: MutableInteractionSource,
    bounded: Boolean,
    lightTheme: Boolean,
    contentColor: Color
): CoroutineScope {
    var scope: CoroutineScope? = null

    setContent {
        scope = rememberCoroutineScope()
        val colors = if (lightTheme) lightColors() else darkColors()

        MaterialTheme(colors) {
            Surface(contentColor = contentColor) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBox(interactionSource, rememberRipple(bounded))
                }
            }
        }
    }
    waitForIdle()
    return scope!!
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