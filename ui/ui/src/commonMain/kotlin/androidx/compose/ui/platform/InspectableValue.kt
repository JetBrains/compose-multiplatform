/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.Modifier

/**
 * An empty [InspectorInfo] DSL.
 */
val NoInspectorInfo: InspectorInfo.() -> Unit = {}

/**
 * Turn on inspector debug information. Used internally during inspection.
 */
var isDebugInspectorInfoEnabled = false

/**
 * A compose value that is inspectable by tools. It gives access to private parts of a value.
 */
interface InspectableValue {

    /**
     * The elements of a compose value.
     */
    val inspectableElements: Sequence<ValueElement>
        get() = emptySequence()

    /**
     * Use this name as the reference name shown in tools of this value if there is no explicit
     * reference name given to the value.
     * Example: a modifier in a modifier list.
     */
    val nameFallback: String?
        get() = null

    /**
     * Use this value as a readable representation of the value.
     */
    val valueOverride: Any?
        get() = null
}

/**
 * A [ValueElement] describes an element of a compose value instance.
 * The [name] typically refers to a (possibly private) property name with its corresponding [value].
 */
data class ValueElement(val name: String, val value: Any?)

/**
 * A builder for an [InspectableValue].
 */
class InspectorInfo {
    /**
     * Provides a [InspectableValue.nameFallback].
     */
    var name: String? = null

    /**
     * Provides a [InspectableValue.valueOverride].
     */
    var value: Any? = null

    /**
     * Provides a [InspectableValue.inspectableElements].
     */
    val properties = ValueElementSequence()
}

/**
 * A builder for a sequence of [ValueElement].
 */
class ValueElementSequence : Sequence<ValueElement> {
    private val elements = mutableListOf<ValueElement>()

    override fun iterator(): Iterator<ValueElement> = elements.iterator()

    /**
     * Specify a sub element with name and value.
     */
    operator fun set(name: String, value: Any?) {
        elements.add(ValueElement(name, value))
    }
}

/**
 * Implementation of [InspectableValue] based on a builder [InspectorInfo] DSL.
 */
abstract class InspectorValueInfo(private val info: InspectorInfo.() -> Unit) : InspectableValue {
    private var _values: InspectorInfo? = null

    private val values: InspectorInfo
        get() {
            val valueInfo = _values ?: InspectorInfo().apply(info)
            _values = valueInfo
            return valueInfo
        }

    override val nameFallback: String?
        get() = values.name

    override val valueOverride: Any?
        get() = values.value

    override val inspectableElements: Sequence<ValueElement>
        get() = values.properties
}

/**
 * Factory method for avoiding DSL allocation when no debug inspector info is needed.
 */
inline fun debugInspectorInfo(
    crossinline definitions: InspectorInfo.() -> Unit
): InspectorInfo.() -> Unit =
    @OptIn(InternalComposeApi::class)
    if (isDebugInspectorInfoEnabled) ({ definitions() }) else NoInspectorInfo

/**
 * Use this to group a common set of modifiers and provide [InspectorInfo] for the resulting
 * modifier.
 *
 * @sample androidx.compose.ui.samples.InspectableModifierSample
 */
inline fun Modifier.inspectable(
    noinline inspectorInfo: InspectorInfo.() -> Unit,
    wrapped: Modifier.() -> Modifier
): Modifier = inspectableWrapper(inspectorInfo, wrapped())

/**
 * Do not use this explicitly. Instead use [Modifier.inspectable].
 */
@PublishedApi
internal fun Modifier.inspectableWrapper(
    inspectorInfo: InspectorInfo.() -> Unit,
    wrapped: Modifier
): Modifier = this.then(InspectableModifierImpl(wrapped, inspectorInfo))

/**
 * Interface for a [Modifier] wrapped for inspector purposes.
 *
 * This gives tools access to the [wrapped] modifier in InspectableModifier.
 * It is expected that the implementation of this interface implements [Modifier.foldIn]
 * similar to [InspectableModifierImpl].
 */
interface InspectableModifier {
    /**
     * The actual modifier that [Modifier.inspectable] has wrapped with its own [InspectorInfo].
     */
    val wrapped: Modifier
}

private class InspectableModifierImpl(
    override val wrapped: Modifier,
    inspectorInfo: InspectorInfo.() -> Unit
) : Modifier.Element, InspectableModifier, InspectorValueInfo(inspectorInfo) {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        wrapped.foldIn(operation(initial, this), operation)

    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R =
        operation(this, wrapped.foldOut(initial, operation))

    override fun any(predicate: (Modifier.Element) -> Boolean): Boolean =
        wrapped.any(predicate)

    override fun all(predicate: (Modifier.Element) -> Boolean): Boolean =
        wrapped.all(predicate)

    override fun equals(other: Any?): Boolean =
        other is InspectableModifier && wrapped == other.wrapped

    override fun hashCode(): Int = wrapped.hashCode() + 31

    override fun toString() = "[" + foldIn("") { acc, element ->
        if (acc.isEmpty()) element.toString() else "$acc, $element"
    } + "]"
}
