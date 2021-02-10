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

import java.awt.SystemTray
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.TrayIcon
import java.awt.TrayIcon.MessageType

/**
 * Notifier is a class that can send system notifications.
 */
class Notifier {

    /**
     * Sends a regular notification with the given title and message.
     *
     * @param title Notification title.
     * @param message Notification message.
     */
    fun notify(title: String, message: String) {
        send(title, message, MessageType.INFO)
    }

    /**
     * Sends a warning notification with the given title and message.
     *
     * @param title Notification title.
     * @param message Notification message.
     */
    fun warn(title: String, message: String) {
        send(title, message, MessageType.WARNING)
    }

    /**
     * Sends a error notification with the given title and message.
     *
     * @param title Notification title.
     * @param message Notification message.
     */
    fun error(title: String, message: String) {
        send(title, message, MessageType.ERROR)
    }

    private fun send(title: String, message: String, type: MessageType) {
        if (SystemTray.isSupported()) {
            val tray = SystemTray.getSystemTray()
            val trayIcons = tray.getTrayIcons()
            if (trayIcons.isNotEmpty()) {
                trayIcons[0].displayMessage(title, message, type)
            } else {
                val trayIcon = TrayIcon(emptyImage())
                tray.add(trayIcon)
                trayIcon.displayMessage(title, message, type)
                tray.remove(trayIcon)
            }
        }
    }

    private fun emptyImage(): Image {
        return BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    }
}