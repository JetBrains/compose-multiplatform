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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.ExperimentalComposeApi

/**
 * Take a [MutableSnapshot] and run [block] within it. When [block] returns successfully,
 * attempt to [MutableSnapshot.apply] the snapshot. Returns the result of [block] or throws
 * [SnapshotApplyConflictException] if snapshot changes attempted by [block] could not be applied.
 *
 * Prior to returning, any changes made to snapshot state (e.g. state holders returned by
 * [androidx.compose.runtime.mutableStateOf] are not visible to other threads. When
 * [withMutableSnapshot] returns successfully those changes will be made visible to other threads
 * and any snapshot observers (e.g. [snapshotFlow]) will be notified of changes.
 *
 * [block] must not suspend if [withMutableSnapshot] is called from a suspend function.
 */
// TODO: determine a good way to prevent/discourage suspending in an inlined [block]
@ExperimentalComposeApi
@Deprecated(
    "Use Snapshot.withMutableSnapshot() instead",
    ReplaceWith(
        "Snapshot.withMutableSnapshot(block)"
    )
)
inline fun <R> withMutableSnapshot(
    block: () -> R
): R = Snapshot.takeMutableSnapshot().run {
    try {
        enter(block).also { apply().check() }
    } catch (t: Throwable) {
        dispose()
        throw t
    }
}