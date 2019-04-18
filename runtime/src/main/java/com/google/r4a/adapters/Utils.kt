package com.google.r4a.adapters

import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup

internal val View.metrics: DisplayMetrics get() = resources.displayMetrics

/**
 * This function will take in a string and pass back a valid resource identifier for
 * View.setTag(...). We should eventually move this to a resource id that's actually generated via
 * AAPT but doing that in this project is proving to be complicated, so for now I'm just doing this
 * as a stop-gap.
 */
internal fun tagKey(key: String): Int {
    return (3 shl 24) or key.hashCode()
}

interface ViewAdapter {
    val id: Int
    fun willInsert(view: View, parent: ViewGroup)
    fun didInsert(view: View, parent: ViewGroup)
    fun didUpdate(view: View, parent: ViewGroup)
}

class ComposeViewAdapter : ViewAdapter {
    override val id = 0
    val adapters = mutableListOf<ViewAdapter>()

    inline fun <T : ViewAdapter> get(id: Int, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        val existing = adapters.firstOrNull { it.id == id } as? T
        if (existing != null) return existing
        val next = factory()
        adapters.add(next)
        return next
    }

    override fun willInsert(view: View, parent: ViewGroup) {
        for (adapter in adapters) adapter.willInsert(view, parent)
    }

    override fun didInsert(view: View, parent: ViewGroup) {
        for (adapter in adapters) adapter.didInsert(view, parent)
    }

    override fun didUpdate(view: View, parent: ViewGroup) {
        for (adapter in adapters) adapter.didUpdate(view, parent)
    }
}

private val viewAdaptersKey = tagKey("ViewAdapter")

internal fun View.getViewAdapterIfExists(): ComposeViewAdapter? {
    return getTag(viewAdaptersKey) as? ComposeViewAdapter
}

fun View.getViewAdapter(): ComposeViewAdapter {
    var adapter = getTag(viewAdaptersKey) as? ComposeViewAdapter
    if (adapter == null) {
        adapter = ComposeViewAdapter()
        setTag(viewAdaptersKey, adapter)
    }
    return adapter
}

inline fun <T : ViewAdapter> View.getOrAddAdapter(id: Int, factory: () -> T): T {
    return getViewAdapter().get(id, factory)
}