/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.compose.ui.platform.simpleIdentityToString

/**
 * Describes the semantic information associated with the owning component
 *
 * The information provided in the configuration is used to to generate the
 * semantics tree.
 */
class SemanticsConfiguration :
    SemanticsPropertyReceiver,
    Iterable<Map.Entry<SemanticsPropertyKey<*>, Any?>> {

    private val props: MutableMap<SemanticsPropertyKey<*>, Any?> = mutableMapOf()

    /**
     * Retrieves the value for the given property, if one has been set.
     * If a value has not been set, throws [IllegalStateException]
     */
    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: SemanticsPropertyKey<T>): T {
        return props.getOrElse(key) {
            throw IllegalStateException("Key not present: $key - consider getOrElse or getOrNull")
        } as T
    }

    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrElse(key: SemanticsPropertyKey<T>, defaultValue: () -> T): T {
        return props.getOrElse(key, defaultValue) as T
    }

    // Unavoidable, guaranteed by [set]
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrElseNullable(key: SemanticsPropertyKey<T>, defaultValue: () -> T?): T? {
        return props.getOrElse(key, defaultValue) as T?
    }

    override fun iterator(): Iterator<Map.Entry<SemanticsPropertyKey<*>, Any?>> {
        return props.iterator()
    }

    override fun <T> set(key: SemanticsPropertyKey<T>, value: T) {
        props[key] = value
    }

    operator fun <T> contains(key: SemanticsPropertyKey<T>): Boolean {
        return props.containsKey(key)
    }

    /**
     * Whether the semantic information provided by the owning component and
     * all of its descendants should be treated as one logical entity.
     *
     * If set to true, the descendants of the owning component's
     * [SemanticsNode] will merge their semantic information into the
     * [SemanticsNode] representing the owning component.
     */
    var isMergingSemanticsOfDescendants: Boolean = false
    var isClearingSemantics: Boolean = false

    // CONFIGURATION COMBINATION LOGIC

    /**
     * Absorb the semantic information from a child SemanticsNode into this configuration.
     *
     * This merges the child's semantic configuration using the `merge()` method defined
     * on the key.  This is used when mergeDescendants is specified (for accessibility focusable
     * nodes).
     */
    @Suppress("UNCHECKED_CAST")
    internal fun mergeChild(child: SemanticsConfiguration) {
        for ((key, nextValue) in child.props) {
            val existingValue = props[key]
            val mergeResult = (key as SemanticsPropertyKey<Any?>).merge(existingValue, nextValue)
            if (mergeResult != null) {
                props[key] = mergeResult
            }
        }
    }

    /**
     * Absorb the semantic information from a peer modifier into this configuration.
     *
     * This is repeatedly called for each semantics {} modifier on one LayoutNode to collapse
     * them into one SemanticsConfiguration. If a key is already seen and the value is
     * AccessibilityAction, the resulting AccessibilityAction's label/action will be the
     * label/action of the outermost modifier with this key and nonnull label/action, or null if no
     * nonnull label/action is found. If the value is not AccessibilityAction, values with a key
     * already seen are ignored (the semantics value of the outermost modifier with a given
     * semantics key is the one used).
     */
    internal fun collapsePeer(peer: SemanticsConfiguration) {
        if (peer.isMergingSemanticsOfDescendants) {
            isMergingSemanticsOfDescendants = true
        }
        if (peer.isClearingSemantics) {
            isClearingSemantics = true
        }
        for ((key, nextValue) in peer.props) {
            if (!props.contains(key)) {
                props[key] = nextValue
            } else if (nextValue is AccessibilityAction<*>) {
                val value = props[key] as AccessibilityAction<*>
                props[key] = AccessibilityAction(
                    value.label ?: nextValue.label,
                    value.action ?: nextValue.action
                )
            }
        }
    }

    /** Returns an exact copy of this configuration. */
    fun copy(): SemanticsConfiguration {
        val copy = SemanticsConfiguration()
        copy.isMergingSemanticsOfDescendants = isMergingSemanticsOfDescendants
        copy.isClearingSemantics = isClearingSemantics
        copy.props.putAll(props)
        return copy
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticsConfiguration) return false

        if (props != other.props) return false
        if (isMergingSemanticsOfDescendants != other.isMergingSemanticsOfDescendants) return false
        if (isClearingSemantics != other.isClearingSemantics) return false

        return true
    }

    override fun hashCode(): Int {
        var result = props.hashCode()
        result = 31 * result + isMergingSemanticsOfDescendants.hashCode()
        result = 31 * result + isClearingSemantics.hashCode()
        return result
    }

    override fun toString(): String {
        val propsString = StringBuilder()
        var nextSeparator = ""

        if (isMergingSemanticsOfDescendants) {
            propsString.append(nextSeparator)
            propsString.append("mergeDescendants=true")
            nextSeparator = ", "
        }

        if (isClearingSemantics) {
            propsString.append(nextSeparator)
            propsString.append("isClearingSemantics=true")
            nextSeparator = ", "
        }

        for ((key, value) in props) {
            propsString.append(nextSeparator)
            propsString.append(key.name)
            propsString.append(" : ")
            propsString.append(value)
            nextSeparator = ", "
        }
        return "${simpleIdentityToString(this@SemanticsConfiguration, null)}{ $propsString }"
    }
}

fun <T> SemanticsConfiguration.getOrNull(key: SemanticsPropertyKey<T>): T? {
    return getOrElseNullable(key) { null }
}
