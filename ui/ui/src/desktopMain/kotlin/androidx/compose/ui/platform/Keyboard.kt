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

import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeysSet
import androidx.compose.ui.input.key.ShortcutInstance
import java.lang.RuntimeException

class AmbiguousShortcut(val superKeysSet: KeysSet) : RuntimeException()

interface KeyboardShortcutHandler {
    fun cancel()
}

@ExperimentalKeyInput
class Keyboard {
    private val shortcuts = mutableListOf<ShortcutInstance>()

    fun shortcut(key: Key, callback: () -> Unit) = shortcut(KeysSet(listOf(key)), callback)

    fun shortcut(keysSet: KeysSet, callback: () -> Unit): KeyboardShortcutHandler {
        checkAmbiguityOfShortcut(keysSet)
        val instance = ShortcutInstance(keysSet, callback)
        shortcuts.add(instance)
        return object : KeyboardShortcutHandler {
            override fun cancel() {
                shortcuts.remove(instance)
            }
        }
    }

    private fun checkAmbiguityOfShortcut(keysSet: KeysSet) {
        shortcuts.forEach {
            if (keysSet.isSubset(it.keysSet)) {
                throw(AmbiguousShortcut(it.keysSet))
            }
        }
    }

    internal fun processKeyInput(keyEvent: KeyEvent): Boolean {
        shortcuts.forEach {
            if (it.process(keyEvent)) {
                return true
            }
        }
        return false
    }
}