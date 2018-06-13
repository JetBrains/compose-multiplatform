package com.google.r4a.adapters

import android.widget.CompoundButton


class CompoundButtonInputController(view: CompoundButton) : CompoundButton.OnCheckedChangeListener,
    InputController<CompoundButton, Boolean>(view) {

    override fun setValue(value: Boolean) {
        view.isChecked = value
    }

    override fun getValue() = view.isChecked

    var onCheckedChange: Function1<Boolean, Unit>? = null

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        onCheckedChange?.invoke(isChecked)
        afterChangeEvent(isChecked)
    }
}
