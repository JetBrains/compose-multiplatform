package com.google.r4a.adapters

import android.widget.NumberPicker

class NumberPickerInputController(view: NumberPicker) : InputController<NumberPicker, Int>(view), NumberPicker.OnValueChangeListener {

    var onValueChange: Function1<Int, Unit>? = null

    override fun getValue(): Int = view.value

    override fun setValue(value: Int) {
        view.value = value
    }

    override fun onValueChange(view: NumberPicker?, oldVal: Int, newVal: Int) {
        onValueChange?.invoke(newVal)
        afterChangeEvent(newVal)
    }
}