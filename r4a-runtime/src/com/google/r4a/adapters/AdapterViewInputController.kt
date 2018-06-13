package com.google.r4a.adapters

import android.view.View
import android.widget.AdapterView

// TODO(lmr): should we use a type parameter here?
class AdapterViewInputController(view: AdapterView<*>) : AdapterView.OnItemSelectedListener,
    InputController<AdapterView<*>, Int>(view) {
    override fun getValue(): Int = view.selectedItemPosition

    override fun setValue(value: Int) {
        view.setSelection(value)
    }

    var onNothingSelected: Function0<Unit>? = null
    var onItemSelected: Function2<Int, Long, Unit>? = null

    var onSelectedIndexChange: Function1<Int, Unit>? = null

    override fun onItemSelected(parent: AdapterView<*>?, unused: View?, position: Int, id: Long) {
        onItemSelected?.invoke(position, id)
        onSelectedIndexChange?.invoke(position)

        afterChangeEvent(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        onNothingSelected?.invoke()
        onSelectedIndexChange?.invoke(-1)
    }
}