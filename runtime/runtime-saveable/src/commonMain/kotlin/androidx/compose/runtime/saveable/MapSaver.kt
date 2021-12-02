/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.saveable

/**
 * The [Saver] implementation which allows to represent your class as a map of values which can
 * be saved individually.
 *
 * What types can be saved is defined by [SaveableStateRegistry], by default everything which can
 * be stored in the Bundle class can be saved.
 *
 * You can use it as a parameter for [rememberSaveable].
 *
 * @sample androidx.compose.runtime.saveable.samples.MapSaverSample
 */
fun <T> mapSaver(
    save: SaverScope.(value: T) -> Map<String, Any?>,
    restore: (Map<String, Any?>) -> T?
) = listSaver<T, Any?>(
    save = {
        mutableListOf<Any?>().apply {
            save(it).forEach { entry ->
                add(entry.key)
                add(entry.value)
            }
        }
    },
    restore = { list ->
        val map = mutableMapOf<String, Any?>()
        check(list.size.rem(2) == 0)
        var index = 0
        while (index < list.size) {
            val key = list[index] as String
            val value = list[index + 1]
            map[key] = value
            index += 2
        }
        restore(map)
    }
)
