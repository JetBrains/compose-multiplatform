/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Creates a [Notification] that is remembered across compositions.
 *
 * @param title Title of the notification
 * @param message The main text of the notification
 * @param type Type of the notification that defines how the notification will be represented to
 * the user (with which icon and which sound)
 */
@Composable
fun rememberNotification(
    title: String,
    message: String,
    type: Notification.Type = Notification.Type.None
): Notification = remember {
    Notification(
        title, message, type
    )
}

/**
 * Notification, that can be sent to the platform and be shown to the user (in tray, notification
 * center, etc; depends on the platform).
 *
 * If notification creates inside Composable function it is better to use
 * [rememberNotification] to avoid creating a new object every recomposition.
 *
 * @param title Title of the notification
 * @param message The main text of the notification
 * @param type Type of the notification that defines how the notification will be represented to
 * the user (with which icon and which sound)
 */
class Notification(
    val title: String,
    val message: String,
    val type: Type = Type.None
) {
    /**
     * Returns a copy of this [Notification] instance optionally overriding the
     * [title], [message], [type] parameter.
     */
    fun copy(
        title: String = this.title,
        message: String = this.message,
        type: Type = this.type
    ) = Notification(title, message, type)

    override fun toString(): String {
        return "Notification(title=$title, message=$message, type=$type)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Notification

        if (title != other.title) return false
        if (message != other.message) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    /**
     * Describes the type of the notification.
     * Usually the platform shows a different icon and plays a different sound when notification
     * will be shown to the user.
     */
    enum class Type {
        /**
         * Simple notification
         */
        None,

        /**
         * Info notification
         */
        Info,

        /**
         * Warning notification
         */
        Warning,

        /**
         * Error notification
         */
        Error,
    }
}