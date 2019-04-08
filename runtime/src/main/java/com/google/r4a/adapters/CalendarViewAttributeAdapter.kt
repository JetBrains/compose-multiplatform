@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.CalendarView
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("CalendarViewInputController")

private val CalendarView.controller: CalendarViewInputController
    get() {
        var controller = getTag(key) as? CalendarViewInputController
        if (controller == null) {
            controller = CalendarViewInputController(this)
            setTag(key, controller)
            setOnDateChangeListener(controller)
        }
        return controller
    }

@RequiresOneOf("controlledDate")
@ConflictsWith("onDateChangeListener")
fun CalendarView.setOnDateChange(onDateChange: (Long) -> Unit) {
    controller.onDateChange = onDateChange
}

@ConflictsWith("date")
@RequiresOneOf("onDateChange")
fun CalendarView.setControlledDate(date: Long) {
    controller.setValueIfNeeded(date)
}