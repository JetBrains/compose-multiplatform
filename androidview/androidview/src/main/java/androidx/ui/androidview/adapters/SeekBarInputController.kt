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

import android.widget.SeekBar

class SeekBarInputController(
    view: SeekBar
) : SeekBar.OnSeekBarChangeListener, InputController<SeekBar, Int>(view) {
    override fun getValue(): Int = view.progress

    override fun setValue(value: Int) {
        view.progress = value
    }

    // TODO(lmr): we could add a different event for onProgressChange that only fired after the user stopped moving it...

    var onProgressChange: Function1<Int, Unit>? = null
    var onStartTrackingTouch: Function0<Unit>? = null
    var onStopTrackingTouch: Function0<Unit>? = null

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        prepareForChange(progress)
        onProgressChange?.invoke(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        onStartTrackingTouch?.invoke()
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        onStopTrackingTouch?.invoke()
    }
}
