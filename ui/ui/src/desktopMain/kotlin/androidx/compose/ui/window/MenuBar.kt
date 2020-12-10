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

import org.jetbrains.skiko.Library
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

/**
 * MenuBar is a class that represents a menu bar that can be attached to a window.
 * The menu bar can be displayed inside a window (Windows, Linux) or at the top of
 * the screen (Mac OS).
 */
class MenuBar {
    internal var menuBar: JMenuBar

    init {
        menuBar = JMenuBar()
        // For the MenuBar to work correctly, we need to set the skiko system properties
        Library.load()
    }

    /**
     * Constructs a MenuBar with the given menus.
     *
     * @param menu MenuBar menus.
     */
    constructor(vararg menu: Menu) {
        menu(menu)
    }

    /**
     * Adds additional menus to the MenuBar.
     */
    fun add(vararg menu: Menu) {
        menu(menu)
    }

    private fun menu(list: Array<out Menu>) {
        for (item in list) {
            val menu = JMenu(item.name)
            menuBar.add(menu)

            for (menuItem in item.list) {
                val value = JMenuItem(menuItem.name)
                value.setAccelerator(menuItem.shortcut)
                value.addActionListener(object : ActionListener {
                    public override fun actionPerformed(e: ActionEvent) {
                        menuItem.action?.invoke()
                    }
                })
                menu.add(value)
            }
        }
    }
}

/**
 * Menu is a class that represents a menu on a menu bar.
 */
class Menu {
    /**
     * Gets the menu name.
     */
    val name: String

    /**
     * Gets the menu items.
     */
    val list: List<MenuItem>

    /**
     * Constructs a Menu with the given name and menu items.
     *
     * @param name Menu name.
     * @param item Menu items.
     */
    constructor(name: String, vararg item: MenuItem) {
        this.name = name
        this.list = item.asList()
    }
}