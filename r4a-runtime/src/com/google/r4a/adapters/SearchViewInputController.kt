package com.google.r4a.adapters

import android.widget.SearchView

class SearchViewInputController(view: SearchView) : InputController<SearchView, String>(view), SearchView.OnQueryTextListener {
    override fun getValue(): String = view.query.toString()

    override fun setValue(value: String) {
        view.setQuery(value, false)
    }

    var onQueryChange: Function1<String, Unit>? = null
    var onSubmit: Function1<String, Unit>? = null

    override fun onQueryTextChange(query: String?): Boolean {
        onQueryChange?.invoke(query ?: "")
        // TODO(lmr): we may only want to call this if onQueryChange isn't set, which seems like a reasonable
        // thing for people to do
        afterChangeEvent(query ?: "")
        return onQueryChange != null // NOTE(lmr): I'm not sure if this is the right thing to do here
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        onSubmit?.invoke(query ?: "")
        return onSubmit != null
    }
}
