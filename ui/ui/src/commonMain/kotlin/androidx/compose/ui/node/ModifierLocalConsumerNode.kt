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

package androidx.compose.ui.node

import androidx.compose.ui.modifier.ModifierLocal
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalReadScope

internal class ModifierLocalConsumerNode(
    wrapped: LayoutNodeWrapper,
    modifier: ModifierLocalConsumer
) : DelegatingLayoutNodeWrapper<ModifierLocalConsumer>(wrapped, modifier), ModifierLocalReadScope {

    override fun onModifierChanged() {
        super.onModifierChanged()
        if (isAttached) notifyConsumerOfChanges()
    }

    override fun attach() {
        super.attach()
        notifyConsumerOfChanges()
    }

    override val <T> ModifierLocal<T>.current: T
        get() = onModifierLocalRead(this)

    private fun notifyConsumerOfChanges() {
        layoutNode.requireOwner().snapshotObserver.observeReads(this, onReadValuesChanged) {
            modifier.onModifierLocalsUpdated(this)
        }
    }

    companion object {
        val onReadValuesChanged: (ModifierLocalConsumerNode) -> Unit = { node ->
            node.notifyConsumerOfChanges()
        }
    }
}