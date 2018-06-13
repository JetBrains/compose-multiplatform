@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.SeekBar
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

private val key = tagKey("SeekBarInputController")

private fun SeekBar.getController(): SeekBarInputController {
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
    getController().onProgressChange = onProgressChange
}

@ConflictsWith("onSeekBarChangeListener")
fun SeekBar.setOnStartTrackingTouch(onStartTrackingTouch: Function0<Unit>) {
    getController().onStartTrackingTouch = onStartTrackingTouch
}

@ConflictsWith("onSeekBarChangeListener")
fun SeekBar.setOnStopTrackingTouch(onStopTrackingTouch: Function0<Unit>) {
    getController().onStopTrackingTouch = onStopTrackingTouch
}

@RequiresOneOf("onProgressChange")
@ConflictsWith("progress")
fun SeekBar.setControlledProgress(progress: Int) {
    getController().setValueIfNeeded(progress)
}

//@RequiresApi(26)
//fun SeekBar.setRange(range: Pair<Int, Int>) {
//    val (min, max) = range
//    val currentMax = getMax()
//    if (min < currentMax) {
//        setMin(min)
//        setMax(max)
//    } else {
//        setMax(max)
//        setMin(min)
//    }
//}