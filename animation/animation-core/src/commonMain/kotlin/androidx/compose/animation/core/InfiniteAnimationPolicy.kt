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

package androidx.compose.animation.core

import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
/**
 * Like [withFrameNanos], but applies the [InfiniteAnimationPolicy] from the calling
 * [CoroutineContext] if there is one.
 */
suspend fun <R> withInfiniteAnimationFrameNanos(onFrame: (frameTimeMillis: Long) -> R): R =
    when (val policy = coroutineContext[InfiniteAnimationPolicy]) {
        null -> withFrameNanos(onFrame)
        else -> policy.onInfiniteOperation { withFrameNanos(onFrame) }
    }

/**
 * Like [withFrameMillis], but applies the [InfiniteAnimationPolicy] from the calling
 * [CoroutineContext] if there is one.
 */
@Suppress("UnnecessaryLambdaCreation")
suspend inline fun <R> withInfiniteAnimationFrameMillis(
    crossinline onFrame: (frameTimeMillis: Long) -> R
): R = withInfiniteAnimationFrameNanos { onFrame(it / 1_000_000L) }
