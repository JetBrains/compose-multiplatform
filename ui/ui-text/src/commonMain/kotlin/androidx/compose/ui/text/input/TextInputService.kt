/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.input

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.util.annotation.VisibleForTesting

/**
 * The input session token.
 *
 * The positive session token means the input session is alive. The session may be expired though.
 * The zero session token means no session.
 * The negative session token means the input session could not be established with some errors.
 */
typealias InputSessionToken = Int

/**
 * A special session token which represents there is no active input session.
 */
const val NO_SESSION: InputSessionToken = 0

/**
 * A special session token which represents the session couldn't be established.
 */
const val INVALID_SESSION: InputSessionToken = -1

/**
 * Provide a communication with platform text input service.
 */
// Open for testing purposes.
@OptIn(ExperimentalTextApi::class)
open class TextInputService(private val platformTextInputService: PlatformTextInputService) {

    private var nextSessionToken: Int = 1
    private var currentSessionToken: InputSessionToken = NO_SESSION

    private inline fun ignoreIfExpired(token: InputSessionToken, block: () -> Unit) {
        if (token > 0 && token == currentSessionToken) {
            block()
        }
    }

    /**
     * Start text input session for given client.
     */
    open fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditOperation>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ): InputSessionToken {
        platformTextInputService.startInput(
            value,
            imeOptions,
            onEditCommand,
            onImeActionPerformed
        )
        currentSessionToken = nextSessionToken++
        return currentSessionToken
    }

    /**
     * Stop text input session.
     */
    open fun stopInput(token: InputSessionToken) = ignoreIfExpired(token) {
        platformTextInputService.stopInput()
    }

    /**
     * Request showing onscreen keyboard
     *
     * There is no guarantee nor callback of the result of this API. The software keyboard or
     * system service may silently ignores this request.
     */
    open fun showSoftwareKeyboard(token: InputSessionToken) = ignoreIfExpired(token) {
        platformTextInputService.showSoftwareKeyboard()
    }

    /**
     * Hide onscreen keyboard
     */
    open fun hideSoftwareKeyboard(token: InputSessionToken) = ignoreIfExpired(token) {
        platformTextInputService.hideSoftwareKeyboard()
    }

    /*
     * Notify the new editor model to IME.
     */
    open fun onStateUpdated(
        token: InputSessionToken,
        oldValue: TextFieldValue?,
        newValue: TextFieldValue
    ) = ignoreIfExpired(token) {
        platformTextInputService.onStateUpdated(oldValue, newValue)
    }

    /**
     * Notify the focused rectangle to the system.
     */
    open fun notifyFocusedRect(token: InputSessionToken, rect: Rect) = ignoreIfExpired(token) {
        platformTextInputService.notifyFocusedRect(rect)
    }
}

/**
 * Platform specific text input service.
 */
@OptIn(ExperimentalTextApi::class)
interface PlatformTextInputService {
    /**
     * Start text input session for given client.
     */
    fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditOperation>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    )

    /**
     * Stop text input session.
     */
    fun stopInput()

    /**
     * Request showing onscreen keyboard
     *
     * There is no guarantee nor callback of the result of this API.
     */
    fun showSoftwareKeyboard()

    /**
     * Hide software keyboard
     */
    fun hideSoftwareKeyboard()

    /*
     * Notify the new editor model to IME.
     */
    fun onStateUpdated(oldValue: TextFieldValue?, newValue: TextFieldValue)

    /**
     * Notify the focused rectangle to the system.
     */
    fun notifyFocusedRect(rect: Rect)
}

/** @suppress */
@InternalTextApi
@Deprecated(level = DeprecationLevel.ERROR, message = "This is internal API")
var textInputServiceFactory: (PlatformTextInputService) -> TextInputService =
    { TextInputService(it) }
    @VisibleForTesting
        set