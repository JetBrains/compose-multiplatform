@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.TimePicker
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("TimePickerInputController")

private fun TimePicker.getController(): TimePickerInputController {
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
    getController().onTimeChange = onTimeChange
}

@RequiresOneOf("onTimeChange")
@RequiresOneOf("controlledMinute")
@ConflictsWith("currentHour")
fun TimePicker.setControlledHour(hour: Int) {
    getController().setHourIfNeeded(hour)
}

@RequiresOneOf("controlledHour")
@RequiresOneOf("onTimeChange")
@ConflictsWith("currentMinute")
fun TimePicker.setControlledMinute(minute: Int) {
    getController().setMinuteIfNeeded(minute)
}