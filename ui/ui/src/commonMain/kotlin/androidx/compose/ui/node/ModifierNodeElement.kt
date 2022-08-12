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
import androidx.compose.ui.areObjectsOfSameType

@ExperimentalComposeUiApi
abstract class ModifierNodeElement<N : Modifier.Node>(
    // it is important to have this `params` here so that they will get included
    // in equals() and hashCode() calculations. Having it as a single object of a
    // generic object allows us to have anonymous objects implement ManagedModifier
    // while still having a reasonable equals implementation.
    internal val params: Any? = null
) : Modifier.Element {
    abstract fun create(): N
    abstract fun update(node: N): N

    override fun hashCode(): Int {
        return params.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModifierNodeElement<*>) return false
        if (!areObjectsOfSameType(this, other)) return false
        return params == other.params
    }
}

// TODO(lmr): make sure this produces reasonable bytecode.
@Suppress("MissingNullability", "ModifierFactoryExtensionFunction")
@ExperimentalComposeUiApi
inline fun <reified T : Modifier.Node> modifierElementOf(
    params: Any?,
    crossinline create: () -> T,
    crossinline update: (T) -> Unit
): Modifier = object : ModifierNodeElement<T>(params) {
    override fun create(): T = create()
    override fun update(node: T): T = node.also(update)
}

@Suppress("MissingNullability", "ModifierFactoryExtensionFunction")
@ExperimentalComposeUiApi
inline fun <reified T : Modifier.Node> modifierElementOf(
    crossinline create: () -> T,
): Modifier = object : ModifierNodeElement<T>(null) {
    override fun create(): T = create()
    override fun update(node: T): T = node
}