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

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.runners.model.Statement
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.ExperimentalTime

/**
 * Registry where all views implementing [ViewRootForTest] should be registered while they
 * are attached to the window. This registry is used by the testing library to query the roots'
 * state.
 */
internal class ComposeRootRegistry {
    private val roots = Collections.newSetFromMap(WeakHashMap<ViewRootForTest, Boolean>())
    private val registryListeners = mutableSetOf<OnRegistrationChangedListener>()

    /**
     * Returns if the registry is setup to receive registrations from [ViewRootForTest]s
     */
    val isSetUp: Boolean
        get() = ViewRootForTest.onViewCreatedCallback == ::onViewRootCreated

    /**
     * Sets up this registry to be notified of any [ViewRootForTest] created
     */
    private fun setupRegistry() {
        ViewRootForTest.onViewCreatedCallback = ::onViewRootCreated
    }

    /**
     * Cleans up the changes made by [setupRegistry]. Call this after your test has run.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun tearDownRegistry() {
        ViewRootForTest.onViewCreatedCallback = null
        synchronized(roots) {
            getUnfilteredComposeRoots().forEach {
                unregisterComposeRoot(it)
            }
            // Remove all listeners as well, now we've unregistered all roots
            synchronized(registryListeners) { registryListeners.clear() }
        }
    }

    private fun onViewRootCreated(root: ViewRootForTest) {
        root.view.addOnAttachStateChangeListener(ViewAttachedListener(root))
    }

    /**
     * Returns a copy of the set of all registered [ViewRootForTest]s, including ones that are
     * normally not relevant (like those whose lifecycle state is not RESUMED).
     */
    fun getUnfilteredComposeRoots(): Set<ViewRootForTest> {
        return synchronized(roots) { roots.toSet() }
    }

    /**
     * Returns a copy of the set of all registered [ViewRootForTest]s that can be interacted with.
     * This method is almost always preferred over [getUnfilteredComposeRoots].
     */
    fun getComposeRoots(): Set<ViewRootForTest> {
        return synchronized(roots) {
            roots.filterTo(mutableSetOf()) {
                it.isLifecycleInResumedState
            }
        }
    }

    /**
     * Adds the given [listener], to be notified when an [ViewRootForTest] registers or unregisters.
     */
    fun addOnRegistrationChangedListener(listener: OnRegistrationChangedListener) {
        synchronized(registryListeners) { registryListeners.add(listener) }
    }

    /**
     * Removes the given [listener].
     */
    fun removeOnRegistrationChangedListener(listener: OnRegistrationChangedListener) {
        synchronized(registryListeners) { registryListeners.remove(listener) }
    }

    private fun dispatchOnRegistrationChanged(composeRoot: ViewRootForTest, isRegistered: Boolean) {
        synchronized(registryListeners) { registryListeners.toList() }.forEach {
            it.onRegistrationChanged(composeRoot, isRegistered)
        }
    }

    /**
     * Registers the [composeRoot] in this registry. Must be called from [View.onAttachedToWindow].
     */
    internal fun registerComposeRoot(composeRoot: ViewRootForTest) {
        synchronized(roots) {
            if (roots.add(composeRoot)) {
                dispatchOnRegistrationChanged(composeRoot, true)
            }
        }
    }

    /**
     * Unregisters the [composeRoot] from this registry. Must be called from
     * [View.onDetachedFromWindow].
     */
    internal fun unregisterComposeRoot(composeRoot: ViewRootForTest) {
        synchronized(roots) {
            if (roots.remove(composeRoot)) {
                dispatchOnRegistrationChanged(composeRoot, false)
            }
        }
    }

    fun getStatementFor(base: Statement): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    setupRegistry()
                    base.evaluate()
                } finally {
                    tearDownRegistry()
                }
            }
        }
    }

    /**
     * Interface to be implemented by components that want to be notified when an [ViewRootForTest]
     * registers or unregisters at this registry.
     */
    interface OnRegistrationChangedListener {
        fun onRegistrationChanged(composeRoot: ViewRootForTest, registered: Boolean)
    }

    private inner class ViewAttachedListener(
        private val composeRoot: ViewRootForTest
    ) : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            registerComposeRoot(composeRoot)
        }

        override fun onViewDetachedFromWindow(view: View) {
            unregisterComposeRoot(composeRoot)
        }
    }
}

private val ComposeRootRegistry.hasComposeRoots: Boolean get() = getComposeRoots().isNotEmpty()

private fun ComposeRootRegistry.ensureComposeRootRegistryIsSetUp() {
    check(isSetUp) {
        "Test not setup properly. Use a ComposeTestRule in your test to be able to interact " +
            "with composables"
    }
}

internal fun ComposeRootRegistry.waitForComposeRoots() {
    ensureComposeRootRegistryIsSetUp()

    if (!hasComposeRoots) {
        val latch = CountDownLatch(1)
        val listener = object : ComposeRootRegistry.OnRegistrationChangedListener {
            override fun onRegistrationChanged(composeRoot: ViewRootForTest, registered: Boolean) {
                if (hasComposeRoots) {
                    latch.countDown()
                }
            }
        }
        try {
            addOnRegistrationChangedListener(listener)
            if (!hasComposeRoots) {
                latch.await(2, TimeUnit.SECONDS)
            }
        } finally {
            removeOnRegistrationChangedListener(listener)
        }
    }
}

@ExperimentalTestApi
@OptIn(ExperimentalTime::class)
internal suspend fun ComposeRootRegistry.awaitComposeRoots() {
    ensureComposeRootRegistryIsSetUp()

    if (!hasComposeRoots) {
        suspendCancellableCoroutine<Unit> { continuation ->
            // Make sure we only resume once
            val didResume = AtomicBoolean(false)
            fun resume(listener: ComposeRootRegistry.OnRegistrationChangedListener) {
                if (didResume.compareAndSet(false, true)) {
                    removeOnRegistrationChangedListener(listener)
                    continuation.resume(Unit)
                }
            }

            // Usually we resume if a compose root is registered while the listener is added
            val listener = object : ComposeRootRegistry.OnRegistrationChangedListener {
                override fun onRegistrationChanged(
                    composeRoot: ViewRootForTest,
                    registered: Boolean
                ) {
                    if (hasComposeRoots) {
                        resume(this)
                    }
                }
            }

            addOnRegistrationChangedListener(listener)
            continuation.invokeOnCancellation {
                removeOnRegistrationChangedListener(listener)
            }

            // But sometimes the compose root was registered before we added
            // the listener, in which case we missed our signal
            if (hasComposeRoots) {
                resume(listener)
            }
        }
    }
}
