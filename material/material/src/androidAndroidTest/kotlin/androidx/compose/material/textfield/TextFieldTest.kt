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

package androidx.compose.material.textfield

import android.content.Context
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AmbientContentAlpha
import androidx.compose.material.AmbientContentColor
import androidx.compose.material.AmbientTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.runOnIdleWithDensity
import androidx.compose.material.setMaterialContent
import androidx.compose.material.setMaterialContentForSizeAssertions
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusReference
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusReference
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.AmbientTextInputService
import androidx.compose.ui.platform.AmbientView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTesting::class)
class TextFieldTest {

    private val ExpectedMinimumTextFieldHeight = 56.dp
    private val ExpectedPadding = 16.dp
    private val IconPadding = 12.dp
    private val ExpectedBaselineOffset = 20.dp
    private val TopPaddingFilledTextfield = 4.dp
    private val IconColorAlpha = 0.54f
    private val TextfieldTag = "textField"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testTextField_minimumHeight() {
        rule.setMaterialContentForSizeAssertions {
            TextField(
                value = "input",
                onValueChange = {},
                label = {},
                modifier = Modifier.preferredHeight(20.dp)
            )
        }
            .assertHeightIsEqualTo(ExpectedMinimumTextFieldHeight)
    }

    @Test
    fun testTextFields_singleFocus() {
        var textField1Focused = false
        val textField1Tag = "TextField1"

        var textField2Focused = false
        val textField2Tag = "TextField2"

        rule.setMaterialContent {
            Column {
                TextField(
                    modifier = Modifier
                        .onFocusChanged { textField1Focused = it.isFocused }
                        .testTag(textField1Tag),
                    value = "input1",
                    onValueChange = {},
                    label = {}
                )
                TextField(
                    modifier = Modifier
                        .onFocusChanged { textField2Focused = it.isFocused }
                        .testTag(textField2Tag),
                    value = "input2",
                    onValueChange = {},
                    label = {}
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
    fun testTextField_getFocus_whenClickedOnSurfaceArea() {
        var focused = false
        rule.setMaterialContent {
            Box {
                TextField(
                    modifier = Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .testTag(TextfieldTag),
                    value = "input",
                    onValueChange = {},
                    label = {}
                )
            }
        }

        // Click on (2, 2) which is Surface area and outside input area
        rule.onNodeWithTag(TextfieldTag).performGesture {
            click(Offset(2f, 2f))
        }

        rule.runOnIdleWithDensity {
            assertThat(focused).isTrue()
        }
    }

    @Test
    fun testTextField_showHideKeyboardBasedOnFocus() {
        val (focusReference, parentFocusReference) = FocusReference.createRefs()
        lateinit var hostView: View
        rule.setMaterialContent {
            hostView = AmbientView.current
            Box {
                TextField(
                    modifier = Modifier
                        .focusReference(parentFocusReference)
                        .focusModifier()
                        .focusReference(focusReference)
                        .testTag(TextfieldTag),
                    value = "input",
                    onValueChange = {},
                    label = {}
                )
            }
        }

        // Shows keyboard when the text field is focused.
        rule.runOnIdle { focusReference.requestFocus() }
        rule.runOnIdle { assertThat(hostView.isSoftwareKeyboardShown).isTrue() }

        // Hides keyboard when the text field is not focused.
        rule.runOnIdle { parentFocusReference.requestFocus() }
        rule.runOnIdle { assertThat(hostView.isSoftwareKeyboardShown).isFalse() }
    }

    @Test
    fun testTextField_clickingOnTextAfterDismissingKeyboard_showsKeyboard() {
        val (focusReference, parentFocusReference) = FocusReference.createRefs()
        lateinit var softwareKeyboardController: SoftwareKeyboardController
        lateinit var hostView: View
        rule.setMaterialContent {
            hostView = AmbientView.current
            Box {
                TextField(
                    modifier = Modifier
                        .focusReference(parentFocusReference)
                        .focusModifier()
                        .focusReference(focusReference)
                        .testTag(TextfieldTag),
                    value = "input",
                    onValueChange = {},
                    onTextInputStarted = { softwareKeyboardController = it },
                    label = {}
                )
            }
        }

        // Shows keyboard when the text field is focused.
        rule.runOnIdle { focusReference.requestFocus() }
        rule.runOnIdle { assertThat(hostView.isSoftwareKeyboardShown).isTrue() }

        // Hide keyboard.
        rule.runOnIdle { softwareKeyboardController.hideSoftwareKeyboard() }

        // Clicking on the text field shows the keyboard.
        rule.onNodeWithTag(TextfieldTag).performClick()
        rule.runOnIdle { assertThat(hostView.isSoftwareKeyboardShown).isTrue() }
    }

    @Test
    fun testTextField_labelPosition_initial_withDefaultHeight() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(
                            text = "label",
                            fontSize = 10.sp,
                            modifier = Modifier
                                .onGloballyPositioned {
                                    labelPosition.value = it.positionInRoot
                                    labelSize.value = it.size
                                }
                        )
                    },
                    modifier = Modifier.preferredHeight(56.dp)
                )
            }
        }

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(labelPosition.value?.y).isEqualTo(
                ((ExpectedMinimumTextFieldHeight.toIntPx() - labelSize.value!!.height) / 2f)
                    .roundToInt().toFloat()
            )
        }
    }

    @Test
    fun testTextField_labelPosition_initial_withCustomHeight() {
        val height = 80.dp
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot
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
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(labelPosition.value?.y).isEqualTo(
                ((height.toIntPx() - labelSize.value!!.height) / 2f).roundToInt().toFloat()
            )
        }
    }

    @Test
    fun testTextField_labelPosition_whenFocused() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        val baseline = Ref<Float>()
        rule.setMaterialContent {
            Box {
                TextField(
                    modifier = Modifier.testTag(TextfieldTag),
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot
                                labelSize.value = it.size
                                baseline.value = it[FirstBaseline].toFloat() +
                                    labelPosition.value!!.y
                            }
                        )
                    }
                )
            }
        }

        // click to focus
        clickAndAdvanceClock()

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(baseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toFloat()
            )
        }
    }

    @Test
    fun testTextField_labelPosition_whenInput() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        val baseline = Ref<Float>()
        rule.setMaterialContent {
            Box {
                TextField(
                    value = "input",
                    onValueChange = {},
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot
                                labelSize.value = it.size
                                baseline.value =
                                    it[FirstBaseline].toFloat() + labelPosition.value!!.y
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
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(baseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toFloat()
            )
        }
    }

    @Test
    fun testTextField_placeholderPosition_withLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    modifier = Modifier
                        .preferredHeight(60.dp)
                        .testTag(TextfieldTag),
                    value = "",
                    onValueChange = {},
                    label = { Text("label") },
                    placeholder = {
                        Text(
                            text = "placeholder",
                            modifier = Modifier.onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot
                                placeholderSize.value = it.size
                            }
                        )
                    }
                )
            }
        }
        // click to focus
        clickAndAdvanceClock()

        rule.runOnIdleWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isGreaterThan(0)
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // placeholder's position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(placeholderPosition.value?.y)
                .isEqualTo(
                    (ExpectedBaselineOffset.toIntPx() + TopPaddingFilledTextfield.toIntPx())
                        .toFloat()
                )
        }
    }

    @Test
    fun testTextField_placeholderPosition_whenNoLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        val height = 60.dp
        rule.setMaterialContent {
            Box {
                TextField(
                    modifier = Modifier.preferredHeight(height).testTag(TextfieldTag),
                    value = "",
                    onValueChange = {},
                    label = {},
                    placeholder = {
                        Text(
                            text = "placeholder",
                            modifier = Modifier.height(20.dp)
                                .onGloballyPositioned {
                                    placeholderPosition.value = it.positionInRoot
                                    placeholderSize.value = it.size
                                }
                        )
                    }
                )
            }
        }
        // click to focus
        clickAndAdvanceClock()

        rule.runOnIdleWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isEqualTo(20.dp.toIntPx())
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(placeholderPosition.value?.y).isEqualTo(
                ((height.toIntPx().toFloat() - placeholderSize.value!!.height) / 2f)
                    .roundToInt()
                    .toFloat()
            )
        }
    }

    @Test
    fun testTextField_noPlaceholder_whenInputNotEmpty() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent {
            Column {
                TextField(
                    modifier = Modifier.testTag(TextfieldTag),
                    value = "input",
                    onValueChange = {},
                    label = {},
                    placeholder = {
                        Text(
                            text = "placeholder",
                            modifier = Modifier.onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot
                                placeholderSize.value = it.size
                            }
                        )
                    }
                )
            }
        }

        // click to focus
        clickAndAdvanceClock()

        rule.runOnIdleWithDensity {
            assertThat(placeholderSize.value).isNull()
            assertThat(placeholderPosition.value).isNull()
        }
    }

    @Test
    fun testTextField_placeholderColorAndTextStyle() {
        rule.setMaterialContent {
            TextField(
                modifier = Modifier.testTag(TextfieldTag),
                value = "",
                onValueChange = {},
                label = {},
                placeholder = {
                    Text("placeholder")
                    assertThat(
                        AmbientContentColor.current.copy(
                            alpha = AmbientContentAlpha.current
                        )
                    )
                        .isEqualTo(
                            MaterialTheme.colors.onSurface.copy(
                                alpha = 0.6f
                            )
                        )
                    assertThat(AmbientTextStyle.current)
                        .isEqualTo(MaterialTheme.typography.subtitle1)
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextfieldTag).performClick()
    }

    @Test
    fun testTextField_trailingAndLeading_sizeAndPosition() {
        val textFieldHeight = 60.dp
        val textFieldWidth = 300.dp
        val size = 30.dp
        val leadingPosition = Ref<Offset>()
        val leadingSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()

        rule.setMaterialContent {
            TextField(
                value = "text",
                onValueChange = {},
                modifier = Modifier.preferredSize(textFieldWidth, textFieldHeight),
                label = {},
                leadingIcon = {
                    Box(
                        Modifier.preferredSize(size).onGloballyPositioned {
                            leadingPosition.value = it.positionInRoot
                            leadingSize.value = it.size
                        }
                    )
                },
                trailingIcon = {
                    Box(
                        Modifier.preferredSize(size).onGloballyPositioned {
                            trailingPosition.value = it.positionInRoot
                            trailingSize.value = it.size
                        }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // leading
            assertThat(leadingSize.value).isEqualTo(IntSize(size.toIntPx(), size.toIntPx()))
            assertThat(leadingPosition.value?.x).isEqualTo(IconPadding.toIntPx().toFloat())
            assertThat(leadingPosition.value?.y).isEqualTo(
                ((textFieldHeight.toIntPx() - leadingSize.value!!.height) / 2f).roundToInt()
                    .toFloat()
            )
            // trailing
            assertThat(trailingSize.value).isEqualTo(IntSize(size.toIntPx(), size.toIntPx()))
            assertThat(trailingPosition.value?.x).isEqualTo(
                (textFieldWidth.toIntPx() - IconPadding.toIntPx() - trailingSize.value!!.width)
                    .toFloat()
            )
            assertThat(trailingPosition.value?.y)
                .isEqualTo(
                    ((textFieldHeight.toIntPx() - trailingSize.value!!.height) / 2f)
                        .roundToInt().toFloat()
                )
        }
    }

    @Test
    fun testTextField_labelPositionX_initial_withTrailingAndLeading() {
        val height = 60.dp
        val iconSize = 30.dp
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot
                            }
                        )
                    },
                    trailingIcon = { Box(Modifier.preferredSize(iconSize)) },
                    leadingIcon = { Box(Modifier.preferredSize(iconSize)) }
                )
            }
        }

        rule.runOnIdleWithDensity {
            assertThat(labelPosition.value?.x).isEqualTo(
                (ExpectedPadding.toIntPx() + IconPadding.toIntPx() + iconSize.toIntPx())
                    .toFloat()
            )
        }
    }

    @Test
    fun testTextField_labelPositionX_initial_withEmptyTrailingAndLeading() {
        val height = 60.dp
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(
                            text = "label",
                            modifier = Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot
                            }
                        )
                    },
                    trailingIcon = {},
                    leadingIcon = {}
                )
            }
        }

        rule.runOnIdleWithDensity {
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
        }
    }

    @Test
    fun testTextField_colorInLeadingTrailing_whenValidInput() {
        rule.setMaterialContent {
            TextField(
                value = "",
                onValueChange = {},
                label = {},
                isErrorValue = false,
                leadingIcon = {
                    assertThat(AmbientContentColor.current)
                        .isEqualTo(
                            MaterialTheme.colors.onSurface.copy(
                                IconColorAlpha
                            )
                        )
                },
                trailingIcon = {
                    assertThat(AmbientContentColor.current)
                        .isEqualTo(
                            MaterialTheme.colors.onSurface.copy(
                                IconColorAlpha
                            )
                        )
                }
            )
        }
    }

    @Test
    fun testTextField_colorInLeadingTrailing_whenInvalidInput() {
        rule.setMaterialContent {
            TextField(
                value = "",
                onValueChange = {},
                label = {},
                isErrorValue = true,
                leadingIcon = {
                    assertThat(AmbientContentColor.current)
                        .isEqualTo(
                            MaterialTheme.colors.onSurface.copy(
                                IconColorAlpha
                            )
                        )
                },
                trailingIcon = {
                    assertThat(AmbientContentColor.current).isEqualTo(MaterialTheme.colors.error)
                }
            )
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun testTextField_imeActionAndKeyboardTypePropagatedDownstream() {
        val textInputService = mock<TextInputService>()
        rule.setContent {
            Providers(
                AmbientTextInputService provides textInputService
            ) {
                val text = remember { mutableStateOf("") }
                TextField(
                    modifier = Modifier.testTag(TextfieldTag),
                    value = text.value,
                    onValueChange = { text.value = it },
                    label = {},
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Email
                    )
                )
            }
        }

        clickAndAdvanceClock()

        rule.runOnIdle {
            verify(textInputService, atLeastOnce()).startInput(
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
    fun testTextField_visualTransformationPropagated() {
        rule.setMaterialContent {
            TextField(
                modifier = Modifier.testTag(TextfieldTag),
                value = "qwerty",
                onValueChange = {},
                label = {},
                visualTransformation = PasswordVisualTransformation('\u0020'),
                backgroundColor = Color.White,
                shape = RectangleShape
            )
        }

        rule.onNodeWithTag(TextfieldTag)
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
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_alphaNotApplied_toCustomBackgroundColorAndTransparentColors() {
        val latch = CountDownLatch(1)

        rule.setMaterialContent {
            Box(Modifier.background(color = Color.White)) {
                TextField(
                    modifier = Modifier
                        .onFocusChanged { if (it.isFocused) latch.countDown() }
                        .testTag(TextfieldTag),
                    value = "",
                    onValueChange = {},
                    label = {},
                    shape = RectangleShape,
                    backgroundColor = Color.Blue,
                    activeColor = Color.Transparent,
                    inactiveColor = Color.Transparent
                )
            }
        }

        rule.onNodeWithTag(TextfieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.Blue,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(rule.density) { 1.dp.toPx() }
            )

        rule.onNodeWithTag(TextfieldTag).performClick()
        assert(latch.await(1, TimeUnit.SECONDS))

        rule.onNodeWithTag(TextfieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.Blue,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(rule.density) { 1.dp.toPx() }
            )
    }

    @Test
    fun testTextField_onTextInputStartedCallback() {
        var controller: SoftwareKeyboardController? = null

        rule.setMaterialContent {
            TextField(
                modifier = Modifier.testTag(TextfieldTag),
                value = "",
                onValueChange = {},
                label = {},
                onTextInputStarted = {
                    controller = it
                }
            )
        }
        assertThat(controller).isNull()

        rule.onNodeWithTag(TextfieldTag)
            .performClick()

        rule.runOnIdle {
            assertThat(controller).isNotNull()
        }
    }

    @Test
    fun testTextField_imeActionCallback_withSoftwareKeyboardController() {
        var controller: SoftwareKeyboardController? = null

        rule.setMaterialContent {
            TextField(
                modifier = Modifier.testTag(TextfieldTag),
                value = "",
                onValueChange = {},
                label = {},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                onImeActionPerformed = { _, softwareKeyboardController ->
                    controller = softwareKeyboardController
                }
            )
        }
        assertThat(controller).isNull()

        rule.onNodeWithTag(TextfieldTag)
            .performImeAction()

        rule.runOnIdle {
            assertThat(controller).isNotNull()
        }
    }

    private fun clickAndAdvanceClock() {
        rule.onNodeWithTag(TextfieldTag).performClick()
        rule.clockTestRule.advanceClock(200L)
    }

    private val View.isSoftwareKeyboardShown: Boolean get() {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // TODO(b/163742556): This is just a proxy for software keyboard visibility. Find a better
        //  way to check if the software keyboard is shown.
        return inputMethodManager.isAcceptingText()
    }
}
