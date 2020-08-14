package androidx.ui.androidview.adapters

import android.widget.DatePicker

class DatePickerInputController(private val view: DatePicker) : DatePicker.OnDateChangedListener {
    private var lastSetYear = view.year
    private var lastSetMonth = view.month
    private var lastSetDay = view.dayOfMonth

    fun init() {
        view.init(lastSetYear, lastSetMonth, lastSetDay, this)
    }

    fun setYearIfNeeded(value: Int) {
        val current = view.year
        lastSetYear = value
        if (current != value) {
            view.updateDate(value, view.month, view.dayOfMonth)
        }
    }

    fun setMonthIfNeeded(value: Int) {
        val current = view.month
        lastSetMonth = value
        if (current != value) {
            view.updateDate(view.year, value, view.dayOfMonth)
        }
    }

    fun setDayIfNeeded(value: Int) {
        val current = view.month
        lastSetMonth = value
        if (current != value) {
            view.updateDate(view.year, view.month, value)
        }
    }

    fun setDateIfNeeded(year: Int, month: Int, day: Int) {
        val currentYear = view.year
        val currentMonth = view.month
        val currentDay = view.dayOfMonth
        lastSetYear = year
        lastSetMonth = month
        lastSetDay = day

        if (currentYear != year || currentMonth != month || currentDay != day) {
            view.updateDate(year, month, day)
        }
    }

    var onDateChange: Function3<Int, Int, Int, Unit>? = null

    override fun onDateChanged(view: DatePicker?, year: Int, month: Int, day: Int) {
        onDateChange?.invoke(year, month, day)
        afterChangeEvent(year, month, day)
    }

    private fun afterChangeEvent(nextYear: Int, nextMonth: Int, nextDay: Int) {
        var shouldUpdate = false
        if (lastSetYear != nextYear && lastSetYear != view.year) {
            shouldUpdate = true
        }
        if (lastSetMonth != nextMonth && lastSetMonth != view.month) {
            shouldUpdate = true
        }
        if (lastSetDay != nextDay && lastSetDay != view.dayOfMonth) {
            shouldUpdate = true
        }
        if (shouldUpdate) {
            setDateIfNeeded(nextYear, nextMonth, nextDay)
        }
    }
}
