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

import android.widget.SearchView
import androidx.ui.androidview.annotations.ConflictsWith
import androidx.ui.androidview.annotations.RequiresOneOf

private val key = tagKey("SearchViewInputController")

private val SearchView.controller: SearchViewInputController
    get() {
        var controller = getTag(key) as? SearchViewInputController
        if (controller == null) {
            controller = SearchViewInputController(this)
            setTag(key, controller)
            setOnQueryTextListener(controller)
        }
        return controller
    }

@RequiresOneOf("controlledQuery")
@ConflictsWith("onQueryTextListener")
fun SearchView.setOnQueryChange(onQueryChange: (String) -> Unit) {
    controller.onQueryChange = onQueryChange
}

@ConflictsWith("onQueryTextListener")
fun SearchView.setOnSubmit(onSubmit: (String) -> Unit) {
    controller.onSubmit = onSubmit
}

@RequiresOneOf("onQueryChange")
fun SearchView.setControlledQuery(query: String) {
    controller.setValueIfNeeded(query)
}

fun SearchView.setMaxWidth(maxWidth: Dimension) = setMaxWidth(maxWidth.toIntPixels(metrics))