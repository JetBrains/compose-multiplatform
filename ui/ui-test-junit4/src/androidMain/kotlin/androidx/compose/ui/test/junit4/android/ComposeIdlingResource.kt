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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.TestAnimationClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule

/**
 * Register compose's idling check to Espresso.
 *
 * This makes sure that Espresso is able to wait for any pending changes in Compose. This
 * resource is automatically registered when any compose testing APIs are used including
 * [createAndroidComposeRule]. If you for some reasons want to only use Espresso but still have it
 * wait for Compose being idle you can use this function.
 */
@Deprecated(
    message = "Global (un)registration of ComposeIdlingResource is no longer supported. Use " +
        "createAndroidComposeRule() and registration will happen when needed",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("")
)
@Suppress("DocumentExceptions")
fun registerComposeWithEspresso(): Unit = throw UnsupportedOperationException(
    "Global (un)registration of ComposeIdlingResource is no longer supported"
)

/**
 * Unregisters resource registered as part of [registerComposeWithEspresso].
 */
@Deprecated(
    message = "Global (un)registration of ComposeIdlingResource is no longer supported. Use " +
        "createAndroidComposeRule() and registration will happen when needed",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("")
)
@Suppress("DocumentExceptions")
fun unregisterComposeFromEspresso(): Unit = throw UnsupportedOperationException(
    "Global (un)registration of ComposeIdlingResource is no longer supported"
)

/**
 * Registers the given [clock] so Espresso can await the animations subscribed to that clock.
 */
@Deprecated(
    message = "Global (un)registration of TestAnimationClocks is no longer supported. Use the " +
        "member function ComposeIdlingResource.registerTestClock(clock) instead",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("composeIdlingResource.registerTestClock(clock)")
)
@ExperimentalTestApi
@Suppress("UNUSED_PARAMETER", "DocumentExceptions")
fun registerTestClock(clock: TestAnimationClock): Unit = throw UnsupportedOperationException(
    "Global (un)registration of TestAnimationClocks is no longer supported. Register clocks " +
        "directly on an instance of ComposeIdlingResource instead"
)

/**
 * Unregisters the [clock] that was registered with [registerTestClock].
 */
@Deprecated(
    message = "Global (un)registration of TestAnimationClocks is no longer supported. Use the " +
        "member function ComposeIdlingResource.unregisterTestClock(clock) instead",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("composeIdlingResource.unregisterTestClock(clock)")
)
@ExperimentalTestApi
@Suppress("UNUSED_PARAMETER", "DocumentExceptions")
fun unregisterTestClock(clock: TestAnimationClock): Unit = throw UnsupportedOperationException(
    "Global (un)registration of TestAnimationClocks is no longer supported. Register clocks " +
        "directly on an instance of ComposeIdlingResource instead"
)

/**
 * Provides an idle check to be registered into Espresso.
 *
 * This makes sure that Espresso is able to wait for any pending changes in Compose. This
 * resource is automatically registered when any compose testing APIs are used including
 * [createAndroidComposeRule].
 */
internal class ComposeIdlingResource(
    private val composeRootRegistry: ComposeRootRegistry
) : IdlingResource {

    @OptIn(ExperimentalTestApi::class)
    private val clocks = mutableSetOf<TestAnimationClock>()

    private var hadAnimationClocksIdle = true
    private var hadNoSnapshotChanges = true
    private var hadNoRecomposerChanges = true
    private var hadNoPendingMeasureLayout = true
    // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
//    private var hadNoPendingDraw = true

    /**
     * Returns whether or not Compose is idle now.
     */
    override val isIdleNow: Boolean
        @OptIn(ExperimentalComposeApi::class)
        get() {
            hadNoSnapshotChanges = !Snapshot.current.hasPendingChanges()
            hadNoRecomposerChanges = !Recomposer.current().hasInvalidations()
            hadAnimationClocksIdle = areAllClocksIdle()
            val composeRoots = composeRootRegistry.getUnfilteredComposeRoots()
            hadNoPendingMeasureLayout = !composeRoots.any { it.hasPendingMeasureOrLayout }
            // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
//            hadNoPendingDraw = !composeRoots.any {
//                val hasContent = it.view.measuredWidth != 0 && it.view.measuredHeight != 0
//                it.view.isDirty && (hasContent || it.view.isLayoutRequested)
//            }

            return hadNoSnapshotChanges &&
                hadNoRecomposerChanges &&
                hadAnimationClocksIdle &&
                // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
                hadNoPendingMeasureLayout /*&&
                hadNoPendingDraw*/
        }

    @OptIn(ExperimentalTestApi::class)
    fun registerTestClock(clock: TestAnimationClock) {
        synchronized(clocks) {
            clocks.add(clock)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun unregisterTestClock(clock: TestAnimationClock) {
        synchronized(clocks) {
            clocks.remove(clock)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun areAllClocksIdle(): Boolean {
        return synchronized(clocks) {
            clocks.all { it.isIdle }
        }
    }

    override fun getDiagnosticMessageIfBusy(): String? {
        val hadSnapshotChanges = !hadNoSnapshotChanges
        val hadRecomposerChanges = !hadNoRecomposerChanges
        val hadRunningAnimations = !hadAnimationClocksIdle
        val hadPendingMeasureLayout = !hadNoPendingMeasureLayout
        // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
//        val hadPendingDraw = !hadNoPendingDraw

        val wasIdle = !hadSnapshotChanges && !hadRecomposerChanges && !hadRunningAnimations &&
            // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
            !hadPendingMeasureLayout /*&& !hadPendingDraw*/

        if (wasIdle) {
            return null
        }

        val busyReasons = mutableListOf<String>()
        if (hadRunningAnimations) {
            busyReasons.add("animations")
        }
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
            message += "hadSnapshotChanges = $hadSnapshotChanges"
        }
        return message
    }
}
