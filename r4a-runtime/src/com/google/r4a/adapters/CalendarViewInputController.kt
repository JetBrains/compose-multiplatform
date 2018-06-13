package com.google.r4a.adapters

import android.widget.CalendarView
import java.util.*

class CalendarViewInputController(view: CalendarView) : InputController<CalendarView, Long>(view), CalendarView.OnDateChangeListener {
    override fun getValue(): Long = view.date

    override fun setValue(value: Long) {
        view.date = value
    }

    var onDateChange: Function1<Long, Unit>? = null

    override fun onSelectedDayChange(x: CalendarView?, year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, dayOfMonth)
        val date = cal.timeInMillis
        onDateChange?.invoke(date)
        afterChangeEvent(date)
    }
}
