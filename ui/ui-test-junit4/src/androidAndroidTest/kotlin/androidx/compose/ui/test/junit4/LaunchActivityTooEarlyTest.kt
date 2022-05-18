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

import androidx.compose.testutils.expectError
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LaunchActivityTooEarlyTest {
    @Test
    fun test() {
        // Launching the CustomActivity _before_ calling runTest
        ActivityScenario.launch(CustomActivity::class.java)

        @OptIn(ExperimentalTestApi::class)
        runEmptyComposeUiTest {
            expectError<IllegalStateException>(
                expectedMessage = "No compose hierarchies found in the app\\. Possible reasons " +
                    "include:.*\\bsetContent was called before the ComposeTestRule ran\\..*"
            ) {
                onNodeWithText("Hello").assertExists()
            }
        }
    }
}
