@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.CompoundButton
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("CompoundButtonInputController")

private fun CompoundButton.getController(): CompoundButtonInputController {
    var controller = getTag(key) as? CompoundButtonInputController
    if (controller == null) {
        controller = CompoundButtonInputController(this)
        setTag(key, controller)
        setOnCheckedChangeListener(controller)
    }
    return controller
}

@RequiresOneOf("controlledChecked")
@ConflictsWith("onCheckedChangeListener")
fun CompoundButton.setOnCheckedChange(onCheckedChange: Function1<Boolean, Unit>) {
    getController().onCheckedChange = onCheckedChange
}

@RequiresOneOf("onCheckedChange")
@ConflictsWith("checked")
fun CompoundButton.setControlledChecked(checked: Boolean) {
    getController().setValueIfNeeded(checked)
}