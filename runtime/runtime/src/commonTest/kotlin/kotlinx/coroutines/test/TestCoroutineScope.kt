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

package kotlinx.coroutines.test

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
public interface TestCoroutineScope: CoroutineScope, UncaughtExceptionCaptor, DelayController {
    /**
     * Call after the test completes.
     * Calls [UncaughtExceptionCaptor.cleanupTestCoroutines] and [DelayController.cleanupTestCoroutines].
     *
     * @throws Throwable the first uncaught exception, if there are any uncaught exceptions.
     * @throws UncompletedCoroutinesError if any pending tasks are active, however it will not throw for suspended
     * coroutines.
     */
    public override fun cleanupTestCoroutines()
}

private class TestCoroutineScopeImpl (
    override val coroutineContext: CoroutineContext
):
    TestCoroutineScope,
    UncaughtExceptionCaptor by coroutineContext.uncaughtExceptionCaptor,
    DelayController by coroutineContext.delayController
{
    override fun cleanupTestCoroutines() {
        coroutineContext.uncaughtExceptionCaptor.cleanupTestCoroutines()
        coroutineContext.delayController.cleanupTestCoroutines()
    }
}

/**
 * A scope which provides detailed control over the execution of coroutines for tests.
 *
 * If the provided context does not provide a [ContinuationInterceptor] (Dispatcher) or [CoroutineExceptionHandler], the
 * scope adds [TestCoroutineDispatcher] and [TestCoroutineExceptionHandler] automatically.
 *
 * @param context an optional context that MAY provide [UncaughtExceptionCaptor] and/or [DelayController]
 */
@Suppress("FunctionName")
@ExperimentalCoroutinesApi // Since 1.2.1, tentatively till 1.3.0
public fun TestCoroutineScope(context: CoroutineContext = EmptyCoroutineContext): TestCoroutineScope {
    var safeContext = context
    if (context[ContinuationInterceptor] == null) safeContext += TestCoroutineDispatcher()
    if (context[CoroutineExceptionHandler] == null) safeContext += TestCoroutineExceptionHandler()
    return TestCoroutineScopeImpl(safeContext)
}

private inline val CoroutineContext.uncaughtExceptionCaptor: UncaughtExceptionCaptor
    get() {
        val handler = this[CoroutineExceptionHandler]
        return handler as? UncaughtExceptionCaptor ?: throw IllegalArgumentException(
            "kotlinx.coroutines.test.TestCoroutineScope requires a UncaughtExceptionCaptor such as " +
                "TestCoroutineExceptionHandler as the CoroutineExceptionHandler"
        )
    }

private inline val CoroutineContext.delayController: DelayController
    get() {
        val handler = this[ContinuationInterceptor]
        return handler as? DelayController ?: throw IllegalArgumentException(
            "kotlinx.coroutines.test.TestCoroutineScope requires a kotlinx.coroutines.test.DelayController such as TestCoroutineDispatcher as " +
                "the ContinuationInterceptor (Dispatcher)"
        )
    }