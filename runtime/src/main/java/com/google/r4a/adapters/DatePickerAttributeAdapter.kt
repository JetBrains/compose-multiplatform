@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.DatePicker
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("DatePickerInputController")

private val DatePicker.controller: DatePickerInputController
    get() {
        var controller = getTag(key) as? DatePickerInputController
        if (controller == null) {
            controller = DatePickerInputController(this)
            setTag(key, controller)
            controller.init()
        }
        return controller
    }

@RequiresOneOf("controlledYear")
@RequiresOneOf("controlledMonth")
@RequiresOneOf("controlledDay")
fun DatePicker.setOnDateChange(onDateChange: (Int, Int, Int) -> Unit) {
    controller.onDateChange = onDateChange
}

@RequiresOneOf("onDateChange")
@RequiresOneOf("controlledYear")
@RequiresOneOf("controlledMonth")
fun DatePicker.setControlledDay(day: Int) {
    controller.setDayIfNeeded(day)
}

@RequiresOneOf("onDateChange")
@RequiresOneOf("controlledYear")
@RequiresOneOf("controlledDay")
fun DatePicker.setControlledMonth(month: Int) {
    controller.setMonthIfNeeded(month)
}

@RequiresOneOf("onDateChange")
@RequiresOneOf("controlledMonth")
@RequiresOneOf("controlledDay")
fun DatePicker.setControlledYear(year: Int) {
    controller.setYearIfNeeded(year)
}