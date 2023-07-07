/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.inspection.util

import java.util.IdentityHashMap

const val NO_ANCHOR_ID = 0

/**
 * A map of anchors with a unique id generator.
 */
class AnchorMap {
    private val anchorLookup = mutableMapOf<Int, Any>()
    private val idLookup = IdentityHashMap<Any, Int>()

    /**
     * Return a unique id for the specified [anchor] instance.
     */
    operator fun get(anchor: Any?): Int =
        anchor?.let { idLookup.getOrPut(it) { generateUniqueId(it) } } ?: NO_ANCHOR_ID

    /**
     * Return the anchor associated with a given unique anchor [id].
     */
    operator fun get(id: Int): Any? = anchorLookup[id]

    private fun generateUniqueId(anchor: Any): Int {
        var id = anchor.hashCode()
        while (id == NO_ANCHOR_ID || anchorLookup.containsKey(id)) {
            id++
        }
        anchorLookup[id] = anchor
        return id
    }
}
