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

import java.io.PrintWriter
import java.io.StringWriter
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * Runs the [block] and asserts that a [AssertionError] is thrown with the [expectedMessage] if
 * [expectError] is `true`, or that nothing is thrown if [expectError] is `false`. The
 * [expectedMessage] is a regex with just the option [DOT_MATCHES_ALL] enabled.
 */
fun expectAssertionError(
    expectError: Boolean,
    expectedMessage: String = ".*",
    block: () -> Unit
) {
    expectError<AssertionError>(expectError, expectedMessage, block)
}

/**
 * Runs the [block] and asserts that a [T] is thrown with the [expectedMessage] if [expectError]
 * is `true`, or that nothing is thrown if [expectError] is `false`. The [expectedMessage] is a
 * regex with just the option [DOT_MATCHES_ALL] enabled.
 *
 * @param expectedMessage A regular expression that matches the entire expected error message. If
 * you don't want to verify the entire error message, use `.*` in the appropriate places. The
 * option [DOT_MATCHES_ALL] is enabled so you can match new lines with `.*`. Don't forget to
 * escape special characters like `[`, `(` or `*` (and double escaping for `\`).
 */
inline fun <reified T : Throwable> expectError(
    expectError: Boolean = true,
    expectedMessage: String = ".*",
    block: () -> Unit
) {
    val expectedClassName = T::class.java.simpleName
    try {
        block()
    } catch (thrown: Throwable) {
        if (!expectError) {
            throwExpectError(null, thrown)
        } else if (thrown !is T) {
            throwExpectError(expectedClassName, thrown)
        } else if (!expectedMessage.toRegex(DOT_MATCHES_ALL).matches(thrown.message ?: "")) {
            throwExpectError(expectedClassName, thrown, expectedMessage)
        }
        // Thrown error matched what was expected
        return
    }
    if (expectError) {
        // Nothing was thrown, but we did expect it
        throwExpectError(expectedClassName)
    }
}

@PublishedApi
internal fun throwExpectError(
    expectedClassName: String?,
    thrown: Throwable? = null,
    expectedMessage: String? = null
) {
    val stackTrace = thrown?.let {
        StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                it.printStackTrace(pw)
            }
            ":\n==============================\n$sw=============================="
        }
    } ?: ""

    fun String.plusMessage(message: String?): String {
        return if (expectedMessage == null) this else "$this with message\n\"\"\"$message\"\"\"\n"
    }

    val expected = expectedClassName?.let { "a $it".plusMessage(expectedMessage) } ?: "nothing"
    val actual = thrown?.run { "a ${javaClass.simpleName}".plusMessage(message) } ?: "nothing"
    throw AssertionError(
        "Expected that $expected would be thrown, but $actual was thrown$stackTrace"
    )
}
