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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.node.Owner
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.TestAnimationClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.isOnUiThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.runners.model.Statement

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
@ExperimentalTesting
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
@ExperimentalTesting
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
internal class ComposeIdlingResource : IdlingResource {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val androidOwnerRegistry = AndroidOwnerRegistry()

    @OptIn(ExperimentalTesting::class)
    private val clocks = mutableSetOf<TestAnimationClock>()

    private var hadAnimationClocksIdle = true
    private var hadNoSnapshotChanges = true
    private var hadNoRecomposerChanges = true
    private var hadNoPendingMeasureLayout = true
    private var hadNoPendingDraw = true

    /**
     * Returns whether or not Compose is idle now.
     */
    override val isIdleNow: Boolean
        @OptIn(ExperimentalComposeApi::class)
        get() {
            hadNoSnapshotChanges = !Snapshot.current.hasPendingChanges()
            hadNoRecomposerChanges = !Recomposer.current().hasInvalidations()
            hadAnimationClocksIdle = areAllClocksIdle()
            val owners = androidOwnerRegistry.getOwners()
            hadNoPendingMeasureLayout = !owners.any { it.hasPendingMeasureOrLayout }
            hadNoPendingDraw = !owners.any {
                val hasContent = it.view.measuredWidth != 0 && it.view.measuredHeight != 0
                it.view.isDirty && (hasContent || it.view.isLayoutRequested)
            }

            return hadNoSnapshotChanges &&
                hadNoRecomposerChanges &&
                hadAnimationClocksIdle &&
                hadNoPendingMeasureLayout &&
                hadNoPendingDraw
        }

    @OptIn(ExperimentalTesting::class)
    fun registerTestClock(clock: TestAnimationClock) {
        synchronized(clocks) {
            clocks.add(clock)
        }
    }

    @OptIn(ExperimentalTesting::class)
    fun unregisterTestClock(clock: TestAnimationClock) {
        synchronized(clocks) {
            clocks.remove(clock)
        }
    }

    @OptIn(ExperimentalTesting::class)
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
        val hadPendingDraw = !hadNoPendingDraw

        val wasIdle = !hadSnapshotChanges && !hadRecomposerChanges && !hadRunningAnimations

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

        var message = "${javaClass.simpleName} is busy due to ${busyReasons.joinToString(", ")}.\n"
        if (busyRecomposing) {
            message += "- Note: Timeout on pending recomposition means that there are most likely" +
                " infinite re-compositions happening in the tested code.\n"
            message += "- Debug: hadRecomposerChanges = $hadRecomposerChanges, "
            message += "hadSnapshotChanges = $hadSnapshotChanges, "
            message += "hadPendingMeasureLayout = $hadPendingMeasureLayout, "
            message += "hadPendingDraw = $hadPendingDraw"
        }
        return message
    }

    fun waitForIdle() {
        check(!isOnUiThread()) {
            "Functions that involve synchronization (Assertions, Actions, Synchronization; " +
                "e.g. assertIsSelected(), doClick(), runOnIdle()) cannot be run " +
                "from the main thread. Did you nest such a function inside " +
                "runOnIdle {}, runOnUiThread {} or setContent {}?"
        }

        // First wait until we have an AndroidOwner (in case an Activity is being started)
        androidOwnerRegistry.waitForAndroidOwners()
        // Then await composition(s)
        runEspressoOnIdle()

        // TODO(b/155774664): waitForAndroidOwners() may be satisfied by an AndroidOwner from an
        //  Activity that is about to be paused, in cases where a new Activity is being started.
        //  That means that AndroidOwnerRegistry.getOwners() may still return an empty list
        //  between now and when the new Activity has created its AndroidOwner, even though
        //  waitForAndroidOwners() suggests that we are now guaranteed one.
    }

    @ExperimentalTesting
    suspend fun awaitIdle() {
        // TODO(b/169038516): when we can query AndroidOwners for measure or layout, remove
        //  runEspressoOnIdle() and replace it with a suspend fun that loops while the
        //  snapshot or the recomposer has pending changes, clocks are busy or owners have
        //  pending measures or layouts; and do the await on AndroidUiDispatcher.Main
        // We use Espresso to wait for composition, measure, layout and draw,
        // and Espresso needs to be called from a non-ui thread; so use Dispatchers.IO
        withContext(Dispatchers.IO) {
            // First wait until we have an AndroidOwner (in case an Activity is being started)
            androidOwnerRegistry.awaitAndroidOwners()
            // Then await composition(s)
            runEspressoOnIdle()
        }
    }

    fun getOwners(): Set<Owner> {
        // TODO(pavlis): Instead of returning a flatMap, let all consumers handle a tree
        //  structure. In case of multiple AndroidOwners, add a fake root
        waitForIdle()

        return androidOwnerRegistry.getOwners().also {
            // TODO(b/153632210): This check should be done by callers of collectOwners
            check(it.isNotEmpty()) {
                "No compose views found in the app. Is your Activity resumed?"
            }
        }
    }

    fun getStatementFor(base: Statement): Statement {
        return androidOwnerRegistry.getStatementFor(base)
    }
}
