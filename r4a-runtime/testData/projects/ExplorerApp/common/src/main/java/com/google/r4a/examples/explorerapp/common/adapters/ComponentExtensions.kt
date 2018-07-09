package com.google.r4a.examples.explorerapp.common.adapters

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import com.google.r4a.Component
import com.google.r4a.CompositionContext

/**
 * Eventually we'd like to have 1st-class support for live-data subscribing, but for now I've added a subscribe
 * method to the base component that allows us to recompose the component every time the data changes.
 *
 * TODO(lmr): Once we have start/stop lifecycle support, we will want to use that here. For now I'm using
 * CompositionContext.find(...) as a stop-gap solution.
 */
fun <T> Component.subscribe(data: LiveData<T>) {
    val component = this
    var observer: Observer<T>? = null
    observer = Observer {
        val context = CompositionContext.find(component)
        if (context == null) {
            data.removeObserver(observer!!)
        } else {
            context.recompose(component)
        }
    }
    data.observeForever(observer)
}