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

package androidx.compose.ui.modifier

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.visitAncestors

/**
 * An opaque key-value holder of [ModifierLocal]s to be used with [ModifierLocalNode].
 *
 * @see modifierLocalMapOf
 */
@ExperimentalComposeUiApi
sealed class ModifierLocalMap() {
    internal abstract operator fun <T> set(key: ModifierLocal<T>, value: T)
    internal abstract operator fun <T> get(key: ModifierLocal<T>): T?
    internal abstract operator fun contains(key: ModifierLocal<*>): Boolean
}

@OptIn(ExperimentalComposeUiApi::class)
internal class SingleLocalMap(
    private val key: ModifierLocal<*>
) : ModifierLocalMap() {
    private var value: Any? by mutableStateOf(null)
    internal fun forceValue(value: Any?) {
        this.value = value
    }

    override operator fun <T> set(key: ModifierLocal<T>, value: T) {
        check(key === this.key)
        this.value = value
    }

    override operator fun <T> get(key: ModifierLocal<T>): T? {
        check(key === this.key)
        @Suppress("UNCHECKED_CAST")
        return value as? T?
    }

    override operator fun contains(key: ModifierLocal<*>): Boolean = key === this.key
}

@OptIn(ExperimentalComposeUiApi::class)
internal class BackwardsCompatLocalMap(
    var element: ModifierLocalProvider<*>
) : ModifierLocalMap() {
    override operator fun <T> set(key: ModifierLocal<T>, value: T) {
        error("Set is not allowed on a backwards compat provider")
    }

    override operator fun <T> get(key: ModifierLocal<T>): T? {
        check(key === element.key)
        @Suppress("UNCHECKED_CAST")
        return element.value as T
    }

    override operator fun contains(key: ModifierLocal<*>): Boolean = key === element.key
}

@OptIn(ExperimentalComposeUiApi::class)
internal class MultiLocalMap(
    vararg entries: Pair<ModifierLocal<*>, Any?>
) : ModifierLocalMap() {
    private val map = mutableStateMapOf<ModifierLocal<*>, Any?>()

    init {
        map.putAll(entries.toMap())
    }

    override operator fun <T> set(key: ModifierLocal<T>, value: T) {
        map[key] = value
    }

    override operator fun <T> get(key: ModifierLocal<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return map[key] as? T?
    }

    override operator fun contains(key: ModifierLocal<*>): Boolean = map.containsKey(key)
}

@OptIn(ExperimentalComposeUiApi::class)
internal object EmptyMap : ModifierLocalMap() {
    override fun <T> set(key: ModifierLocal<T>, value: T) = error("")
    override fun <T> get(key: ModifierLocal<T>): T? = error("")
    override fun contains(key: ModifierLocal<*>): Boolean = false
}

/**
 * A [androidx.compose.ui.Modifier.Node] that is capable of consuming and providing [ModifierLocal]
 * values.
 *
 * This is the [androidx.compose.ui.Modifier.Node] equivalent of the [ModifierLocalConsumer]
 * and [ModifierLocalProvider] interfaces.
 *
 * @sample androidx.compose.ui.samples.JustReadingOrProvidingModifierLocalNodeSample
 *
 * @see modifierLocalOf
 * @see ModifierLocal
 * @see androidx.compose.runtime.CompositionLocal
 */
@ExperimentalComposeUiApi
interface ModifierLocalNode : ModifierLocalReadScope, DelegatableNode {
    /**
     * The map of provided ModifierLocal <-> value pairs that this node is providing. This value
     * must be overridden if you are going to provide any values. It should be overridden as a
     * field-backed property initialized with values for all of the keys that it will ever possibly
     * provide.
     *
     * By default, this property will be set to an empty map, which means that this node will only
     * consume [ModifierLocal]s and will not provide any new values.
     *
     * If you would like to change a value provided in the map over time, you must use the [provide]
     * API.
     *
     * @see modifierLocalMapOf
     * @see provide
     */
    val providedValues: ModifierLocalMap get() = EmptyMap

    /**
     * This method will cause this node to provide a new [value] for [key]. This can be called at
     * any time on the UI thread, but in order to use this API, [providedValues] must be
     * implemented and [key] must be a key that was included in it.
     *
     * By providing this new value, any [ModifierLocalNode] below it in the tree will read this
     * [value] when reading [current], until another [ModifierLocalNode] provides a value for the
     * same [key], however, consuming [ModifierLocalNode]s will NOT be notified that a new value
     * was provided.
     */
    fun <T> provide(key: ModifierLocal<T>, value: T) {
        require(providedValues !== EmptyMap) {
            "In order to provide locals you must override providedValues: ModifierLocalMap"
        }
        require(providedValues.contains(key)) {
            "Any provided key must be initially provided in the overridden providedValues: " +
                "ModifierLocalMap property. Key $key was not found."
        }
        providedValues[key] = value
    }

    /**
     * Read a [ModifierLocal] that was provided by other modifiers to the left of this modifier,
     * or above this modifier in the layout tree.
     */
    override val <T> ModifierLocal<T>.current: T
        get() {
            require(node.isAttached)
            val key = this
            visitAncestors(Nodes.Locals) {
                if (it.providedValues.contains(key)) {
                    @Suppress("UNCHECKED_CAST")
                    return it.providedValues[key] as T
                }
            }
            return key.defaultFactory()
        }
}

/**
 * Creates an empty [ModifierLocalMap]
 */
@ExperimentalComposeUiApi
fun modifierLocalMapOf(): ModifierLocalMap = EmptyMap

/**
 * Creates a [ModifierLocalMap] with a single key and value initialized to null.
 */
@ExperimentalComposeUiApi
fun <T> modifierLocalMapOf(
    key: ModifierLocal<T>
): ModifierLocalMap = SingleLocalMap(key)

/**
 * Creates a [ModifierLocalMap] with a single key and value. The provided [entry] should have
 * [Pair::first] be the [ModifierLocal] key, and the [Pair::second] be the corresponding value.
 */
@ExperimentalComposeUiApi
fun <T> modifierLocalMapOf(
    entry: Pair<ModifierLocal<T>, T>
): ModifierLocalMap = SingleLocalMap(entry.first).also { it[entry.first] = entry.second }

/**
 * Creates a [ModifierLocalMap] with several keys, all initialized with values of null
 */
@ExperimentalComposeUiApi
fun modifierLocalMapOf(
    vararg keys: ModifierLocal<*>
): ModifierLocalMap = MultiLocalMap(*keys.map { it to null }.toTypedArray())

/**
 * Creates a [ModifierLocalMap] with multiple keys and values. The provided [entries] should have
 * each item's [Pair::first] be the [ModifierLocal] key, and the [Pair::second] be the
 * corresponding value.
 */
@ExperimentalComposeUiApi
fun modifierLocalMapOf(
    vararg entries: Pair<ModifierLocal<*>, Any>
): ModifierLocalMap = MultiLocalMap(*entries)
