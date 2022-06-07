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

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.InternalTestApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class IdlingResourceRegistry
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@InternalTestApi
internal constructor(
    private val pollScopeOverride: CoroutineScope?
) : IdlingResource {
    // Publicly facing constructor, that doesn't override the poll scope
    @OptIn(InternalTestApi::class)
    constructor() : this(null)

    private val lock = Any()

    // All registered IdlingResources, both idle and busy ones
    private val idlingResources = mutableSetOf<IdlingResource>()
    // Each busy resource is mapped to the job that polls it
    private val busyResources = mutableSetOf<IdlingResource>()
    // The job that polls the resources until they are idle
    private var pollJob: Job = Job().also { it.complete() }
    // The scope in which to launch the poll job, or await the poll job
    private val pollScope = pollScopeOverride ?: CoroutineScope(Dispatchers.Main)

    private val isPolling: Boolean
        get() = !pollJob.isCompleted

    // Callback to be called every time when the last busy resource becomes idle
    private var onIdle: (() -> Unit)? = null

    /**
     * Returns if all resources are idle
     */
    override val isIdleNow: Boolean get() {
        @Suppress("DEPRECATION_ERROR")
        return synchronized(lock) {
            // If a poll job is running, we're not idle now. Let the job do its job.
            !isPolling && areAllResourcesIdle()
        }
    }

    /**
     * Installs a callback that will be called when the registry transitions from busy to idle.
     * Intended for the owner of the registry (e.g. AndroidComposeTestRule).
     */
    internal fun setOnIdleCallback(callback: () -> Unit) {
        onIdle = callback
    }

    /**
     * Registers the [idlingResource] into the registry
     */
    fun registerIdlingResource(idlingResource: IdlingResource) {
        @Suppress("DEPRECATION_ERROR")
        synchronized(lock) {
            idlingResources.add(idlingResource)
        }
    }

    /**
     * Unregisters the [idlingResource] from the registry
     */
    fun unregisterIdlingResource(idlingResource: IdlingResource) {
        @Suppress("DEPRECATION_ERROR")
        synchronized(lock) {
            idlingResources.remove(idlingResource)
            busyResources.remove(idlingResource)
        }
    }

    /**
     * Like [isIdleNow], but starts a poll job if the registry is not idle and no poll job is yet
     * running.
     */
    internal fun isIdleOrEnsurePolling(): Boolean {
        @Suppress("DEPRECATION_ERROR")
        return synchronized(lock) {
            !isPolling && areAllResourcesIdle().also { isIdle ->
                if (!isIdle) {
                    pollJob = pollScope.launch {
                        do {
                            delay(20)
                        } while (!areAllResourcesIdle())
                        onIdle?.invoke()
                    }
                }
            }
        }
    }

    /**
     * Checks all resources for idleness, updates [busyResources] and returns if the registry is
     * idle now.
     */
    private fun areAllResourcesIdle(): Boolean {
        @Suppress("DEPRECATION_ERROR")
        return synchronized(lock) {
            busyResources.clear()
            idlingResources.filterTo(busyResources) { !it.isIdleNow }.isEmpty()
        }
    }

    override fun getDiagnosticMessageIfBusy(): String? {
        val (idle, busy) =
            @Suppress("DEPRECATION_ERROR")
            synchronized(lock) {
                if (busyResources.isEmpty()) {
                    return null
                }
                Pair(
                    (idlingResources - busyResources).toList(),
                    busyResources.map { it.getDiagnosticMessageIfBusy() ?: it.toString() }
                )
            }
        return "IdlingResourceRegistry has the following idling resources registered:" +
            busy.joinToString { "\n- [busy] ${it.indentBy("         ")}" } +
            idle.joinToString { "\n- [idle] $it" } +
            if (idle.isEmpty() && busy.isEmpty()) "\n<none>" else ""
    }

    /**
     * Adds the given [prefix] after all non-terminal new lines.
     *
     * For example: `"\nfoo\nbar\n".indentBy("-")` gives `"\n-foo\n-bar\n"`
     */
    private fun String.indentBy(prefix: String): String {
        return replace("\n(?=.)".toRegex(), "\n$prefix")
    }

    fun <R> withRegistry(block: () -> R): R {
        try {
            return block()
        } finally {
            if (pollScopeOverride == null) {
                if (pollScope.coroutineContext[Job] != null) pollScope.cancel()
            }
        }
    }
}
