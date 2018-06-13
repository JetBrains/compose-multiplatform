@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.AdapterView
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("AdapterViewInputController")

private fun AdapterView<*>.getController(): AdapterViewInputController {
    var listener = getTag(key) as? AdapterViewInputController
    if (listener == null) {
        listener = AdapterViewInputController(this)
        setTag(key, listener)
        onItemSelectedListener = listener
    }
    return listener
}

@RequiresOneOf("controlledSelectedIndex")
@ConflictsWith("onItemSelectedListener")
fun AdapterView<*>.setOnSelectedIndexChange(listener: (Int) -> Unit) {
    getController().onSelectedIndexChange = listener
}

@RequiresOneOf("onSelectedIndexChange")
@ConflictsWith("selection")
fun AdapterView<*>.setControlledSelectedIndex(selectedIndex: Int) {
    getController().setValueIfNeeded(selectedIndex)
}