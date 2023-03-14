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

package androidx.compose.ui.test.junit4

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ComposeTestRuleWaitUntilTest {

    @get:Rule
    val rule = createComposeRule()

    companion object {
        private const val TestTag = "TestTag"
        private const val Timeout = 500L
    }

    @Composable
    private fun TaggedBox() = Box(
        Modifier
            .size(10.dp, 10.dp)
            .testTag(TestTag)
    )

    @Test
    fun waitUntilNodeCount_succeedsWhen_nodeCountCorrect() {
        rule.setContent {
            TaggedBox()
            TaggedBox()
            TaggedBox()
        }

        rule.waitUntilNodeCount(hasTestTag(TestTag), 3, Timeout)
    }

    @Test
    fun waitUntilNodeCount_throwsWhen_nodeCountIncorrect() {
        rule.setContent {
            TaggedBox()
            TaggedBox()
            TaggedBox()
        }

        expectError<ComposeTimeoutException>(
            expectedMessage = "Condition still not satisfied after $Timeout ms"
        ) {
            rule.waitUntilNodeCount(hasTestTag(TestTag), 2, Timeout)
        }
    }

    @Test
    fun waitUntilAtLeastOneExists_succeedsWhen_nodesExist() {
        rule.setContent {
            TaggedBox()
            TaggedBox()
        }

        rule.waitUntilAtLeastOneExists(hasTestTag(TestTag))
    }

    @Test
    fun waitUntilAtLeastOneExists_throwsWhen_nodesDoNotExist() {
        rule.setContent {
            Box(Modifier.size(10.dp))
        }

        expectError<ComposeTimeoutException>(
            expectedMessage = "Condition still not satisfied after $Timeout ms"
        ) {
            rule.waitUntilAtLeastOneExists(hasTestTag(TestTag), Timeout)
        }
    }

    @Test
    fun waitUntilExactlyOneExists_succeedsWhen_oneNodeExists() {
        rule.setContent {
            TaggedBox()
        }

        rule.waitUntilExactlyOneExists(hasTestTag(TestTag))
    }

    @Test
    fun waitUntilExactlyOneExists_throwsWhen_twoNodesExist() {
        rule.setContent {
            TaggedBox()
            TaggedBox()
        }

        expectError<ComposeTimeoutException>(
            expectedMessage = "Condition still not satisfied after $Timeout ms"
        ) {
            rule.waitUntilExactlyOneExists(hasTestTag(TestTag), Timeout)
        }
    }

    @Test
    fun waitUntilDoesNotExists_succeedsWhen_nodeDoesNotExist() {
        rule.setContent {
            Box(Modifier.size(10.dp))
        }

        rule.waitUntilDoesNotExist(hasTestTag(TestTag), timeoutMillis = Timeout)
    }

    @Test
    fun waitUntilDoesNotExists_throwsWhen_nodeExistsUntilTimeout() {
        rule.setContent {
            TaggedBox()
        }

        expectError<ComposeTimeoutException>(
            expectedMessage = "Condition still not satisfied after $Timeout ms"
        ) {
            rule.waitUntilDoesNotExist(hasTestTag(TestTag), timeoutMillis = Timeout)
        }
    }
}