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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlin.math.max
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class OutlinedTextFieldTest {
    private val ExpectedMinimumTextFieldHeight = TextFieldDefaults.MinHeight
    private val ExpectedDefaultTextFieldWidth = TextFieldDefaults.MinWidth
    private val ExpectedPadding = TextFieldPadding
    private val IconPadding = HorizontalIconPadding
    private val TextFieldTag = "textField"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testOutlinedTextField_setSmallWidth() {
        rule.setMaterialContentForSizeAssertions {
            OutlinedTextField(
                value = "input",
                onValueChange = {},
                modifier = Modifier.requiredWidth(40.dp)
            )
        }
            .assertWidthIsEqualTo(40.dp)
    }

    @Test
    fun testOutlinedTextField_defaultWidth() {
        rule.setMaterialContentForSizeAssertions {
            OutlinedTextField(
                value = "input",
                onValueChange = {}
            )
        }
            .assertWidthIsEqualTo(ExpectedDefaultTextFieldWidth)
    }

    @Test
    fun testOutlinedTextFields_singleFocus() {
        var textField1Focused = false
        val textField1Tag = "TextField1"

        var textField2Focused = false
        val textField2Tag = "TextField2"

        rule.setMaterialContent(lightColorScheme()) {
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .testTag(textField1Tag)
                        .onFocusChanged { textField1Focused = it.isFocused },
                    value = "input1",
                    onValueChange = {}
                )
                OutlinedTextField(
                    modifier = Modifier
                        .testTag(textField2Tag)
                        .onFocusChanged { textField2Focused = it.isFocused },
                    value = "input2",
                    onValueChange = {}
                )
            }
        }

        rule.onNodeWithTag(textField1Tag).performClick()

        rule.runOnIdle {
            assertThat(textField1Focused).isTrue()
            assertThat(textField2Focused).isFalse()
        }

        rule.onNodeWithTag(textField2Tag).performClick()

        rule.runOnIdle {
            assertThat(textField1Focused).isFalse()
            assertThat(textField2Focused).isTrue()
        }
    }

    @Test
    fun testOutlinedTextField_getFocus_whenClickedOnInternalArea() {
        var focused = false
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    modifier = Modifier
                        .testTag(TextFieldTag)
                        .onFocusChanged { focused = it.isFocused },
                    value = "input",
                    onValueChange = {}
                )
            }
        }

        // Click on (2, 2) which is a background area and outside input area
        rule.onNodeWithTag(TextFieldTag).performTouchInput {
            click(Offset(2f, 2f))
        }

        rule.runOnIdleWithDensity {
            assertThat(focused).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testOutlinedTextField_noTopPadding_ifNoLabel() {
        val density = Density(4f)
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                Box(Modifier.testTag("box").background(Color.Red)) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            unfocusedBorderColor = Color.White
                        ),
                        shape = RectangleShape
                    )
                }
            }
        }

        rule.onNodeWithTag("box").captureToImage().assertShape(
            density = density,
            horizontalPadding = 1.dp, // OutlinedTextField border thickness
            verticalPadding = 1.dp, // OutlinedTextField border thickness
            backgroundColor = Color.White, // OutlinedTextField border color
            shapeColor = Color.Red, // Color of background as OutlinedTextField is transparent
            shape = RectangleShape
        )
    }

    @Test
    fun testOutlinedTextField_labelPosition_initial_singleLine() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    singleLine = true,
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                                labelSize.value = it.size
                            }
                        )
                    }
                )
            }
        }

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.roundToPx().toFloat()
            )
            // label is centered in 56.dp default container, plus additional 8.dp padding on top
            val minimumHeight = ExpectedMinimumTextFieldHeight.roundToPx()
            assertThat(labelPosition.value?.y).isEqualTo(
                ((minimumHeight - labelSize.value!!.height) / 2f).roundToInt() + 8.dp.roundToPx()
            )
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_initial_withMultiLineLabel() {
        val textFieldWidth = 200.dp
        val labelSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.requiredWidth(textFieldWidth),
                    label = {
                        Text(
                            text = "long long long long long long long long long long long long",
                            modifier = Modifier.onGloballyPositioned {
                                labelSize.value = it.size
                            }
                        )
                    }
                )
            }
        }

        rule.runOnIdleWithDensity {
            // label size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width)
                .isEqualTo(textFieldWidth.roundToPx() - 2 * ExpectedPadding.roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_initial_withDefaultHeight() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                                labelSize.value = it.size
                            }
                        )
                    }
                )
            }
        }

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.roundToPx().toFloat()
            )
            // label is aligned to the top with padding, plus additional 8.dp padding on top
            assertThat(labelPosition.value?.y).isEqualTo(
                TextFieldPadding.roundToPx() + 8.dp.roundToPx()
            )
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_whenFocused() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()

        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "",
                onValueChange = {},
                label = {
                    Text(
                        text = "label",
                        modifier = Modifier.onGloballyPositioned {
                            labelPosition.value = it.positionInRoot()
                            labelSize.value = it.size
                        }
                    )
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.roundToPx().toFloat()
            )
            assertThat(labelPosition.value?.y).isEqualTo(getLabelPosition(labelSize))
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_whenFocused_withMultiLineLabel() {
        val textFieldWidth = 200.dp
        val labelSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.testTag(TextFieldTag).requiredWidth(textFieldWidth),
                    label = {
                        Text(
                            text = "long long long long long long long long long long long long",
                            modifier = Modifier.onGloballyPositioned {
                                labelSize.value = it.size
                            }
                        )
                    }
                )
            }
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // label size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width)
                .isEqualTo(textFieldWidth.roundToPx() - 2 * ExpectedPadding.roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_whenInput() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "input",
                onValueChange = {},
                label = {
                    Text(
                        text = "label",
                        modifier = Modifier.onGloballyPositioned {
                            labelPosition.value = it.positionInRoot()
                            labelSize.value = it.size
                        }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.roundToPx().toFloat()
            )
            assertThat(labelPosition.value?.y).isEqualTo(getLabelPosition(labelSize))
        }
    }

    @Test
    fun testOutlinedTextField_placeholderPosition_withLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    value = "",
                    onValueChange = {},
                    label = { Text("label") },
                    placeholder = {
                        Text(
                            text = "placeholder",
                            modifier = Modifier.onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot()
                                placeholderSize.value = it.size
                            }
                        )
                    }
                )
            }
        }
        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isGreaterThan(0)
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.roundToPx().toFloat()
            )
            // placeholder is aligned to the top with padding, plus additional 8.dp padding on top
            assertThat(placeholderPosition.value?.y).isEqualTo(
                TextFieldPadding.roundToPx() + 8.dp.roundToPx()
            )
        }
    }

    @Test
    fun testOutlinedTextField_placeholderPosition_whenNoLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    value = "",
                    onValueChange = {},
                    placeholder = {
                        Text(
                            text = "placeholder",
                            modifier = Modifier.onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot()
                                placeholderSize.value = it.size
                            }
                        )
                    }
                )
            }
        }
        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isGreaterThan(0)
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.roundToPx().toFloat()
            )
            // placeholder is placed with fixed padding
            assertThat(placeholderPosition.value?.y).isEqualTo(TextFieldPadding.roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_noPlaceholder_whenInputNotEmpty() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            Column {
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    value = "input",
                    onValueChange = {},
                    placeholder = {
                        Text(
                            text = "placeholder",
                            modifier = Modifier.onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot()
                                placeholderSize.value = it.size
                            }
                        )
                    }
                )
            }
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            assertThat(placeholderSize.value).isNull()
            assertThat(placeholderPosition.value).isNull()
        }
    }

    @Test
    fun testOutlinedTextField_placeholderColorAndTextStyle() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "",
                onValueChange = {},
                placeholder = {
                    Text("placeholder")
                    assertThat(LocalTextStyle.current)
                        .isEqualTo(MaterialTheme.typography.bodyLarge)
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()
    }

    @Test
    fun testOutlinedTextField_trailingAndLeading_sizeAndPosition_defaultIcon() {
        val textFieldWidth = 300.dp
        val leadingPosition = Ref<Offset>()
        val leadingSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        val density = Density(2f)
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.width(textFieldWidth),
                    label = { Text("label") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            Modifier.onGloballyPositioned {
                                leadingPosition.value = it.positionInRoot()
                                leadingSize.value = it.size
                            }
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            Modifier.onGloballyPositioned {
                                trailingPosition.value = it.positionInRoot()
                                trailingSize.value = it.size
                            }
                        )
                    },
                )
            }
        }

        rule.runOnIdle {
            with(density) {
                val minimumHeight = ExpectedMinimumTextFieldHeight.roundToPx()
                val size = 24.dp // default icon size
                // leading
                assertThat(leadingSize.value).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(leadingPosition.value?.x).isEqualTo(IconPadding.roundToPx().toFloat())
                assertThat(leadingPosition.value?.y).isEqualTo(
                    ((minimumHeight - leadingSize.value!!.height) / 2f).roundToInt() +
                        8.dp.roundToPx()
                )
                // trailing
                assertThat(trailingSize.value).isEqualTo(
                    IntSize(
                        size.roundToPx(),
                        size.roundToPx()
                    )
                )
                assertThat(trailingPosition.value?.x).isEqualTo(
                    (
                        textFieldWidth.roundToPx() - IconPadding.roundToPx() -
                            trailingSize.value!!.width
                        ).toFloat()
                )
                assertThat(trailingPosition.value?.y).isEqualTo(
                    ((minimumHeight - trailingSize.value!!.height) / 2f).roundToInt() +
                        8.dp.roundToPx()
                )
            }
        }
    }

    @Test
    fun testOutlinedTextField_trailingAndLeading_sizeAndPosition_defaultIconButton() {
        val textFieldWidth = 300.dp
        val textFieldHeight = 80.dp
        val density = Density(2f)

        var leadingPosition: Offset? = null
        var leadingSize: IntSize? = null
        var trailingPosition: Offset? = null
        var trailingSize: IntSize? = null

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.width(textFieldWidth).height(textFieldHeight),
                    leadingIcon = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned {
                                leadingPosition = it.positionInRoot()
                                leadingSize = it.size
                            }
                        ) { Icon(Icons.Default.Favorite, null) }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned {
                                trailingPosition = it.positionInRoot()
                                trailingSize = it.size
                            }
                        ) { Icon(Icons.Default.Favorite, null) }
                    }
                )
            }
        }

        rule.runOnIdle {
            val size = 48.dp // default IconButton size

            with(density) {
                // leading
                assertThat(leadingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(leadingPosition?.x).isEqualTo(0f)
                assertThat(leadingPosition?.y).isEqualTo(
                    ((textFieldHeight.roundToPx() - leadingSize!!.height) / 2f).roundToInt()
                )
                // trailing
                assertThat(trailingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(trailingPosition?.x).isEqualTo(
                    (textFieldWidth.roundToPx() - trailingSize!!.width).toFloat()
                )
                assertThat(trailingPosition?.y).isEqualTo(
                    ((textFieldHeight.roundToPx() - trailingSize!!.height) / 2f).roundToInt()
                )
            }
        }
    }

    @Test
    fun testOutlinedTextField_trailingAndLeading_sizeAndPosition_nonDefaultSizeIcon() {
        val textFieldWidth = 300.dp
        val textFieldHeight = 80.dp
        val size = 72.dp
        val density = Density(2f)

        var leadingPosition: Offset? = null
        var leadingSize: IntSize? = null
        var trailingPosition: Offset? = null
        var trailingSize: IntSize? = null

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.width(textFieldWidth).height(textFieldHeight),
                    leadingIcon = {
                        Box(
                            Modifier.size(size).onGloballyPositioned {
                                leadingPosition = it.positionInRoot()
                                leadingSize = it.size
                            }
                        )
                    },
                    trailingIcon = {
                        Box(
                            Modifier.size(size).onGloballyPositioned {
                                trailingPosition = it.positionInRoot()
                                trailingSize = it.size
                            }
                        )
                    },
                )
            }
        }

        rule.runOnIdle {
            with(density) {
                // leading
                assertThat(leadingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(leadingPosition?.x).isEqualTo(0f)
                assertThat(leadingPosition?.y).isEqualTo(
                    ((textFieldHeight.roundToPx() - leadingSize!!.height) / 2f).roundToInt()
                )
                // trailing
                assertThat(trailingSize).isEqualTo(
                    IntSize(
                        size.roundToPx(),
                        size.roundToPx()
                    )
                )
                assertThat(trailingPosition?.x).isEqualTo(
                    (textFieldWidth.roundToPx() - trailingSize!!.width).toFloat()
                )
                assertThat(trailingPosition?.y).isEqualTo(
                    ((textFieldHeight.roundToPx() - trailingSize!!.height) / 2f).roundToInt()
                )
            }
        }
    }

    @Test
    fun testOutlinedTextField_labelPositionX_initial_withTrailingAndLeading() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                        )
                    },
                    trailingIcon = { Icon(Icons.Default.Favorite, null) },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) }
                )
            }
        }

        rule.runOnIdleWithDensity {
            val iconSize = 24.dp // default icon size
            assertThat(labelPosition.value?.x).isEqualTo(
                (ExpectedPadding.roundToPx() + IconPadding.roundToPx() + iconSize.roundToPx())
                    .toFloat()
            )
        }
    }

    @Test
    fun testOutlinedTextField_labelPositionX_initial_withNullTrailingAndLeading() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                        )
                    },
                    trailingIcon = null,
                    leadingIcon = null
                )
            }
        }

        rule.runOnIdleWithDensity {
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.roundToPx().toFloat()
            )
        }
    }

    @Test
    fun testOutlinedTextField_colorInLeadingTrailing_whenValidInput() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                isError = false,
                leadingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }

    @Test
    fun testOutlinedTextField_colorInLeadingTrailing_whenInvalidInput() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                isError = true,
                leadingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    assertThat(LocalContentColor.current).isEqualTo(MaterialTheme.colorScheme.error)
                }
            )
        }
    }

    @Test
    fun testOutlinedTextField_imeActionAndKeyboardTypePropagatedDownstream() {
        val platformTextInputService = mock<PlatformTextInputService>()
        val textInputService = TextInputService(platformTextInputService)
        rule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                val text = remember { mutableStateOf("") }
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    value = text.value,
                    onValueChange = { text.value = it },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Email
                    )
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdle {
            verify(platformTextInputService, atLeastOnce()).startInput(
                value = any(),
                imeOptions = eq(
                    ImeOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Go
                    )
                ),
                onEditCommand = any(),
                onImeActionPerformed = any()
            )
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testOutlinedTextField_visualTransformationPropagated() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.background(color = Color.White)) {
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    value = "qwerty",
                    onValueChange = {},
                    visualTransformation = PasswordVisualTransformation('\u0020')
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.White,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(rule.density) { 3.dp.toPx() }
            )
    }

    @Test
    fun testErrorSemantics_defaultMessage() {
        lateinit var errorMessage: String
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "test",
                onValueChange = {},
                isError = true
            )
            errorMessage = getString(Strings.DefaultErrorMessage)
        }

        rule.onNodeWithText("test")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage)
            )
    }

    @Test
    fun testErrorSemantics_messageOverridable() {
        val errorMessage = "Special symbols not allowed"
        rule.setMaterialContent(lightColorScheme()) {
            val isError = remember { mutableStateOf(true) }
            OutlinedTextField(
                value = "test",
                onValueChange = {},
                modifier = Modifier.semantics { if (isError.value) error(errorMessage) },
                isError = isError.value
            )
        }

        rule.onNodeWithText("test")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage))
    }

    @Test
    fun testOutlinedTextField_withLabel_doesNotCrash_rowHeightWithMinIntrinsics() {
        var size: IntSize? = null
        var dividerSize: IntSize? = null
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Box(Modifier.onGloballyPositioned { size = it.size }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(10.dp)
                            .onGloballyPositioned { dividerSize = it.size }
                    )
                    OutlinedTextField(
                        value = text.value,
                        label = { Text(text = "Label") },
                        onValueChange = { text.value = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(dividerSize).isNotNull()
            assertThat(size).isNotNull()
            assertThat(dividerSize!!.height).isEqualTo(size!!.height)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testOutlinedTextField_appliesContainerColor() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.testTag(TextFieldTag),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color.Red,
                    unfocusedBorderColor = Color.Red
                ),
                shape = RectangleShape
            )
        }

        rule.onNodeWithTag(TextFieldTag).captureToImage().assertPixels {
            Color.Red
        }
    }

    @Test
    fun testOutlinedTextField_withLabel_doesNotCrash_columnWidthWithMinIntrinsics() {
        var textFieldSize: IntSize? = null
        var dividerSize: IntSize? = null
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Box {
                Column(Modifier.width(IntrinsicSize.Min)) {
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .onGloballyPositioned { dividerSize = it.size }
                    )
                    OutlinedTextField(
                        value = text.value,
                        label = { Text(text = "Label") },
                        onValueChange = { text.value = it },
                        modifier = Modifier.onGloballyPositioned { textFieldSize = it.size }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(dividerSize).isNotNull()
            assertThat(textFieldSize).isNotNull()
            assertThat(dividerSize!!.width).isEqualTo(textFieldSize!!.width)
        }
    }

    @Test
    fun testOutlinedTextField_labelStyle() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified

        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = {
                    textStyle = LocalTextStyle.current
                    contentColor = LocalContentColor.current
                },
                modifier = Modifier.focusRequester(focusRequester),
                colors = TextFieldDefaults.textFieldColors(
                    unfocusedLabelColor = unfocusedLabelColor,
                    focusedLabelColor = focusedLabelColor
                )
            )
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(unfocusedLabelColor)
            assertThat(textStyle.color).isEqualTo(Color.Unspecified)
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(focusedLabelColor)
            assertThat(textStyle.color).isEqualTo(Color.Unspecified)
        }
    }

    @Test
    fun testOutlinedTextField_labelStyle_whenBodySmallStyleColorProvided() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        val bodySmallColor = Color.Green
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified

        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            val bodySmall = MaterialTheme.typography.bodySmall.copy(color = bodySmallColor)
            MaterialTheme(typography = Typography(bodySmall = bodySmall)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors = TextFieldDefaults.textFieldColors(
                        unfocusedLabelColor = unfocusedLabelColor,
                        focusedLabelColor = focusedLabelColor
                    )
                )
            }
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(unfocusedLabelColor)
            assertThat(contentColor).isEqualTo(unfocusedLabelColor)
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(bodySmallColor)
            assertThat(contentColor).isEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testOutlinedTextField_labelStyle_middle_whenBodySmallStyleColorProvided() {
        val expectedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified
        val focusRequester = FocusRequester()

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            val bodySmall = MaterialTheme.typography.bodySmall.copy(color = expectedLabelColor)
            MaterialTheme(typography = Typography(bodySmall = bodySmall)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors = TextFieldDefaults.textFieldColors(
                        unfocusedLabelColor = expectedLabelColor,
                        focusedLabelColor = focusedLabelColor
                    )
                )
            }
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        // animation duration is 150, advancing by 75 to get into middle of animation
        rule.mainClock.advanceTimeBy(75)

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(expectedLabelColor)
            // color should be a lerp between 'start' and 'end' colors. We check here that it's
            // not equal to either of them
            assertThat(contentColor).isNotEqualTo(expectedLabelColor)
            assertThat(contentColor).isNotEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testOutlinedTextField_labelStyle_whenBothTypographiesColorProvided() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        val bodySmallColor = Color.Green
        val bodyLargeColor = Color.Black
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified
        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            val bodySmall = MaterialTheme.typography.bodySmall.copy(color = bodySmallColor)
            val bodyLarge = MaterialTheme.typography.bodyLarge.copy(color = bodyLargeColor)
            MaterialTheme(typography = Typography(bodySmall = bodySmall, bodyLarge = bodyLarge)) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors = TextFieldDefaults.textFieldColors(
                        unfocusedLabelColor = unfocusedLabelColor,
                        focusedLabelColor = focusedLabelColor
                    )
                )
            }
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(bodyLargeColor)
            assertThat(contentColor).isEqualTo(unfocusedLabelColor)
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(bodySmallColor)
            assertThat(contentColor).isEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testOutlinedTextField_withIntrinsicsMeasurement_getsIdle() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Row(Modifier.height(IntrinsicSize.Min)) {
                OutlinedTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    label = { Text("Label") }
                )
                Divider(Modifier.fillMaxHeight())
            }
        }

        rule.onNodeWithText("Label")
            .assertExists()
            .assertIsDisplayed()
            .performTextInput("text")

        rule.onNodeWithText("text").assertExists()
    }

    @Test
    fun testOutlinedTextField_intrinsicsMeasurement_correctHeight() {
        var height = 0
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Box(Modifier.onGloballyPositioned {
                height = it.size.height
            }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    OutlinedTextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        placeholder = { Text("placeholder") }
                    )
                    Divider(Modifier.fillMaxHeight())
                }
            }
        }

        with(rule.density) {
            assertThat(height).isEqualTo((TextFieldDefaults.MinHeight).roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_intrinsicsMeasurement_withLeadingIcon_correctHeight() {
        var height = 0
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Box(Modifier.onGloballyPositioned {
                height = it.size.height
            }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    OutlinedTextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        placeholder = { Text("placeholder") },
                        leadingIcon = { Icon(Icons.Default.Favorite, null) }
                    )
                    Divider(Modifier.fillMaxHeight())
                }
            }
        }

        with(rule.density) {
            assertThat(height).isEqualTo((TextFieldDefaults.MinHeight).roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_intrinsicsMeasurement_withTrailingIcon_correctHeight() {
        var height = 0
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Box(Modifier.onGloballyPositioned {
                height = it.size.height
            }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    OutlinedTextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        placeholder = { Text("placeholder") },
                        trailingIcon = { Icon(Icons.Default.Favorite, null) }
                    )
                    Divider(Modifier.fillMaxHeight())
                }
            }
        }

        with(rule.density) {
            assertThat(height).isEqualTo((TextFieldDefaults.MinHeight).roundToPx())
        }
    }

    @Test
    fun outlinedTextField_stringOverload_doesNotCallOnValueChange_whenCompositionUpdatesOnly() {
        var callbackCounter = 0

        rule.setContent {
            val focusManager = LocalFocusManager.current
            val text = remember { mutableStateOf("A") }

            OutlinedTextField(
                value = text.value,
                onValueChange = {
                    callbackCounter += 1
                    text.value = it

                    // causes TextFieldValue's composition clearing
                    focusManager.clearFocus(true)
                },
                modifier = Modifier.testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag)
            .performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TextFieldTag)
            .performTextClearance()

        rule.runOnIdle {
            assertThat(callbackCounter).isEqualTo(1)
        }
    }

    private fun getLabelPosition(labelSize: Ref<IntSize>): Int {
        val labelHalfHeight = labelSize.value!!.height / 2
        val paddingTop = with(rule.density) { OutlinedTextFieldTopPadding.toPx() }
        // vertical position is the default padding - half height
        // in case negative position, fix to 0
        return max(paddingTop - labelHalfHeight, 0f).roundToInt()
    }
}
