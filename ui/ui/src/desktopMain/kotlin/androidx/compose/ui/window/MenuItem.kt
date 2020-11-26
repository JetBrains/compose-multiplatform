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
package androidx.compose.ui.window

import androidx.compose.ui.input.key.Key
import java.awt.Toolkit
import javax.swing.KeyStroke

/**
 * MenuItem is a class that represents an implementation of an item in a menu.
 * Can be used with Menu or Tray.
 */
class MenuItem {

    /**
     * Gets the MenuItem name.
     */
    val name: String

    /**
     * Gets the MenuItem action when it is clicked or a keyboard shortcut is pressed (if specified).
     */
    val action: (() -> Unit)?

    /**
     * Gets the MenuItem shortcut.
     */
    val shortcut: KeyStroke

    /**
     * Constructs a MenuItem with the given name, action, and keyboard shortcut.
     *
     * @param name MenuItem name.
     * @param onClick MenuItem action.
     * @param shortcut MenuItem keyboard shortcut.
     */
    constructor(
        name: String,
        onClick: (() -> Unit)? = null,
        shortcut: KeyStroke = KeyStroke(Key.Unknown)
    ) {
        this.name = name
        this.action = onClick
        this.shortcut = shortcut
    }
}

/**
 * Returns KeyStroke for the given key. KeyStroke contains a OS-specific modifier
 * (Command on Mac OS and Control on Windows and Linux) and the given key.
 *
 * @param key Keyboard key.
 *
 * @return KeyStroke for the given key.
 */
fun KeyStroke(key: Key): KeyStroke {
    return KeyStroke.getKeyStroke(
        key.keyCode,
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
    )
}