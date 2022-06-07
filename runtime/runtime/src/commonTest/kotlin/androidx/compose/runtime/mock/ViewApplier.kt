/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime.mock

import androidx.compose.runtime.AbstractApplier

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
class ViewApplier(root: View) : AbstractApplier<View>(root) {
    var onBeginChangesCalled = 0
        private set

    var onEndChangesCalled = 0
        private set

    override fun insertTopDown(index: Int, instance: View) {
        // Ignored as the tree is built bottom-up.
    }

    override fun insertBottomUp(index: Int, instance: View) {
        current.addAt(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.removeAt(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.moveAt(from, to, count)
    }

    override fun onClear() {
        root.children.clear()
    }

    override fun onBeginChanges() {
        onBeginChangesCalled++
    }

    override fun onEndChanges() {
        onEndChangesCalled++
    }
}
