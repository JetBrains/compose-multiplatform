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

package androidx.compose.ui.test

import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.AbstractCoroutineContextKey
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher

/**
 * A [ContinuationInterceptor] that delegates intercepting to the given [TestDispatcher][delegate],
 * and applies any changes to the global snapshot immediately after each intercepted continuation
 * has [resumed][Continuation.resumeWith].
 *
 * This means that all snapshot backed state changes that were made in coroutines intercepted by
 * this class will be advertised to any observers after each individual coroutine rather than
 * after a set of coroutines.
 *
 * For the testing framework specifically, this enables calls to [MainTestClock.advanceTimeBy] to
 * recompose during the same call to `advanceTimeBy`, if state was changed by coroutines that were
 * dispatched more than a frame before the end of that advancement.
 */
@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
internal class ApplyingContinuationInterceptor(
    private val delegate: TestDispatcher
) : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor,
    Delay by delegate {

    companion object Key : AbstractCoroutineContextKey<
        ContinuationInterceptor,
        ApplyingContinuationInterceptor
    >(
        ContinuationInterceptor,
        { it as? ApplyingContinuationInterceptor }
    )

    override val key: CoroutineContext.Key<*> get() = Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return delegate.interceptContinuation(SendApplyContinuation(continuation))
    }

    private class SendApplyContinuation<T>(
        private val continuation: Continuation<T>
    ) : Continuation<T> {
        override val context: CoroutineContext
            get() = continuation.context

        override fun resumeWith(result: Result<T>) {
            continuation.resumeWith(result)
            Snapshot.sendApplyNotifications()
        }
    }
}
