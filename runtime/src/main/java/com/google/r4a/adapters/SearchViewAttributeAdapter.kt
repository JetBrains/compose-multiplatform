@file:Suppress("unused")

package com.google.r4a.adapters

import android.widget.SearchView
import com.google.r4a.annotations.ConflictsWith
import com.google.r4a.annotations.RequiresOneOf

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