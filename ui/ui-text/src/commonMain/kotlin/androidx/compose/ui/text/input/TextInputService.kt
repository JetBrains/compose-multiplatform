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
 * Handles communication with the IME. Informs about the IME changes via [EditCommand]s and
 * provides utilities for working with software keyboard.
 */
// Open for testing purposes.
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
     *
     * @param value initial [TextFieldValue]
     * @param imeOptions IME configuration
     * @param onEditCommand callback to inform about changes requested by IME
     * @param onImeActionPerformed callback to inform if an IME action such as [ImeAction.Done]
     * etc occurred.
     */
    open fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
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
     *
     * If the [token] is not valid no action will be performed.
     *
     * @param token the token returned by [startInput] call.
     */
    open fun stopInput(token: InputSessionToken) = ignoreIfExpired(token) {
        platformTextInputService.stopInput()
    }

    /**
     * Request showing onscreen keyboard.
     *
     * There is no guarantee that the keyboard will be shown. The software keyboard or
     * system service may silently ignore this request.
     *
     * If the [token] is not valid no action will be performed.
     *
     * @param token the token returned by [startInput] call.
     */
    open fun showSoftwareKeyboard(token: InputSessionToken) = ignoreIfExpired(token) {
        platformTextInputService.showSoftwareKeyboard()
    }

    /**
     * Hide onscreen keyboard.
     *
     * If the [token] is not valid no action will be performed.
     *
     * @param token the token returned by [startInput] call.
     */
    open fun hideSoftwareKeyboard(token: InputSessionToken) = ignoreIfExpired(token) {
        platformTextInputService.hideSoftwareKeyboard()
    }

    /**
     * Notify IME about the new [TextFieldValue] and latest state of the editing buffer. [oldValue]
     * is the state of the buffer before the changes applied by the [newValue].
     *
     * [oldValue] represents the changes that was requested by IME on the buffer, and [newValue]
     * is the final state of the editing buffer that was requested by the application. In cases
     * where [oldValue] is not equal to [newValue], it would mean the IME suggested value is
     * rejected, and the IME connection will be restarted with the newValue.
     *
     * If the [token] is not valid no action will be performed.
     *
     * @param token the token returned by [startInput] call.
     * @param oldValue the value that was requested by IME on the buffer
     * @param newValue final state of the editing buffer that was requested by the application
     */
    open fun updateState(
        token: InputSessionToken,
        oldValue: TextFieldValue?,
        newValue: TextFieldValue
    ) = ignoreIfExpired(token) {
        platformTextInputService.updateState(oldValue, newValue)
    }

    /**
     * Notify the focused rectangle to the system.
     *
     * If the [token] is not valid no action will be performed.
     *
     * @param token the token returned by [startInput] call.
     * @param rect the rectangle that describes the boundaries on the screen that requires focus
     */
    open fun notifyFocusedRect(token: InputSessionToken, rect: Rect) = ignoreIfExpired(token) {
        platformTextInputService.notifyFocusedRect(rect)
    }
}

/**
 * Platform specific text input service.
 */
interface PlatformTextInputService {
    /**
     * Start text input session for given client.
     *
     * @see TextInputService.startInput
     */
    fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    )

    /**
     * Stop text input session.
     *
     * @see TextInputService.stopInput
     */
    fun stopInput()

    /**
     * Request showing onscreen keyboard
     *
     * There is no guarantee nor callback of the result of this API.
     *
     * @see TextInputService.showSoftwareKeyboard
     */
    fun showSoftwareKeyboard()

    /**
     * Hide software keyboard
     *
     * @see TextInputService.hideSoftwareKeyboard
     */
    fun hideSoftwareKeyboard()

    /*
     * Notify the new editor model to IME.
     *
     * @see TextInputService.updateState
     */
    fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue)

    /**
     * Notify the focused rectangle to the system.
     *
     * @see TextInputService.notifyFocusedRect
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