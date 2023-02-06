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
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * A [Modifier.Element] which manages an instance of a particular [Modifier.Node] implementation. A
 * given [Modifier.Node] implementation can only be used when a [ModifierNodeElement] which creates
 * and updates that implementation is applied to a Layout.
 *
 * A [ModifierNodeElement] should be very lightweight, and do little more than hold the information
 * necessary to
 *
 * @see Modifier.Node
 * @see Modifier.Element
 * @see modifierElementOf
 */
@ExperimentalComposeUiApi
abstract class ModifierNodeElement<N : Modifier.Node>(
    /**
     * An object which holds all of the "inputs" or "parameters" that will be passed into the
     * [Modifier.Node]. Even though most inputs to the create/update of a [Modifier.Node] may
     * come from a captured reference, it is important to pass them into here explicitly as well so
     * that they will get included in equals() and hashCode() calculations. Having it as a single
     * object on the abstract class allows us to have anonymous objects implement
     * [ModifierNodeElement] while still having a reasonable equals implementation.
     */
    internal val params: Any? = null,
    internal val autoInvalidate: Boolean = true,
    /**
     * This lambda will construct a debug-only set of information for use with tooling.
     *
     * @see InspectorValueInfo
     */
    inspectorInfo: InspectorInfo.() -> Unit
) : Modifier.Element, InspectorValueInfo(inspectorInfo) {
    /**
     * This will be called the first time the modifier is applied to the Layout and it should
     * construct and return the corresponding [Modifier.Node] instance.
     */
    abstract fun create(): N

    /**
     * Called when a modifier is applied to a Layout whose [params] have changed from the previous
     * application. This lambda will have the current node instance passed in as a parameter, and
     * it is expected that the node will be brought up to date.
     */
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

/**
 * A helpful API for constructing a [ModifierNodeElement] corresponding to a particular
 * [Modifier.Node] implementation.
 *
 * @param key An object used to determine whether or not the created node should be updated or not.
 * @param create The initial creation of the node. This will be called the first time the modifier
 *  is applied to the Layout and it should construct the corresponding [Modifier.Node] instance,
 *  referencing any captured inputs necessary.
 * @param update Called when a modifier is applied to a Layout whose [key] have changed from the
 *  previous application. This lambda will have the current node instance passed in as a parameter,
 *  and it is expected that the node will be brought up to date.
 * @param definitions This lambda will construct a debug-only set of information for use with
 *  tooling.
 *
 * @sample androidx.compose.ui.samples.ModifierElementOfSample
 * @sample androidx.compose.ui.samples.LayoutModifierNodeSample
 * @sample androidx.compose.ui.samples.DrawModifierNodeSample
 * @sample androidx.compose.ui.samples.GlobalPositionAwareModifierNodeSample
 * @sample androidx.compose.ui.samples.LayoutAwareModifierNodeSample
 * @sample androidx.compose.ui.samples.PointerInputModifierNodeSample
 *
 * @see ModifierNodeElement
 * @see Modifier.Element
 */
// TODO(lmr): make sure this produces reasonable bytecode.
@Suppress("MissingNullability", "ModifierFactoryExtensionFunction")
@ExperimentalComposeUiApi
inline fun <reified T : Modifier.Node> modifierElementOf(
    key: Any?,
    crossinline create: () -> T,
    crossinline update: (T) -> Unit,
    crossinline definitions: InspectorInfo.() -> Unit
): Modifier = object : ModifierNodeElement<T>(key, true, debugInspectorInfo(definitions)) {
    override fun create(): T = create()
    override fun update(node: T): T = node.also(update)
}

/**
 * A helpful API for constructing a [ModifierNodeElement] corresponding to a particular
 * [Modifier.Node] implementation. This overload is expected to be used for parameter-less Modifier
 * factories. For Modifier factories with parameters, consider using the overload of this method
 * which accepts a "params" and "update" parameter.
 *
 * @param create The initial creation of the node. This will be called the first time the modifier
 *  is applied to the Layout and it should construct the corresponding [Modifier.Node] instance
 * @param definitions This lambda will construct a debug-only set of information for use with
 *  tooling.
 *
 * @sample androidx.compose.ui.samples.ModifierElementOfSample
 * @sample androidx.compose.ui.samples.SemanticsModifierNodeSample
 *
 * @see ModifierNodeElement
 * @see Modifier.Element
 */
@Suppress("MissingNullability", "ModifierFactoryExtensionFunction")
@ExperimentalComposeUiApi
inline fun <reified T : Modifier.Node> modifierElementOf(
    crossinline create: () -> T,
    crossinline definitions: InspectorInfo.() -> Unit
): Modifier = object : ModifierNodeElement<T>(null, true, debugInspectorInfo(definitions)) {
    override fun create(): T = create()
    override fun update(node: T): T = node
}