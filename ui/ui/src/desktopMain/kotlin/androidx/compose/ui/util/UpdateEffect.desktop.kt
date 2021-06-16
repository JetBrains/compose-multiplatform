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

package androidx.compose.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import kotlinx.coroutines.channels.Channel

/**
 * When [UpdateEffect] enters the composition it will call [update] and will capture all state
 * which is used in this function.
 *
 * When any state is changed, [update] will be called again on the next recomposition.
 *
 * [update] always be called in UI thread.
 */
@Composable
internal fun UpdateEffect(update: () -> Unit) {
    val tasks = remember { Channel<() -> Unit>(Channel.RENDEZVOUS) }
    val currentUpdate by rememberUpdatedState(update)

    LaunchedEffect(Unit) {
        for (task in tasks) {
            task()
        }
    }

    DisposableEffect(Unit) {
        val snapshotObserver = SnapshotStateObserver { command ->
            command()
        }
        snapshotObserver.start()

        fun performUpdate() {
            snapshotObserver.observeReads(
                Unit,
                onValueChangedForScope = { tasks.trySend(::performUpdate) }
            ) {
                currentUpdate()
            }
        }

        performUpdate()

        onDispose {
            snapshotObserver.stop()
            snapshotObserver.clear()
        }
    }
}