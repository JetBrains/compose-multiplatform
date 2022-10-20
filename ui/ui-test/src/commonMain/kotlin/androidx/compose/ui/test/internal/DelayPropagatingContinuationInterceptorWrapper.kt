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

package androidx.compose.ui.test.internal

import androidx.compose.ui.test.InternalTestApi
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher

/**
 * A [ContinuationInterceptor] that wraps another interceptor and implements [Delay]. If the wrapped
 * interceptor also implements [Delay], the delay implementation is delegated to it, otherwise it's
 * delegated to the default delay implementation (i.e. [Dispatchers.Default]). It is necessary that
 * interceptors used in tests, with one of the [TestDispatcher]s, propagate delay like this in order
 * to work with the delay skipping that those dispatchers perform.
 */
@OptIn(InternalCoroutinesApi::class)
@InternalTestApi
abstract class DelayPropagatingContinuationInterceptorWrapper(
    wrappedInterceptor: ContinuationInterceptor?
) : AbstractCoroutineContextElement(ContinuationInterceptor),
    ContinuationInterceptor,
    // Coroutines will internally use the Default dispatcher as the delay if the
    // ContinuationInterceptor does not implement Delay.
    Delay by ((wrappedInterceptor as? Delay) ?: (Dispatchers.Default as Delay))