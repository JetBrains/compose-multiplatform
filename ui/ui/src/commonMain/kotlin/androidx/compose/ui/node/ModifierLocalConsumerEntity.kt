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

internal class ModifierLocalConsumerEntity(
    var provider: ModifierLocalProviderEntity,
    val modifier: ModifierLocalConsumer
) : () -> Unit, OwnerScope, ModifierLocalReadScope {
    private val modifierLocalsRead = mutableVectorOf<ModifierLocal<*>>()
    var isAttached = false
        private set

    override val isValid: Boolean
        get() = isAttached

    fun attach() {
        isAttached = true
        notifyConsumerOfChanges()
    }

    /**
     * The attach has been done, but we want to notify changes after the tree is completely applied.
     */
    fun attachDelayed() {
        isAttached = true
        invalidateConsumer()
    }

    fun detach() {
        modifier.onModifierLocalsUpdated(DetachedModifierLocalReadScope)
        isAttached = false
    }

    override val <T> ModifierLocal<T>.current: T
        get() {
            modifierLocalsRead += this
            val provider = provider.findModifierLocalProvider(this)
            return if (provider == null) {
                defaultFactory()
            } else {
                // We need a cast because type information is erased.
                // When we check for equality of the key it implies that the types are equal too.
                @Suppress("UNCHECKED_CAST")
                provider.value as T
            }
        }

    fun invalidateConsumersOf(local: ModifierLocal<*>) {
        if (local in modifierLocalsRead) {
            // Trigger the value to be read again
            provider.layoutNode.owner?.registerOnEndApplyChangesListener(this)
        }
    }

    fun notifyConsumerOfChanges() {
        // If the node is not attached, we don't notify the consumers.
        // Ultimately when the node is attached, this function will be called again.
        if (!isAttached) return

        modifierLocalsRead.clear()
        val snapshotObserver = provider.layoutNode.requireOwner().snapshotObserver
        snapshotObserver.observeReads(this, onReadValuesChanged) {
            modifier.onModifierLocalsUpdated(this)
        }
    }

    /**
     * Called when the modifiers have changed and we don't know what may have happened, so
     * the consumer has to be re-run after the tree is configured.
     */
    fun invalidateConsumer() {
        provider.layoutNode.owner?.registerOnEndApplyChangesListener(this)
    }

    /**
     * The listener for [UiApplier.onEndChanges]. This is called when we need to trigger
     * the consumer to update its values.
     */
    override fun invoke() {
        notifyConsumerOfChanges()
    }

    companion object {
        val onReadValuesChanged: (ModifierLocalConsumerEntity) -> Unit = { node ->
            node.notifyConsumerOfChanges()
        }
        val DetachedModifierLocalReadScope = object : ModifierLocalReadScope {
            override val <T> ModifierLocal<T>.current: T
                get() = defaultFactory()
        }
    }
}
