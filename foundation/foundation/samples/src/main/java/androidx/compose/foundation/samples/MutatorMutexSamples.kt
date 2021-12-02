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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

@Sampled
fun mutatorMutexStateObject() {
    @Stable
    class ScrollState(position: Int = 0) {
        private var _position by mutableStateOf(position)
        var position: Int
            get() = _position.coerceAtMost(range)
            set(value) {
                _position = value.coerceIn(0, range)
            }

        private var _range by mutableStateOf(0)
        var range: Int
            get() = _range
            set(value) {
                _range = value.coerceAtLeast(0)
            }

        var isScrolling by mutableStateOf(false)
            private set

        private val mutatorMutex = MutatorMutex()

        /**
         * Only one caller to [scroll] can be in progress at a time.
         */
        suspend fun <R> scroll(
            block: suspend () -> R
        ): R = mutatorMutex.mutate {
            isScrolling = true
            try {
                block()
            } finally {
                // MutatorMutex.mutate ensures mutual exclusion between blocks.
                // By setting back to false in the finally block inside mutate, we ensure that we
                // reset the state upon cancellation before the next block starts to run (if any).
                isScrolling = false
            }
        }
    }

    /**
     * Arbitrary animations can be defined as extensions using only public API
     */
    suspend fun ScrollState.animateTo(target: Int) {
        scroll {
            animate(from = position, to = target) { newPosition ->
                position = newPosition
            }
        }
    }

    /**
     * Presents two buttons for animating a scroll to the beginning or end of content.
     * Pressing one will cancel any current animation in progress.
     */
    @Composable
    fun ScrollControls(scrollState: ScrollState) {
        Row {
            val scope = rememberCoroutineScope()
            Button(onClick = { scope.launch { scrollState.animateTo(0) } }) {
                Text("Scroll to beginning")
            }
            Button(onClick = { scope.launch { scrollState.animateTo(scrollState.range) } }) {
                Text("Scroll to end")
            }
        }
    }
}

@Sampled
fun mutatorMutexStateObjectWithReceiver() {
    @Stable
    class ScrollState(position: Int = 0) {
        private var _position = mutableStateOf(position)
        val position: Int by _position

        private val mutatorMutex = MutatorMutex()

        /**
         * Only [block] in a call to [scroll] may change the value of [position].
         */
        suspend fun <R> scroll(
            block: suspend MutableState<Int>.() -> R
        ): R = mutatorMutex.mutateWith(_position, block = block)
    }
}

@Suppress("UNUSED_PARAMETER")
private suspend fun animate(from: Int, to: Int, onFrame: (position: Int) -> Unit): Unit = TODO()

@Suppress("UNUSED_PARAMETER")
@Composable
private fun Button(onClick: () -> Unit, content: @Composable () -> Unit): Unit = TODO()
