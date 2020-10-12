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

data class KeysSet(val keys: List<Key>) {
    operator fun plus(key: Key): KeysSet {
        return KeysSet(keys + key)
    }

    fun isSubset(of: KeysSet) = of.keys.containsAll(keys)
}

operator fun Key.plus(other: Key): KeysSet {
    return KeysSet(listOf(this, other))
}

@ExperimentalKeyInput
class KeysSetInstance(val keysSet: KeysSet) {
    var activeKeys = listOf<Key>()

    fun process(event: KeyEvent): Boolean {
        return if (keysSet.keys.contains(event.key)) {
            when (event.type) {
                KeyEventType.KeyDown -> {
                    val newKeys = activeKeys + event.key
                    if (newKeys.size == keysSet.keys.size) {
                        activeKeys = listOf()
                        true
                    } else {
                        activeKeys = newKeys
                        false
                    }
                }
                KeyEventType.KeyUp -> {
                    activeKeys = activeKeys - event.key
                    false
                }
                else -> false
            }
        } else {
            false
        }
    }
}

@ExperimentalKeyInput
class ShortcutInstance(val keysSet: KeysSet, val callback: () -> Unit) {
    private val keysSetInstance = KeysSetInstance(keysSet)

    fun process(event: KeyEvent): Boolean {
        if (keysSetInstance.process(event)) {
            callback()
            return true
        } else {
            return false
        }
    }
}

@ExperimentalKeyInput
@Composable
fun ShortcutHandler(keysSet: KeysSet, onShortcut: () -> Unit): (KeyEvent) -> Boolean {
    val instance = remember { ShortcutInstance(keysSet, onShortcut) }
    return instance::process
}

@ExperimentalKeyInput
@Composable
fun ShortcutHandler(key: Key, onShortcut: () -> Unit) =
    ShortcutHandler(KeysSet(listOf(key)), onShortcut)