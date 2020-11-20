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

package androidx.compose.ui.input.key

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import java.util.TreeMap

/**
 * Defines a set of keys. Can be used in keys handlers, see
 * [androidx.compose.ui.platform.Keyboard] and [Modifier.shortcuts]
 */
class KeysSet(internal val keys: Set<Key>) {
    /**
     * Returns a new [KeysSet] consists of current keys set + additional key
     *
     * @param key: additional key
     */
    operator fun plus(key: Key): KeysSet {
        return KeysSet(keys + key)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeysSet

        if (keys != other.keys) return false

        return true
    }

    override fun hashCode(): Int {
        return keys.hashCode()
    }

    override fun toString(): String {
        return "KeysSet(keys=$keys)"
    }
}

/**
 * Converts two keys into [KeysSet]
 */
operator fun Key.plus(other: Key): KeysSet {
    return KeysSet(setOf(this, other))
}

/**
 * [KeysSet] constructor for single-key sets
 */
fun KeysSet(key: Key): KeysSet {
    return KeysSet(setOf(key))
}

private fun makeHandlers() = TreeMap<KeysSet, () -> Unit>(compareByDescending { it.keys.size })

@ExperimentalKeyInput
internal class ShortcutsInstance(
    internal var handlers: TreeMap<KeysSet, () -> Unit> = makeHandlers()
) {
    private var pressedKeys = mutableSetOf<Key>()

    fun process(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.Unknown) {
            return false
        }
        syncPressedKeys(event)
        return findHandler()?.let {
            it()
            true
        } ?: false
    }

    internal fun setHandler(keysSet: KeysSet, handler: () -> Unit) {
        handlers[keysSet] = handler
    }

    internal fun removeHandler(keysSet: KeysSet) {
        handlers.remove(keysSet)
    }

    private fun syncPressedKeys(event: KeyEvent) {
        when (event.type) {
            KeyEventType.KeyDown -> {
                pressedKeys.add(event.key)
            }
            KeyEventType.KeyUp -> {
                pressedKeys.remove(event.key)
            }
            else -> {}
        }
    }

    private fun findHandler(): (() -> Unit)? {
        handlers.forEach { (keysSet, handler) ->
            if (pressedKeys.containsAll(keysSet.keys)) {
                return handler
            }
        }
        return null
    }
}

/**
 * [KeyEvent] handler which tracks pressed keys and triggers matched callbacks
 *
 * @see [keyInputFilter]
 * @see [androidx.compose.ui.platform.Keyboard] to define window-scoped shortcuts
 */
@ExperimentalKeyInput
@Composable
fun Modifier.shortcuts(builder: (ShortcutsBuilderScope).() -> Unit) = composed {
    val instance = remember { ShortcutsInstance() }
    instance.handlers = ShortcutsBuilderScope().also(builder).handlers
    keyInputFilter(instance::process)
}

class ShortcutsBuilderScope {
    internal val handlers = makeHandlers()
    /**
     * @param keysSet: represents a set of keys that can be simultaneously pressed
     * @param callback: called when all keys in [keysSet] are pressed
     */
    fun on(keysSet: KeysSet, callback: () -> Unit) {
        handlers[keysSet] = callback
    }

    /**
     * @param key: [Key] instance
     * @param callback: called when [key] is pressed
     */
    fun on(key: Key, callback: () -> Unit) {
        handlers[KeysSet(key)] = callback
    }
}