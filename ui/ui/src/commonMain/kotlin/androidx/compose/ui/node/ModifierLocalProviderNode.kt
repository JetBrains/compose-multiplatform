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
import androidx.compose.ui.modifier.ModifierLocalProvider

internal class ModifierLocalProviderNode <T> (
    wrapped: LayoutNodeWrapper,
    modifier: ModifierLocalProvider<T>
) : DelegatingLayoutNodeWrapper<ModifierLocalProvider<T>>(wrapped, modifier), () -> Unit {
    override fun attach() {
        super.attach()

        // Invalidate children that read this ModifierLocal
        layoutNode.owner?.registerOnEndApplyChangesListener(this)
    }

    override fun detach() {
        // Notify anyone who has read from me that the value has changed
        wrapped.invalidateConsumersOf(modifier.key)
        super.detach()
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        layoutNode.owner?.registerOnEndApplyChangesListener(this)
    }

    override fun invalidateConsumersOf(local: ModifierLocal<*>) {
        // If this provides a value for local, we don't have to notify the sub-tree
        if (modifier.key != local) {
            super.invalidateConsumersOf(local)
        }
    }

    override fun findModifierLocalProvider(local: ModifierLocal<*>): ModifierLocalProviderNode<*>? {
        if (modifier.key == local) {
            return this
        }
        return super.findModifierLocalProvider(local)
    }

    /**
     * The listener for [UiApplier.onEndChanges].
     */
    override fun invoke() {
        if (isAttached) {
            wrapped.invalidateConsumersOf(modifier.key)
        }
    }
}