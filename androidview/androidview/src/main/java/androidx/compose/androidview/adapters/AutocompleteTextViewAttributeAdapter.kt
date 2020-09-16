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

package androidx.compose.androidview.adapters

import android.widget.AutoCompleteTextView
import androidx.compose.runtime.Composable

private val AutoCompleteTextView.composeAdapter: ArrayAdapter<Any>
    get() {
        @Suppress("UNCHECKED_CAST")
        var adapter = adapter as? ArrayAdapter<Any>
        if (adapter == null) {
            adapter = ArrayAdapter<Any>()
            setAdapter(adapter)
        }
        return adapter
    }

// TODO(lmr): we want versions of this that have type parameters, but the codegen right now doesn't handle this properly.

fun AutoCompleteTextView.setData(data: Collection<Any>) {
    composeAdapter.items = data.toMutableList()
}

fun AutoCompleteTextView.setComposeItem(composeItem: @Composable (Any) -> Unit) {
    composeAdapter.composable = composeItem
}