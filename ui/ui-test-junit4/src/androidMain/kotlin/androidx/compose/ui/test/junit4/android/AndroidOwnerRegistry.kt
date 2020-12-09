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
import androidx.compose.ui.test.ExperimentalTesting
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
 * are attached to the window. This registry is used by the testing library to query the owners's
 * state.
 */
internal class AndroidOwnerRegistry {
    private val owners = Collections.newSetFromMap(WeakHashMap<ViewRootForTest, Boolean>())
    private val registryListeners = mutableSetOf<OnRegistrationChangedListener>()

    /**
     * Returns if the registry is setup to receive registrations from [ViewRootForTest]s
     */
    val isSetUp: Boolean
        get() = ViewRootForTest.onViewCreatedCallback == ::onComposeViewCreated

    /**
     * Sets up this registry to be notified of any [ViewRootForTest] created
     */
    private fun setupRegistry() {
        ViewRootForTest.onViewCreatedCallback = ::onComposeViewCreated
    }

    /**
     * Cleans up the changes made by [setupRegistry]. Call this after your test has run.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun tearDownRegistry() {
        ViewRootForTest.onViewCreatedCallback = null
        synchronized(owners) {
            getUnfilteredOwners().forEach {
                unregisterOwner(it)
            }
            // Remove all listeners as well, now we've unregistered all owners
            synchronized(registryListeners) { registryListeners.clear() }
        }
    }

    private fun onComposeViewCreated(owner: ViewRootForTest) {
        owner.view.addOnAttachStateChangeListener(OwnerAttachedListener(owner))
    }

    /**
     * Returns a copy of the set of all registered [ViewRootForTest]s, including ones that are
     * normally not relevant (like those whose lifecycle state is not RESUMED).
     */
    fun getUnfilteredOwners(): Set<ViewRootForTest> {
        return synchronized(owners) { owners.toSet() }
    }

    /**
     * Returns a copy of the set of all registered [ViewRootForTest]s that can be interacted with.
     * This method is almost always preferred over [getUnfilteredOwners].
     */
    fun getOwners(): Set<ViewRootForTest> {
        return synchronized(owners) {
            owners.filterTo(mutableSetOf()) {
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

    private fun dispatchOnRegistrationChanged(owner: ViewRootForTest, isRegistered: Boolean) {
        synchronized(registryListeners) { registryListeners.toList() }.forEach {
            it.onRegistrationChanged(owner, isRegistered)
        }
    }

    /**
     * Registers the [owner] in this registry. Must be called from [View.onAttachedToWindow].
     */
    internal fun registerOwner(owner: ViewRootForTest) {
        synchronized(owners) {
            if (owners.add(owner)) {
                dispatchOnRegistrationChanged(owner, true)
            }
        }
    }

    /**
     * Unregisters the [owner] from this registry. Must be called from [View.onDetachedFromWindow].
     */
    internal fun unregisterOwner(owner: ViewRootForTest) {
        synchronized(owners) {
            if (owners.remove(owner)) {
                dispatchOnRegistrationChanged(owner, false)
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
        fun onRegistrationChanged(owner: ViewRootForTest, registered: Boolean)
    }

    private inner class OwnerAttachedListener(
        private val owner: ViewRootForTest
    ) : View.OnAttachStateChangeListener {

        // Note: owner.view === view, because the owner _is_ the view,
        // and this listener is only referenced from within the view.

        override fun onViewAttachedToWindow(view: View) {
            registerOwner(owner)
        }

        override fun onViewDetachedFromWindow(view: View) {
            unregisterOwner(owner)
        }
    }
}

private val AndroidOwnerRegistry.hasAndroidOwners: Boolean get() = getOwners().isNotEmpty()

private fun AndroidOwnerRegistry.ensureAndroidOwnerRegistryIsSetUp() {
    check(isSetUp) {
        "Test not setup properly. Use a ComposeTestRule in your test to be able to interact " +
            "with composables"
    }
}

internal fun AndroidOwnerRegistry.waitForAndroidOwners() {
    ensureAndroidOwnerRegistryIsSetUp()

    if (!hasAndroidOwners) {
        val latch = CountDownLatch(1)
        val listener = object : AndroidOwnerRegistry.OnRegistrationChangedListener {
            override fun onRegistrationChanged(owner: ViewRootForTest, registered: Boolean) {
                if (hasAndroidOwners) {
                    latch.countDown()
                }
            }
        }
        try {
            addOnRegistrationChangedListener(listener)
            if (!hasAndroidOwners) {
                latch.await(2, TimeUnit.SECONDS)
            }
        } finally {
            removeOnRegistrationChangedListener(listener)
        }
    }
}

@ExperimentalTesting
@OptIn(ExperimentalTime::class)
internal suspend fun AndroidOwnerRegistry.awaitAndroidOwners() {
    ensureAndroidOwnerRegistryIsSetUp()

    if (!hasAndroidOwners) {
        suspendCancellableCoroutine<Unit> { continuation ->
            // Make sure we only resume once
            val didResume = AtomicBoolean(false)
            fun resume(listener: AndroidOwnerRegistry.OnRegistrationChangedListener) {
                if (didResume.compareAndSet(false, true)) {
                    removeOnRegistrationChangedListener(listener)
                    continuation.resume(Unit)
                }
            }

            // Usually we resume if an ComposeViewTestMarker is registered while the listener is added
            val listener = object : AndroidOwnerRegistry.OnRegistrationChangedListener {
                override fun onRegistrationChanged(
                    owner: ViewRootForTest,
                    registered: Boolean
                ) {
                    if (hasAndroidOwners) {
                        resume(this)
                    }
                }
            }

            addOnRegistrationChangedListener(listener)
            continuation.invokeOnCancellation {
                removeOnRegistrationChangedListener(listener)
            }

            // Sometimes the ComposeViewTestMarker was registered before we added
            // the listener, in which case we missed our signal
            if (hasAndroidOwners) {
                resume(listener)
            }
        }
    }
}
