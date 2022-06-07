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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.tokens.DialogTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
class AlertDialogTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun customStyleProperties_shouldApply() {
        var buttonContentColor = Color.Unspecified
        var expectedButtonContentColor = Color.Unspecified
        var iconContentColor = Color.Unspecified
        var titleContentColor = Color.Unspecified
        var textContentColor = Color.Unspecified
        rule.setContent {
            AlertDialog(
                onDismissRequest = {},
                modifier = Modifier.border(10.dp, Color.Blue),
                icon = {
                    Icon(Icons.Filled.Favorite, contentDescription = null)
                    iconContentColor = LocalContentColor.current
                },
                title = {
                    Text(text = "Title")
                    titleContentColor = LocalContentColor.current
                },
                text = {
                    Text("Text")
                    textContentColor = LocalContentColor.current
                },
                confirmButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Confirm")
                        buttonContentColor = LocalContentColor.current
                        expectedButtonContentColor = DialogTokens.ActionLabelTextColor.toColor()
                    }
                },
                containerColor = Color.Yellow,
                tonalElevation = 0.dp,
                iconContentColor = Color.Green,
                titleContentColor = Color.Magenta,
                textContentColor = Color.DarkGray
            )
        }

        // Assert background
        rule.onNode(isDialog())
            .captureToImage()
            .assertContainsColor(Color.Yellow) // Background
            .assertContainsColor(Color.Blue) // Modifier border

        // Assert content colors
        rule.runOnIdle {
            assertThat(buttonContentColor).isEqualTo(expectedButtonContentColor)
            assertThat(iconContentColor).isEqualTo(Color.Green)
            assertThat(titleContentColor).isEqualTo(Color.Magenta)
            assertThat(textContentColor).isEqualTo(Color.DarkGray)
        }
    }

    /**
     * Ensure that Dialogs don't press up against the edges of the screen.
     */
    @Test
    fun alertDialogDoesNotConsumeFullScreenWidth() {
        val dialogWidthCh = Channel<Int>(Channel.CONFLATED)
        var maxDialogWidth = 0
        var screenWidth by mutableStateOf(0)
        rule.setContent {
            val context = LocalContext.current
            val density = LocalDensity.current
            val resScreenWidth = context.resources.configuration.screenWidthDp
            with(density) {
                screenWidth = resScreenWidth.dp.roundToPx()
                maxDialogWidth = AlertDialogMaxWidth.roundToPx()
            }

            AlertDialog(
                modifier = Modifier.onSizeChanged { dialogWidthCh.trySend(it.width) }
                    .fillMaxWidth(),
                onDismissRequest = {},
                title = { Text(text = "Title") },
                text = {
                    Text(
                        "This area typically contains the supportive text " +
                            "which presents the details regarding the Dialog's purpose."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Dismiss")
                    }
                },
            )
        }

        runBlocking {
            val dialogWidth = withTimeout(5_000) { dialogWidthCh.receive() }
            assertThat(dialogWidth).isLessThan(maxDialogWidth)
            assertThat(dialogWidth).isLessThan(screenWidth)
        }
    }

    /** Ensure the Dialog's min width. */
    @Test
    fun alertDialog_minWidth() {
        val dialogWidthCh = Channel<Int>(Channel.CONFLATED)
        var minDialogWidth = 0
        rule.setContent {
            with(LocalDensity.current) { minDialogWidth = AlertDialogMinWidth.roundToPx() }
            AlertDialog(
                modifier = Modifier.onSizeChanged { dialogWidthCh.trySend(it.width) },
                onDismissRequest = {},
                title = { Text(text = "Title") },
                text = { Text("Short") },
                confirmButton = {
                    TextButton(onClick = { /* doSomething() */ }) {
                        Text("Confirm")
                    }
                }
            )
        }

        runBlocking {
            val dialogWidth = withTimeout(5_000) { dialogWidthCh.receive() }
            assertThat(dialogWidth).isEqualTo(minDialogWidth)
        }
    }

    @Test
    fun alertDialog_withIcon_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            AlertDialog(
                onDismissRequest = {},
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.testTag(IconTestTag)
                    )
                },
                title = { Text(text = "Title", modifier = Modifier.testTag(TitleTestTag)) },
                text = { Text("Text", modifier = Modifier.testTag(TextTestTag)) },
                confirmButton = {
                    TextButton(
                        onClick = { /* doSomething() */ },
                        Modifier.testTag(ConfirmButtonTestTag).semantics(mergeDescendants = true) {}
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { /* doSomething() */ },
                        Modifier.testTag(DismissButtonTestTag).semantics(mergeDescendants = true) {}
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }

        val dialogBounds = rule.onNode(isDialog()).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()
        val confirmBtBounds = rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot()
        val dismissBtBounds = rule.onNodeWithTag(DismissButtonTestTag).getUnclippedBoundsInRoot()

        rule.onNodeWithTag(IconTestTag)
            // Dialog's icon should be centered (icon size is 24dp)
            .assertLeftPositionInRootIsEqualTo((dialogBounds.width - 24.dp) / 2)
            // Dialog's icon should be 24dp from the top
            .assertTopPositionInRootIsEqualTo(24.dp)

        rule.onNodeWithTag(TitleTestTag)
            // Title should be centered (default alignment when an icon presence)
            .assertLeftPositionInRootIsEqualTo((dialogBounds.width - titleBounds.width) / 2)
            // Title should be 16dp below the icon.
            .assertTopPositionInRootIsEqualTo(iconBounds.bottom + 16.dp)

        rule.onNodeWithTag(TextTestTag)
            // Text should be 24dp from the start.
            .assertLeftPositionInRootIsEqualTo(24.dp)
            // Text should be 16dp below the title.
            .assertTopPositionInRootIsEqualTo(titleBounds.bottom + 16.dp)

        rule.onNodeWithTag(ConfirmButtonTestTag)
            // Confirm button should be 24dp from the right.
            .assertLeftPositionInRootIsEqualTo(dialogBounds.right - 24.dp - confirmBtBounds.width)
            // Buttons should be 18dp from the bottom (test button default height is 48dp).
            .assertTopPositionInRootIsEqualTo(dialogBounds.bottom - 18.dp - 48.dp)

        // Check the measurements between the components.
        (confirmBtBounds.top - textBounds.bottom).assertIsEqualTo(
            18.dp,
            "padding between the text and the button"
        )
        (confirmBtBounds.top).assertIsEqualTo(dismissBtBounds.top, "dialog buttons top alignment")
        (confirmBtBounds.bottom).assertIsEqualTo(
            dismissBtBounds.bottom,
            "dialog buttons bottom alignment"
        )
        (confirmBtBounds.left - 8.dp).assertIsEqualTo(
            dismissBtBounds.right,
            "horizontal padding between the dialog buttons"
        )
    }

    @Test
    fun alertDialog_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = "Title", modifier = Modifier.testTag(TitleTestTag)) },
                text = { Text("Text", modifier = Modifier.testTag(TextTestTag)) },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { /* doSomething() */ },
                        Modifier.testTag(DismissButtonTestTag).semantics(mergeDescendants = true) {}
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }

        val dialogBounds = rule.onNode(isDialog()).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()
        val dismissBtBounds = rule.onNodeWithTag(DismissButtonTestTag).getUnclippedBoundsInRoot()

        rule.onNodeWithTag(TitleTestTag)
            // Title should 24dp from the left.
            .assertLeftPositionInRootIsEqualTo(24.dp)
            // Title should be 24dp from the top.
            .assertTopPositionInRootIsEqualTo(24.dp)

        rule.onNodeWithTag(TextTestTag)
            // Text should be 24dp from the start.
            .assertLeftPositionInRootIsEqualTo(24.dp)
            // Text should be 16dp below the title.
            .assertTopPositionInRootIsEqualTo(titleBounds.bottom + 16.dp)

        rule.onNodeWithTag(DismissButtonTestTag)
            // Dismiss button should be 24dp from the right.
            .assertLeftPositionInRootIsEqualTo(dialogBounds.right - 24.dp - dismissBtBounds.width)
            // Buttons should be 18dp from the bottom (test button default height is 48dp).
            .assertTopPositionInRootIsEqualTo(dialogBounds.bottom - 18.dp - 48.dp)

        (dismissBtBounds.top - textBounds.bottom).assertIsEqualTo(
            18.dp,
            "padding between the text and the button"
        )
    }

    @Test
    fun alertDialog_positioningWithLazyColumnText() {
        rule.setMaterialContent(lightColorScheme()) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = "Title", modifier = Modifier.testTag(TitleTestTag)) },
                text = {
                    LazyColumn(modifier = Modifier.testTag(TextTestTag)) {
                        items(100) {
                            Text(
                                text = "Message!"
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { /* doSomething() */ },
                        Modifier.testTag(DismissButtonTestTag).semantics(mergeDescendants = true) {}
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }

        val dialogBounds = rule.onNode(isDialog()).getUnclippedBoundsInRoot()
        val titleBounds = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()
        val dismissBtBounds = rule.onNodeWithTag(DismissButtonTestTag).getUnclippedBoundsInRoot()

        rule.onNodeWithTag(TitleTestTag)
            // Title should 24dp from the left.
            .assertLeftPositionInRootIsEqualTo(24.dp)
            // Title should be 24dp from the top.
            .assertTopPositionInRootIsEqualTo(24.dp)

        rule.onNodeWithTag(TextTestTag)
            // Text should be 24dp from the start.
            .assertLeftPositionInRootIsEqualTo(24.dp)
            // Text should be 16dp below the title.
            .assertTopPositionInRootIsEqualTo(titleBounds.bottom + 16.dp)

        rule.onNodeWithTag(DismissButtonTestTag)
            // Dismiss button should be 24dp from the right.
            .assertLeftPositionInRootIsEqualTo(dialogBounds.right - 24.dp - dismissBtBounds.width)
            // Buttons should be 18dp from the bottom (test button default height is 48dp).
            .assertTopPositionInRootIsEqualTo(dialogBounds.bottom - 18.dp - 48.dp)

        (dismissBtBounds.top - textBounds.bottom).assertIsEqualTo(
            18.dp,
            "padding between the text and the button"
        )
    }
}

private val AlertDialogMinWidth = 280.dp
private val AlertDialogMaxWidth = 560.dp
private const val IconTestTag = "icon"
private const val TitleTestTag = "title"
private const val TextTestTag = "text"
private const val ConfirmButtonTestTag = "confirmButton"
private const val DismissButtonTestTag = "dismissButton"
