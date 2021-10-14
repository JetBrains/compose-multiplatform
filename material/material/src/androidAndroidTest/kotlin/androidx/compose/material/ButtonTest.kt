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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@MediumTest
@RunWith(AndroidJUnit4::class)
class ButtonTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent {
            Box {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}) {
                    Text("myButton")
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
    }

    @Test
    fun disabledSemantics() {
        rule.setMaterialContent {
            Box {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}, enabled = false) {
                    Text("myButton")
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        rule.setMaterialContent {
            Box {
                Button(onClick = onClick) {
                    Text(text)
                }
            }
        }

        // TODO(b/129400818): this actually finds the text, not the button as
        // merge semantics aren't implemented yet
        rule.onNodeWithText(text)
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun canBeDisabled() {
        val tag = "myButton"

        rule.setMaterialContent {
            var enabled by remember { mutableStateOf(true) }
            val onClick = { enabled = false }
            Box {
                Button(modifier = Modifier.testTag(tag), onClick = onClick, enabled = enabled) {
                    Text("Hello")
                }
            }
        }
        rule.onNodeWithTag(tag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun clickIsIndependentBetweenButtons() {
        var button1Counter = 0
        val button1OnClick: () -> Unit = { ++button1Counter }
        val button1Tag = "button1"

        var button2Counter = 0
        val button2OnClick: () -> Unit = { ++button2Counter }
        val button2Tag = "button2"

        val text = "myButton"

        rule.setMaterialContent {
            Column {
                Button(modifier = Modifier.testTag(button1Tag), onClick = button1OnClick) {
                    Text(text)
                }
                Button(modifier = Modifier.testTag(button2Tag), onClick = button2OnClick) {
                    Text(text)
                }
            }
        }

        rule.onNodeWithTag(button1Tag)
            .performClick()

        rule.runOnIdle {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(0)
        }

        rule.onNodeWithTag(button2Tag)
            .performClick()

        rule.runOnIdle {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(1)
        }
    }

    @Test
    fun buttonHeightIsFromSpec(): Unit = with(rule.density) {
        if (rule.density.fontScale > 1f) {
            // This test can be reasonable failing on the non default font scales
            // so lets skip it.
            return
        }
        rule.setMaterialContent {
            Button(onClick = {}) {
                Text("Test button")
            }
        }

        rule.onNode(hasClickAction())
            .getBoundsInRoot().height.assertIsEqualTo(36.dp, "height")
    }

    @Test
    fun ButtonWithLargeFontSizeIsLargerThenMinHeight() {
        rule.setMaterialContent {
            Button(onClick = {}) {
                Text(
                    text = "Test button",
                    fontSize = 50.sp
                )
            }
        }

        rule.onNode(hasClickAction())
            .assertHeightIsAtLeast(37.dp)
    }

    @Test
    fun containedButtonPropagateDefaultTextStyle() {
        rule.setMaterialContent {
            Button(onClick = {}) {
                assertThat(LocalTextStyle.current).isEqualTo(MaterialTheme.typography.button)
            }
        }
    }

    @Test
    fun outlinedButtonPropagateDefaultTextStyle() {
        rule.setMaterialContent {
            OutlinedButton(onClick = {}) {
                assertThat(LocalTextStyle.current).isEqualTo(MaterialTheme.typography.button)
            }
        }
    }

    @Test
    fun textButtonPropagateDefaultTextStyle() {
        rule.setMaterialContent {
            TextButton(onClick = {}) {
                assertThat(LocalTextStyle.current).isEqualTo(MaterialTheme.typography.button)
            }
        }
    }

    @Test
    fun containedButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(16.dp) { modifier, text ->
            Button(onClick = {}, modifier = modifier, content = text)
        }
    }

    @Test
    fun outlinedButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(16.dp) { modifier, text ->
            OutlinedButton(onClick = {}, modifier = modifier, content = text)
        }
    }

    @Test
    fun textButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(8.dp) { modifier, text ->
            TextButton(onClick = {}, modifier = modifier, content = text)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun shapeAndColorFromThemeIsUsed() {
        val shape = CutCornerShape(10.dp)
        var surface = Color.Transparent
        var primary = Color.Transparent
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            primary = MaterialTheme.colors.primary
            CompositionLocalProvider(LocalShapes provides Shapes(small = shape)) {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}, elevation = null) {
                    Box(Modifier.size(10.dp, 10.dp))
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = primary,
                backgroundColor = surface,
                shapeOverlapPixelCount = with(rule.density) { 1.dp.toPx() }
            )
    }

    @Test
    fun buttonContentColorIsCorrect() {
        var onPrimary = Color.Transparent
        var content = Color.Transparent
        rule.setMaterialContent {
            onPrimary = MaterialTheme.colors.onPrimary
            Button(onClick = {}) {
                content = LocalContentColor.current
            }
        }

        assertThat(content).isEqualTo(onPrimary)
    }

    @Test
    fun outlinedButtonContentColorIsCorrect() {
        var primary = Color.Transparent
        var content = Color.Transparent
        rule.setMaterialContent {
            primary = MaterialTheme.colors.primary
            OutlinedButton(onClick = {}) {
                content = LocalContentColor.current
            }
        }

        assertThat(content).isEqualTo(primary)
    }

    @Test
    fun textButtonContentColorIsCorrect() {
        var primary = Color.Transparent
        var content = Color.Transparent
        rule.setMaterialContent {
            primary = MaterialTheme.colors.primary
            TextButton(onClick = {}) {
                content = LocalContentColor.current
            }
        }

        assertThat(content).isEqualTo(primary)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun containedButtonDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        var onSurface = Color.Transparent
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            onSurface = MaterialTheme.colors.onSurface
            Button(
                modifier = Modifier.testTag("myButton"),
                onClick = {},
                enabled = false,
                shape = RectangleShape
            ) {}
        }

        rule.onNodeWithTag("myButton")
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = 0.dp,
                verticalPadding = 0.dp,
                backgroundColor = surface,
                shapeColor = onSurface.copy(alpha = 0.12f).compositeOver(surface)
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun containedButtonWithCustomColorDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        var onSurface = Color.Transparent
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            onSurface = MaterialTheme.colors.onSurface
            Button(
                modifier = Modifier.testTag("myButton"),
                onClick = {},
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red
                ),
                shape = RectangleShape
            ) {}
        }

        rule.onNodeWithTag("myButton")
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = 0.dp,
                verticalPadding = 0.dp,
                backgroundColor = surface,
                shapeColor = onSurface.copy(alpha = 0.12f).compositeOver(surface)
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun outlinedButtonDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            OutlinedButton(
                modifier = Modifier.testTag("myButton"),
                onClick = {},
                enabled = false,
                shape = RectangleShape,
                border = null
            ) {}
        }

        rule.onNodeWithTag("myButton")
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = 0.dp,
                verticalPadding = 0.dp,
                shape = RectangleShape,
                shapeColor = surface,
                backgroundColor = surface
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun textButtonDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            TextButton(
                modifier = Modifier.testTag("myButton"),
                onClick = {},
                enabled = false,
                shape = RectangleShape
            ) {}
        }

        rule.onNodeWithTag("myButton")
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = 0.dp,
                verticalPadding = 0.dp,
                shape = RectangleShape,
                shapeColor = surface,
                backgroundColor = surface
            )
    }

    @Test
    fun containedButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var disabledAlpha = 1f
        rule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            disabledAlpha = ContentAlpha.disabled
            Button(onClick = {}, enabled = false) {
                content = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            }
        }

        assertThat(content).isEqualTo(onSurface.copy(alpha = disabledAlpha))
    }

    @Test
    fun outlinedButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var disabledAlpha = 1f
        rule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            disabledAlpha = ContentAlpha.disabled
            OutlinedButton(onClick = {}, enabled = false) {
                content = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            }
        }

        assertThat(content).isEqualTo(onSurface.copy(alpha = disabledAlpha))
    }

    @Test
    fun textButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var disabledAlpha = 1f
        rule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            disabledAlpha = ContentAlpha.disabled
            TextButton(onClick = {}, enabled = false) {
                content = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            }
        }

        assertThat(content).isEqualTo(onSurface.copy(alpha = disabledAlpha))
    }

    @Test
    fun contentIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent {
            Box {
                Button({}, Modifier.onGloballyPositioned { buttonCoordinates = it }) {
                    Box(
                        Modifier.size(2.dp)
                            .onGloballyPositioned { contentCoordinates = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot()
            val contentBounds = contentCoordinates!!.boundsInRoot()
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(rule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.roundToPx().toFloat())
            }
            assertWithinOnePixel(buttonBounds.center, contentBounds.center)
        }
    }

    @Test
    fun minHeightAndMinWidthCanBeOverridden() {
        rule.setMaterialContent {
            Button(
                onClick = {},
                contentPadding = PaddingValues(),
                modifier = Modifier.requiredWidthIn(20.dp).requiredHeightIn(15.dp).testTag("button")
            ) {
                Spacer(Modifier.requiredSize(10.dp))
            }
        }

        rule.onNodeWithTag("button")
            .apply {
                with(getBoundsInRoot()) {
                    width.assertIsEqualTo(20.dp, "width")
                    height.assertIsEqualTo(15.dp, "height")
                }
            }
    }

    @Test
    fun weightModifierOnButton() {
        var item1Bounds = Rect(0f, 0f, 0f, 0f)
        var buttonBounds = Rect(0f, 0f, 0f, 0f)
        rule.setMaterialContent {
            Column {
                Spacer(
                    Modifier.requiredSize(10.dp).weight(1f).onGloballyPositioned {
                        item1Bounds = it.boundsInRoot()
                    }
                )

                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).onGloballyPositioned {
                        buttonBounds = it.boundsInRoot()
                    }
                ) {
                    Text("Button")
                }

                Spacer(Modifier.requiredSize(10.dp).weight(1f))
            }
        }

        assertThat(item1Bounds.top).isNotEqualTo(0f)
        assertThat(buttonBounds.left).isEqualTo(0f)
    }

    @Test
    fun buttonContentIsRow() {
        var buttonBounds = Rect(0f, 0f, 0f, 0f)
        var item1Bounds = Rect(0f, 0f, 0f, 0f)
        var item2Bounds = Rect(0f, 0f, 0f, 0f)
        rule.setMaterialContent {
            Button(
                onClick = {},
                modifier = Modifier.onGloballyPositioned {
                    buttonBounds = it.boundsInRoot()
                }
            ) {
                Spacer(
                    Modifier.requiredSize(10.dp).onGloballyPositioned {
                        item1Bounds = it.boundsInRoot()
                    }
                )
                Spacer(
                    Modifier.requiredWidth(10.dp).requiredHeight(5.dp).onGloballyPositioned {
                        item2Bounds = it.boundsInRoot()
                    }
                )
            }
        }

        assertThat(item1Bounds.center.y).isWithin(1f).of(buttonBounds.center.y)
        assertThat(item2Bounds.center.y).isWithin(1f).of(buttonBounds.center.y)
        assertThat(item1Bounds.right).isWithin(1f).of(buttonBounds.center.x)
        assertThat(item2Bounds.left).isWithin(1f).of(buttonBounds.center.x)
    }

    @Test
    fun buttonClickableInMinimumTouchTarget() {
        var clicked = false
        val tag = "button"
        rule.setMaterialContent {
            Box(Modifier.fillMaxSize()) {
                Button(
                    modifier = Modifier.testTag(tag).requiredSize(10.dp),
                    onClick = { clicked = !clicked }
                ) {
                    Box(Modifier.size(10.dp))
                }
            }
        }

        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .performTouchInput {
                click(Offset(-1f, -1f))
            }

        assertThat(clicked).isTrue()
    }

    private fun assertLeftPaddingIs(
        padding: Dp,
        button: @Composable (Modifier, @Composable RowScope.() -> Unit) -> Unit
    ) {
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent {
            Box {
                button(Modifier.onGloballyPositioned { parentCoordinates = it }) {
                    Text(
                        "Test button",
                        Modifier.onGloballyPositioned { childCoordinates = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            val topLeft = childCoordinates!!.localToWindow(Offset.Zero).x -
                parentCoordinates!!.localToWindow(Offset.Zero).x
            val currentPadding = with(rule.density) {
                padding.roundToPx().toFloat()
            }
            assertThat(currentPadding).isEqualTo(topLeft)
        }
    }
}

fun assertWithinOnePixel(expected: Offset, actual: Offset) {
    assertWithinOnePixel(expected.x, actual.x)
    assertWithinOnePixel(expected.y, actual.y)
}

fun assertWithinOnePixel(expected: Float, actual: Float) {
    val diff = abs(expected - actual)
    assertThat(diff).isLessThan(1.1f)
}
