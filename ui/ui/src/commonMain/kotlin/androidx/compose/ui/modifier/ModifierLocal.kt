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

package androidx.compose.ui.modifier

import androidx.compose.runtime.Stable

/**
 * [ModifierLocal]s provide a means of inter-modifier communication. When a modifier provides a
 * [ModifierLocal], it is available to modifiers on the right of the providing modifier for the
 * current layout node. The provided [ModifierLocal]s are also available to layout nodes that are
 * children of the current layout node.
 *
 * One must create a [ModifierLocal] instance, which can be referenced by consumers statically.
 * [ModifierLocal] instances themselves hold no data, and can be thought of as a type-safe
 * identifier for the data being passed to other modifiers to the right of the providing
 * modifier or down the tree. [ModifierLocal] factory functions take a single parameter: a
 * factory to create a default value in cases where a [ModifierLocal] is used without a Provider.
 * If this is a situation you would rather not handle, you can throw an error in this factory.
 *
 * To add a value that can be accessed by other modifiers, create an instance of a
 * [ProvidableModifierLocal] and add it to the tree by using a [modifierLocalProvider]. Now other
 * modifiers can access the provided value by using a [modifierLocalConsumer].
 *
 * @see ProvidableModifierLocal
 * @see modifierLocalOf
 * @see modifierLocalProvider
 * @see modifierLocalConsumer
 */
@Stable
sealed class ModifierLocal<T> constructor(internal val defaultFactory: () -> T)

/**
 * [ProvidableModifierLocal]s are [ModifierLocal]s that can be used to provide values using a
 * [ModifierLocalProvider].
 *
 * When you create an instance of a [ProvidableModifierLocal], and want to prevent users of
 * your library from providing new values but want to allow the values to be consumed, expose a
 * [ModifierLocal] instead.
 *
 * @see ModifierLocal
 * @see modifierLocalOf
 * @see modifierLocalProvider
 * @see modifierLocalConsumer
 */
@Stable
class ProvidableModifierLocal<T>(defaultFactory: () -> T) : ModifierLocal<T>(defaultFactory)

/**
 * Creates a [ProvidableModifierLocal] and specifies a default factory.
 *
 * @param defaultFactory a factory to create a default value in cases where a [ModifierLocal] is
 * consumed without a Provider. If this is a situation you would rather not handle, you can throw
 * an error in this factory.
 *
 * Here are examples where a modifier can communicate with another in the same modifier chain:
 *
 * Sample 1: Modifier sending a message to another to its right.
 *
 * @sample androidx.compose.ui.samples.ModifierLocalParentChildCommunicationWithinLayoutNodeSample
 *
 * Sample 2: Modifier sending a message to another to its left.
 *
 * @sample androidx.compose.ui.samples.ModifierLocalChildParentCommunicationWithinLayoutNodeSample
 *
 * Here are examples where a modifier can communicate with another across layout nodes:
 *
 * Sample 1: Modifier sending a message to a modifier on a child layout node.
 *
 * @sample androidx.compose.ui.samples.ModifierLocalParentChildCommunicationInterLayoutNodeSample
 *
 * Sample 2: Modifier sending a message to a modifier on a parent layout node.
 *
 * @sample androidx.compose.ui.samples.ModifierLocalChildParentCommunicationInterLayoutNodeSample
 *
 * @see ProvidableModifierLocal
 * @see ModifierLocal
 * @see modifierLocalProvider
 * @see modifierLocalConsumer
 */
fun <T> modifierLocalOf(defaultFactory: () -> T): ProvidableModifierLocal<T> =
    ProvidableModifierLocal(defaultFactory)

/**
 * This scope gives us access to modifier locals that are provided by other modifiers to the left
 * of this modifier, or above this modifier in the layout tree.
 *
 * @see modifierLocalOf
 */
interface ModifierLocalReadScope {
    /**
     * Read a [ModifierLocal] that was provided by other modifiers to the left of this modifier,
     * or above this modifier in the layout tree.
     */
    val <T> ModifierLocal<T>.current: T
}