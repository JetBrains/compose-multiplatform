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

import android.widget.NumberPicker
import androidx.ui.androidview.annotations.ConflictsWith
import androidx.ui.androidview.annotations.RequiresOneOf

private val key = tagKey("NumberPickerInputController")

private val NumberPicker.controller: NumberPickerInputController
    get() {
        var controller = getTag(key) as? NumberPickerInputController
        if (controller == null) {
            controller = NumberPickerInputController(this)
            setTag(key, controller)
            setOnValueChangedListener(controller)
        }
        return controller
    }

@ConflictsWith("onValueChangedListener")
@RequiresOneOf("controlledValue")
fun NumberPicker.setOnValueChange(onValueChange: (Int) -> Unit) {
    controller.onValueChange = onValueChange
}

@ConflictsWith("value")
@RequiresOneOf("onValueChange")
fun NumberPicker.setControlledValue(value: Int) {
    controller.setValueIfNeeded(value)
}