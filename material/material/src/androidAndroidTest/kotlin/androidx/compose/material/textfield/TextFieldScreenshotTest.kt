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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.material.AmbientContentColor
import androidx.compose.material.GOLDEN_MATERIAL
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.setMaterialContent
import androidx.compose.runtime.Providers
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.move
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class TextFieldScreenshotTest {
    private val TextFieldTag = "TextField"

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun textField_withInput() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                TextField(
                    value = "Text",
                    onValueChange = {},
                    label = { Text("Label") }
                )
            }
        }

        assertAgainstGolden("filled_textField_withInput")
    }

    @Test
    fun textField_notFocused() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                TextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") }
                )
            }
        }

        assertAgainstGolden("filled_textField_not_focused")
    }

    @Test
    fun textField_focused() {
        rule.setMaterialContent {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                TextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") }
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag)
            // split click into (down) and (move, up) to enforce a composition in between
            .performGesture { down(center) }
            .performGesture { move(); up() }

        assertAgainstGolden("filled_textField_focused")
    }

    @Test
    fun textField_focused_rtl() {
        rule.setMaterialContent {
            Providers(AmbientLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.semantics(mergeDescendants = true) {}.testTag(TextFieldTag)) {
                    TextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Label") }
                    )
                }
            }
        }

        rule.onNodeWithTag(TextFieldTag)
            // split click into (down) and (move, up) to enforce a composition in between
            .performGesture { down(center) }
            .performGesture { move(); up() }

        assertAgainstGolden("filled_textField_focused_rtl")
    }

    @Test
    fun textField_error_focused() {
        rule.setMaterialContent {
            TextField(
                value = "Input",
                onValueChange = {},
                label = { Text("Label") },
                isErrorValue = true,
                modifier = Modifier.testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag)
            // split click into (down) and (move, up) to enforce a composition in between
            .performGesture { down(center) }
            .performGesture { move(); up() }

        assertAgainstGolden("filled_textField_focused_errorState")
    }

    @Test
    fun textField_error_notFocused() {
        rule.setMaterialContent {
            TextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                isErrorValue = true,
                modifier = Modifier.testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("filled_textField_notFocused_errorState")
    }

    @Test
    fun textField_textColor_fallbackToContentColor() {
        rule.setMaterialContent {
            Providers(AmbientContentColor provides Color.Green) {
                TextField(
                    value = "Hello, world!",
                    onValueChange = {},
                    modifier = Modifier.testTag(TextFieldTag)
                )
            }
        }

        assertAgainstGolden("filled_textField_textColor_defaultContentColor")
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}