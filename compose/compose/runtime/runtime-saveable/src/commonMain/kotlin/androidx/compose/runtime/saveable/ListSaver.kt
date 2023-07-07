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
 * The [Saver] implementation which allows to represent your [Original] class as a list of
 * [Saveable] values.
 *
 * What types can be saved is defined by [SaveableStateRegistry], by default everything which can
 * be stored in the Bundle class can be saved.
 *
 * You can use it as a parameter for [rememberSaveable].
 *
 * @sample androidx.compose.runtime.saveable.samples.ListSaverSample
 */
fun <Original, Saveable> listSaver(
    save: SaverScope.(value: Original) -> List<Saveable>,
    restore: (list: List<Saveable>) -> Original?
): Saver<Original, Any> = @Suppress("UNCHECKED_CAST") Saver(
    save = {
        val list = save(it)
        for (index in list.indices) {
            val item = list[index]
            if (item != null) {
                require(canBeSaved(item))
            }
        }
        if (list.isNotEmpty()) ArrayList(list) else null
    },
    restore = restore as (Any) -> Original?
)
