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

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ExposedDropdownMenuTest {

    @get:Rule
    val rule = createComposeRule()

    private val EDMBoxTag = "ExposedDropdownMenuBoxTag"
    private val TFTag = "TextFieldTag"
    private val TrailingIconTag = "TrailingIconTag"
    private val EDMTag = "ExposedDropdownMenuTag"
    private val MenuItemTag = "MenuItemTag"
    private val OptionName = "Option 1"

    @Test
    fun expandedBehaviour_expandsOnClickAndCollapsesOnOutside() {
        var textFieldBounds = Rect.Zero
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                onTextFieldBoundsChanged = {
                    textFieldBounds = it
                }
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertDoesNotExist()

        // Click on the TextField
        rule.onNodeWithTag(TFTag).performClick()

        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Click outside EDM
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(
            (textFieldBounds.right + 1).toInt(),
            (textFieldBounds.bottom + 1).toInt(),
        )

        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun expandedBehaviour_collapseOnTextFieldClick() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it }
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(EDMTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Click on the TextField
        rule.onNodeWithTag(TFTag).performClick()

        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
    }

    @Test
    fun expandedBehaviour_expandsAndFocusesTextFieldOnTrailingIconClick() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(TrailingIconTag, useUnmergedTree = true).assertIsDisplayed()

        // Click on the Trailing Icon
        rule.onNodeWithTag(TrailingIconTag, useUnmergedTree = true).performClick()

        rule.onNodeWithTag(TFTag).assertIsFocused()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()
    }

    @Test
    fun uiProperties_menuMatchesTextWidth() {
        var textFieldBounds by mutableStateOf(Rect.Zero)
        var menuBounds by mutableStateOf(Rect.Zero)
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it },
                onTextFieldBoundsChanged = {
                    textFieldBounds = it
                },
                onMenuBoundsChanged = {
                    menuBounds = it
                }
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        rule.runOnIdle {
            assertThat(menuBounds.width).isEqualTo(textFieldBounds.width)
        }
    }

    @Test
    fun EDMBehaviour_rightOptionIsChosen() {
        rule.setMaterialContent(lightColorScheme()) {
            var expanded by remember { mutableStateOf(true) }
            ExposedDropdownMenuForTest(
                expanded = expanded,
                onExpandChange = { expanded = it }
            )
        }

        rule.onNodeWithTag(TFTag).assertIsDisplayed()
        rule.onNodeWithTag(MenuItemTag).assertIsDisplayed()

        // Choose the option
        rule.onNodeWithTag(MenuItemTag).performClick()

        // Menu should collapse
        rule.onNodeWithTag(MenuItemTag).assertDoesNotExist()
        rule.onNodeWithTag(TFTag).assertTextContains(OptionName)
    }

    @Test
    fun doesNotCrashWhenAnchorDetachedFirst() {
        var parent: FrameLayout? = null
        rule.setContent {
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        addView(ComposeView(context).apply {
                            setContent {
                                Box {
                                    ExposedDropdownMenuBox(expanded = true, onExpandedChange = {}) {
                                        Box(Modifier.size(20.dp))
                                    }
                                }
                            }
                        })
                    }.also { parent = it }
                }
            )
        }

        rule.runOnIdle {
            parent!!.removeAllViews()
        }

        rule.waitForIdle()

        // Should not have crashed.
    }

    @Composable
    fun ExposedDropdownMenuForTest(
        expanded: Boolean,
        onExpandChange: (Boolean) -> Unit,
        onTextFieldBoundsChanged: ((Rect) -> Unit)? = null,
        onMenuBoundsChanged: ((Rect) -> Unit)? = null
    ) {
        var selectedOptionText by remember { mutableStateOf("") }
        Box(Modifier.fillMaxSize()) {
            ExposedDropdownMenuBox(
                modifier = Modifier.testTag(EDMBoxTag).align(Alignment.Center),
                expanded = expanded,
                onExpandedChange = { onExpandChange(!expanded) }
            ) {
                TextField(
                    modifier = Modifier.testTag(TFTag)
                        .onGloballyPositioned {
                            onTextFieldBoundsChanged?.invoke(it.boundsInRoot())
                        },
                    value = selectedOptionText,
                    onValueChange = { selectedOptionText = it },
                    label = { Text("Label") },
                    trailingIcon = {
                        Box(
                            modifier = Modifier.testTag(TrailingIconTag)
                        ) {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        }
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    modifier = Modifier.testTag(EDMTag).onGloballyPositioned {
                        onMenuBoundsChanged?.invoke(it.boundsInRoot())
                    },
                    expanded = expanded,
                    onDismissRequest = { onExpandChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(OptionName) },
                        onClick = {
                            selectedOptionText = OptionName
                            onExpandChange(false)
                        },
                        modifier = Modifier.testTag(MenuItemTag)
                    )
                }
            }
        }
    }
}