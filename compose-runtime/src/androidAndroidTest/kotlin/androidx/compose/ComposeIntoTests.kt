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

package androidx.compose

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeIntoTests: BaseComposeTest() {
    @After
    fun teardown() {
        Compose.clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    @MediumTest
    fun testMultipleSetContentCalls() {
        val activity = activityRule.activity

        var initializationCount = 0
        var commitCount = 0
        val composable = @Composable {
            onActive { initializationCount++ }
            onCommit { commitCount++ }
        }

        activity.show(composable)
        activity.waitForAFrame()

        assertEquals(1, initializationCount)
        assertEquals(1, commitCount)

        activity.show(composable)
        activity.waitForAFrame()

        // if we call setContent multiple times, we want to ensure that it doesn't tear
        // down the whole hierarchy, so onActive should only get called once.
        assertEquals(1, initializationCount)
        assertEquals(2, commitCount)
    }
}
