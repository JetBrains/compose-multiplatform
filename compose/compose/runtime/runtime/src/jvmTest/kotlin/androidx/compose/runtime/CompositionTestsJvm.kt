/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.mock.EmptyApplier
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.junit.Test


class CompositionTestsJvm {

    // This test can't be in commonTest because there's no runBlocking in JS
    @Test(timeout = 10000)
    fun testCompositionAndRecomposerDeadlock() {
        runBlocking {
            withGlobalSnapshotManager {
                repeat(100) {
                    val job = Job(parent = coroutineContext[Job])
                    val coroutineContext = Dispatchers.Unconfined + job
                    val recomposer = Recomposer(coroutineContext)

                    launch(
                        coroutineContext + BroadcastFrameClock(),
                        start = CoroutineStart.UNDISPATCHED
                    ) {
                        recomposer.runRecomposeAndApplyChanges()
                    }

                    val composition = Composition(EmptyApplier(), recomposer)
                    composition.setContent {

                        val innerComposition = Composition(
                            EmptyApplier(),
                            rememberCompositionContext(),
                        )

                        DisposableEffect(composition) {
                            onDispose {
                                innerComposition.dispose()
                            }
                        }
                    }

                    var value by mutableStateOf(1)
                    launch(Dispatchers.Default + job) {
                        while (true) {
                            value += 1
                            delay(1)
                        }
                    }

                    composition.dispose()
                    recomposer.close()
                    job.cancel()
                }
            }
        }
    }

    private inline fun CoroutineScope.withGlobalSnapshotManager(block: CoroutineScope.() -> Unit) {
        val channel = Channel<Unit>(Channel.CONFLATED)
        val job = launch {
            channel.consumeEach {
                Snapshot.sendApplyNotifications()
            }
        }
        val unregisterToken = Snapshot.registerGlobalWriteObserver {
            channel.trySend(Unit)
        }
        try {
            block()
        } finally {
            unregisterToken.dispose()
            job.cancel()
        }
    }

}
