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

package androidx.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Collects values from this [Flow] and represents its latest value via [State]. Every time there
 * would be new value posted into the [Flow] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * @sample androidx.compose.samples.FlowSample
 *
 * @param context [CoroutineContext] to use for collecting.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T : Any> Flow<T>.collectAsState(
    context: CoroutineContext = Dispatchers.Main
): State<T?> = collectAsState(null, context)

/**
 * Collects values from this [Flow] and represents its latest value via [State]. Every time there
 * would be new value posted into the [Flow] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * @sample androidx.compose.samples.FlowWithInitialSample
 *
 * @param context [CoroutineContext] to use for collecting.
 */
@Composable
fun <T> Flow<T>.collectAsState(
    initial: T,
    context: CoroutineContext = Dispatchers.Main
): State<T> {
    val state = state { initial }
    onPreCommit(this, context) {
        val job = CoroutineScope(context).launch {
            collect {
                FrameManager.framed {
                    state.value = it
                }
            }
        }
        onDispose { job.cancel() }
    }
    return state
}
