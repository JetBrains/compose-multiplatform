/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.node

import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach

/**
 * @suppress
 */
// TODO(b/150806128): We should decide if we want to make this public API or not. Right now it is needed
//  for convenient LayoutParams usage in compose with views.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ViewAdapter {
    val id: Int
    fun willInsert(view: View, parent: ViewGroup)
    fun didInsert(view: View, parent: ViewGroup)
    fun didUpdate(view: View, parent: ViewGroup)
}

/**
 * @suppress
 */
// TODO(b/150806128): We should decide if we want to make this public API or not. Right now it is needed
//  for convenient LayoutParams usage in compose with views.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T : ViewAdapter> View.getOrAddAdapter(id: Int, factory: () -> T): T {
    return getViewAdapter().get(id, factory)
}

internal class MergedViewAdapter : ViewAdapter {
    override val id = 0
    val adapters = mutableListOf<ViewAdapter>()

    inline fun <T : ViewAdapter> get(id: Int, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        val existing = adapters.fastFirstOrNull { it.id == id } as? T
        if (existing != null) return existing
        val next = factory()
        adapters.add(next)
        return next
    }

    override fun willInsert(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.willInsert(view, parent) }
    }

    override fun didInsert(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.didInsert(view, parent) }
    }

    override fun didUpdate(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.didUpdate(view, parent) }
    }
}

/**
 * This function will take in a string and pass back a valid resource identifier for
 * View.setTag(...). We should eventually move this to a resource id that's actually generated via
 * AAPT but doing that in this project is proving to be complicated, so for now I'm just doing this
 * as a stop-gap.
 */
internal fun tagKey(key: String): Int {
    return (3 shl 24) or key.hashCode()
}

private val viewAdaptersKey = tagKey("ViewAdapter")

internal fun View.getViewAdapterIfExists(): MergedViewAdapter? {
    return getTag(viewAdaptersKey) as? MergedViewAdapter
}

internal fun View.getViewAdapter(): MergedViewAdapter {
    var adapter = getTag(viewAdaptersKey) as? MergedViewAdapter
    if (adapter == null) {
        adapter = MergedViewAdapter()
        setTag(viewAdaptersKey, adapter)
    }
    return adapter
}