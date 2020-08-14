/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.androidview.adapters

import android.view.View

/**
 * A Ref is essentially a "value-holder" class that can be used with Compose to get controlled access to the underlying
 * view instances that are constructed as a result of a compose() pass in Compose.
 */
class Ref<T> {
    // TODO(lmr): One idea is that Ref<T> could implement LiveData<T>, which means people could observe when the value
    // changes. In this case, proxyTo essentially becomes a switchMap
    internal var proxyTo: Ref<T>? = null

    var value: T? = null
        get() {
            val proxyTo = proxyTo
            return if (proxyTo == null) field else proxyTo.value
        }
        set(value) { // TODO(popam): make internal when non-ir module is removed
            val proxyTo = proxyTo
            if (proxyTo == null) {
                field = value
            } else {
                proxyTo.value = value
            }
        }
}

/**
 * Components that want to expose a public "ref" API should use this interface. Views are reffables of themselves, but
 * components can provide a custom object as their public interface if they would like to provide an imperative API
 * for some purpose. This is not recommended unless absolutely necessary.
 */
interface Reffable<T> {
    var ref: Ref<T>?
}

/**
 * A common use case for refs is to have a component that wraps a view that you want to be a "drop-in" replacement
 * for the view it is wrapping, so the ref should be the type of the underlying view
 */
interface RefForwarder<T> : Reffable<T> {
    override var ref: Ref<T>?
        get() = null
        set(value) {
            refToForward.proxyTo = value
        }

    val refToForward: Ref<T>
}

private val refKey = tagKey("Ref")
internal var <T : View> T.storedRef: Ref<T>?
    get() {
        @Suppress("UNCHECKED_CAST")
        return getTag(refKey) as? Ref<T>
    }
    set(value) {
        setTag(refKey, value)
    }

fun <T : View> T.setRef(ref: Ref<T>) {
    storedRef?.let { it.value = null }
    storedRef = ref
    ref.value = this
}