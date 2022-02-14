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

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.modifier.ModifierLocal
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalReadScope

internal class ModifierLocalConsumerNode(
    wrapped: LayoutNodeWrapper,
    modifier: ModifierLocalConsumer
) : DelegatingLayoutNodeWrapper<ModifierLocalConsumer>(wrapped, modifier), () -> Unit,
    ModifierLocalReadScope {
    private val modifierLocalsRead = mutableVectorOf<ModifierLocal<*>>()

    override fun onModifierChanged() {
        super.onModifierChanged()
        notifyConsumerOfChanges()
    }

    override fun attach() {
        super.attach()
        notifyConsumerOfChanges()
    }

    override val <T> ModifierLocal<T>.current: T
        get() {
            // Track that we read this ModifierLocal so that it can be invalidated later
            modifierLocalsRead += this
            val provider = wrappedBy?.findModifierLocalProvider(this)
            return if (provider == null) {
                defaultFactory()
            } else {
                // We need a cast because type information is erased.
                // When we check for equality of the key it implies that the types are equal too.
                @Suppress("UNCHECKED_CAST")
                provider.modifier.value as T
            }
        }

    override fun invalidateConsumersOf(local: ModifierLocal<*>) {
        if (local in modifierLocalsRead) {
            // Trigger the value to be read again
            layoutNode.owner?.registerOnEndApplyChangesListener(this)
        }
        super.invalidateConsumersOf(local)
    }

    fun notifyConsumerOfChanges() {
        // If the node is not attached, we don't notify the consumers.
        // Ultimately when the node is attached, this function will be called again.
        if (!isAttached) return

        modifierLocalsRead.clear()
        layoutNode.requireOwner().snapshotObserver.observeReads(this, onReadValuesChanged) {
            modifier.onModifierLocalsUpdated(this)
        }
    }

    /**
     * The listener for [UiApplier.onEndChanges].
     */
    override fun invoke() {
        notifyConsumerOfChanges()
    }

    companion object {
        val onReadValuesChanged: (ModifierLocalConsumerNode) -> Unit = { node ->
            node.notifyConsumerOfChanges()
        }
    }
}