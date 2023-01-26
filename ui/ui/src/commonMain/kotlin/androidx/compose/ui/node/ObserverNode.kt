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

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier

/**
 * [Modifier.Node]s that implement ObserverNode can provide their own implementation of
 * [onObservedReadsChanged] that will be called whenever the value of read object has changed.
 * To trigger [onObservedReadsChanged], read values within an [observeReads] block.
 */
@ExperimentalComposeUiApi
interface ObserverNode : DelegatableNode {

    /**
     * This callback is called when any values that are read within the [observeReads] block
     * changes.
     */
    fun onObservedReadsChanged()

    @ExperimentalComposeUiApi
    companion object {
        internal val OnObserveReadsChanged: (ObserverNode) -> Unit = {
            if (it.node.isAttached) it.onObservedReadsChanged()
        }
    }
}

/**
 * Use this function to observe reads within the specified [block].
 */
@ExperimentalComposeUiApi
fun <T> T.observeReads(block: () -> Unit) where T : Modifier.Node, T : ObserverNode {
    requireOwner().snapshotObserver.observeReads(
        target = this,
        onChanged = ObserverNode.OnObserveReadsChanged,
        block = block
    )
}
