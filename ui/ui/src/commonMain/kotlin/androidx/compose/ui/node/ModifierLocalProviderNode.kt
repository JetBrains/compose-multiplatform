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
) : DelegatingLayoutNodeWrapper<ModifierLocalProvider<T>>(wrapped, modifier) {

    override fun <V> onModifierLocalRead(modifierLocal: ModifierLocal<V>): V {
        return if (modifier.key == modifierLocal) {
            // We need a cast because type information is erased.
            // When we check for equality of the key it implies that the types are equal too.
            @Suppress("UNCHECKED_CAST")
            modifier.value as V
        } else {
            super.onModifierLocalRead(modifierLocal)
        }
    }
}