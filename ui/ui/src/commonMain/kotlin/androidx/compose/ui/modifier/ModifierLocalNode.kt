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

@ExperimentalComposeUiApi
interface ModifierLocalNode : ModifierLocalReadScope, DelegatableNode {
    val providedValues: ModifierLocalMap get() = EmptyMap
    fun <T> provide(key: ModifierLocal<T>, value: T) {
        require(providedValues !== EmptyMap) {
            "In order to provide locals you must override providedValues: ModifierLocalMap"
        }
        providedValues[key] = value
    }

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

@ExperimentalComposeUiApi
fun modifierLocalMapOf(): ModifierLocalMap = EmptyMap

@ExperimentalComposeUiApi
fun <T> modifierLocalMapOf(
    key: ModifierLocal<T>
): ModifierLocalMap = SingleLocalMap(key)

@ExperimentalComposeUiApi
fun <T> modifierLocalMapOf(
    entry: Pair<ModifierLocal<T>, T>
): ModifierLocalMap = SingleLocalMap(entry.first).also { it[entry.first] = entry.second }

@ExperimentalComposeUiApi
fun modifierLocalMapOf(
    vararg keys: ModifierLocal<*>
): ModifierLocalMap = MultiLocalMap(*keys.map { it to null }.toTypedArray())

@ExperimentalComposeUiApi
fun modifierLocalMapOf(
    vararg entries: Pair<ModifierLocal<*>, Any>
): ModifierLocalMap = MultiLocalMap(*entries)
