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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.internal.keyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesktopAlertDialogTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun alignedToCenter_inPureWindow() {
        val rootSize = IntSize(1024, 768) // default value
        val dialogSize = IntSize(150, 150)
        var location = Offset.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                @OptIn(ExperimentalMaterialApi::class)
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("AlerDialog") },
                    text = { Text("Apply?") },
                    confirmButton = { Button(onClick = {}) { Text("Apply") } },
                    modifier = Modifier.size(dialogSize.width.dp, dialogSize.height.dp)
                        .onGloballyPositioned { location = it.positionInRoot() }
                )
            }
        }
        rule.runOnIdle {
           assertThat(location).isEqualTo(calculateCenterPosition(rootSize, dialogSize))
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `pressing ESC button invokes onDismissRequest`() {
        val dialogSize = IntSize(150, 150)

        var dismissCount = 0
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                @OptIn(ExperimentalMaterialApi::class)
                AlertDialog(
                    onDismissRequest = { dismissCount++ },
                    title = { Text("AlerDialog") },
                    text = { Text("Apply?") },
                    confirmButton = { Button(onClick = {}) { Text("Apply") } },
                    modifier = Modifier.size(dialogSize.width.dp, dialogSize.height.dp)
                        .testTag("alertDialog")
                )
            }
        }

        rule.onNodeWithTag("alertDialog")
            .performKeyPress(keyEvent(Key.Escape, KeyEventType.KeyDown))

        rule.runOnIdle {
            assertEquals(1, dismissCount)
        }

        rule.onNodeWithTag("alertDialog")
            .performKeyPress(keyEvent(Key.Escape, KeyEventType.KeyUp))

        rule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/2857
    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun `shadow drawn at content bounds`() {
        // Show an AlertDialog with very large horizontal padding and check that the pixel
        // at the edge of where the dialog would have been without padding has the same color as the
        // background.
        val screenshot = renderComposeScene(400, 400){
            AlertDialog(
                modifier = Modifier
                    .size(width = 400.dp, height = 100.dp)
                    .padding(horizontal = 150.dp),
                onDismissRequest = {},
                title = {},
                text = {},
                dismissButton = {},
                confirmButton = {},
            )
        }

        val pixels = screenshot.toComposeImageBitmap().toPixelMap()
        val backgroundPixel = pixels[0, 0]
        val nearEdgeWithoutPaddingPixel = pixels[0, 200]
        val nearRealEdgePixel = pixels[149, 200]

        assertEquals(nearEdgeWithoutPaddingPixel, backgroundPixel)

        // Also check that the shadow is present near the actual edge of the content
        assertNotEquals(nearRealEdgePixel, backgroundPixel)
    }

    private fun calculateCenterPosition(rootSize: IntSize, childSize: IntSize): Offset {
        val x = (rootSize.width - childSize.width) / 2f
        val y = (rootSize.height - childSize.height) / 2f
        return Offset(x, y)
    }
}
