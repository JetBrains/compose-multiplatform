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

package androidx.compose.testutils

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExpectTest {

    class TestException(message: String? = null) : Exception(message)

    @Test
    fun expectError_gotError() {
        expectError<TestException> {
            throw TestException()
        }
    }

    @Test
    fun expectError_gotErrorWithMessage() {
        expectError<TestException> {
            throw TestException("message")
        }
    }

    @Test
    fun expectError_gotErrorWithMultilineMessage() {
        expectError<TestException> {
            throw TestException("message\nwith 2 lines")
        }
    }

    @Test
    fun expectError_gotNothing() {
        expectErrorMessage(
            "Expected that a TestException would be thrown, but nothing was thrown"
        ) {
            expectError<TestException> {
            }
        }
    }

    @Test
    fun expectError_gotDifferentError() {
        expectErrorMessage(
            "Expected that a TestException would be thrown, " +
                "but a IllegalStateException was thrown:\n=="
        ) {
            expectError<TestException> {
                throw IllegalStateException()
            }
        }
    }

    @Test
    fun expectNoError_gotNoError() {
        expectError<TestException>(false) {
        }
    }

    @Test
    fun expectNoError_gotError() {
        expectErrorMessage(
            "Expected that nothing would be thrown, but a TestException was thrown:\n=="
        ) {
            expectError<TestException>(false) {
                throw TestException()
            }
        }
    }

    @Test
    fun expectNoError_gotDifferentError() {
        expectErrorMessage(
            "Expected that nothing would be thrown, but a IllegalStateException was thrown:\n=="
        ) {
            expectError<TestException>(false) {
                throw IllegalStateException()
            }
        }
    }

    @Test
    fun expectErrorWithMessage_gotErrorWithMessage() {
        expectError<TestException>(expectedMessage = "message") {
            throw TestException("message")
        }
    }

    @Test
    fun expectErrorWithMessage_gotErrorWithDifferentMessage() {
        expectErrorMessage(
            "Expected that a TestException with message\n\"\"\"message\"\"\"\n would be thrown, " +
                "but a TestException with message\n\"\"\"message x\"\"\"\n was thrown:\n=="
        ) {
            expectError<TestException>(expectedMessage = "message") {
                throw TestException("message x")
            }
        }
    }

    private fun expectErrorMessage(expectedErrorMessage: String, block: () -> Unit) {
        try {
            block()
        } catch (e: AssertionError) {
            assertWithMessage("expectError threw an AssertionError with the wrong message")
                .that(e.message)
                .startsWith(expectedErrorMessage)
            return
        }
        throw AssertionError("Expected an AssertionError, but it wasn't thrown")
    }
}