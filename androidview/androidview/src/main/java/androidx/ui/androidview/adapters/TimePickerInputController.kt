package androidx.ui.androidview.adapters

import android.widget.TimePicker

@Suppress("DEPRECATION")
class TimePickerInputController(private val view: TimePicker) : TimePicker.OnTimeChangedListener {

    private var lastSetHour = view.currentHour
    private var lastSetMinute = view.currentMinute

    fun setHourIfNeeded(value: Int) {
        val current = view.currentHour
        lastSetHour = value
        if (current != value) {
            view.currentHour = value
        }
    }

    fun setMinuteIfNeeded(value: Int) {
        val current = view.currentMinute
        lastSetMinute = value
        if (current != value) {
            view.currentMinute = value
        }
    }

    var onTimeChange: Function2<Int, Int, Unit>? = null

    override fun onTimeChanged(view: TimePicker?, hourOfDay: Int, minute: Int) {
        onTimeChange?.invoke(hourOfDay, minute)
        afterChangeEvent(hourOfDay, minute)
    }

    private fun afterChangeEvent(nextHour: Int, nextMinute: Int) {
        if (lastSetHour != nextHour && lastSetHour != view.currentHour) {
            setHourIfNeeded(lastSetHour)
        }
        if (lastSetMinute != nextMinute && lastSetMinute != view.currentMinute) {
            setMinuteIfNeeded(lastSetMinute)
        }
    }
}
