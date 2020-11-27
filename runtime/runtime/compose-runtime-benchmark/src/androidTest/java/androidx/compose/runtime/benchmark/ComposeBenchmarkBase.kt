/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotReadObserver
import androidx.compose.runtime.snapshots.SnapshotWriteObserver
import androidx.compose.runtime.snapshots.takeMutableSnapshot
import androidx.compose.ui.platform.setContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.DelayController
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule

@OptIn(ExperimentalComposeApi::class, InternalComposeApi::class)
abstract class ComposeBenchmarkBase {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(ComposeActivity::class.java)

    fun measureCompose(block: @Composable () -> Unit) {
        val activity = activityRule.activity
        var composition: Composition? = null
        benchmarkRule.measureRepeated {
            composition = activity.setContent(Recomposer.current(), block)

            runWithTimingDisabled {
                composition?.dispose()
            }
        }
        composition?.dispose()
    }

    fun measureRecompose(block: RecomposeReceiver.() -> Unit) {
        val receiver = RecomposeReceiver()
        receiver.block()
        var activeComposer: Composer<*>? = null

        val activity = activityRule.activity

        val composition = activity.setContent {
            activeComposer = currentComposer
            receiver.composeCb()
        }

        val composer = activeComposer
        require(composer != null) { "Composer was null" }
        val readObserver: SnapshotReadObserver = { composer.recordReadOf(it) }
        val writeObserver: SnapshotWriteObserver = { composer.recordWriteOf(it) }
        val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
            composer.recordModificationsOf(changed)
        }
        try {
            benchmarkRule.measureRepeated {
                runWithTimingDisabled {
                    receiver.updateModelCb()
                    Snapshot.sendApplyNotifications()
                }
                val didSomething = composer.performRecompose(readObserver, writeObserver)
                assertTrue(didSomething)
                runWithTimingDisabled {
                    receiver.resetCb()
                    Snapshot.sendApplyNotifications()
                    composer.performRecompose(readObserver, writeObserver)
                }
            }
        } finally {
            unregisterApplyObserver()
            composition.dispose()
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun DelayController.measureRecomposeSuspending(
        block: RecomposeReceiver.() -> Unit
    ) = coroutineScope {
        val receiver = RecomposeReceiver()
        receiver.block()

        val activity = activityRule.activity

        val recomposer = Recomposer(coroutineContext)
        launch { recomposer.runRecomposeAndApplyChanges() }

        val composition = activity.setContent(recomposer) {
            receiver.composeCb()
        }

        var iterations = 0
        benchmarkRule.measureRepeatedSuspendable {
            runWithTimingDisabled {
                receiver.updateModelCb()
                Snapshot.sendApplyNotifications()
            }
            assertTrue(
                "recomposer does not have invalidations for frame",
                recomposer.hasInvalidations()
            )
            advanceUntilIdle()
            assertFalse(
                "recomposer has invalidations for frame",
                recomposer.hasInvalidations()
            )
            runWithTimingDisabled {
                receiver.resetCb()
                Snapshot.sendApplyNotifications()
                advanceUntilIdle()
            }
            iterations++
        }

        composition.dispose()
        recomposer.shutDown()
    }
}

inline fun BenchmarkRule.measureRepeatedSuspendable(block: BenchmarkRule.Scope.() -> Unit) {
    // Note: this is an extension function to discourage calling from Java.

    // Extract members to locals, to ensure we check #applied, and we don't hit accessors
    val localState = getState()
    val localScope = scope

    while (localState.keepRunningInline()) {
        block(localScope)
    }
}

@OptIn(ExperimentalComposeApi::class, InternalComposeApi::class)
fun Composer<*>.performRecompose(
    readObserver: SnapshotReadObserver,
    writeObserver: SnapshotWriteObserver
): Boolean {
    val snapshot = takeMutableSnapshot(readObserver, writeObserver)
    val result = snapshot.enter {
        recompose().also { applyChanges() }
    }
    snapshot.apply().check()
    return result
}

class RecomposeReceiver {
    var composeCb: @Composable () -> Unit = @Composable { }
    var updateModelCb: () -> Unit = { }
    var resetCb: () -> Unit = {}

    fun compose(block: @Composable () -> Unit) {
        composeCb = block
    }

    fun reset(block: () -> Unit) {
        resetCb = block
    }

    fun update(block: () -> Unit) {
        updateModelCb = block
    }
}
