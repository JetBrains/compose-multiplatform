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

import android.widget.TimePicker
import androidx.ui.androidview.annotations.ConflictsWith
import androidx.ui.androidview.annotations.RequiresOneOf

private val key = tagKey("TimePickerInputController")

private val TimePicker.controller: TimePickerInputController
    get() {
        var controller = getTag(key) as? TimePickerInputController
        if (controller == null) {
            controller = TimePickerInputController(this)
            setTag(key, controller)
            setOnTimeChangedListener(controller)
        }
        return controller
    }

@ConflictsWith("onTimeChangedListener")
@RequiresOneOf("hour")
@RequiresOneOf("minute")
fun TimePicker.setOnTimeChange(onTimeChange: (Int, Int) -> Unit) {
    controller.onTimeChange = onTimeChange
}

@RequiresOneOf("onTimeChange")
@RequiresOneOf("controlledMinute")
@ConflictsWith("currentHour")
fun TimePicker.setControlledHour(hour: Int) {
    controller.setHourIfNeeded(hour)
}

@RequiresOneOf("controlledHour")
@RequiresOneOf("onTimeChange")
@ConflictsWith("currentMinute")
fun TimePicker.setControlledMinute(minute: Int) {
    controller.setMinuteIfNeeded(minute)
}