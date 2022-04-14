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
import androidx.compose.ui.modifier.ModifierLocalProvider

internal class ModifierLocalProviderEntity(
    val layoutNode: LayoutNode,
    val modifier: ModifierLocalProvider<*>
) : () -> Unit {
    /**
     * The next (wrapped) ModifierLocalProviderEntity on the LayoutNode. This forms the
     * linked list of providers.
     */
    var next: ModifierLocalProviderEntity? = null

    /**
     * The previous (wrapped by) ModifierLocalProviderEntity on the LayoutNode. This forms
     * the reverse direction linked list of providers.
     */
    var prev: ModifierLocalProviderEntity? = null

    /**
     * True if the provider is attached to the LayoutNode and the LayoutNode is attached to
     * the hierarchy.
     */
    var isAttached = false
        private set

    /**
     * A list of [ModifierLocalConsumerEntity]s that are after (wrapped by) this provider
     * and may read [modifier]'s value.
     */
    val consumers = mutableVectorOf<ModifierLocalConsumerEntity>()

    fun attach() {
        isAttached = true

        // Invalidate children that read this ModifierLocal
        invalidateConsumersOf(modifier.key, stopIfProvided = false)

        consumers.forEach { it.attach() }
    }

    /**
     * The attach has been done, but we want to notify changes after the tree is completely applied.
     */
    fun attachDelayed() {
        isAttached = true
        // Invalidate children that read this ModifierLocal
        layoutNode.owner?.registerOnEndApplyChangesListener(this)

        consumers.forEach { it.attachDelayed() }
    }

    fun detach() {
        isAttached = false
        consumers.forEach { it.detach() }

        // Notify anyone who has read from me that the value has changed
        invalidateConsumersOf(modifier.key, stopIfProvided = false)
    }

    /**
     * Invalidates consumers of [local]. If [stopIfProvided] is `true` and this provides the
     * a value for [local], then consumers are not invalidated.
     */
    private fun invalidateConsumersOf(local: ModifierLocal<*>, stopIfProvided: Boolean) {
        // If this provides a value for local, we don't have to notify the sub-tree
        if (stopIfProvided && modifier.key == local) {
            return
        }
        consumers.forEach { it.invalidateConsumersOf(local) }
        next?.invalidateConsumersOf(local, stopIfProvided = true)
            ?: layoutNode._children.forEach {
                it.modifierLocalsHead.invalidateConsumersOf(local, stopIfProvided = true)
            }
    }

    /**
     * Returns the [ModifierLocalProvider] that provides [local] or `null` if there isn't
     * a provider.
     */
    @Suppress("ModifierFactoryExtensionFunction", "ModifierFactoryReturnType")
    fun findModifierLocalProvider(local: ModifierLocal<*>): ModifierLocalProvider<*>? {
        if (modifier.key == local) {
            return modifier
        }
        return prev?.findModifierLocalProvider(local)
            ?: layoutNode.parent?.modifierLocalsTail?.findModifierLocalProvider(local)
    }

    /**
     * The listener for [UiApplier.onEndChanges]. This is called when we need to invalidate
     * all consumers of the modifier.
     */
    override fun invoke() {
        if (isAttached) {
            invalidateConsumersOf(modifier.key, stopIfProvided = false)
        }
    }
}