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

package androidx.compose.ui.platform

import androidx.compose.runtime.withFrameNanos
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * Provides a policy that will be applied to animations that get their frame time from
 * [withInfiniteAnimationFrameNanos][androidx.compose.animation.core.withInfiniteAnimationFrameNanos]
 * or
 * [withInfiniteAnimationFrameMillis][androidx.compose.animation.core.withInfiniteAnimationFrameMillis]
 * This can be used to intervene in infinite animations to make them finite, for example by
 * cancelling such coroutines.
 *
 * By default no policy is installed, except in instrumented tests that use
 * [androidx.compose.ui.test.junit4.ComposeTestRule].
 */
@JvmDefaultWithCompatibility
interface InfiniteAnimationPolicy : CoroutineContext.Element {
    /**
     * Call this to apply the policy on the given suspending [block]. Execution of the block is
     * determined by the policy implementation. For example, a test policy could decide not to
     * run the block, or trace its execution.
     *
     * The block is intended to be part of and will therefore be treated as an infinite animation,
     * one that after returning from [onInfiniteOperation] will call it again. If the block is
     * not part of an infinite animation, the policy will still be applied.
     */
    suspend fun <R> onInfiniteOperation(block: suspend () -> R): R

    override val key: CoroutineContext.Key<*> get() = Key

    companion object Key : CoroutineContext.Key<InfiniteAnimationPolicy>
}

/**
 * Like [withFrameNanos], but applies the [InfiniteAnimationPolicy] from the calling
 * [CoroutineContext] if there is one.
 *
 * Note that this is an exact copy of the implementation in the `animation-core` module. We need
 * access to it in this module, but other changes are being considered to this API so we don't want
 * to go moving APIs around now if we might change them anyway. b/230369229 tracks cleaning up this
 * clipboard inheritance.
 */
internal suspend fun <R> withInfiniteAnimationFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
    when (val policy = coroutineContext[InfiniteAnimationPolicy]) {
        null -> withFrameNanos(onFrame)
        else -> policy.onInfiniteOperation { withFrameNanos(onFrame) }
    }