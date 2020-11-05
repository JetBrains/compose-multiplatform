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

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.compose.ui.test.TestAnimationClock
import androidx.compose.ui.test.junit4.runOnUiThread
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * In case Espresso times out, implementing this interface enables our resources to explain why
 * they failed to synchronize in case they were busy.
 */
internal interface IdlingResourceWithDiagnostics {
    // TODO: Consider this as a public API.
    fun getDiagnosticMessageIfBusy(): String?
}

/**
 * Register compose's idling check to Espresso.
 *
 * This makes sure that Espresso is able to wait for any pending changes in Compose. This
 * resource is automatically registered when any compose testing APIs are used including
 * [createAndroidComposeRule]. If you for some reasons want to only use Espresso but still have it
 * wait for Compose being idle you can use this function.
 */
fun registerComposeWithEspresso() {
    ComposeIdlingResource.registerSelfIntoEspresso()
    FirstDrawIdlingResource.registerSelfIntoEspresso()
}

/**
 * Unregisters resource registered as part of [registerComposeWithEspresso].
 */
fun unregisterComposeFromEspresso() {
    ComposeIdlingResource.unregisterSelfFromEspresso()
    FirstDrawIdlingResource.unregisterSelfFromEspresso()
}

/**
 * Registers the given [clock] so Espresso can await the animations subscribed to that clock.
 */
fun registerTestClock(clock: TestAnimationClock) {
    ComposeIdlingResource.registerTestClock(clock)
}

/**
 * Unregisters the [clock] that was registered with [registerTestClock].
 */
fun unregisterTestClock(clock: TestAnimationClock) {
    ComposeIdlingResource.unregisterTestClock(clock)
}

/**
 * Provides an idle check to be registered into Espresso.
 *
 * This makes sure that Espresso is able to wait for any pending changes in Compose. This
 * resource is automatically registered when any compose testing APIs are used including
 * [createAndroidComposeRule]. If you for some reasons want to only use Espresso but still have it
 * wait for Compose being idle you can register this yourself via [registerSelfIntoEspresso].
 */
internal object ComposeIdlingResource : BaseIdlingResource(), IdlingResourceWithDiagnostics {

    override fun getName(): String = "ComposeIdlingResource"

    private var isIdleCheckScheduled = false

    private val clocks = mutableSetOf<TestAnimationClock>()

    private val handler = Handler(Looper.getMainLooper())

    private var hadAnimationClocksIdle = true
    private var hadNoSnapshotChanges = true
    private var hadNoRecomposerChanges = true
    private var lastCompositionAwaiters = 0

    private var compositionAwaiters = AtomicInteger(0)

    /**
     * Returns whether or not Compose is idle, without starting to poll if it is not.
     */
    @OptIn(ExperimentalComposeApi::class)
    fun isIdle(): Boolean {
        @Suppress("DEPRECATION")
        return runOnUiThread {
            hadNoSnapshotChanges = !Snapshot.current.hasPendingChanges()
            hadNoRecomposerChanges = !Recomposer.current().hasInvalidations()
            hadAnimationClocksIdle = areAllClocksIdle()
            lastCompositionAwaiters = compositionAwaiters.get()

            check(lastCompositionAwaiters >= 0) {
                "More CompositionAwaiters were removed then added ($lastCompositionAwaiters)"
            }

            hadNoSnapshotChanges && hadNoRecomposerChanges && hadAnimationClocksIdle &&
                lastCompositionAwaiters == 0
        }
    }

    /**
     * Returns whether or not Compose is idle, and starts polling if it is not. Will always be
     * called from the main thread by Espresso, and should _only_ be called from Espresso. Use
     * [isIdle] if you need to query the idleness of Compose manually.
     */
    override fun isIdleNow(): Boolean {
        val isIdle = isIdle()
        if (!isIdle) {
            scheduleIdleCheck()
        }
        return isIdle
    }

    private fun scheduleIdleCheck() {
        if (!isIdleCheckScheduled) {
            isIdleCheckScheduled = true
            handler.postDelayed(
                {
                    isIdleCheckScheduled = false
                    if (isIdle()) {
                        transitionToIdle()
                    } else {
                        scheduleIdleCheck()
                    }
                }, /* delayMillis = */ 20
            )
        }
    }

    /**
     * Called by [CompositionAwaiter] to indicate that this [ComposeIdlingResource] should report
     * busy to Espresso while that [CompositionAwaiter] is checking idleness.
     */
    internal fun addCompositionAwaiter() {
        compositionAwaiters.incrementAndGet()
    }

    /**
     * Called by [CompositionAwaiter] to indicate that this [ComposeIdlingResource] can report
     * idle as far as the calling [CompositionAwaiter] is concerned.
     */
    internal fun removeCompositionAwaiter() {
        compositionAwaiters.decrementAndGet()
    }

    internal fun registerTestClock(clock: TestAnimationClock) {
        synchronized(clocks) {
            clocks.add(clock)
        }
    }

    internal fun unregisterTestClock(clock: TestAnimationClock) {
        synchronized(clocks) {
            clocks.remove(clock)
        }
    }

    private fun areAllClocksIdle(): Boolean {
        return synchronized(clocks) {
            clocks.all { it.isIdle }
        }
    }

    override fun getDiagnosticMessageIfBusy(): String? {
        val hadSnapshotChanges = !hadNoSnapshotChanges
        val hadRecomposerChanges = !hadNoRecomposerChanges
        val hadRunningAnimations = !hadAnimationClocksIdle
        val numCompositionAwaiters = lastCompositionAwaiters
        val wasAwaitingCompositions = numCompositionAwaiters > 0

        val wasIdle = !hadSnapshotChanges && !hadRecomposerChanges &&
            !hadRunningAnimations && !wasAwaitingCompositions

        if (wasIdle) {
            return null
        }

        val busyReasons = mutableListOf<String>()
        if (hadRunningAnimations) {
            busyReasons.add("animations")
        }
        val busyRecomposing = hadSnapshotChanges || hadRecomposerChanges || wasAwaitingCompositions
        if (busyRecomposing) {
            busyReasons.add("pending recompositions")
        }

        var message = "$name is busy due to ${busyReasons.joinToString(", ")}.\n"
        if (busyRecomposing) {
            message += "- Note: Timeout on pending recomposition means that there are most likely" +
                " infinite re-compositions happening in the tested code.\n"
            message += "- Debug: hadRecomposerChanges = $hadRecomposerChanges, "
            message += "hadSnapshotChanges = $hadSnapshotChanges, "
            message += "numCompositionAwaiters = $numCompositionAwaiters"
        }
        return message
    }
}

private object FirstDrawIdlingResource : BaseIdlingResource() {
    override fun getName(): String = "FirstDrawIdlingResource"

    override fun isIdleNow(): Boolean {
        return FirstDrawRegistry.haveAllDrawn().also {
            if (!it) {
                FirstDrawRegistry.setOnDrawnCallback(::transitionToIdle)
            }
        }
    }

    override fun unregisterSelfFromEspresso() {
        super.unregisterSelfFromEspresso()
        FirstDrawRegistry.setOnDrawnCallback(null)
    }
}

internal sealed class BaseIdlingResource : IdlingResource {
    private val isRegistered = AtomicBoolean(false)
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    final override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }

    protected fun transitionToIdle() {
        resourceCallback?.onTransitionToIdle()
    }

    /**
     * Registers this resource into Espresso.
     *
     * Can be called multiple times.
     */
    internal fun registerSelfIntoEspresso() {
        if (isRegistered.compareAndSet(false, true)) {
            IdlingRegistry.getInstance().register(this)
        }
    }

    /**
     * Unregisters this resource from Espresso.
     *
     * Can be called multiple times.
     */
    internal open fun unregisterSelfFromEspresso() {
        if (isRegistered.compareAndSet(true, false)) {
            IdlingRegistry.getInstance().unregister(this)
        }
    }
}
