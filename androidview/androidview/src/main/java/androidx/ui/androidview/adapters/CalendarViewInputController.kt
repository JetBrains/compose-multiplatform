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

package androidx.ui.androidview.adapters

import android.widget.CalendarView
import java.util.Calendar

class CalendarViewInputController(
    view: CalendarView
) : InputController<CalendarView, Long>(view), CalendarView.OnDateChangeListener {
    override fun getValue(): Long = view.date

    override fun setValue(value: Long) {
        view.date = value
    }

    var onDateChange: Function1<Long, Unit>? = null

    override fun onSelectedDayChange(x: CalendarView, year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, dayOfMonth)
        val date = cal.timeInMillis
        // view.getDate() returns incorrect date even if it shows in UI correctly, so update date here manually
        // see bug : b/113224600
        x.date = date
        prepareForChange(date)
        onDateChange?.invoke(date)
    }
}
