@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.AdapterView
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("AdapterViewInputController")

private val AdapterView<*>.controller: AdapterViewInputController
    get() {
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
    controller.onSelectedIndexChange = listener
}

@RequiresOneOf("onSelectedIndexChange")
@ConflictsWith("selection")
fun AdapterView<*>.setControlledSelectedIndex(selectedIndex: Int) {
    controller.setValueIfNeeded(selectedIndex)
}