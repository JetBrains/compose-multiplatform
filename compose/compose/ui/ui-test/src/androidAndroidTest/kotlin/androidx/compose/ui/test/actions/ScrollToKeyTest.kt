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

package androidx.compose.ui.test.actions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.indexForKey
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollToKeyTest {
    @get:Rule
    val rule = createComposeRule()

    private fun key(index: Int): String = "key_$index"
    private fun tag(index: Int): String = "tag_$index"

    @Test
    fun scrollToKeyInsideViewport() {
        // Setup a list
        rule.setContent { LazyColumnContent() }

        // ScrollToKey "key_1"
        rule.onNodeWithTag(tag(1)).assertExists()
        rule.onNode(hasScrollToKeyAction()).assertExists()
        rule.onNode(hasScrollToKeyAction()).performScrollToKey("key_1")
        rule.onNodeWithTag(tag(1)).assertExists()
    }

    @Test
    fun scrollToKeyOutOfViewport() {
        // Setup a list
        rule.setContent { LazyColumnContent() }

        // ScrollToKey "key_10"
        rule.onNodeWithTag(tag(10)).assertDoesNotExist()
        rule.onNode(hasScrollToKeyAction()).assertExists()
        rule.onNode(hasScrollToKeyAction()).performScrollToKey("key_10")
        rule.onNodeWithTag(tag(10)).assertExists()
    }

    @Test
    fun scrollToNonExistentKey() {
        // Setup a list
        rule.setContent { LazyColumnContent() }

        // ScrollToKey "hello"
        expectError<IllegalArgumentException>(
            expectedMessage = "Failed to scroll to the item identified by \"hello\", " +
                "couldn't find the key."
        ) {
            rule.onNode(hasScrollToKeyAction()).performScrollToKey("hello")
        }
    }

    @Test
    fun missingSemantics_ScrollToIndex() {
        // Setup a node without ScrollToIndex, but with IndexForKey
        rule.setContent {
            Spacer(Modifier.testTag("tag").semantics { indexForKey { 0 } })
        }

        // Verify that it doesn't support performScrollToKey
        rule.onNode(hasScrollToKeyAction()).assertDoesNotExist()
        expectError<AssertionError>(
            expectedMessage = "Failed to scroll to the item identified by \"1\", " +
                "the node is missing \\[ScrollToIndex\\].*"
        ) {
            rule.onNodeWithTag("tag").performScrollToKey(1)
        }
    }

    @Test
    fun missingSemantics_IndexForKey() {
        // Setup a node without IndexForKey, but with ScrollToIndex
        rule.setContent {
            Spacer(Modifier.testTag("tag").semantics { scrollToIndex { true } })
        }

        // Verify that it doesn't support performScrollToKey
        rule.onNode(hasScrollToKeyAction()).assertDoesNotExist()
        expectError<AssertionError>(
            expectedMessage = "Failed to scroll to the item identified by \"1\", " +
                "the node is missing \\[IndexForKey\\].*"
        ) {
            rule.onNodeWithTag("tag").performScrollToKey(1)
        }
    }

    @Composable
    fun LazyColumnContent() {
        LazyColumn(Modifier.requiredSize(100.dp)) {
            items(
                items = List(20) { it },
                key = { key(it) }
            ) {
                Spacer(Modifier.requiredHeight(30.dp).fillMaxWidth().testTag(tag(it)))
            }
        }
    }
}
