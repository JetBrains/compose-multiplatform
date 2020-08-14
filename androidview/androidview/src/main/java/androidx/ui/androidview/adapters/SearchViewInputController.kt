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

import android.widget.SearchView

class SearchViewInputController(
    view: SearchView
) : InputController<SearchView, String>(view), SearchView.OnQueryTextListener {
    override fun getValue(): String = view.query.toString()

    override fun setValue(value: String) {
        view.setQuery(value, false)
    }

    var onQueryChange: Function1<String, Unit>? = null
    var onSubmit: Function1<String, Unit>? = null

    override fun onQueryTextChange(query: String?): Boolean {
        prepareForChange(query ?: "")
        onQueryChange?.invoke(query ?: "")
        // TODO(lmr): we may only want to call this if onQueryChange isn't set, which seems like a reasonable
        // thing for people to do
        // NOTE(lmr): I'm not sure if this is the right thing to do here
        return onQueryChange != null
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        onSubmit?.invoke(query ?: "")
        return onSubmit != null
    }
}
