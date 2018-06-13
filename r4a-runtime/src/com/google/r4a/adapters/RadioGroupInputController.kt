package com.google.r4a.adapters

import android.widget.RadioGroup

class RadioGroupInputController(view: RadioGroup): RadioGroup.OnCheckedChangeListener, InputController<RadioGroup, Int>(view) {
    override fun getValue() = view.checkedRadioButtonId

    override fun setValue(value: Int) {
        view.check(value)
    }

    var onCheckedIdChange: Function1<Int, Unit>? = null

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        onCheckedIdChange?.invoke(checkedId)
        afterChangeEvent(checkedId)
    }
}
