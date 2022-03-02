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

package androidx.compose.ui.test

import androidx.compose.testutils.expectError
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RootExistenceAssertTest {
    companion object {
        private const val NoComposeHierarchiesFound =
            "No compose hierarchies found in the app\\. Possible reasons include:" +
                ".*\\bsetContent was called before the ComposeTestRule ran\\..*"
    }

    @get:Rule
    val rule = createEmptyComposeRule()

    @Test
    fun noContent_assertExists() {
        expectError<IllegalStateException>(expectedMessage = NoComposeHierarchiesFound) {
            rule.onNodeWithTag("item")
                .assertExists()
        }
    }

    @Test
    fun noContent_assertDoesNotExist() {
        rule.onNodeWithTag("item")
            .assertDoesNotExist()
    }

    @Test
    fun noContent_queryMultipleAssertZero() {
        rule.onAllNodesWithTag("item")
            .assertCountEquals(0)
    }

    @Test
    fun noContent_queryMultipleAssertOne() {
        expectError<IllegalStateException>(expectedMessage = NoComposeHierarchiesFound) {
            rule.onAllNodesWithTag("item")
                .assertCountEquals(1)
        }
    }
}
