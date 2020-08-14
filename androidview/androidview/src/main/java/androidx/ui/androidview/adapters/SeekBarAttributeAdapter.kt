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

import android.widget.SeekBar
import androidx.ui.androidview.annotations.ConflictsWith
import androidx.ui.androidview.annotations.RequiresOneOf

private val key = tagKey("SeekBarInputController")

private val SeekBar.controller: SeekBarInputController
    get() {
        var controller = getTag(key) as? SeekBarInputController
        if (controller == null) {
            controller = SeekBarInputController(this)
            setTag(key, controller)
            setOnSeekBarChangeListener(controller)
        }
        return controller
    }

@RequiresOneOf("controlledProgress")
@ConflictsWith("onSeekBarChangeListener")
fun SeekBar.setOnProgressChange(onProgressChange: Function1<Int, Unit>) {
    controller.onProgressChange = onProgressChange
}

@ConflictsWith("onSeekBarChangeListener")
fun SeekBar.setOnStartTrackingTouch(onStartTrackingTouch: Function0<Unit>) {
    controller.onStartTrackingTouch = onStartTrackingTouch
}

@ConflictsWith("onSeekBarChangeListener")
fun SeekBar.setOnStopTrackingTouch(onStopTrackingTouch: Function0<Unit>) {
    controller.onStopTrackingTouch = onStopTrackingTouch
}

@RequiresOneOf("onProgressChange")
@ConflictsWith("progress")
fun SeekBar.setControlledProgress(progress: Int) {
    controller.setValueIfNeeded(progress)
}

// @RequiresApi(26)
// fun SeekBar.setRange(range: Pair<Int, Int>) {
//    val (min, max) = range
//    val currentMax = getMax()
//    if (min < currentMax) {
//        setMin(min)
//        setMax(max)
//    } else {
//        setMax(max)
//        setMin(min)
//    }
// }