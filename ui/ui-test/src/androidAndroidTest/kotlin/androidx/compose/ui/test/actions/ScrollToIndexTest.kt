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
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollToIndexTest {
    @get:Rule
    val rule = createComposeRule()

    private fun tag(index: Int): String = "tag_$index"

    @Test
    fun scrollToIndexInsideViewport() {
        // Setup a list
        rule.setContent { LazyColumnContent() }

        // ScrollToIndex 1
        rule.onNodeWithTag(tag(1)).assertExists()
        rule.onNode(hasScrollToIndexAction()).assertExists()
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        rule.onNodeWithTag(tag(1)).assertExists()
    }

    @Test
    fun scrollToIndexOutOfViewport() {
        // Setup a list
        rule.setContent { LazyColumnContent() }

        // ScrollToIndex 10
        rule.onNodeWithTag(tag(10)).assertDoesNotExist()
        rule.onNode(hasScrollToIndexAction()).assertExists()
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
        rule.onNodeWithTag(tag(10)).assertExists()
    }

    @Test
    fun scrollToIndexLargerThanSize() {
        // Setup a list
        rule.setContent { LazyColumnContent() }

        // ScrollToIndex 20
        expectError<IllegalArgumentException>(
            expectedMessage = "Can't scroll to index 20, it is out of bounds.*"
        ) {
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
        }
    }

    @Test
    fun scrollToIndexSmallerThanZero() {
        // Setup a list
        rule.setContent { LazyColumnContent() }

        // ScrollToIndex -2
        expectError<IllegalArgumentException>(
            expectedMessage = "Can't scroll to index -2, it is out of bounds.*"
        ) {
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(-2)
        }
    }

    @Test
    fun missingScrollToIndexSemantics() {
        // Setup a node without ScrollToIndex
        rule.setContent { Spacer(Modifier.testTag("tag")) }

        // Verify that it doesn't support performScrollToIndex
        rule.onNode(hasScrollToIndexAction()).assertDoesNotExist()
        expectError<AssertionError>(
            expectedMessage = "Failed to scroll to index 1, " +
                "the node is missing \\[ScrollToIndex\\].*"
        ) {
            rule.onNodeWithTag("tag").performScrollToIndex(1)
        }
    }

    @Composable
    fun LazyColumnContent() {
        LazyColumn(Modifier.requiredSize(100.dp)) {
            items(List(20) { it }) {
                Spacer(Modifier.requiredHeight(30.dp).fillMaxWidth().testTag(tag(it)))
            }
        }
    }
}