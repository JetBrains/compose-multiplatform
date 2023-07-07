/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.test.testWithTimeout


private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L

@ExperimentalCoroutinesApi
internal fun runTest(
    context: CoroutineContext = EmptyCoroutineContext,
    dispatchTimeoutMs: Long = DEFAULT_DISPATCH_TIMEOUT_MS,
    timeoutMs: Long? = null,
    testBody: suspend TestScope.() -> Unit
): TestResult = kotlinx.coroutines.test.runTest(context, dispatchTimeoutMs) {
    val testScope = this
    if (timeoutMs == null) {
        testBody()
    } else {
        testWithTimeout(timeoutMs) {
            testBody(testScope)
        }
    }
}