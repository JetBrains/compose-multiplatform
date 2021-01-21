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

package androidx.compose.ui.test.junit4.android

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.junit4.MainTestClockImpl
import androidx.compose.ui.test.junit4.createAndroidComposeRule

/**
 * Provides an idle check to be registered into Espresso.
 *
 * This makes sure that Espresso is able to wait for any pending changes in Compose. This
 * resource is automatically registered when any compose testing APIs are used including
 * [createAndroidComposeRule].
 */
internal class ComposeIdlingResourceNew(
    private val composeRootRegistry: ComposeRootRegistry,
    private val clock: MainTestClockImpl,
    private val mainRecomposer: Recomposer
) : IdlingResource {

    private var hadAwaitersOnMainClock = true
    private var hadSnapshotChanges = true
    private var hadRecomposerChanges = true
    private var hadPendingMeasureLayout = true

    override val isIdleNow: Boolean
        @OptIn(ExperimentalComposeApi::class)
        get() {
            fun shouldPumpTime(): Boolean {
                hadAwaitersOnMainClock = clock.hasAwaiters
                hadSnapshotChanges = Snapshot.current.hasPendingChanges()
                hadRecomposerChanges = mainRecomposer.hasPendingWork

                val needsRecompose = hadAwaitersOnMainClock || hadSnapshotChanges ||
                    hadRecomposerChanges
                return clock.autoAdvance && needsRecompose
            }

            var i = 0
            while (i < 100 && shouldPumpTime()) {
                clock.advanceTimeByFrame()
                ++i
            }

            val composeRoots = composeRootRegistry.getComposeRoots()
            hadPendingMeasureLayout = composeRoots.any { it.hasPendingMeasureOrLayout }

            return !shouldPumpTime() && !hadPendingMeasureLayout
        }

    override fun getDiagnosticMessageIfBusy(): String? {
        val wasBusy = hadSnapshotChanges || hadRecomposerChanges || hadAwaitersOnMainClock ||
            hadPendingMeasureLayout

        if (wasBusy) {
            return null
        }

        val busyReasons = mutableListOf<String>()
        val busyRecomposing = hadSnapshotChanges || hadRecomposerChanges
        if (busyRecomposing) {
            busyReasons.add("pending recompositions")
        }
        if (hadPendingMeasureLayout) {
            busyReasons.add("pending measure/layout")
        }

        var message = "${javaClass.simpleName} is busy due to ${busyReasons.joinToString(", ")}.\n"
        if (busyRecomposing) {
            message += "- Note: Timeout on pending recomposition means that there are most likely" +
                " infinite re-compositions happening in the tested code.\n"
            message += "- Debug: hadRecomposerChanges = $hadRecomposerChanges, "
            message += "hadSnapshotChanges = $hadSnapshotChanges, "
            message += "hadAwaitersOnMainClock = $hadAwaitersOnMainClock"
        }
        return message
    }
}
