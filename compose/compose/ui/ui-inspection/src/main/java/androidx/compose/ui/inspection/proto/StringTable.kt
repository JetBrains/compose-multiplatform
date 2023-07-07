/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.compose.ui.inspection.proto

import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol

/**
 * A class which associates Strings with integers, where duplicate strings all share the same
 * numeric value.
 *
 * This class exists to allow us to significantly shrink payloads that get sent to the host, as lots
 * of text is across the layout tree will be the same.
 */
class StringTable {
    private val innerMap = mutableMapOf<String, Int>()

    fun put(str: String): Int {
        return innerMap.computeIfAbsent(str) { innerMap.size + 1 }
    }

    fun toStringEntries(): List<LayoutInspectorComposeProtocol.StringEntry> {
        return innerMap.entries.map { entry ->
            LayoutInspectorComposeProtocol.StringEntry.newBuilder().apply {
                str = entry.key
                id = entry.value
            }.build()
        }
    }
}
