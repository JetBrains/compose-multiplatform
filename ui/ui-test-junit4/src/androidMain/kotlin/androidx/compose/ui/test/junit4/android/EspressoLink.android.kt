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

import androidx.compose.ui.test.junit4.IdlingResourceRegistry
import androidx.compose.ui.test.junit4.IdlingStrategy
import androidx.compose.ui.test.junit4.isOnUiThread
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResourceTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.runners.model.Statement

/**
 * Idling strategy for regular Android Instrumented tests, built on Espresso.
 *
 * This installs the [IdlingResourceRegistry] as an [IdlingResource] into Espresso and delegates
 * all the work to Espresso. We wrap [Espresso.onIdle] so we can print more informative error
 * messages.
 */
internal class EspressoLink(
    private val registry: IdlingResourceRegistry
) : IdlingResource, IdlingStrategy {

    override val canSynchronizeOnUiThread: Boolean = false

    override fun getName(): String = "Compose-Espresso link"

    override fun isIdleNow(): Boolean {
        return registry.isIdleOrEnsurePolling()
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        registry.setOnIdleCallback {
            callback?.onTransitionToIdle()
        }
    }

    fun getDiagnosticMessageIfBusy(): String? = registry.getDiagnosticMessageIfBusy()

    override fun getStatementFor(base: Statement): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    IdlingRegistry.getInstance().register(this@EspressoLink)
                    base.evaluate()
                } finally {
                    IdlingRegistry.getInstance().unregister(this@EspressoLink)
                }
            }
        }
    }

    override fun runUntilIdle() {
        check(!isOnUiThread()) {
            "Functions that involve synchronization (Assertions, Actions, Synchronization; " +
                "e.g. assertIsSelected(), doClick(), runOnIdle()) cannot be run " +
                "from the main thread. Did you nest such a function inside " +
                "runOnIdle {}, runOnUiThread {} or setContent {}?"
        }
        runEspressoOnIdle()
    }

    override suspend fun awaitIdle() {
        // Espresso.onIdle() must be called from a non-ui thread; so use Dispatchers.IO
        withContext(Dispatchers.IO) {
            runUntilIdle()
        }
    }
}

internal fun runEspressoOnIdle() {
    try {
        Espresso.onIdle()
    } catch (e: Throwable) {

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
        if (resource is EspressoLink) {
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
