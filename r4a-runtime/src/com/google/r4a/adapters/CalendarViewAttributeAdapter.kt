@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.CalendarView
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("CalendarViewInputController")

private fun CalendarView.getController(): CalendarViewInputController {
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
    getController().onDateChange = onDateChange
}

@ConflictsWith("date")
@RequiresOneOf("onDateChange")
fun CalendarView.setControlledDate(date: Long) {
    getController().setValueIfNeeded(date)
}