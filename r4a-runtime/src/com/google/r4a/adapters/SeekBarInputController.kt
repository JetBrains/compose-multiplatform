package com.google.r4a.adapters

import android.widget.SeekBar


class SeekBarInputController(view: SeekBar) : SeekBar.OnSeekBarChangeListener, InputController<SeekBar, Int>(view) {
    override fun getValue(): Int = view.progress

    override fun setValue(value: Int) {
        view.progress = value
    }

    // TODO(lmr): we could add a different event for onProgressChange that only fired after the user stopped moving it...

    var onProgressChange: Function1<Int, Unit>? = null
    var onStartTrackingTouch: Function0<Unit>? = null
    var onStopTrackingTouch: Function0<Unit>? = null

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        onProgressChange?.invoke(progress)
        afterChangeEvent(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        onStartTrackingTouch?.invoke()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        onStopTrackingTouch?.invoke()
    }
}
