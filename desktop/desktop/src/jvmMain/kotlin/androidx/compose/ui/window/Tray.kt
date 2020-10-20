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

import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.awt.Image
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.TrayIcon.MessageType

class Tray {
    private lateinit var trayIcon: TrayIcon
    private var init: Boolean = false

    init {
        init = SystemTray.isSupported()
    }

    constructor() {}

    constructor(image: Image) {
        if (!init) {
            return
        }

        trayIcon = TrayIcon(image)
        trayIcon.setImageAutoSize(true)
    }

    constructor(image: Image, tooltip: String) {
        if (!init) {
            return
        }

        trayIcon = TrayIcon(image, tooltip)
        trayIcon.setImageAutoSize(true)
    }

    fun icon(image: Image) {
        if (!init) {
            return
        }
        if (this::trayIcon.isInitialized) {
            trayIcon.setImage(image)
        } else {
            trayIcon = TrayIcon(image)
            trayIcon.setImageAutoSize(true)
        }
    }

    fun menu(vararg item: MenuItem) {
        if (!init) {
            return
        }
        val popup = PopupMenu()
        for (menuItem in item) {
            val value = java.awt.MenuItem(menuItem.name)
            value.addActionListener(object : ActionListener {
                public override fun actionPerformed(e: ActionEvent) {
                    menuItem.action?.invoke()
                }
            })
            popup.add(value)
        }
        trayIcon.setPopupMenu(popup)
        try {
            SystemTray.getSystemTray().add(trayIcon)
        } catch (e: Exception) {
            println("TrayIcon could not be added.")
        }
    }

    fun notify(title: String, message: String) {
        if (!init) {
            return
        }
        trayIcon.displayMessage(title, message, MessageType.INFO)
    }

    fun warn(title: String, message: String) {
        if (!init) {
            return
        }
        trayIcon.displayMessage(title, message, MessageType.WARNING)
    }

    fun error(title: String, message: String) {
        if (!init) {
            return
        }
        trayIcon.displayMessage(title, message, MessageType.ERROR)
    }

    fun remove() {
        try {
            SystemTray.getSystemTray().remove(trayIcon)
        } catch (e: Exception) {
            println("TrayIcon could not be removed.")
        }
    }
}
