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
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.tryPopulateReflectively

/**
 * A [Modifier.Element] which manages an instance of a particular [Modifier.Node] implementation. A
 * given [Modifier.Node] implementation can only be used when a [ModifierNodeElement] which creates
 * and updates that implementation is applied to a Layout.
 *
 * A [ModifierNodeElement] should be very lightweight, and do little more than hold the information
 * necessary to create and maintain an instance of the associated [Modifier.Node] type.
 *
 * @sample androidx.compose.ui.samples.ModifierNodeElementSample
 * @sample androidx.compose.ui.samples.SemanticsModifierNodeSample
 *
 * @see Modifier.Node
 * @see Modifier.Element
 */
@ExperimentalComposeUiApi
abstract class ModifierNodeElement<N : Modifier.Node> : Modifier.Element, InspectableValue {

    /**
     * If this property returns `true`, then nodes will be automatically invalidated after the
     * [update] callback completes (For example, if the returned Node is a [DrawModifierNode], its
     * [DrawModifierNode.invalidateDraw] function will be invoked automatically as part of
     * auto invalidation).
     *
     * This is enabled by default, and provides a convenient mechanism to schedule invalidation
     * and apply changes made to the modifier. You may choose to set this to `false` if your
     * modifier has auto-invalidatable properties that do not frequently require invalidation to
     * improve performance by skipping unnecessary invalidation. If `autoInvalidate` is set to
     * `false`, you must call the appropriate invalidate functions manually in [update] for the
     * new attributes to become visible.
     */
    open val autoInvalidate: Boolean
        get() = true

    private var _inspectorValues: InspectorInfo? = null
    private val inspectorValues: InspectorInfo
        get() = _inspectorValues ?: InspectorInfo()
            .apply {
                name = this@ModifierNodeElement::class.simpleName
                inspectableProperties()
            }
            .also { _inspectorValues = it }

    final override val nameFallback: String?
        get() = inspectorValues.name

    final override val valueOverride: Any?
        get() = inspectorValues.value

    final override val inspectableElements: Sequence<ValueElement>
        get() = inspectorValues.properties

    /**
     * This will be called the first time the modifier is applied to the Layout and it should
     * construct and return the corresponding [Modifier.Node] instance.
     */
    abstract fun create(): N

    /**
     * Called when a modifier is applied to a Layout whose inputs have changed from the previous
     * application. This function will have the current node instance passed in as a parameter, and
     * it is expected that the node will be brought up to date.
     */
    abstract fun update(node: N): N

    /**
     * Populates an [InspectorInfo] object with attributes to display in the layout inspector. This
     * is called by tooling to resolve the properties of this modifier. By convention, implementors
     * should set the [name][InspectorInfo.name] to the function name of the modifier.
     *
     * The default implementation will attempt to reflectively populate the inspector info with the
     * properties declared on the subclass. It will also set the [name][InspectorInfo.name] property
     * to the name of this instance's class by default (not the name of the modifier function).
     * Modifier property population depends on the kotlin-reflect library. If it is not in the
     * classpath at runtime, the default implementation of this function will populate the
     * properties with an error message.
     *
     * If you override this function and provide the properties you wish to display, you do not need
     * to call `super`. Doing so may result in duplicate properties appearing in the layout
     * inspector.
     */
    open fun InspectorInfo.inspectableProperties() {
        tryPopulateReflectively(this@ModifierNodeElement)
    }

    // Require hashCode() to be implemented. Using a data class is sufficient. Singletons and
    // modifiers with no parameters may implement this function by returning an arbitrary constant.
    abstract override fun hashCode(): Int

    // Require equals() to be implemented. Using a data class is sufficient. Singletons may
    // implement this function with referential equality (`this === other`). Modifiers with no
    // inputs may implement this function by checking the type of the other object.
    abstract override fun equals(other: Any?): Boolean
}