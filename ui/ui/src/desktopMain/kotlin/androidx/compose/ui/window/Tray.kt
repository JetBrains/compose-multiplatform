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

/**
 * Tray is class for working with the system tray.
 */
class Tray {
    private lateinit var trayIcon: TrayIcon
    private var init: Boolean = false

    init {
        init = SystemTray.isSupported()
    }

    /**
     * Constructs a Tray with the empty icon image.
     */
    constructor() {}

    /**
     * Constructs a Tray with the given icon image.
     *
     * @param image Tray icon image.
     */
    constructor(image: Image) {
        if (!init) {
            return
        }

        trayIcon = TrayIcon(image)
        trayIcon.setImageAutoSize(true)
    }

    /**
     * Constructs a Tray with the given icon image and tooltip.
     *
     * @param image Tray icon image.
     * @param tooltip Tray icon tooltip.
     */
    constructor(image: Image, tooltip: String) {
        if (!init) {
            return
        }

        trayIcon = TrayIcon(image, tooltip)
        trayIcon.setImageAutoSize(true)
    }

    /**
     * Sets the Tray icon image.
     *
     * @param image Tray icon image.
     */
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

    /**
     * Sets the Tray menu.
     *
     * @param item Menu items.
     */
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

    /**
     * Sends a regular notification with the given title and message.
     *
     * @param title Notification title.
     * @param message Notification message.
     */
    fun notify(title: String, message: String) {
        if (!init) {
            return
        }
        trayIcon.displayMessage(title, message, MessageType.INFO)
    }

    /**
     * Sends a warning notification with the given title and message.
     *
     * @param title Notification title.
     * @param message Notification message.
     */
    fun warn(title: String, message: String) {
        if (!init) {
            return
        }
        trayIcon.displayMessage(title, message, MessageType.WARNING)
    }

    /**
     * Sends a error notification with the given title and message.
     *
     * @param title Notification title.
     * @param message Notification message.
     */
    fun error(title: String, message: String) {
        if (!init) {
            return
        }
        trayIcon.displayMessage(title, message, MessageType.ERROR)
    }

    /**
     * Removes the tray icon from the system tray.
     */
    fun remove() {
        try {
            SystemTray.getSystemTray().remove(trayIcon)
        } catch (e: Exception) {
            println("TrayIcon could not be removed.")
        }
    }
}
