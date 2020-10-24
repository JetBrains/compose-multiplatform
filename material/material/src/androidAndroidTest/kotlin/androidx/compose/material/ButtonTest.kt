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
import androidx.compose.foundation.AmbientContentColor
import androidx.compose.foundation.AmbientTextStyle
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertHeightIsAtLeast
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsNotEnabled
import androidx.ui.test.assertShape
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.hasClickAction
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithText
import androidx.ui.test.performClick
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
    @FlakyTest // TODO: b/158341686
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
            // Then confirm it's disabled with no click action after clicking it
            .assertHasNoClickAction()
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
    fun buttonHeightIsFromSpec() {
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
            .assertHeightIsEqualTo(36.dp)
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
                assertThat(AmbientTextStyle.current).isEqualTo(MaterialTheme.typography.button)
            }
        }
    }

    @Test
    fun outlinedButtonPropagateDefaultTextStyle() {
        rule.setMaterialContent {
            OutlinedButton(onClick = {}) {
                assertThat(AmbientTextStyle.current).isEqualTo(MaterialTheme.typography.button)
            }
        }
    }

    @Test
    fun textButtonPropagateDefaultTextStyle() {
        rule.setMaterialContent {
            TextButton(onClick = {}) {
                assertThat(AmbientTextStyle.current).isEqualTo(MaterialTheme.typography.button)
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
            Providers(AmbientShapes provides Shapes(small = shape)) {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}, elevation = null) {
                    Box(Modifier.preferredSize(10.dp, 10.dp))
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .captureToBitmap()
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
                content = AmbientContentColor.current
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
                content = AmbientContentColor.current
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
                content = AmbientContentColor.current
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
        val padding = 8.dp
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            onSurface = MaterialTheme.colors.onSurface
            Box(Modifier.testTag("myButton")) {
                // stack allows to verify there is no shadow
                Box(Modifier.padding(padding)) {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RectangleShape
                    ) {}
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                horizontalPadding = padding,
                verticalPadding = padding,
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
        val padding = 8.dp
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            onSurface = MaterialTheme.colors.onSurface
            Box(Modifier.testTag("myButton")) {
                // stack allows to verify there is no shadow
                Box(Modifier.padding(padding)) {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonConstants.defaultButtonColors(
                            backgroundColor = Color.Red
                        ),
                        shape = RectangleShape
                    ) {}
                }
            }
        }

        rule.onNodeWithTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                horizontalPadding = padding,
                verticalPadding = padding,
                backgroundColor = surface,
                shapeColor = onSurface.copy(alpha = 0.12f).compositeOver(surface)
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun outlinedButtonDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        val padding = 8.dp
        rule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            // stack allows to verify there is no shadow
            Box(Modifier.padding(padding)) {
                OutlinedButton(
                    modifier = Modifier.testTag("myButton"),
                    onClick = {},
                    enabled = false,
                    shape = RectangleShape,
                    border = null
                ) {}
            }
        }

        rule.onNodeWithTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = rule.density,
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
            // stack allows to verify there is no shadow
            Box(Modifier.padding(8.dp)) {
                TextButton(
                    modifier = Modifier.testTag("myButton"),
                    onClick = {},
                    enabled = false,
                    shape = RectangleShape
                ) {}
            }
        }

        rule.onNodeWithTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = surface,
                backgroundColor = surface
            )
    }

    @Test
    fun containedButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var emphasis: Emphasis? = null
        rule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            emphasis = AmbientEmphasisLevels.current.disabled
            Button(onClick = {}, enabled = false) {
                content = AmbientContentColor.current
            }
        }

        assertThat(content).isEqualTo(emphasis!!.applyEmphasis(onSurface))
    }

    @Test
    fun outlinedButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var emphasis: Emphasis? = null
        rule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            emphasis = AmbientEmphasisLevels.current.disabled
            OutlinedButton(onClick = {}, enabled = false) {
                content = AmbientContentColor.current
            }
        }

        assertThat(content).isEqualTo(emphasis!!.applyEmphasis(onSurface))
    }

    @Test
    fun textButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var emphasis: Emphasis? = null
        rule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            emphasis = AmbientEmphasisLevels.current.disabled
            TextButton(onClick = {}, enabled = false) {
                content = AmbientContentColor.current
            }
        }

        assertThat(content).isEqualTo(emphasis!!.applyEmphasis(onSurface))
    }

    @Test
    fun contentIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent {
            Box {
                Button({}, Modifier.onGloballyPositioned { buttonCoordinates = it }) {
                    Box(
                        Modifier.preferredSize(2.dp)
                            .onGloballyPositioned { contentCoordinates = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot
            val contentBounds = contentCoordinates!!.boundsInRoot
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(rule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.toIntPx().toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.toIntPx().toFloat())
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
                modifier = Modifier.widthIn(20.dp).heightIn(15.dp).testTag("button")
            ) {
                Spacer(Modifier.size(10.dp))
            }
        }

        rule.onNodeWithTag("button")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(15.dp)
    }

    @Test
    fun weightModifierOnButton() {
        var item1Bounds = Rect(0f, 0f, 0f, 0f)
        var buttonBounds = Rect(0f, 0f, 0f, 0f)
        rule.setMaterialContent {
            Column {
                Spacer(
                    Modifier.size(10.dp).weight(1f).onGloballyPositioned {
                        item1Bounds = it.boundsInRoot
                    }
                )

                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).onGloballyPositioned {
                        buttonBounds = it.boundsInRoot
                    }
                ) {
                    Text("Button")
                }

                Spacer(Modifier.size(10.dp).weight(1f))
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
                    buttonBounds = it.boundsInRoot
                }
            ) {
                Spacer(
                    Modifier.size(10.dp).onGloballyPositioned {
                        item1Bounds = it.boundsInRoot
                    }
                )
                Spacer(
                    Modifier.width(10.dp).height(5.dp).onGloballyPositioned {
                        item2Bounds = it.boundsInRoot
                    }
                )
            }
        }

        assertThat(item1Bounds.center.y).isWithin(1f).of(buttonBounds.center.y)
        assertThat(item2Bounds.center.y).isWithin(1f).of(buttonBounds.center.y)
        assertThat(item1Bounds.right).isWithin(1f).of(buttonBounds.center.x)
        assertThat(item2Bounds.left).isWithin(1f).of(buttonBounds.center.x)
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
            val topLeft = childCoordinates!!.localToGlobal(Offset.Zero).x -
                parentCoordinates!!.localToGlobal(Offset.Zero).x
            val currentPadding = with(rule.density) {
                padding.toIntPx().toFloat()
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
