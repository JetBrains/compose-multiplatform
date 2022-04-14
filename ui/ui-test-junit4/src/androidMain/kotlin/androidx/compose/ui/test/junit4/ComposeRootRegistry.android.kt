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

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ViewRootForTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Registry where all views implementing [ViewRootForTest] should be registered while they
 * are attached to the window. This registry is used by the testing library to query the roots'
 * state.
 */
internal class ComposeRootRegistry {

    private val lock = Any()
    private val allRoots = Collections.newSetFromMap(WeakHashMap<ViewRootForTest, Boolean>())
    private val resumedRoots = mutableSetOf<ViewRootForTest>()
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
        synchronized(lock) {
            // Stop accepting new roots
            ViewRootForTest.onViewCreatedCallback = null
            // Unregister the world
            getCreatedComposeRoots().forEach {
                unregisterComposeRoot(it)
            }
            // Clear all references
            allRoots.clear()
            resumedRoots.clear()
            registryListeners.clear()
        }
    }

    private fun onViewRootCreated(root: ViewRootForTest) {
        // Need to register immediately to accommodate ViewRoots that have delayed
        // setContent until they are attached to the window (e.g. popups and dialogs).
        synchronized(lock) {
            if (isSetUp) {
                allRoots.add(root)
                root.view.addOnAttachStateChangeListener(StateChangeHandler(root))
            }
        }
    }

    /**
     * Returns a copy of the set of all created [ViewRootForTest]s, including ones that are
     * normally not relevant (like those whose lifecycle state is not RESUMED). You probably need
     * [getRegisteredComposeRoots] though. Do not store any of these results, always call this
     * method again if you need access to anything in this set.
     */
    fun getCreatedComposeRoots(): Set<ViewRootForTest> {
        synchronized(lock) {
            while (true) {
                try {
                    return allRoots.toSet()
                } catch (_: ConcurrentModificationException) {
                    // A weakly referenced key may have been cleared while copying the set
                    // Keep trying until we succeed
                } catch (_: NoSuchElementException) {
                    // Same as above
                }
            }
        }
    }

    /**
     * Returns a copy of the set of all registered [ViewRootForTest]s that can be interacted with.
     * This method is almost always preferred over [getCreatedComposeRoots].
     */
    fun getRegisteredComposeRoots(): Set<ViewRootForTest> {
        return synchronized(lock) { resumedRoots.toSet() }
    }

    /**
     * Registers the [composeRoot] in this registry. Must be called from [View.onAttachedToWindow].
     */
    internal fun registerComposeRoot(composeRoot: ViewRootForTest) {
        synchronized(lock) {
            if (isSetUp && resumedRoots.add(composeRoot)) {
                dispatchOnRegistrationChanged(composeRoot, true)
            }
        }
    }

    /**
     * Unregisters the [composeRoot] from this registry. Must be called from
     * [View.onDetachedFromWindow].
     */
    internal fun unregisterComposeRoot(composeRoot: ViewRootForTest) {
        synchronized(lock) {
            if (resumedRoots.remove(composeRoot)) {
                dispatchOnRegistrationChanged(composeRoot, false)
            }
        }
    }

    fun <R> withRegistry(block: () -> R): R {
        try {
            setupRegistry()
            return block()
        } finally {
            tearDownRegistry()
        }
    }

    /**
     * Interface to be implemented by components that want to be notified when an [ViewRootForTest]
     * registers or unregisters at this registry.
     */
    interface OnRegistrationChangedListener {
        fun onRegistrationChanged(composeRoot: ViewRootForTest, registered: Boolean)
    }

    /**
     * Adds the given [listener], to be notified when an [ViewRootForTest] registers or unregisters.
     */
    fun addOnRegistrationChangedListener(listener: OnRegistrationChangedListener) {
        synchronized(lock) { registryListeners.add(listener) }
    }

    /**
     * Removes the given [listener].
     */
    fun removeOnRegistrationChangedListener(listener: OnRegistrationChangedListener) {
        synchronized(lock) { registryListeners.remove(listener) }
    }

    private fun dispatchOnRegistrationChanged(composeRoot: ViewRootForTest, isRegistered: Boolean) {
        synchronized(lock) { registryListeners.toList() }.forEach {
            it.onRegistrationChanged(composeRoot, isRegistered)
        }
    }

    private inner class StateChangeHandler(
        private val composeRoot: ViewRootForTest
    ) : View.OnAttachStateChangeListener, LifecycleEventObserver, OnRegistrationChangedListener {
        private var removeObserver: (() -> Unit)? = null

        override fun onViewAttachedToWindow(view: View) {
            // Only add lifecycle observer. If the root is resumed,
            // the lifecycle observer will get notified.
            // TODO: This can be missing if the ComposeView is in a ViewOverlay.
            // If so, we do nothing and bail.
            val lifecycle = ViewTreeLifecycleOwner.get(view)?.lifecycle ?: return
            lifecycle.addObserver(this)
            // Setup a lambda to remove the observer when we're detached from the window. When
            // that happens, we won't have access to the lifecycle anymore.
            removeObserver = {
                lifecycle.removeObserver(this)
            }
        }

        override fun onViewDetachedFromWindow(view: View) {
            removeLifecycleObserver()
            // Also unregister the root, as we won't receive lifecycle events anymore
            unregisterComposeRoot()
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_RESUME) {
                registerComposeRoot(composeRoot)
                // Listen to when it is unregistered: it happens
                // when the registry is torn down prematurely
                addOnRegistrationChangedListener(this)
            } else {
                unregisterComposeRoot()
            }
        }

        override fun onRegistrationChanged(composeRoot: ViewRootForTest, registered: Boolean) {
            if (composeRoot == this.composeRoot && !registered) {
                // If we are unregistered, stop observing the lifecycle
                removeLifecycleObserver()
            }
        }

        private fun unregisterComposeRoot() {
            removeOnRegistrationChangedListener(this)
            unregisterComposeRoot(composeRoot)
        }

        private fun removeLifecycleObserver() {
            // Lifecycle observers can only be added/removed on the main thread, but
            // this method can be called from any thread when the registry is torn down.
            if (Looper.myLooper() != Looper.getMainLooper()) {
                // Post at front of queue to make sure we remove
                // the observer before it can receive new events
                Handler(Looper.getMainLooper()).postAtFrontOfQueue {
                    removeLifecycleObserverMainThread()
                }
            } else {
                removeLifecycleObserverMainThread()
            }
        }

        private fun removeLifecycleObserverMainThread() {
            removeObserver?.invoke()?.also {
                removeObserver = null
            }
        }
    }
}

private val ComposeRootRegistry.hasComposeRoots: Boolean
    get() = getRegisteredComposeRoots().isNotEmpty()

private fun ComposeRootRegistry.ensureComposeRootRegistryIsSetUp() {
    check(isSetUp) {
        "Test not setup properly. Use a ComposeTestRule in your test to be able to interact " +
            "with composables"
    }
}

internal fun ComposeRootRegistry.waitForComposeRoots(atLeastOneRootExpected: Boolean) {
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
                if (atLeastOneRootExpected) {
                    latch.await(2, TimeUnit.SECONDS)
                } else {
                    latch.await(500, TimeUnit.MILLISECONDS)
                }
            }
        } finally {
            removeOnRegistrationChangedListener(listener)
        }
    }
}

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
