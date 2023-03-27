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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TextFieldDefaults.OutlinedTextFieldDecorationBox
import androidx.compose.material3.TextFieldDefaults.TextFieldDecorationBox
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class TextFieldDecorationBoxTest {
    @get:Rule
    val rule = createComposeRule()

    private val Density = Density(1f)
    private val LabelHeight = 40.dp
    private val InnerTextFieldHeight = 50.dp
    private val InnerTextFieldWidth = 100.dp

    @Test
    fun outlinedTextFieldBox_overrideTopPadding_multiLine() {
        assertVerticalSizeAndPosition_outlinedTextField(
            padding = TextFieldDefaults.outlinedTextFieldPadding(top = 10.dp),
            singleLine = false,
            expectedHeight = 10.dp + InnerTextFieldHeight + TextFieldPadding,
            expectedPosition = 10.dp
        )
    }

    @Test
    fun outlinedTextFieldBox_overrideTopPadding_singleLine() {
        assertVerticalSizeAndPosition_outlinedTextField(
            padding = TextFieldDefaults.outlinedTextFieldPadding(top = 10.dp),
            singleLine = true,
            expectedHeight = 10.dp + InnerTextFieldHeight + TextFieldPadding,
            expectedPosition = (10.dp + TextFieldPadding) / 2
        )
    }

    @Test
    fun outlinedTextFieldBox_overrideBottomPadding_multiLine() {
        assertVerticalSizeAndPosition_outlinedTextField(
            padding = TextFieldDefaults.outlinedTextFieldPadding(bottom = 10.dp),
            singleLine = false,
            expectedHeight = TextFieldPadding + InnerTextFieldHeight + 10.dp,
            expectedPosition = TextFieldPadding
        )
    }

    @Test
    fun outlinedTextFieldBox_overrideBottomPadding_singleLine() {
        assertVerticalSizeAndPosition_outlinedTextField(
            padding = TextFieldDefaults.outlinedTextFieldPadding(bottom = 10.dp),
            singleLine = true,
            expectedHeight = TextFieldPadding + InnerTextFieldHeight + 10.dp,
            expectedPosition = (10.dp + TextFieldPadding) / 2
        )
    }

    @Test
    fun outlinedTextFieldBox_overrideStartPadding() {
        assertHorizontalSizeAndPosition_outlinedTextField(
            padding = TextFieldDefaults.outlinedTextFieldPadding(start = 10.dp),
            rtl = false,
            expectedWidth = 10.dp + InnerTextFieldWidth + TextFieldPadding,
            expectedPosition = 10.dp
        )
    }

    @Test
    fun outlinedTextFieldBox_overrideStartPadding_rtl() {
        assertHorizontalSizeAndPosition_outlinedTextField(
            padding = TextFieldDefaults.outlinedTextFieldPadding(start = 10.dp),
            rtl = true,
            expectedWidth = 10.dp + InnerTextFieldWidth + TextFieldPadding,
            expectedPosition = TextFieldPadding
        )
    }

    @Test
    fun outlinedTextFieldBox_overrideEndPadding() {
        assertHorizontalSizeAndPosition_outlinedTextField(
            padding = TextFieldDefaults.outlinedTextFieldPadding(end = 20.dp),
            rtl = false,
            expectedWidth = TextFieldPadding + InnerTextFieldWidth + 20.dp,
            expectedPosition = TextFieldPadding
        )
    }

    @Test
    fun outlinedTextFieldBox_overrideEndPadding_rtl() {
        assertHorizontalSizeAndPosition_outlinedTextField(
            padding = TextFieldDefaults.outlinedTextFieldPadding(end = 20.dp),
            rtl = true,
            expectedWidth = TextFieldPadding + InnerTextFieldWidth + 20.dp,
            expectedPosition = 20.dp
        )
    }

    @Test
    fun textFieldBox_overrideTopPadding_singleLine_withoutLabel() {
        assertVerticalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithoutLabelPadding(top = 40.dp),
            singleLine = true,
            hasLabel = false,
            expectedHeight = 40.dp + InnerTextFieldHeight + TextFieldPadding,
            expectedPosition = (40.dp + TextFieldPadding) / 2
        )
    }

    @Test
    fun textFieldBox_overrideTopPadding_singleLine_withLabel() {
        assertVerticalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithLabelPadding(top = 40.dp),
            singleLine = true,
            hasLabel = true,
            expectedHeight = 40.dp + LabelHeight + InnerTextFieldHeight +
                TextFieldWithLabelVerticalPadding,
            expectedPosition = 40.dp + LabelHeight
        )
    }

    @Test
    fun textFieldBox_overrideBottomPadding_singleLine_withoutLabel() {
        assertVerticalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithoutLabelPadding(bottom = 40.dp),
            singleLine = true,
            hasLabel = false,
            expectedHeight = TextFieldPadding + InnerTextFieldHeight + 40.dp,
            expectedPosition = (TextFieldPadding + 40.dp) / 2
        )
    }

    @Test
    fun textFieldBox_overrideBottomPadding_singleLine_withLabel() {
        assertVerticalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithLabelPadding(bottom = 40.dp),
            singleLine = true,
            hasLabel = true,
            expectedHeight = TextFieldWithLabelVerticalPadding + LabelHeight +
                InnerTextFieldHeight + 40.dp,
            expectedPosition = TextFieldWithLabelVerticalPadding + LabelHeight
        )
    }

    @Test
    fun textFieldBox_overrideTopPadding_multiLine_withoutLabel() {
        assertVerticalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithoutLabelPadding(top = 40.dp),
            singleLine = false,
            hasLabel = false,
            expectedHeight = 40.dp + InnerTextFieldHeight + TextFieldPadding,
            expectedPosition = 40.dp
        )
    }

    @Test
    fun textFieldBox_overrideTopPadding_multiLine_withLabel() {
        assertVerticalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithLabelPadding(top = 40.dp),
            singleLine = false,
            hasLabel = true,
            expectedHeight = 40.dp + LabelHeight + InnerTextFieldHeight +
                TextFieldWithLabelVerticalPadding,
            expectedPosition = 40.dp + LabelHeight
        )
    }

    @Test
    fun textFieldBox_overrideBottomPadding_multiLine_withoutLabel() {
        assertVerticalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithoutLabelPadding(bottom = 40.dp),
            singleLine = false,
            hasLabel = false,
            expectedHeight = TextFieldPadding + InnerTextFieldHeight + 40.dp,
            expectedPosition = TextFieldPadding
        )
    }

    @Test
    fun textFieldBox_overrideBottomPadding_multiLine_withLabel() {
        assertVerticalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithLabelPadding(bottom = 40.dp),
            singleLine = false,
            hasLabel = true,
            expectedHeight = TextFieldWithLabelVerticalPadding + LabelHeight +
                InnerTextFieldHeight + 40.dp,
            expectedPosition = TextFieldWithLabelVerticalPadding + LabelHeight
        )
    }

    @Test
    fun textFieldBox_overrideStartPadding_withLabel() {
        assertHorizontalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithLabelPadding(start = 40.dp),
            rtl = false,
            hasLabel = true,
            expectedWidth = 40.dp + InnerTextFieldWidth + TextFieldPadding,
            expectedPosition = 40.dp
        )
    }

    @Test
    fun textFieldBox_overrideStartPadding_withLabel_rtl() {
        assertHorizontalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithLabelPadding(start = 40.dp),
            rtl = true,
            hasLabel = true,
            expectedWidth = 40.dp + InnerTextFieldWidth + TextFieldPadding,
            expectedPosition = TextFieldPadding
        )
    }

    @Test
    fun textFieldBox_overrideStartPadding_withoutLabel() {
        assertHorizontalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithoutLabelPadding(start = 40.dp),
            rtl = false,
            hasLabel = false,
            expectedWidth = 40.dp + InnerTextFieldWidth + TextFieldPadding,
            expectedPosition = 40.dp
        )
    }

    @Test
    fun textFieldBox_overrideStartPadding_withoutLabel_rtl() {
        assertHorizontalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithoutLabelPadding(start = 40.dp),
            rtl = true,
            hasLabel = false,
            expectedWidth = 40.dp + InnerTextFieldWidth + TextFieldPadding,
            expectedPosition = TextFieldPadding
        )
    }

    @Test
    fun textFieldBox_overrideEndPadding_withLabel() {
        assertHorizontalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithLabelPadding(end = 40.dp),
            rtl = false,
            hasLabel = true,
            expectedWidth = TextFieldPadding + InnerTextFieldWidth + 40.dp,
            expectedPosition = TextFieldPadding
        )
    }

    @Test
    fun textFieldBox_overrideEndPadding_withLabel_rtl() {
        assertHorizontalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithLabelPadding(end = 40.dp),
            rtl = true,
            hasLabel = true,
            expectedWidth = TextFieldPadding + InnerTextFieldWidth + 40.dp,
            expectedPosition = 40.dp
        )
    }

    @Test
    fun textFieldBox_overrideEndPadding_withoutLabel() {
        assertHorizontalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithoutLabelPadding(end = 40.dp),
            rtl = false,
            hasLabel = false,
            expectedWidth = TextFieldPadding + InnerTextFieldWidth + 40.dp,
            expectedPosition = TextFieldPadding
        )
    }

    @Test
    fun textFieldBox_overrideEndPadding_withoutLabel_rtl() {
        assertHorizontalSizeAndPosition_textField(
            padding = TextFieldDefaults.textFieldWithoutLabelPadding(end = 40.dp),
            rtl = true,
            hasLabel = false,
            expectedWidth = TextFieldPadding + InnerTextFieldWidth + 40.dp,
            expectedPosition = 40.dp
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun outlinedTextFieldBox_defaultBorderColor_comesFromColors() {
        val textFieldWidth = 300
        val textFieldHeight = 150
        val borderWidth = 40
        val value = "Text"

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density) {
                val interactionSource = remember { MutableInteractionSource() }
                val singleLine = true
                val colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = Color.Red
                )
                BasicTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.size(
                        with(Density) { textFieldWidth.toDp() },
                        with(Density) { textFieldHeight.toDp() }
                    ),
                    singleLine = singleLine,
                    interactionSource = interactionSource
                ) {
                    OutlinedTextFieldDecorationBox(
                        value = value,
                        innerTextField = it,
                        enabled = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        singleLine = singleLine,
                        container = {
                            TextFieldDefaults.OutlinedBorderContainerBox(
                                enabled = true,
                                isError = false,
                                colors = colors,
                                interactionSource = interactionSource,
                                shape = RectangleShape,
                                unfocusedBorderThickness = with(Density) { borderWidth.toDp() }
                            )
                        },
                        colors = colors,
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }
        }

        rule.onNodeWithText(value)
            .captureToImage()
            .assertPixels(IntSize(textFieldWidth, textFieldHeight)) {
                // to account for edge pixels
                if (it.x in 2..(textFieldWidth - 2) && it.y in 2..(borderWidth - 2)) {
                    Color.Red
                } else {
                    null
                }
            }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun textFieldBox_defaultIndicatorLineColor_comesFromColors() {
        val textFieldWidth = 300
        val textFieldHeight = 150
        val borderWidth = 40
        val value = "Text"

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density) {
                val interactionSource = remember { MutableInteractionSource() }
                val singleLine = true
                val colors = TextFieldDefaults.textFieldColors(
                    unfocusedIndicatorColor = Color.Red
                )
                BasicTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier
                        .indicatorLine(enabled = true,
                            isError = false,
                            colors = colors,
                            interactionSource = interactionSource,
                            unfocusedIndicatorLineThickness = with(Density) { borderWidth.toDp() })
                        .size(with(Density) { textFieldWidth.toDp() },
                            with(Density) { textFieldHeight.toDp() }),
                    singleLine = singleLine,
                    interactionSource = interactionSource
                ) {
                    TextFieldDecorationBox(
                        value = value,
                        innerTextField = it,
                        enabled = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        singleLine = singleLine,
                        colors = colors,
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }
        }

        rule.onNodeWithText(value)
            .captureToImage()
            .assertPixels(IntSize(textFieldWidth, textFieldHeight)) {
                // to account for edge pixels
                if (it.x in 2..(textFieldWidth - 2) &&
                    it.y in (textFieldHeight - borderWidth + 2)..(textFieldHeight - 2)
                ) {
                    Color.Red
                } else {
                    null
                }
            }
    }

    @Test
    fun textFieldBox_overridePadding_unfocusedState_withoutLabel_withPlaceholder() {
        val placeholderDimension = 50.dp
        val verticalPadding = 10.dp
        val value = ""
        var size: IntSize? = null

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides Density) {
                val interactionSource = remember { MutableInteractionSource() }
                val singleLine = false
                BasicTextField(
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.onSizeChanged { size = it },
                    singleLine = singleLine,
                    interactionSource = interactionSource
                ) {
                    TextFieldDecorationBox(
                        value = value,
                        innerTextField = it,
                        enabled = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        singleLine = singleLine,
                        placeholder = { Spacer(Modifier.size(placeholderDimension)) },
                        contentPadding = PaddingValues(vertical = verticalPadding)
                    )
                }
            }
        }

        rule.runOnIdle {
            with(Density) {
                assertThat(size).isNotNull()
                assertThat(size!!.height).isEqualTo(
                    (placeholderDimension + verticalPadding * 2).roundToPx())
            }
        }
    }

    @Test
    fun outlinedTextFieldBox_innerTextLocation_withMultilineLabel() {
        assertSizeAndPosition(
            padding = TextFieldDefaults.outlinedTextFieldPadding(),
            singleLine = false,
            expectedSize = LabelHeight / 2 + InnerTextFieldHeight + TextFieldPadding,
            expectedPosition = LabelHeight / 2,
            isVertical = true,
            isOutlined = true,
            label = {
                // imitates the multiline label
                Box(Modifier.size(10.dp, LabelHeight))
            }
        )
    }

    @Test
    fun outlinedTextFieldBox_singleLine_innerTextLocation_withMultilineLabel() {
        assertSizeAndPosition(
            padding = TextFieldDefaults.outlinedTextFieldPadding(),
            singleLine = true,
            expectedSize = LabelHeight / 2 + InnerTextFieldHeight + TextFieldPadding,
            expectedPosition = LabelHeight / 2,
            isVertical = true,
            isOutlined = true,
            label = {
                // imitates the multiline label
                Box(Modifier.size(10.dp, LabelHeight))
            }
        )
    }

    private fun assertVerticalSizeAndPosition_outlinedTextField(
        padding: PaddingValues,
        singleLine: Boolean,
        expectedHeight: Dp,
        expectedPosition: Dp
    ) {
        assertSizeAndPosition(
            padding = padding,
            singleLine = singleLine,
            expectedSize = expectedHeight,
            expectedPosition = expectedPosition,
            isVertical = true,
            isOutlined = true,
        )
    }

    private fun assertHorizontalSizeAndPosition_outlinedTextField(
        padding: PaddingValues,
        rtl: Boolean,
        expectedWidth: Dp,
        expectedPosition: Dp
    ) {
        assertSizeAndPosition(
            padding = padding,
            singleLine = true,
            expectedSize = expectedWidth,
            expectedPosition = expectedPosition,
            isVertical = false,
            isOutlined = true,
            layoutDirection = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        )
    }

    private fun assertVerticalSizeAndPosition_textField(
        padding: PaddingValues,
        singleLine: Boolean,
        hasLabel: Boolean,
        expectedHeight: Dp,
        expectedPosition: Dp
    ) {
        assertSizeAndPosition(
            padding = padding,
            singleLine = singleLine,
            expectedSize = expectedHeight,
            expectedPosition = expectedPosition,
            isVertical = true,
            isOutlined = false,
            label = if (hasLabel) {
                { Text("Label", modifier = Modifier.height(LabelHeight)) }
            } else { null },
        )
    }

    private fun assertHorizontalSizeAndPosition_textField(
        padding: PaddingValues,
        rtl: Boolean,
        hasLabel: Boolean,
        expectedWidth: Dp,
        expectedPosition: Dp
    ) {
        assertSizeAndPosition(
            padding = padding,
            singleLine = true,
            expectedSize = expectedWidth,
            expectedPosition = expectedPosition,
            isVertical = false,
            isOutlined = false,
            layoutDirection = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            label = if (hasLabel) {
                { Text("Label", modifier = Modifier.height(LabelHeight)) }
            } else { null },
        )
    }

    private fun assertSizeAndPosition(
        padding: PaddingValues,
        singleLine: Boolean,
        expectedSize: Dp,
        expectedPosition: Dp,
        isVertical: Boolean,
        isOutlined: Boolean,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        label: @Composable (() -> Unit)? = null,
    ) {
        var size: IntSize? = null
        var position: Offset? = null
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalDensity provides Density
            ) {
                Box(Modifier.onSizeChanged { size = it }) {
                    val value = "Text"
                    val interactionSource = remember { MutableInteractionSource() }
                    BasicTextField(
                        value = value,
                        onValueChange = {},
                        singleLine = singleLine,
                        interactionSource = interactionSource
                    ) {
                        val innerTextField: @Composable () -> Unit = {
                            Box(
                                Modifier
                                    .size(InnerTextFieldWidth, InnerTextFieldHeight)
                                    .onGloballyPositioned {
                                        position = it.positionInRoot()
                                    }
                            ) { it() }
                        }
                        if (isOutlined) {
                            OutlinedTextFieldDecorationBox(
                                value = value,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = singleLine,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = interactionSource,
                                contentPadding = padding,
                                label = label
                            )
                        } else {
                            TextFieldDecorationBox(
                                value = value,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = singleLine,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = interactionSource,
                                contentPadding = padding,
                                label = label
                            )
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            with(Density) {
                assertThat(size).isNotNull()
                if (isVertical) {
                    assertThat(size!!.height).isEqualTo(expectedSize.roundToPx())
                } else {
                    assertThat(size!!.width).isEqualTo(expectedSize.roundToPx())
                }
                assertThat(position).isNotNull()
                if (isVertical) {
                    assertThat(position!!.y.roundToInt()).isEqualTo(expectedPosition.roundToPx())
                } else {
                    assertThat(position!!.x.roundToInt()).isEqualTo(expectedPosition.roundToPx())
                }
            }
        }
    }
}