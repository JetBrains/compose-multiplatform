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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
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
import androidx.compose.ui.draw.clip
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
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun bounded_lightTheme_pressed() {
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
            "ripple_bounded_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun bounded_lightTheme_hovered() {
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
            HoverInteraction.Enter(),
            "ripple_bounded_hovered",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun bounded_lightTheme_focused() {
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
            FocusInteraction.Focus(),
            "ripple_bounded_focused",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun bounded_lightTheme_dragged() {
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
            "ripple_bounded_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.16f)
        )
    }

    @Test
    fun unbounded_lightTheme_pressed() {
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
            "ripple_unbounded_pressed",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun unbounded_lightTheme_hovered() {
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
            HoverInteraction.Enter(),
            "ripple_unbounded_hovered",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.08f)
        )
    }

    @Test
    fun unbounded_lightTheme_focused() {
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
            FocusInteraction.Focus(),
            "ripple_unbounded_focused",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.12f)
        )
    }

    @Test
    fun unbounded_lightTheme_dragged() {
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
            "ripple_unbounded_dragged",
            calculateResultingRippleColor(contentColor, rippleOpacity = 0.16f)
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
                            RippleBoxWithBackground(
                                interactionSource,
                                rememberRipple(),
                                bounded = true
                            )
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
    fun customRippleTheme_hovered() {
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
                            RippleBoxWithBackground(
                                interactionSource,
                                rememberRipple(),
                                bounded = true
                            )
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
            HoverInteraction.Enter(),
            "ripple_customtheme_hovered",
            expectedColor
        )
    }

    @Test
    fun customRippleTheme_focused() {
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
                            RippleBoxWithBackground(
                                interactionSource,
                                rememberRipple(),
                                bounded = true
                            )
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
            FocusInteraction.Focus(),
            "ripple_customtheme_focused",
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
                            RippleBoxWithBackground(
                                interactionSource,
                                rememberRipple(),
                                bounded = true
                            )
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

    /**
     * Note: no corresponding test for pressed ripples since RippleForeground does not update the
     * color of currently active ripples unless they are being drawn on the UI thread
     * (which should only happen if the target radius also changes).
     */
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
                            RippleBoxWithBackground(
                                interactionSource,
                                rememberRipple(),
                                bounded = true
                            )
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
                                RippleBoxWithBackground(interactionSource, ripple, bounded = true)
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            scope!!.launch {
                interactionSource.emit(PressInteraction.Press(Offset(10f, 10f)))
            }
        }

        rule.waitForIdle()
        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

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
     * [RippleBoxWithBackground]
     */
    private fun assertRippleMatches(
        scope: CoroutineScope,
        interactionSource: MutableInteractionSource,
        interaction: Interaction,
        goldenIdentifier: String,
        expectedCenterPixelColor: Color
    ) {
        // Pause the clock if we are drawing a state layer
        if (interaction !is PressInteraction) {
            rule.mainClock.autoAdvance = false
        }

        // Start ripple
        rule.runOnIdle {
            scope.launch {
                interactionSource.emit(interaction)
            }
        }

        // Advance to the end of the ripple / state layer animation
        rule.waitForIdle()
        if (interaction is PressInteraction) {
            // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
            // synchronization. Instead just wait until after the ripples are finished animating.
            Thread.sleep(300)
        } else {
            rule.mainClock.advanceTimeBy(milliseconds = 300)
        }

        // Capture and compare screenshots
        val screenshot = rule.onNodeWithTag(Tag)
            .captureToImage()

        screenshot.assertAgainstGolden(screenshotRule, goldenIdentifier)

        // Compare expected and actual pixel color
        val centerPixel = screenshot
            .asAndroidBitmap()
            .run {
                getPixel(width / 2, height / 2)
            }

        Truth.assertThat(Color(centerPixel)).isEqualTo(expectedCenterPixelColor)
    }
}

/**
 * Generic Button like component with a border that allows injecting an [Indication], and has a
 * background with the same color around it - this makes the ripple contrast better and make it
 * more visible in screenshots.
 *
 * @param interactionSource the [MutableInteractionSource] that is used to drive the ripple state
 * @param ripple ripple [Indication] placed inside the surface
 * @param bounded whether [ripple] is bounded or not - this controls the clipping behavior
 */
@Composable
private fun RippleBoxWithBackground(
    interactionSource: MutableInteractionSource,
    ripple: Indication,
    bounded: Boolean
) {
    Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
        Surface(
            Modifier.padding(25.dp),
            color = RippleBoxBackgroundColor
        ) {
            val shape = RoundedCornerShape(20)
            // If the ripple is bounded, we want to clip to the shape, otherwise don't clip as
            // the ripple should draw outside the bounds.
            val clip = if (bounded) Modifier.clip(shape) else Modifier
            Box(
                Modifier.padding(25.dp).width(40.dp).height(40.dp)
                    .border(BorderStroke(2.dp, Color.Black), shape)
                    .background(color = RippleBoxBackgroundColor, shape = shape)
                    .then(clip)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple
                    )
            ) {}
        }
    }
}

/**
 * Sets the content to a [RippleBoxWithBackground] with a [MaterialTheme] and surrounding [Surface]
 *
 * @param interactionSource [MutableInteractionSource] used to drive the ripple inside the
 * [RippleBoxWithBackground]
 * @param bounded whether the ripple inside the [RippleBoxWithBackground] is bounded
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
        val colors = if (lightTheme) lightColorScheme() else darkColorScheme()

        MaterialTheme(colors) {
            Surface(contentColor = contentColor) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RippleBoxWithBackground(interactionSource, rememberRipple(bounded), bounded)
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