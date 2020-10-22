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

package androidx.compose.ui.test.android

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.AndroidOwner
import java.util.Collections
import java.util.WeakHashMap

/**
 * Registry where all [AndroidOwner]s should be registered while they are attached to the window.
 * This registry is used by the testing library to query the owners's state.
 */
internal object AndroidOwnerRegistry {
    private val owners = Collections.newSetFromMap(WeakHashMap<AndroidOwner, Boolean>())
    private val registryListeners = mutableSetOf<OnRegistrationChangedListener>()

    /**
     * Returns if the registry is setup to receive registrations from [AndroidOwner]s
     */
    val isSetUp: Boolean
        get() = AndroidOwner.onAndroidOwnerCreatedCallback == ::onAndroidOwnerCreated

    /**
     * Sets up this registry to be notified of any [AndroidOwner] created
     */
    internal fun setupRegistry() {
        AndroidOwner.onAndroidOwnerCreatedCallback = ::onAndroidOwnerCreated
    }

    /**
     * Cleans up the changes made by [setupRegistry]. Call this after your test has run.
     */
    internal fun tearDownRegistry() {
        AndroidOwner.onAndroidOwnerCreatedCallback = null
        synchronized(owners) {
            getUnfilteredOwners().forEach {
                unregisterOwner(it)
            }
            // Remove all listeners as well, now we've unregistered all owners
            synchronized(registryListeners) { registryListeners.clear() }
        }
    }

    private fun onAndroidOwnerCreated(owner: AndroidOwner) {
        owner.view.addOnAttachStateChangeListener(OwnerAttachedListener(owner))
    }

    /**
     * Returns a copy of the set of all registered [AndroidOwner]s, including ones that are
     * normally not relevant (like those whose lifecycle state is not RESUMED).
     */
    fun getUnfilteredOwners(): Set<AndroidOwner> {
        return synchronized(owners) { owners.toSet() }
    }

    /**
     * Returns a copy of the set of all registered [AndroidOwner]s that can be interacted with.
     * This method is almost always preferred over [getUnfilteredOwners].
     */
    fun getOwners(): Set<AndroidOwner> {
        return synchronized(owners) {
            owners.filterTo(mutableSetOf()) {
                it.viewTreeOwners?.lifecycleOwner
                    ?.lifecycle?.currentState == Lifecycle.State.RESUMED
            }
        }
    }

    /**
     * Adds the given [listener], to be notified when an [AndroidOwner] registers or unregisters.
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

    private fun dispatchOnRegistrationChanged(owner: AndroidOwner, isRegistered: Boolean) {
        synchronized(registryListeners) { registryListeners.toList() }.forEach {
            it.onRegistrationChanged(owner, isRegistered)
        }
    }

    /**
     * Registers the [owner] in this registry. Must be called from [View.onAttachedToWindow].
     */
    internal fun registerOwner(owner: AndroidOwner) {
        synchronized(owners) {
            if (owners.add(owner)) {
                dispatchOnRegistrationChanged(owner, true)
            }
        }
    }

    /**
     * Unregisters the [owner] from this registry. Must be called from [View.onDetachedFromWindow].
     */
    internal fun unregisterOwner(owner: AndroidOwner) {
        synchronized(owners) {
            if (owners.remove(owner)) {
                dispatchOnRegistrationChanged(owner, false)
            }
        }
    }

    /**
     * Interface to be implemented by components that want to be notified when an [AndroidOwner]
     * registers or unregisters at this registry.
     */
    interface OnRegistrationChangedListener {
        fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean)
    }

    private class OwnerAttachedListener(
        private val owner: AndroidOwner
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
