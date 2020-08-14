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

import android.view.View
import android.widget.AdapterView

// TODO(lmr): should we use a type parameter here?
class AdapterViewInputController(view: AdapterView<*>) : AdapterView.OnItemSelectedListener,
    InputController<AdapterView<*>, Int>(view) {
    override fun getValue(): Int = view.selectedItemPosition

    override fun setValue(value: Int) {
        view.setSelection(value)
    }

    var onNothingSelected: Function0<Unit>? = null
    var onItemSelected: Function2<Int, Long, Unit>? = null

    var onSelectedIndexChange: Function1<Int, Unit>? = null

    override fun onItemSelected(parent: AdapterView<*>?, unused: View?, position: Int, id: Long) {
        prepareForChange(position)
        onItemSelected?.invoke(position, id)
        onSelectedIndexChange?.invoke(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        onNothingSelected?.invoke()
        onSelectedIndexChange?.invoke(-1)
    }
}