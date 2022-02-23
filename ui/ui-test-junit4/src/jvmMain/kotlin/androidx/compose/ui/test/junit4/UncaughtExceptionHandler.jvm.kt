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

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Similar to [TestCoroutineExceptionHandler], but with clearing all thrown exceptions
 *
 * If we don't clear exceptions they will be thrown twice in this example
 * (the exception will be thrown inside awaitIdle and after the test):
 * ```
 * @Test
 * fun test() {
 *    runBlocking(Dispatchers.Main) {
 *        try {
 *            rule.awaitIdle()
 *        } catch (e: SomeException) {
 *            // ignore
 *        }
 *    }
 * }
 * ```
 */
// TODO(b/200151447): When this moves over to ui-test, move the code sample above to ui-test:samples
internal class UncaughtExceptionHandler :
    AbstractCoroutineContextElement(CoroutineExceptionHandler),
    CoroutineExceptionHandler {
    private var exception: Throwable? = null

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        synchronized(this) {
            if (this.exception == null) {
                this.exception = exception
            } else {
                this.exception!!.addSuppressed(exception)
            }
        }
    }

    /**
     * Checks if the [UncaughtExceptionHandler] has caught uncaught exceptions. If so, will
     * rethrow the first to fail the test. The rest exceptions will be added to the first and
     * marked as `suppressed`.
     *
     * The next call of this method will not throw already thrown exception.
     *
     * Rather than only calling this only at the end of the test, as recommended by
     * [UncaughtExceptionCaptor.cleanupTestCoroutines],
     * try calling this at a few strategic
     * points to fail the test asap after the exception was caught.
     */
    fun throwUncaught() {
        synchronized(this) {
            val exception = exception
            if (exception != null) {
                this.exception = null
                throw exception
            }
        }
    }
}