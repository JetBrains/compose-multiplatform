@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.AutoCompleteTextView

private fun AutoCompleteTextView.getR4aAdapter(): ArrayAdapter<Any> {
    var adapter = adapter as? ArrayAdapter<Any>
    if (adapter == null) {
        adapter = ArrayAdapter<Any>()
        setAdapter(adapter)
    }
    return adapter
}

// TODO(lmr): we want versions of this that have type parameters, but the codegen right now doesn't handle this properly.

fun AutoCompleteTextView.setData(data: Collection<Any>) {
    getR4aAdapter().items = data.toMutableList()
}

fun AutoCompleteTextView.setComposeItem(composeItem: Function1<Any, Unit>) {
    getR4aAdapter().composable = composeItem
}