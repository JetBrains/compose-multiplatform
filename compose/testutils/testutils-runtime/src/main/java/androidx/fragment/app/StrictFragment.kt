/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.fragment.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import androidx.annotation.LayoutRes

/**
 * This fragment watches its primary lifecycle events and throws IllegalStateException
 * if any of them are called out of order or from a bad/unexpected state.
 */
open class StrictFragment(@LayoutRes contentLayoutId: Int = 0) : Fragment(contentLayoutId) {
    var currentState: State = State.DETACHED

    var calledOnAttach: Boolean = false
    var calledOnCreate: Boolean = false
    var calledOnActivityCreated: Boolean = false
    var calledOnStart: Boolean = false
    var calledOnResume: Boolean = false
    var calledOnSaveInstanceState: Boolean = false
    var calledOnPause: Boolean = false
    var calledOnStop: Boolean = false
    var calledOnDestroy: Boolean = false
    var calledOnDetach: Boolean = false
    var calledOnAttachFragment: Boolean = false
    var lastSavedInstanceState: Bundle? = null

    open fun onStateChanged(fromState: State) {
        checkGetActivity()
    }

    fun checkGetActivity() {
        checkNotNull(activity) {
            "getActivity() returned null at unexpected time"
        }
    }

    fun checkActivityNotDestroyed() {
        if (Build.VERSION.SDK_INT >= 17) {
            check(!requireActivity().isDestroyed)
        }
    }

    fun checkState(caller: String, vararg expected: State) {
        if (expected.isEmpty()) {
            throw IllegalArgumentException("must supply at least one expected state")
        }
        for (expect in expected) {
            if (currentState == expect) {
                return
            }
        }
        val expectString = StringBuilder(expected[0].toString())
        for (i in 1 until expected.size) {
            expectString.append(" or ").append(expected[i])
        }
        throw IllegalStateException(
            "$caller called while fragment was $currentState; " +
                "expected $expectString"
        )
    }

    fun checkStateAtLeast(caller: String, minState: State) {
        if (currentState < minState) {
            throw IllegalStateException(
                "$caller called while fragment was $currentState; " +
                    "expected at least $minState"
            )
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        calledOnAttachFragment = true
    }

    override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)
        checkActivityNotDestroyed()
        checkState("onInflate", State.DETACHED)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        checkActivityNotDestroyed()
        calledOnAttach = true
        checkState("onAttach", State.DETACHED)
        currentState = State.ATTACHED
        onStateChanged(State.DETACHED)
        @Suppress("Deprecation") // We're not setting retainInstance, just supporting it
        if (retainInstance && calledOnCreate) {
            // We were created previously
            currentState = State.CREATED
            onStateChanged(State.ATTACHED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkActivityNotDestroyed()
        if (calledOnCreate && !calledOnDestroy) {
            throw IllegalStateException("onCreate called more than once with no onDestroy")
        }
        calledOnCreate = true
        lastSavedInstanceState = savedInstanceState
        checkState("onCreate", State.ATTACHED)
        currentState = State.CREATED
        onStateChanged(State.ATTACHED)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("Deprecation") // we're just calling the superclass method with the same name
        super.onActivityCreated(savedInstanceState)
        checkActivityNotDestroyed()
        calledOnActivityCreated = true
        checkState("onActivityCreated", State.ATTACHED, State.CREATED, State.VIEW_CREATED)
        val fromState = currentState
        currentState = State.ACTIVITY_CREATED
        onStateChanged(fromState)
    }

    override fun onStart() {
        super.onStart()
        checkActivityNotDestroyed()
        calledOnStart = true
        checkState("onStart", State.CREATED, State.ACTIVITY_CREATED)
        currentState = State.STARTED
        onStateChanged(State.ACTIVITY_CREATED)
    }

    override fun onResume() {
        super.onResume()
        checkActivityNotDestroyed()
        calledOnResume = true
        checkState("onResume", State.STARTED)
        currentState = State.RESUMED
        onStateChanged(State.STARTED)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        calledOnSaveInstanceState = true
        checkGetActivity()
        // FIXME: We should not allow onSaveInstanceState except when STARTED or greater.
        // But FragmentManager currently does it in saveAllState for fragments on the
        // back stack, so fragments may be in the CREATED state.
        checkStateAtLeast("onSaveInstanceState", State.CREATED)
    }

    override fun onPause() {
        super.onPause()
        calledOnPause = true
        checkState("onPause", State.RESUMED)
        currentState = State.STARTED
        onStateChanged(State.RESUMED)
    }

    override fun onStop() {
        super.onStop()
        calledOnStop = true
        checkState("onStop", State.STARTED)
        currentState = State.CREATED
        onStateChanged(State.STARTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        calledOnDestroy = true
        checkState("onDestroy", State.CREATED)
        currentState = State.ATTACHED
        onStateChanged(State.CREATED)
    }

    override fun onDetach() {
        super.onDetach()
        calledOnDetach = true
        checkState("onDestroy", State.CREATED, State.ATTACHED)
        val fromState = currentState
        currentState = State.DETACHED
        onStateChanged(fromState)
    }

    enum class State {
        DETACHED,
        ATTACHED,
        CREATED,
        VIEW_CREATED,
        ACTIVITY_CREATED,
        STARTED,
        RESUMED
    }
}
