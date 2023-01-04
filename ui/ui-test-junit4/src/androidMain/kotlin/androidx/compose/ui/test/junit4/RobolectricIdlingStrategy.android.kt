/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.IdlingPolicies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Idling strategy for use with Robolectric.
 *
 * When running on Robolectric, the following things are different:
 * 1. IdlingResources are not queried. We drive Compose from the ComposeIdlingResource, so we
 * need to do that manually here.
 * 2. Draw passes don't happen. Compose performs most measure and layout passes during the draw
 * pass, so we need to manually trigger an actual measure/layout pass when needed.
 * 3. Awaiting idleness must happen on the main thread. On Espresso it's exactly the other way
 * around, so we need to invert our thread checks.
 *
 * Note that we explicitly don't install our [IdlingResourceRegistry] into Espresso even though
 * it would be a noop anyway: if at some point in the future they will be supported, our behavior
 * would silently change (potentially leading to breakages).
 */
internal class RobolectricIdlingStrategy(
    private val composeRootRegistry: ComposeRootRegistry,
    private val composeIdlingResource: ComposeIdlingResource
) : IdlingStrategy {
    override val canSynchronizeOnUiThread: Boolean = true

    override fun runUntilIdle() {
        val policy = IdlingPolicies.getMasterIdlingPolicy()
        val timeoutMillis = policy.idleTimeoutUnit.toMillis(policy.idleTimeout)
        runOnUiThread {
            // Use Java's clock, Android's clock is mocked
            val start = System.currentTimeMillis()
            var iteration = 0
            // This doesn't just check if compose is idle, it also potentially performs work to try
            // to get to idle. That work may queue up work on the main queue, so even if compose
            // reports idle we need to drain the main queue at least one more time to ensure that
            // is complete. However, we also need to check compose again after draining the queue
            // since it may have queued up more compose work.
            var composeIsIdle = composeIdlingResource.isIdleNow
            do {
                // Check if we hit the timeout
                if (System.currentTimeMillis() - start >= timeoutMillis) {
                    throw AppNotIdleException.create(
                        emptyList(),
                        "Compose did not get idle after $iteration attempts in " +
                            "${policy.idleTimeout} ${policy.idleTimeoutUnit}. " +
                            "Please check your measure/layout lambdas, they may be " +
                            "causing an infinite composition loop. Or set Espresso's " +
                            "master idling policy if you require a longer timeout."
                    )
                }
                iteration++
                // Run Espresso.onIdle() to drain the main message queue. This may queue up more
                // work for compose, so we need to check if compose is idle one more time.
                runEspressoOnIdle()
                // Check if we need a measure/layout pass.
                requestLayoutIfNeeded()
                // Check if draining the main queue queued up any more compose work. Even if compose
                // itself reports idle, reading this property queue up work on the main queue, so
                // always drain it one more time. We only consider compose truly idle if it reported
                // idle both before and after draining the main queue.
                val composeWasIdleBeforeDrain = composeIsIdle
                composeIsIdle = composeIdlingResource.isIdleNow
                // Repeat while not idle.
            } while (!(composeIsIdle && composeWasIdleBeforeDrain))
        }
    }

    override suspend fun awaitIdle() {
        // On Robolectric, Espresso.onIdle() must be called from the main thread; so use
        // Dispatchers.Main. Use `.immediate` in case we're already on the main thread.
        withContext(Dispatchers.Main.immediate) {
            runUntilIdle()
        }
    }

    /**
     * Calls [requestLayout][android.view.View.requestLayout] on all compose hosts that are
     * awaiting a measure/layout pass, because the draw pass that it is normally awaiting never
     * happens on Robolectric.
     */
    private fun requestLayoutIfNeeded(): Boolean {
        val composeRoots = composeRootRegistry.getRegisteredComposeRoots()
        return composeRoots.filter { it.shouldWaitForMeasureAndLayout }
            .onEach { it.view.requestLayout() }
            .isNotEmpty()
    }
}
