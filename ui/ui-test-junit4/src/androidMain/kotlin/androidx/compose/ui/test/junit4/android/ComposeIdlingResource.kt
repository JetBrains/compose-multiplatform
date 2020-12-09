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
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.node.Owner
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.TestAnimationClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.isOnUiThread
import androidx.compose.ui.test.junit4.runOnUiThread
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResourceTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.runners.model.Statement
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
internal class ComposeIdlingResource : IdlingResource, IdlingResourceWithDiagnostics {

    override fun getName(): String = "ComposeIdlingResource"

    private var isIdleCheckScheduled = false
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val androidOwnerRegistry = AndroidOwnerRegistry()

    @OptIn(ExperimentalTesting::class)
    private val clocks = mutableSetOf<TestAnimationClock>()

    private val handler = Handler(Looper.getMainLooper())

    private var hadAnimationClocksIdle = true
    private var hadNoSnapshotChanges = true
    private var hadNoRecomposerChanges = true
    private var lastCompositionAwaiters = 0
    private var hadNoPendingMeasureLayout = true
    // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
//    private var hadNoPendingDraw = true

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
            val owners = androidOwnerRegistry.getUnfilteredOwners()
            hadNoPendingMeasureLayout = !owners.any { it.hasPendingMeasureOrLayout }
            // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
//            hadNoPendingDraw = !owners.any {
//                val hasContent = it.view.measuredWidth != 0 && it.view.measuredHeight != 0
//                it.view.isDirty && (hasContent || it.view.isLayoutRequested)
//            }

            check(lastCompositionAwaiters >= 0) {
                "More CompositionAwaiters were removed then added ($lastCompositionAwaiters)"
            }

            hadNoSnapshotChanges &&
                hadNoRecomposerChanges &&
                hadAnimationClocksIdle &&
                lastCompositionAwaiters == 0 &&
                // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
                hadNoPendingMeasureLayout /*&&
                hadNoPendingDraw*/
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

    private fun transitionToIdle() {
        resourceCallback?.onTransitionToIdle()
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
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
        val numCompositionAwaiters = lastCompositionAwaiters
        val wasAwaitingCompositions = numCompositionAwaiters > 0
        val hadPendingMeasureLayout = !hadNoPendingMeasureLayout
        // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
//        val hadPendingDraw = !hadNoPendingDraw

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
            message += "numCompositionAwaiters = $numCompositionAwaiters, "
            message += "hadPendingMeasureLayout = $hadPendingMeasureLayout"
            // TODO(b/174244530): Include hadNoPendingDraw when it is reliable
//            message += ", hadPendingDraw = $hadPendingDraw"
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
        return androidOwnerRegistry.getStatementFor(
            object : Statement() {
                override fun evaluate() {
                    try {
                        IdlingRegistry.getInstance().register(this@ComposeIdlingResource)
                        base.evaluate()
                    } finally {
                        IdlingRegistry.getInstance().unregister(this@ComposeIdlingResource)
                    }
                }
            }
        )
    }
}

// TODO(b/168223213): Make the CompositionAwaiter a suspend fun, remove ComposeIdlingResource
//  and blocking await Espresso.onIdle().
internal fun ComposeIdlingResource.runEspressoOnIdle() {
    val compositionAwaiter = CompositionAwaiter(this)
    try {
        compositionAwaiter.start()
        Espresso.onIdle()
    } catch (e: Throwable) {
        compositionAwaiter.cancel()

        // Happens on the global time out, usually when global idling time out is less
        // or equal to dynamic idling time out or when the timeout is not due to individual
        // idling resource. This does not necessary mean that it can't be due to idling
        // resource being busy. So we try to check if it failed due to compose being busy and
        // add some extra information to the developer.
        val appNotIdleMaybe = tryToFindCause<AppNotIdleException>(e)
        if (appNotIdleMaybe != null) {
            rethrowWithMoreInfo(appNotIdleMaybe, wasGlobalTimeout = true)
        }

        // Happens on idling resource taking too long. Espresso gives out which resources caused
        // it but it won't allow us to give any extra information. So we check if it was our
        // resource and give more info if we can.
        val resourceNotIdleMaybe = tryToFindCause<IdlingResourceTimeoutException>(e)
        if (resourceNotIdleMaybe != null) {
            rethrowWithMoreInfo(resourceNotIdleMaybe, wasGlobalTimeout = false)
        }

        // No match, rethrow
        throw e
    }
}

private fun rethrowWithMoreInfo(e: Throwable, wasGlobalTimeout: Boolean) {
    var diagnosticInfo = ""
    val listOfIdlingResources = mutableListOf<String>()
    IdlingRegistry.getInstance().resources.forEach { resource ->
        if (resource is IdlingResourceWithDiagnostics) {
            val message = resource.getDiagnosticMessageIfBusy()
            if (message != null) {
                diagnosticInfo += "$message \n"
            }
        }
        listOfIdlingResources.add(resource.name)
    }
    if (diagnosticInfo.isNotEmpty()) {
        val prefix = if (wasGlobalTimeout) {
            "Global time out"
        } else {
            "Idling resource timed out"
        }
        throw ComposeNotIdleException(
            "$prefix: possibly due to compose being busy.\n" +
                diagnosticInfo +
                "All registered idling resources: " +
                listOfIdlingResources.joinToString(", "),
            e
        )
    }
    // No extra info, re-throw the original exception
    throw e
}

/**
 * Tries to find if the given exception or any of its cause is of the type of the provided
 * throwable T. Returns null if there is no match. This is required as some exceptions end up
 * wrapped in Runtime or Concurrent exceptions.
 */
private inline fun <reified T : Throwable> tryToFindCause(e: Throwable): Throwable? {
    var causeToCheck: Throwable? = e
    while (causeToCheck != null) {
        if (causeToCheck is T) {
            return causeToCheck
        }
        causeToCheck = causeToCheck.cause
    }
    return null
}

/**
 * Thrown in cases where Compose can't get idle in Espresso's defined time limit.
 */
class ComposeNotIdleException(message: String?, cause: Throwable?) : Throwable(message, cause)
