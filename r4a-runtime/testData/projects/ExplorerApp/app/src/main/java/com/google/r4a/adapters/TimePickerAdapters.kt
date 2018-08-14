package com.google.r4a.adapters

import android.widget.TimePicker

fun TimePicker.setOnTimeChanged(listener: (view: TimePicker?, hourOfDay: Int, minuteOfHour: Int) -> Unit) {
    this.setOnTimeChangedListener(object: TimePicker.OnTimeChangedListener {
        override fun onTimeChanged(view: TimePicker?, hourOfDay: Int, minuteOfHour: Int) {
            listener(view, hourOfDay, minuteOfHour)
        }
    })
}