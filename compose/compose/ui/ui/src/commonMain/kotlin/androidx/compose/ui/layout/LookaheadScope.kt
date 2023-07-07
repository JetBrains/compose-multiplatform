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

package androidx.compose.ui.layout

import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.node.LayoutNode

/**
 * [LookaheadScope] manages a disposable snapshot. Lookahead measure pass runs in this
 * snapshot, which gets disposed after lookahead such that lookahead pass does not result
 * in any state changes to the global snapshot.
 */
internal class LookaheadScope(val root: LayoutNode) {

    private var disposableSnapshot: MutableSnapshot? = null

    /**
     * This method runs the [block] in a snapshot that will be disposed. It is used
     * in the lookahead pass, where no state changes are intended to be applied to the global
     * snapshot.
     */
    fun <T> withDisposableSnapshot(block: () -> T): T {
        check(disposableSnapshot == null) {
            "Disposable snapshot is already active"
        }
        return Snapshot.takeMutableSnapshot().let {
            disposableSnapshot = it
            try {
                it.enter(block)
            } finally {
                it.dispose()
                disposableSnapshot = null
            }
        }
    }
}
