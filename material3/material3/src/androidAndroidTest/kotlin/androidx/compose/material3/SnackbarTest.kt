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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertIsNotEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SnackbarTest {

    @get:Rule
    val rule = createComposeRule()

    private val longText = "Message is very long and long and long and long and long " +
        "and long and long and long and long and long and long"

    @Test
    fun defaultSnackbar_semantics() {
        var clicked = false
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Snackbar(
                    content = { Text("Message") },
                    action = {
                        TextButton(onClick = { clicked = true }) {
                            Text("UNDO")
                        }
                    }
                )
            }
        }

        rule.onNodeWithText("Message")
            .assertExists()

        assertThat(clicked).isFalse()

        rule.onNodeWithText("UNDO")
            .performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun snackbar_shortTextOnly_defaultSizes() {
        val snackbar = rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 300.dp
        ) {
            Snackbar(
                content = {
                    Text("Message")
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(48.dp)

        val firstBaseLine = rule.onNodeWithText("Message").getAlignmentLinePosition(FirstBaseline)
        val lastBaseLine = rule.onNodeWithText("Message").getAlignmentLinePosition(LastBaseline)
        firstBaseLine.assertIsNotEqualTo(0.dp, "first baseline")
        firstBaseLine.assertIsEqualTo(lastBaseLine, "first baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithText("Message").getUnclippedBoundsInRoot()

        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top

        textTopOffset.assertIsEqualTo(textBottomOffset)
    }

    @Test
    fun snackbar_shortTextOnly_bigFont_centered() {
        val snackbar = rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 300.dp
        ) {
            Snackbar(
                content = {
                    Text("Message", fontSize = 30.sp)
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)

        val firstBaseLine = rule.onNodeWithText("Message").getAlignmentLinePosition(FirstBaseline)
        val lastBaseLine = rule.onNodeWithText("Message").getAlignmentLinePosition(LastBaseline)
        firstBaseLine.assertIsNotEqualTo(0.dp, "first baseline")
        firstBaseLine.assertIsEqualTo(lastBaseLine, "first baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithText("Message").getUnclippedBoundsInRoot()

        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top

        textTopOffset.assertIsEqualTo(textBottomOffset)
    }

    @Test
    fun snackbar_shortTextAndButton_alignment() {
        val snackbar = rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 300.dp
        ) {
            Snackbar(
                content = {
                    Text("Message")
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.clipToBounds().testTag("button")
                    ) {
                        Text("Undo")
                    }
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(48.dp)

        val textBaseLine = rule.onNodeWithText("Message").getAlignmentLinePosition(FirstBaseline)
        val buttonBaseLine = rule.onNodeWithTag("button").getAlignmentLinePosition(FirstBaseline)
        textBaseLine.assertIsNotEqualTo(0.dp, "text baseline")
        buttonBaseLine.assertIsNotEqualTo(0.dp, "button baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithText("Message").getUnclippedBoundsInRoot()
        val buttonBounds = rule.onNodeWithText("Undo").getBoundsInRoot()

        val buttonTopOffset = buttonBounds.top - snackBounds.top
        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top
        textTopOffset.assertIsEqualTo(textBottomOffset)

        (buttonBaseLine + buttonTopOffset).assertIsEqualTo(textBaseLine + textTopOffset)
    }

    @Test
    fun snackbar_shortTextAndButton_bigFont_alignment() {
        val snackbar = rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 400.dp
        ) {
            val fontSize = 30.sp
            Snackbar(
                content = {
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

        val textBaseLine = rule.onNodeWithText("Message").getAlignmentLinePosition(FirstBaseline)
        val buttonBaseLine = rule.onNodeWithTag("button").getAlignmentLinePosition(FirstBaseline)
        textBaseLine.assertIsNotEqualTo(0.dp, "text baseline")
        buttonBaseLine.assertIsNotEqualTo(0.dp, "button baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithText("Message").getUnclippedBoundsInRoot()
        val buttonBounds = rule.onNodeWithText("Undo").getUnclippedBoundsInRoot()

        val buttonTopOffset = buttonBounds.top - snackBounds.top
        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top
        textTopOffset.assertIsEqualTo(textBottomOffset)

        (buttonBaseLine + buttonTopOffset).assertIsEqualTo(textBaseLine + textTopOffset)
    }

    @Test
    fun snackbar_longText_sizes() {
        val snackbar = rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 300.dp
        ) {
            Snackbar(
                content = {
                    Text(longText, Modifier.testTag("text"), maxLines = 2)
                }
            )
        }
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(68.dp)

        val firstBaseline = rule.onNodeWithTag("text").getFirstBaselinePosition()
        val lastBaseline = rule.onNodeWithTag("text").getLastBaselinePosition()

        firstBaseline.assertIsNotEqualTo(0.dp, "first baseline")
        lastBaseline.assertIsNotEqualTo(0.dp, "last baseline")
        firstBaseline.assertIsNotEqualTo(lastBaseline, "first baseline")

        val snackBounds = snackbar.getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()

        val textTopOffset = textBounds.top - snackBounds.top
        val textBottomOffset = textBounds.top - snackBounds.top

        textTopOffset.assertIsEqualTo(textBottomOffset)
    }

    @Test
    fun snackbar_longTextAndButton_alignment() {
        val snackbar = rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 300.dp
        ) {
            Snackbar(
                content = {
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

        val textFirstBaseLine = rule.onNodeWithTag("text").getFirstBaselinePosition()
        val textLastBaseLine = rule.onNodeWithTag("text").getLastBaselinePosition()

        textFirstBaseLine.assertIsNotEqualTo(0.dp, "first baseline")
        textLastBaseLine.assertIsNotEqualTo(0.dp, "last baseline")
        textFirstBaseLine.assertIsNotEqualTo(textLastBaseLine, "first baseline")

        rule.onNodeWithTag("text")
            .assertTopPositionInRootIsEqualTo(30.dp - textFirstBaseLine)

        val buttonBounds = rule.onNodeWithTag("button").getUnclippedBoundsInRoot()
        val snackBounds = snackbar.getUnclippedBoundsInRoot()

        val buttonCenter = buttonBounds.top + (buttonBounds.height / 2)
        buttonCenter.assertIsEqualTo(snackBounds.height / 2, "button center")
    }

    @Test
    fun snackbar_textAndButtonOnSeparateLine_alignment() {
        val snackbar = rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 300.dp
        ) {
            Snackbar(
                content = {
                    Text("Message", Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp))
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.testTag("button")
                    ) {
                        Text("Undo", Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp))
                    }
                },
                actionOnNewLine = true
            )
        }

        val textFirstBaseLine = rule.onNodeWithText("Message").getFirstBaselinePosition()
        val textLastBaseLine = rule.onNodeWithText("Message").getLastBaselinePosition()
        val textBounds = rule.onNodeWithText("Message").getUnclippedBoundsInRoot()
        val buttonBounds = rule.onNodeWithTag("button").getUnclippedBoundsInRoot()

        rule.onNodeWithText("Message")
            .assertTopPositionInRootIsEqualTo(30.dp - textFirstBaseLine)

        val lastBaselineToBottom = max(18.dp, 48.dp - textLastBaseLine)

        rule.onNodeWithTag("button").assertTopPositionInRootIsEqualTo(
            lastBaselineToBottom + textBounds.top + textLastBaseLine
        )

        snackbar
            .assertHeightIsEqualTo(2.dp + buttonBounds.top + buttonBounds.height)
            .assertWidthIsEqualTo(8.dp + buttonBounds.left + buttonBounds.width)
    }

    @Test
    fun defaultSnackbar_dataVersion_proxiesParameters() {
        var clicked = false
        val snackbarVisuals = object : SnackbarVisuals {
            override val message: String = "Data message"
            override val actionLabel: String = "UNDO"
            override val withDismissAction: Boolean = false
            override val duration: SnackbarDuration = SnackbarDuration.Short
        }
        val snackbarData = object : SnackbarData {
            override val visuals: SnackbarVisuals = snackbarVisuals

            override fun performAction() {
                clicked = true
            }

            override fun dismiss() {}
        }
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Snackbar(snackbarData = snackbarData)
            }
        }

        rule.onNodeWithText("Data message")
            .assertExists()

        assertThat(clicked).isFalse()

        rule.onNodeWithText("UNDO")
            .performClick()

        assertThat(clicked).isTrue()
    }
}
