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
import androidx.compose.ui.input.key.ShortcutsInstance
import androidx.compose.ui.input.key.shortcuts

/**
 * Window-scoped keyboard handling.
 *
 * @see [shortcuts] to setup event handlers based on the element that is in focus
 */
@ExperimentalKeyInput
class Keyboard {
    private val shortcutsInstance = lazy {
        ShortcutsInstance()
    }

    /**
     * Set a callback for [KeysSet]. If callback for the same [KeysSet] already exists, it
     * overrides it.
     *
     * @param keysSet: [KeysSet] instance to react
     * @param callback: Called when all keys from keysSet are pressed
     */
    fun setShortcut(keysSet: KeysSet, callback: () -> Unit) {
        shortcutsInstance.value.setHandler(keysSet, callback)
    }

    /**
     * Set a callback for [Key]. If callback for the same [Key] already exists, it
     * overrides it.
     *
     * @param key: [Key] instance to react
     * @param callback: Called when all keys from keysSet are pressed
     */
    fun setShortcut(key: Key, callback: () -> Unit) {
        shortcutsInstance.value.setHandler(KeysSet(key), callback)
    }

    /**
     * Remove a callback for [KeysSet]. If no such callback it's noop
     *
     * @param keysSet: [KeysSet] instance
     */
    fun removeShortcut(keysSet: KeysSet) {
        shortcutsInstance.value.removeHandler(keysSet)
    }

    internal fun processKeyInput(keyEvent: KeyEvent): Boolean {
        return if (shortcutsInstance.isInitialized()) {
            shortcutsInstance.value.process(keyEvent)
        } else {
            false
        }
    }
}