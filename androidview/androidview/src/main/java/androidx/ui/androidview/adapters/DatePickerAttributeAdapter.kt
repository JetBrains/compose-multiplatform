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

import android.widget.DatePicker
import androidx.ui.androidview.annotations.RequiresOneOf

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