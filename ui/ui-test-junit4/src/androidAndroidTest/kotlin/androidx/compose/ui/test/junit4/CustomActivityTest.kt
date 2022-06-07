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

package androidx.compose.ui.test.junit4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.testutils.expectError
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

class CustomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Box {
                    Button(onClick = {}) {
                        Text("Hello")
                    }
                }
            }
        }
    }
}

/**
 * Tests that we can launch custom activities via [createAndroidComposeRule].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class CustomActivityTest {
    private companion object {
        const val ContentAlreadySetError = "androidx\\.compose\\.ui\\.test\\.junit4\\." +
            "CustomActivity@[0-9A-Fa-f]* has already set content\\. If you have populated the " +
            "Activity with a ComposeView, make sure to call setContent on that ComposeView " +
            "instead of on the test rule; and make sure that that call to `setContent \\{\\}` " +
            "is done after the ComposeTestRule has run"
    }

    @Test
    fun launchCustomActivity() = runAndroidComposeUiTest<CustomActivity> {
        onNodeWithText("Hello").assertExists()
    }

    @Test
    fun setContentOnActivityWithContent() = runAndroidComposeUiTest<CustomActivity> {
        expectError<IllegalStateException>(expectedMessage = ContentAlreadySetError) {
            setContent { Text("Hello") }
        }
    }
}
