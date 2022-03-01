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

package androidx.compose.ui.test.junit4

import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.IdlingResource

/**
 * Provides an idle check to be registered into Espresso.
 *
 * This makes sure that Espresso is able to wait for any pending changes in Compose. This
 * resource is automatically registered when any compose testing APIs are used including
 * [createAndroidComposeRule].
 */
internal class ComposeIdlingResource(
    private val composeRootRegistry: ComposeRootRegistry,
    private val clock: MainTestClockImpl,
    private val mainRecomposer: Recomposer
) : IdlingResource {

    private var hadAwaitersOnMainClock = false
    private var hadSnapshotChanges = false
    private var hadRecomposerChanges = false
    private var hadPendingSetContent = false
    private var hadPendingMeasureLayout = false

    override val isIdleNow: Boolean
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

            // pending set content needs all created compose roots,
            // because by definition they will not be in resumed state
            hadPendingSetContent =
                composeRootRegistry.getCreatedComposeRoots().any { it.isBusyAttaching }

            val composeRoots = composeRootRegistry.getRegisteredComposeRoots()
            hadPendingMeasureLayout = composeRoots.any { it.hasPendingMeasureOrLayout }

            return !shouldPumpTime() &&
                !hadPendingSetContent &&
                !hadPendingMeasureLayout
        }

    override fun getDiagnosticMessageIfBusy(): String? {
        val wasBusy = hadSnapshotChanges || hadRecomposerChanges || hadAwaitersOnMainClock ||
            hadPendingSetContent || hadPendingMeasureLayout

        if (!wasBusy) {
            return null
        }

        val busyReasons = mutableListOf<String>()
        val busyRecomposing = hadSnapshotChanges || hadRecomposerChanges || hadAwaitersOnMainClock
        if (busyRecomposing) {
            busyReasons.add("pending recompositions")
        }
        if (hadPendingSetContent) {
            busyReasons.add("pending setContent")
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

internal val ViewRootForTest.isBusyAttaching: Boolean
    get() {
        // If the rootView has a parent, it is the ViewRootImpl, which is set in
        // windowManager.addView(). If the rootView doesn't have a parent, the view hasn't been
        // attached to a window yet, or is removed again.
        return view.rootView.parent != null && !view.isAttachedToWindow
    }