package com.google.r4a.examples.explorerapp.common.adapters

import android.view.View

// TODO(lmr): this could be represented as LiveData<T> as well and be observed

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

    internal fun forceSet(value: Any) {
        this.value = value as T
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
interface RefForwarder<T>: Reffable<T> {
    override var ref: Ref<T>?
        get() = null
        set(value) {
            refToForward.proxyTo = value
        }

    val refToForward: Ref<T>
}

fun View.setRef(ref: Ref<*>) {
    // TODO(lmr): we have to do this right now to get refs working because the call doesn't resolve properly since it
    // has a generic parameter. Once we properly use CallBuilder to generate these calls, we should get rid of this.
    ref.forceSet(this)
}

/**
 * Get the underlying reference to the view
 */
//fun <T: View> T.setRef(ref: Ref<T>) {
    // Views by default will be "reffable" in that users can get a ref to the underlying view. Since this is
    // essentially where refs "bottom out", the exact time with which these refs are set may end up being important...

    // Naively, we can just do something pretty simple here like set the value directly in the setter like this. The
    // problem with this is that the exact time which the value gets set might be before something meaningful can be
    // done, meaning that the user will have to do something to schedule the right time. Additionally, the value doesn't
    // "unset", meaning it's possible the ref might not GC properly.Nevertheless, for simplicity that is the approach
    // I'm taking at the moment.
//    ref.value = this

    // we *could* do something a little bit smarter, like setting the value when the view is attached to the window, and
    // unsetting it when it is detached.
//    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
//        override fun onViewDetachedFromWindow(v: View?) {
//            ref.value = null
//        }
//
//        override fun onViewAttachedToWindow(v: View?) {
//            ref.value = this@setRef
//        }
//    })
//}