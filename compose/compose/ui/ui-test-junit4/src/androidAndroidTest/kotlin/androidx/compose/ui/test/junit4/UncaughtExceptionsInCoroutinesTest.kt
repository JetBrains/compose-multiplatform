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

package androidx.compose.ui.test.junit4

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.testutils.expectError
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class UncaughtExceptionsInCoroutinesTest {

    private class TestException : Exception()

    // Run the test twice so we can verify if a failed test took down the test suite:
    // - Results have 1 failed test:
    //   exception handler isn't installed correctly
    // - Results have 2 failed tests:
    //   exception handler is installed correctly, but verifying thrown error is wrong

    @Test
    fun test1() = runComposeUiTest {
        expectError<TestException> {
            throwInLaunchedEffect()
        }
    }

    @Test
    fun test2() = runComposeUiTest {
        expectError<TestException> {
            throwInLaunchedEffect()
        }
    }

    private fun ComposeUiTest.throwInLaunchedEffect() {
        setContent {
            LaunchedEffect(Unit) {
                withFrameNanos {}
                throw TestException()
            }
        }
    }
}
