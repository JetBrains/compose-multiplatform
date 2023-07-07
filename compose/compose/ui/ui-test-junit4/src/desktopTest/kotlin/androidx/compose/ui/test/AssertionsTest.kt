/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlin.test.assertFails
import org.junit.Rule
import org.junit.Test


/**
 * Tests the assert (e.g. [assertTextEquals]) functionality of the testing framework.
 */
class AssertionsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testAssertExists() {
        rule.setContent {
            Box(Modifier.testTag("tag"))
        }

        rule.onNodeWithTag("tag").assertExists()
        assertFails {
            rule.onNodeWithTag("non-tag").assertExists()
        }
    }

    @Test
    fun testAssertDoesNotExist() {
        rule.setContent {
            Box(Modifier.testTag("tag"))
        }

        rule.onNodeWithTag("text").assertDoesNotExist()
        assertFails {
            rule.onNodeWithTag("tag").assertDoesNotExist()
        }
    }

    @Test
    fun testAssertIsDisplayed() {
        rule.setContent {
            Column(
                Modifier.size(100.dp)
            ) {
                Box(
                    Modifier
                        .testTag("tag1")
                        .size(100.dp)
                )
                Box(
                    Modifier
                        .testTag("tag2")
                        .size(100.dp)
                )
            }
        }

        rule.onNodeWithTag("tag1").assertIsDisplayed()
        assertFails {
            rule.onNodeWithTag("tag2").assertIsDisplayed()
        }
    }

    @Test
    fun testAssertIsNotDisplayed() {
        rule.setContent {
            Column(
                Modifier.size(100.dp)
            ) {
                Box(
                    Modifier
                        .testTag("tag1")
                        .size(100.dp)
                )
                Box(
                    Modifier
                        .testTag("tag2")
                        .size(100.dp)
                )
            }
        }

        rule.onNodeWithTag("tag2").assertIsNotDisplayed()
        assertFails {
            rule.onNodeWithTag("tag1").assertIsNotDisplayed()
        }
    }

    @Test
    fun testAssertIsEnabled() {
        rule.setContent {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag("tag1")
            ) {}
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag("tag2")
            ) {}
        }

        rule.onNodeWithTag("tag1").assertIsEnabled()
        assertFails {
            rule.onNodeWithTag("tag2").assertIsEnabled()
        }
    }

    @Test
    fun testAssertIsNotEnabled() {
        rule.setContent {
            Button(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag("tag1")
            ) {}
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag("tag2")
            ) {}
        }

        rule.onNodeWithTag("tag2").assertIsNotEnabled()
        assertFails {
            rule.onNodeWithTag("tag1").assertIsNotEnabled()
        }
    }

    @Test
    fun testAssertIsOn() {
        rule.setContent {
            Checkbox(
                checked = true,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag1")
            )
            Checkbox(
                checked = false,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag1").assertIsOn()
        assertFails {
            rule.onNodeWithTag("tag2").assertIsOn()
        }
    }

    @Test
    fun testAssertIsOff() {
        rule.setContent {
            Checkbox(
                checked = true,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag1")
            )
            Checkbox(
                checked = false,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag2").assertIsOff()
        assertFails {
            rule.onNodeWithTag("tag1").assertIsOff()
        }
    }

    @Test
    fun testAssertIsSelected() {
        rule.setContent {
            RadioButton(
                selected = true,
                onClick = { },
                modifier = Modifier.testTag("tag1")
            )
            RadioButton(
                selected = false,
                onClick = { },
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag1").assertIsSelected()
        assertFails {
            rule.onNodeWithTag("tag2").assertIsSelected()
        }
    }

    @Test
    fun testAssertIsNotSelected() {
        rule.setContent {
            RadioButton(
                selected = true,
                onClick = { },
                modifier = Modifier.testTag("tag1")
            )
            RadioButton(
                selected = false,
                onClick = { },
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag2").assertIsNotSelected()
        assertFails {
            rule.onNodeWithTag("tag1").assertIsNotSelected()
        }
    }

    @Test
    fun testAssertIsToggleable() {
        rule.setContent {
            Checkbox(
                checked = false,
                onCheckedChange = { },
                modifier = Modifier.testTag("tag1")
            )
            Text(
                text = "Text",
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag1").assertIsToggleable()
        assertFails {
            rule.onNodeWithTag("tag2").assertIsToggleable()
        }
    }

    @Test
    fun testAssertIsSelectable() {
        rule.setContent {
            RadioButton(
                selected = false,
                onClick = { },
                modifier = Modifier.testTag("tag1")
            )
            Text(
                text = "Text",
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag1").assertIsSelectable()
        assertFails {
            rule.onNodeWithTag("tag2").assertIsSelectable()
        }
    }

    @Test
    fun testAssertIsFocused() {
        rule.setContent {
            val focusRequester = remember { FocusRequester() }
            Box(
                Modifier
                    .testTag("tag1")
                    .focusRequester(focusRequester)
                    .focusable()
            )
            Box(
                Modifier
                    .testTag("tag2")
                    .focusable()
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        rule.onNodeWithTag("tag1").assertIsFocused()
        assertFails {
            rule.onNodeWithTag("tag2").assertIsFocused()
        }
    }

    @Test
    fun testAssertIsNotFocused() {
        rule.setContent {
            rule.setContent {
                val focusRequester = remember { FocusRequester() }
                Box(
                    Modifier
                        .testTag("tag1")
                        .focusRequester(focusRequester)
                        .focusable()
                )
                Box(
                    Modifier
                        .testTag("tag2")
                        .focusable()
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }

        rule.onNodeWithTag("tag2").assertIsNotFocused()
        assertFails {
            rule.onNodeWithTag("tag1").assertIsNotFocused()
        }
    }

    @Test
    fun testAssertContentDescriptionEquals() {
        rule.setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag1")
            ) {
                Box(Modifier.semantics { contentDescription = "desc1" })
                Box(Modifier.semantics { contentDescription = "desc2" })
            }
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag2")
            ) {
                Box(Modifier.semantics { contentDescription = "desc1" })
            }
        }

        rule.onNodeWithTag("tag1").assertContentDescriptionEquals("desc1", "desc2")
        assertFails {
            rule.onNodeWithTag("tag2").assertContentDescriptionEquals("desc1", "desc2")
        }
    }

    @Test
    fun testAssertContentDescriptionContains() {
        rule.setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag1")
            ) {
                Box(Modifier.semantics { contentDescription = "desc1" })
                Box(Modifier.semantics { contentDescription = "desc2" })
            }
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag2")
            ) {
                Box(Modifier.semantics { contentDescription = "desc" })
            }
        }

        rule.onNodeWithTag("tag1").assertContentDescriptionContains("desc1")
        rule.onNodeWithTag("tag1").assertContentDescriptionContains("desc2")
        assertFails {
            rule.onNodeWithTag("tag2").assertContentDescriptionContains("desc1")
        }
    }

    @Test
    fun testAssertTextEquals() {
        rule.setContent {
            Text(
                text = "Hello, Compose",
                modifier = Modifier.testTag("tag1")
            )
            TextField(
                value = "Hello, TextField",
                onValueChange = {},
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag1").assertTextEquals("Hello, Compose")
        rule.onNodeWithTag("tag2").assertTextEquals("Hello, TextField")
        assertFails {
            rule.onNodeWithTag("tag1").assertTextEquals("Hello")
        }
    }

    @Test
    fun testAssertTextContains() {
        rule.setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag")
            ) {
                Text(text = "text1")
                Text(text = "text2")
            }
        }

        rule.onNodeWithTag("tag").assertTextContains("text1")
        rule.onNodeWithTag("tag").assertTextContains("text2")
        assertFails {
            rule.onNodeWithTag("tag").assertTextContains("Hello")
        }
    }

    @Test
    fun testAssertValueEquals() {
        rule.setContent {
            Box(Modifier.semantics { stateDescription = "desc" }.testTag("tag"))
        }

        rule.onNodeWithTag("tag").assertValueEquals("desc")
        assertFails {
            rule.onNodeWithTag("tag").assertValueEquals("text")
        }
    }

    @Test
    fun testAssertRangeInfoEquals() {
        rule.setContent {
            Slider(
                valueRange = 0f..100f,
                value = 50f,
                onValueChange = {},
                modifier = Modifier.testTag("tag")
            )
        }

        rule.onNodeWithTag("tag").assertRangeInfoEquals(
            ProgressBarRangeInfo(
                current = 50f,
                range = 0f..100f
            )
        )
        assertFails {
            rule.onNodeWithTag("tag").assertRangeInfoEquals(
                ProgressBarRangeInfo(
                    current = 49f,
                    range = 0f..100f
                )
            )
        }
    }

    @Test
    fun testAssertHasClickAction() {
        rule.setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag1")
            ) {}
            Text(
                text = "Hello",
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag1").assertHasClickAction()
        assertFails {
            rule.onNodeWithTag("tag2").assertHasClickAction()
        }
    }

    @Test
    fun testAssertHasNoClickAction() {
        rule.setContent {
            Button(
                onClick = {},
                modifier = Modifier.testTag("tag1")
            ) {}
            Text(
                text = "Hello",
                modifier = Modifier.testTag("tag2")
            )
        }

        rule.onNodeWithTag("tag2").assertHasNoClickAction()
        assertFails {
            rule.onNodeWithTag("tag1").assertHasNoClickAction()
        }
    }

    @Test
    fun testAssertAny() {
        rule.setContent {
            Text(
                text = "Hello",
                modifier = Modifier.testTag("tag")
            )
            Text(
                text = "Compose",
                modifier = Modifier.testTag("tag")
            )
        }

        rule.onAllNodesWithTag("tag").assertAny(hasText("Hello"))
        rule.onAllNodesWithTag("tag").assertAny(hasText("Compose"))
        assertFails {
            rule.onAllNodesWithTag("tag").assertAny(hasText("Text"))
        }
    }

    @Test
    fun testAssertAll() {
        rule.setContent {
            Text(
                text = "Hello, World",
                modifier = Modifier.testTag("tag")
            )
            Text(
                text = "Hello, Compose",
                modifier = Modifier.testTag("tag")
            )
        }

        rule.onAllNodesWithTag("tag").assertAll(hasText("Hello", substring = true))
        assertFails {
            rule.onAllNodesWithTag("tag").assertAll(hasText("World", substring = true))
        }
    }
}
