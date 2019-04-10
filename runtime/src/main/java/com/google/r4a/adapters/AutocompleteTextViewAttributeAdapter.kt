@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.AutoCompleteTextView
import com.google.r4a.Composable

private val AutoCompleteTextView.composeAdapter: ArrayAdapter<Any>
    get() {
        @Suppress("UNCHECKED_CAST")
        var adapter = adapter as? ArrayAdapter<Any>
        if (adapter == null) {
            adapter = ArrayAdapter<Any>()
            setAdapter(adapter)
        }
        return adapter
    }

// TODO(lmr): we want versions of this that have type parameters, but the codegen right now doesn't handle this properly.

fun AutoCompleteTextView.setData(data: Collection<Any>) {
    composeAdapter.items = data.toMutableList()
}

fun AutoCompleteTextView.setComposeItem(composeItem: @Composable() (Any)->Unit) {
    composeAdapter.composable = composeItem
}