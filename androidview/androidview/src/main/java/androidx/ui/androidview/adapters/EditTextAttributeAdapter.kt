/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package androidx.ui.androidview.adapters

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.ui.androidview.annotations.ConflictsWith
import androidx.ui.androidview.annotations.RequiresOneOf

private val key = tagKey("EditTextInputController")

private val EditText.controller: EditTextInputController
    get() {
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
    controller.onControlledTextChanged = onTextChange
}

@ConflictsWith("onTextChangedListener")
fun EditText.setOnTextChanged(onTextChanged: Function4<CharSequence?, Int, Int, Int, Unit>) {
    controller.onTextChangedCharSequence = onTextChanged
}

@ConflictsWith("onTextChangedListener")
fun EditText.setOnAfterTextChanged(onAfterTextChanged: Function1<Editable?, Unit>) {
    controller.onAfterTextChanged = onAfterTextChanged
}

@ConflictsWith("onTextChangedListener")
fun EditText.setOnBeforeTextChanged(
    onBeforeTextChanged: Function4<CharSequence?, Int, Int, Int, Unit>
) {
    controller.onBeforeTextChanged = onBeforeTextChanged
}

@ConflictsWith(
    "onTextChange",
    "onBeforeTextChanged",
    "onAfterTextChanged",
    "onTextChanged"
)
fun EditText.setOnTextChangedListener(onTextChangedListener: TextWatcher) {
    addTextChangedListener(onTextChangedListener)
}

@ConflictsWith("text")
@RequiresOneOf("onTextChange")
fun EditText.setControlledText(value: String) {
    controller.setValueIfNeeded(value)
}
