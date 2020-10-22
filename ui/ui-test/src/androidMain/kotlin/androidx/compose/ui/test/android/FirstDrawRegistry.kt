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

import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.AndroidOwner
import java.util.Collections
import java.util.WeakHashMap

internal object FirstDrawRegistry {
    private val notYetDrawn = Collections.newSetFromMap(WeakHashMap<AndroidOwner, Boolean>())
    private var onDrawnCallback: (() -> Unit)? = null

    private val listener = object : AndroidOwnerRegistry.OnRegistrationChangedListener {
        override fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean) {
            if (registered) {
                notYetDrawn.add(owner)
                owner.view.viewTreeObserver.addOnDrawListener(FirstDrawListener(owner))
            } else {
                notYetDrawn.remove(owner)
                dispatchOnDrawn()
            }
        }
    }

    /**
     * Sets up this registry to listen to the [AndroidOwnerRegistry]. Call this method before
     * [AndroidOwner]s are registered into that registry, which is before the test activity is
     * created, or simply right after the [AndroidOwnerRegistry] is setup.
     */
    internal fun setupRegistry() {
        AndroidOwnerRegistry.addOnRegistrationChangedListener(listener)
    }

    /**
     * Cleans up the changes made by [setupRegistry]. Call this after your test has run.
     */
    internal fun tearDownRegistry() {
        AndroidOwnerRegistry.removeOnRegistrationChangedListener(listener)
    }

    /**
     * Returns if all registered owners have finished at least one draw call.
     */
    fun haveAllDrawn(): Boolean {
        return notYetDrawn.all {
            val viewTreeOwners = it.viewTreeOwners
            viewTreeOwners == null ||
                viewTreeOwners.lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED
        }
    }

    /**
     * Adds a [callback] to be called when all registered [AndroidOwner]s have drawn at least
     * once. The callback will be removed after it is called.
     */
    fun setOnDrawnCallback(callback: (() -> Unit)?) {
        onDrawnCallback = callback
    }

    /**
     * Should be called when a registered owner has drawn for the first time. Can be called after
     * subsequent draws as well, but that is not required.
     */
    private fun notifyOwnerDrawn(owner: AndroidOwner) {
        notYetDrawn.remove(owner)
        dispatchOnDrawn()
    }

    private fun dispatchOnDrawn() {
        if (haveAllDrawn()) {
            onDrawnCallback?.invoke()
            onDrawnCallback = null
        }
    }

    private class FirstDrawListener(private val owner: AndroidOwner) :
        ViewTreeObserver.OnDrawListener {
        private var invoked = false

        override fun onDraw() {
            if (!invoked) {
                invoked = true
                owner.view.post {
                    // The view was drawn
                    notifyOwnerDrawn(owner)
                    val viewTreeObserver = owner.view.viewTreeObserver
                    if (viewTreeObserver.isAlive) {
                        viewTreeObserver.removeOnDrawListener(this)
                    }
                }
            }
        }
    }
}