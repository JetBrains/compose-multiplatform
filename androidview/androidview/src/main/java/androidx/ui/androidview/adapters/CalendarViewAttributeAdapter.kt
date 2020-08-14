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

import android.widget.CalendarView
import androidx.ui.androidview.annotations.ConflictsWith
import androidx.ui.androidview.annotations.RequiresOneOf

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