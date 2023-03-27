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

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class SearchBarTest {
    @get:Rule
    val rule = createComposeRule()

    private val SearchBarTestTag = "SearchBar"
    private val IconTestTag = "Icon"
    private val BackTestTag = "Back"

    @Test
    fun searchBar_becomesActiveOnClick_andInactiveOnBack() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                var active by remember { mutableStateOf(false) }

                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    query = "Query",
                    onQueryChange = {},
                    onSearch = {},
                    active = active,
                    onActiveChange = { active = it },
                ) {
                    Button(
                        onClick = { dispatcher.onBackPressed() },
                        modifier = Modifier.testTag(BackTestTag),
                        content = { Text("Content") },
                    )
                }
            }
        }

        // For the purposes of this test, the content is the back button
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertIsDisplayed()

        rule.onNodeWithTag(BackTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()
    }

    @Test
    fun searchBar_onImeAction_executesSearchCallback() {
        var capturedSearchQuery = ""

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                var active by remember { mutableStateOf(true) }

                SearchBar(
                    query = "Query",
                    onQueryChange = {},
                    onSearch = { capturedSearchQuery = it },
                    active = active,
                    onActiveChange = { active = it },
                    content = { Text("Content") },
                )
            }
        }
        // onNodeWithText instead of onNodeWithTag to access the underlying text field
        rule.onNodeWithText("Query").performImeAction()
        assertThat(capturedSearchQuery).isEqualTo("Query")
    }

    @Test
    fun searchBar_inactiveSize() {
        rule.setMaterialContentForSizeAssertions {
            SearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Hint") },
                content = {},
            )
        }
            .assertWidthIsEqualTo(SearchBarMinWidth)
            .assertHeightIsEqualTo(SearchBarDefaults.InputFieldHeight + SearchBarVerticalPadding)
    }

    @Test
    fun searchBar_activeSize() {
        val totalHeight = 500.dp
        val totalWidth = 325.dp
        val searchBarSize = Ref<IntSize>()

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.size(width = totalWidth, height = totalHeight)) {
                SearchBar(
                    modifier = Modifier.onGloballyPositioned {
                        searchBarSize.value = it.size
                    },
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    active = true,
                    onActiveChange = {},
                    placeholder = { Text("Hint") },
                    content = { Text("Content") },
                )
            }
        }

        rule.runOnIdleWithDensity {
            assertThat(searchBarSize.value?.width).isEqualTo(totalWidth.roundToPx())
            assertThat(searchBarSize.value?.height).isEqualTo(totalHeight.roundToPx())
        }
    }

    @Test
    fun searchBar_clickingIconButton_doesNotExpandSearchBarItself() {
        var iconClicked = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                var active by remember { mutableStateOf(false) }

                SearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    query = "Query",
                    onQueryChange = {},
                    onSearch = {},
                    active = active,
                    onActiveChange = { active = it },
                    trailingIcon = {
                        IconButton(
                            onClick = { iconClicked = true },
                            modifier = Modifier.testTag(IconTestTag)
                        ) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                    }
                ) {
                    Text("Content")
                }
            }
        }

        rule.onNodeWithText("Content").assertDoesNotExist()

        // Click icon, not search bar
        rule.onNodeWithTag(IconTestTag).performClick()
        assertThat(iconClicked).isTrue()
        rule.onNodeWithText("Content").assertDoesNotExist()

        // Click search bar
        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithText("Content").assertIsDisplayed()
    }

    @Test
    fun dockedSearchBar_becomesActiveOnClick_andInactiveOnBack() {
        rule.setMaterialContent(lightColorScheme()) {
            Column(Modifier.fillMaxSize()) {
                val dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                var active by remember { mutableStateOf(false) }

                DockedSearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    query = "Query",
                    onQueryChange = {},
                    onSearch = {},
                    active = active,
                    onActiveChange = { active = it },
                ) {
                    Button(
                        onClick = { dispatcher.onBackPressed() },
                        modifier = Modifier.testTag(BackTestTag),
                        content = { Text("Content") },
                    )
                }
            }
        }

        // For the purposes of this test, the content is the back button
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()

        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertIsDisplayed()

        rule.onNodeWithTag(BackTestTag).performClick()
        rule.onNodeWithTag(BackTestTag).assertDoesNotExist()
    }

    @Test
    fun dockedSearchBar_onImeAction_executesSearchCallback() {
        var capturedSearchQuery = ""

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                var active by remember { mutableStateOf(true) }

                DockedSearchBar(
                    query = "Query",
                    onQueryChange = {},
                    onSearch = { capturedSearchQuery = it },
                    active = active,
                    onActiveChange = { active = it },
                    content = { Text("Content") },
                )
            }
        }
        // onNodeWithText instead of onNodeWithTag to access the underlying text field
        rule.onNodeWithText("Query").performImeAction()
        assertThat(capturedSearchQuery).isEqualTo("Query")
    }

    @Test
    fun dockedSearchBar_inactiveSize() {
        rule.setMaterialContentForSizeAssertions {
            DockedSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Hint") },
                content = {},
            )
        }
            .assertWidthIsEqualTo(SearchBarMinWidth)
            .assertHeightIsEqualTo(SearchBarDefaults.InputFieldHeight)
    }

    @Test
    fun dockedSearchBar_activeSize() {
        rule.setMaterialContentForSizeAssertions {
            DockedSearchBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
                active = true,
                onActiveChange = {},
                placeholder = { Text("Hint") },
                content = { Text("Content") },
            )
        }
            .assertWidthIsEqualTo(SearchBarMinWidth)
            .assertHeightIsEqualTo(SearchBarDefaults.InputFieldHeight + DockedActiveTableMinHeight)
    }

    @Test
    fun dockedSearchBar_clickingIconButton_doesNotExpandSearchBarItself() {
        var iconClicked = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.fillMaxSize()) {
                var active by remember { mutableStateOf(false) }

                DockedSearchBar(
                    modifier = Modifier.testTag(SearchBarTestTag),
                    query = "Query",
                    onQueryChange = {},
                    onSearch = {},
                    active = active,
                    onActiveChange = { active = it },
                    trailingIcon = {
                        IconButton(
                            onClick = { iconClicked = true },
                            modifier = Modifier.testTag(IconTestTag)
                        ) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                    }
                ) {
                    Text("Content")
                }
            }
        }

        rule.onNodeWithText("Content").assertDoesNotExist()

        // Click icon, not search bar
        rule.onNodeWithTag(IconTestTag).performClick()
        assertThat(iconClicked).isTrue()
        rule.onNodeWithText("Content").assertDoesNotExist()

        // Click search bar
        rule.onNodeWithTag(SearchBarTestTag).performClick()
        rule.onNodeWithText("Content").assertIsDisplayed()
    }
}