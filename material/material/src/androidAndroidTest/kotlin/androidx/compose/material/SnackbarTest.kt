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
import androidx.compose.runtime.Providers
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Text
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.DpConstraints
import androidx.compose.foundation.layout.Stack
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEqualTo
import androidx.ui.test.assertIsNotEqualTo
import androidx.ui.test.assertShape
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.performClick
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithText
import androidx.ui.test.getAlignmentLinePosition
import androidx.ui.test.getUnclippedBoundsInRoot
import androidx.compose.foundation.text.FirstBaseline
import androidx.compose.foundation.text.LastBaseline
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val longText = "Message is very long and long and long and long and long " +
            "and long and long and long and long and long and long"

    @Test
    fun defaultSnackbar_semantics() {
        var clicked = false
        composeTestRule.setMaterialContent {
            Stack {
                Snackbar(text = { Text("Message") }, action = {
                    TextButton(onClick = { clicked = true }) {
                        Text("UNDO")
                    }
                })
            }
        }

        onNodeWithText("Message")
            .assertExists()

        assertThat(clicked).isFalse()

        onNodeWithText("UNDO")
            .performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun snackbar_shortTextOnly_defaultSizes() {
        val snackbar = composeTestRule.setMaterialContentForSizeAssertions(
            DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    Text("Message")
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(48.dp)

        val firstBaseLine = onNodeWithText("Message").getAlignmentLinePosition(FirstBaseline)
        val lastBaseLine = onNodeWithText("Message").getAlignmentLinePosition(LastBaseline)
        firstBaseLine.assertIsNotEqualTo(0.dp, "first baseline")
        firstBaseLine.assertIsEqualTo(lastBaseLine, "first baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = onNodeWithText("Message").getUnclippedBoundsInRoot()

        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top

        textTopOffset.assertIsEqualTo(textBottomOffset)
    }

    @Test
    fun snackbar_shortTextOnly_bigFont_centered() {
        val snackbar = composeTestRule.setMaterialContentForSizeAssertions(
            DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    Text("Message", fontSize = 30.sp)
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)

        val firstBaseLine = onNodeWithText("Message").getAlignmentLinePosition(FirstBaseline)
        val lastBaseLine = onNodeWithText("Message").getAlignmentLinePosition(LastBaseline)
        firstBaseLine.assertIsNotEqualTo(0.dp, "first baseline")
        firstBaseLine.assertIsEqualTo(lastBaseLine, "first baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = onNodeWithText("Message").getUnclippedBoundsInRoot()

        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top

        textTopOffset.assertIsEqualTo(textBottomOffset)
    }

    @Test
    fun snackbar_shortTextAndButton_alignment() {
        val snackbar = composeTestRule.setMaterialContentForSizeAssertions(
            DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    Text("Message")
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.testTag("button")
                    ) {
                        Text("Undo")
                    }
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(48.dp)

        val textBaseLine = onNodeWithText("Message").getAlignmentLinePosition(FirstBaseline)
        val buttonBaseLine = onNodeWithTag("button").getAlignmentLinePosition(FirstBaseline)
        textBaseLine.assertIsNotEqualTo(0.dp, "text baseline")
        buttonBaseLine.assertIsNotEqualTo(0.dp, "button baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = onNodeWithText("Message").getUnclippedBoundsInRoot()
        val buttonBounds = onNodeWithText("Undo").getUnclippedBoundsInRoot()

        val buttonTopOffset = buttonBounds.top - snackBounds.top
        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top
        textTopOffset.assertIsEqualTo(textBottomOffset)

        (buttonBaseLine + buttonTopOffset).assertIsEqualTo(textBaseLine + textTopOffset)
    }

    @Test
    fun snackbar_shortTextAndButton_bigFont_alignment() {
        val snackbar = composeTestRule.setMaterialContentForSizeAssertions(
            DpConstraints(maxWidth = 400.dp)
        ) {
            val fontSize = 30.sp
            Snackbar(
                text = {
                    Text("Message", fontSize = fontSize)
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.testTag("button")
                    ) {
                        Text("Undo", fontSize = fontSize)
                    }
                }
            )
        }

        val textBaseLine = onNodeWithText("Message").getAlignmentLinePosition(FirstBaseline)
        val buttonBaseLine = onNodeWithTag("button").getAlignmentLinePosition(FirstBaseline)
        textBaseLine.assertIsNotEqualTo(0.dp, "text baseline")
        buttonBaseLine.assertIsNotEqualTo(0.dp, "button baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = onNodeWithText("Message").getUnclippedBoundsInRoot()
        val buttonBounds = onNodeWithText("Undo").getUnclippedBoundsInRoot()

        val buttonTopOffset = buttonBounds.top - snackBounds.top
        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top
        textTopOffset.assertIsEqualTo(textBottomOffset)

        (buttonBaseLine + buttonTopOffset).assertIsEqualTo(textBaseLine + textTopOffset)
    }

    @Test
    fun snackbar_longText_sizes() {
        val snackbar = composeTestRule.setMaterialContentForSizeAssertions(
            DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    Text(longText, Modifier.testTag("text"), maxLines = 2)
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(68.dp)

        val firstBaseline = onNodeWithTag("text").getFirstBaselinePosition()
        val lastBaseline = onNodeWithTag("text").getLastBaselinePosition()

        firstBaseline.assertIsNotEqualTo(0.dp, "first baseline")
        lastBaseline.assertIsNotEqualTo(0.dp, "last baseline")
        firstBaseline.assertIsNotEqualTo(lastBaseline, "first baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = onNodeWithTag("text").getUnclippedBoundsInRoot()

        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top

        textTopOffset.assertIsEqualTo(textBottomOffset)
    }

    @Test
    fun snackbar_longTextAndButton_alignment() {
        val snackbar = composeTestRule.setMaterialContentForSizeAssertions(
            DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    Text(longText, Modifier.testTag("text"), maxLines = 2)
                },
                action = {
                    TextButton(
                        modifier = Modifier.testTag("button"),
                        onClick = {}
                    ) {
                        Text("Undo")
                    }
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(68.dp)

        val textFirstBaseLine = onNodeWithTag("text").getFirstBaselinePosition()
        val textLastBaseLine = onNodeWithTag("text").getLastBaselinePosition()

        textFirstBaseLine.assertIsNotEqualTo(0.dp, "first baseline")
        textLastBaseLine.assertIsNotEqualTo(0.dp, "last baseline")
        textFirstBaseLine.assertIsNotEqualTo(textLastBaseLine, "first baseline")

        onNodeWithTag("text")
            .assertTopPositionInRootIsEqualTo(30.dp - textFirstBaseLine)

        val buttonBounds = onNodeWithTag("button").getUnclippedBoundsInRoot()
        val snackBounds = snackbar.getUnclippedBoundsInRoot()

        val buttonCenter = buttonBounds.top + (buttonBounds.height / 2)
        buttonCenter.assertIsEqualTo(snackBounds.height / 2, "button center")
    }

    @Test
    fun snackbar_textAndButtonOnSeparateLine_alignment() {
        val snackbar = composeTestRule.setMaterialContentForSizeAssertions(
            DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    Text("Message")
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.testTag("button")
                    ) {
                        Text("Undo")
                    }
                },
                actionOnNewLine = true
            )
        }

        val textFirstBaseLine = onNodeWithText("Message").getFirstBaselinePosition()
        val textLastBaseLine = onNodeWithText("Message").getLastBaselinePosition()
        val textBounds = onNodeWithText("Message").getUnclippedBoundsInRoot()
        val buttonBounds = onNodeWithTag("button").getUnclippedBoundsInRoot()

        onNodeWithText("Message")
            .assertTopPositionInRootIsEqualTo(30.dp - textFirstBaseLine)

        onNodeWithTag("button")
            .assertTopPositionInRootIsEqualTo(18.dp + textBounds.top + textLastBaseLine)

        snackbar
            .assertHeightIsEqualTo(8.dp + buttonBounds.top + buttonBounds.height)
            .assertWidthIsEqualTo(8.dp + buttonBounds.left + buttonBounds.width)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shapeAndColorFromThemeIsUsed() {
        val shape = CutCornerShape(8.dp)
        var background = Color.Yellow
        var snackBarColor = Color.Transparent
        composeTestRule.setMaterialContent {
            Stack {
                background = MaterialTheme.colors.surface
                // Snackbar has a background color of onSurface with an alpha applied blended
                // on top of surface
                snackBarColor = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    .compositeOver(background)
                Providers(ShapesAmbient provides Shapes(medium = shape)) {
                    Snackbar(modifier = Modifier
                        .semantics(mergeAllDescendants = true) {}
                        .testTag("snackbar"),
                        text = { Text("") }
                    )
                }
            }
        }

        onNodeWithTag("snackbar")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = shape,
                shapeColor = snackBarColor,
                backgroundColor = background,
                shapeOverlapPixelCount = with(composeTestRule.density) { 2.dp.toPx() }
            )
    }

    @Test
    @OptIn(ExperimentalMaterialApi::class)
    fun defaultSnackbar_dataVersion_proxiesParameters() {
        var clicked = false
        val snackbarData = object : SnackbarData {
            override val message: String = "Data message"
            override val actionLabel: String? = "UNDO"
            override val duration: SnackbarDuration = SnackbarDuration.Short

            override fun performAction() {
                clicked = true
            }

            override fun dismiss() {}
        }
        composeTestRule.setMaterialContent {
            Stack {
                Snackbar(snackbarData = snackbarData)
            }
        }

        onNodeWithText("Data message")
            .assertExists()

        assertThat(clicked).isFalse()

        onNodeWithText("UNDO")
            .performClick()

        assertThat(clicked).isTrue()
    }
}