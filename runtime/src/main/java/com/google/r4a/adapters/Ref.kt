package com.google.r4a.adapters

import android.view.View

/**
 * A Ref is essentially a "value-holder" class that can be used with R4A to get controlled access to the underlying
 * view instances that are constructed as a result of a compose() pass in R4A.
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
        internal set(value) {
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