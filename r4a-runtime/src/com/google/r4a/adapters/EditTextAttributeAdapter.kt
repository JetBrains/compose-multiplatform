@file:Suppress("unused")

package com.google.r4a.adapters

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("EditTextInputController")

private fun EditText.getController(): EditTextInputController {
    var controller = getTag(key) as? EditTextInputController
    if (controller == null) {
        controller = EditTextInputController(this)
        setTag(key, controller)
        addTextChangedListener(controller)
    }
    return controller
}

@ConflictsWith("onTextChangedListener")
@RequiresOneOf("controlledText")
fun EditText.setOnTextChange(onTextChange: Function1<String, Unit>) {
    getController().onTextChangedString = onTextChange
}

@ConflictsWith("onTextChangedListener")
fun EditText.setOnTextChanged(onTextChanged: Function4<CharSequence?, Int, Int, Int, Unit>) {
    getController().onTextChangedCharSequence = onTextChanged
}

@ConflictsWith("onTextChangedListener")
fun EditText.setOnAfterTextChanged(onAfterTextChanged: Function1<Editable?, Unit>) {
    getController().onAfterTextChanged = onAfterTextChanged
}

@ConflictsWith("onTextChangedListener")
fun EditText.setOnBeforeTextChanged(onBeforeTextChanged: Function4<CharSequence?, Int, Int, Int, Unit>) {
    getController().onBeforeTextChanged = onBeforeTextChanged
}


@ConflictsWith("onTextChange", "onBeforeTextChanged", "onAfterTextChanged", "onTextChanged")
fun EditText.setOnTextChangedListener(onTextChangedListener: TextWatcher) {
    addTextChangedListener(onTextChangedListener)
}

@ConflictsWith("text")
@RequiresOneOf("onTextChange")
fun EditText.setControlledText(value: CharSequence) {
    getController().setValueIfNeeded(value.toString())
}
