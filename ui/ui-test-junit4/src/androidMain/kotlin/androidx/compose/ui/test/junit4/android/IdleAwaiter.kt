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

import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.InternalTestingApi
import androidx.compose.ui.test.junit4.isOnUiThread
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResourceTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.ExperimentalTime

@OptIn(InternalTestingApi::class)
internal class IdleAwaiter(private val composeIdlingResource: ComposeIdlingResource) {

    fun waitForIdle() {
        check(!isOnUiThread()) {
            "Functions that involve synchronization (Assertions, Actions, Synchronization; " +
                "e.g. assertIsSelected(), doClick(), runOnIdle()) cannot be run " +
                "from the main thread. Did you nest such a function inside " +
                "runOnIdle {}, runOnUiThread {} or setContent {}?"
        }

        // First wait until we have an AndroidOwner (in case an Activity is being started)
        waitForAndroidOwners()
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
            awaitAndroidOwners()
            // Then await composition(s)
            runEspressoOnIdle()
        }
    }

    // TODO(168223213): Make the CompositionAwaiter a suspend fun, remove ComposeIdlingResource
    //  and blocking await Espresso.onIdle().
    private fun runEspressoOnIdle() {
        fun rethrowWithMoreInfo(e: Throwable, wasGlobalTimeout: Boolean) {
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

        val compositionAwaiter = CompositionAwaiter(composeIdlingResource)
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

    private fun ensureAndroidOwnerRegistryIsSetUp() {
        check(AndroidOwnerRegistry.isSetUp) {
            "Test not setup properly. Use a ComposeTestRule in your test to be able to interact " +
                "with composables"
        }
    }

    private fun waitForAndroidOwners() {
        ensureAndroidOwnerRegistryIsSetUp()

        fun hasAndroidOwners(): Boolean = AndroidOwnerRegistry.getOwners().isNotEmpty()

        if (!hasAndroidOwners()) {
            val latch = CountDownLatch(1)
            val listener = object : AndroidOwnerRegistry.OnRegistrationChangedListener {
                override fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean) {
                    if (hasAndroidOwners()) {
                        latch.countDown()
                    }
                }
            }
            try {
                AndroidOwnerRegistry.addOnRegistrationChangedListener(listener)
                if (!hasAndroidOwners()) {
                    latch.await(2, TimeUnit.SECONDS)
                }
            } finally {
                AndroidOwnerRegistry.removeOnRegistrationChangedListener(listener)
            }
        }
    }

    @ExperimentalTesting
    @OptIn(ExperimentalTime::class)
    private suspend fun awaitAndroidOwners() {
        ensureAndroidOwnerRegistryIsSetUp()

        fun hasAndroidOwners(): Boolean = AndroidOwnerRegistry.getOwners().isNotEmpty()

        if (!hasAndroidOwners()) {
            suspendCancellableCoroutine<Unit> { continuation ->
                // Make sure we only resume once
                val didResume = AtomicBoolean(false)
                fun resume(listener: AndroidOwnerRegistry.OnRegistrationChangedListener) {
                    if (didResume.compareAndSet(false, true)) {
                        AndroidOwnerRegistry.removeOnRegistrationChangedListener(listener)
                        continuation.resume(Unit)
                    }
                }

                // Usually we resume if an AndroidOwner is registered while the listener is added
                val listener = object : AndroidOwnerRegistry.OnRegistrationChangedListener {
                    override fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean) {
                        if (hasAndroidOwners()) {
                            resume(this)
                        }
                    }
                }

                AndroidOwnerRegistry.addOnRegistrationChangedListener(listener)
                continuation.invokeOnCancellation {
                    AndroidOwnerRegistry.removeOnRegistrationChangedListener(listener)
                }

                // Sometimes the AndroidOwner was registered before we added
                // the listener, in which case we missed our signal
                if (hasAndroidOwners()) {
                    resume(listener)
                }
            }
        }
    }
}

/**
 * Thrown in cases where Compose can't get idle in Espresso's defined time limit.
 */
class ComposeNotIdleException(message: String?, cause: Throwable?) : Throwable(message, cause)
