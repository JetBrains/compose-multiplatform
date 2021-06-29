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

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InfiniteAnimationPolicyTest {

    @Test
    fun withInfiniteAnimationFrameNanos_policyIsApplied() {
        withInfiniteAnimationFrame_policyIsApplied {
            withInfiniteAnimationFrameNanos {}
        }
    }

    @Test
    fun withInfiniteAnimationFrameMillis_policyIsApplied() {
        withInfiniteAnimationFrame_policyIsApplied {
            withInfiniteAnimationFrameMillis {}
        }
    }

    private fun <R> withInfiniteAnimationFrame_policyIsApplied(block: suspend () -> R) {
        var applied = false
        val policy = object : InfiniteAnimationPolicy {
            override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
                applied = true
                // We don't need to run the `block()` in our test, but we do need a return value
                // of R. Throw a CancellationException to cancel the coroutine instead.
                throw CancellationException()

                // The reason why we can't run block() here, is because block() calls through to
                // `DefaultMonotonicFrameClock.withFrameNanos`, which in host side tests
                // dispatches on the main thread. But we're already blocking the main thread, so
                // that would deadlock.
            }
        }

        val caught = runCatching {
            runBlocking(policy) {
                block()
            }
        }

        assertThat(applied).isTrue()
        assertThat(caught.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }
}
