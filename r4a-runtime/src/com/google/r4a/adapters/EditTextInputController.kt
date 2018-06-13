package com.google.r4a.adapters

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

// Right now this class is needed in order to have EditText behave as a controlled input. The problem is that it
// requires materializing CharSequences as Strings in multiple places, which could have a negative performance
// impact for longer inputs
class EditTextInputController(view: EditText) : TextWatcher, InputController<EditText, String>(view) {
    override fun getValue() = view.text.toString()
    override fun setValue(value: String) = view.setTextKeepState(value)

    var onTextChangedString: Function1<String, Unit>? = null
    var onTextChangedCharSequence: Function4<CharSequence?, Int, Int, Int, Unit>? = null
    var onAfterTextChanged: Function1<Editable?, Unit>? = null
    var onBeforeTextChanged: Function4<CharSequence?, Int, Int, Int, Unit>? = null

    override fun afterTextChanged(s: Editable?) {
        onAfterTextChanged?.invoke(s)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        onBeforeTextChanged?.invoke(s, start, count, after)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val nextValue = s.toString()
        onTextChangedString?.invoke(nextValue)
        onTextChangedCharSequence?.invoke(s, start, before, count)
        afterChangeEvent(nextValue)
    }
}