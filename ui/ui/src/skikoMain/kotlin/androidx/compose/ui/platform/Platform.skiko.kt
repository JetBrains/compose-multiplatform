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
package androidx.compose.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue

// TODO(demin): make it public when we stabilize it after implementing it for uikit and js
internal interface Platform {
    val windowInfo: WindowInfo
    val focusManager: FocusManager
    fun requestFocusForOwner(): Boolean
    val textInputService: PlatformTextInputService
    fun accessibilityController(owner: SemanticsOwner): AccessibilityController
    fun setPointerIcon(pointerIcon: PointerIcon)

    companion object {
        @OptIn(ExperimentalComposeUiApi::class)
        val Empty = object : Platform {
            override val windowInfo = WindowInfoImpl().apply {
                // true is a better default if platform doesn't provide WindowInfo.
                // otherwise UI will be rendered always in unfocused mode
                // (hidden textfield cursor, gray titlebar, etc)
                isWindowFocused = true
            }

            override val focusManager = object : FocusManager {
                override fun clearFocus(force: Boolean) = Unit
                override fun moveFocus(focusDirection: FocusDirection) = false
            }

            override fun requestFocusForOwner() = false

            override val textInputService = object : PlatformTextInputService {
                override fun startInput(
                    value: TextFieldValue,
                    imeOptions: ImeOptions,
                    onEditCommand: (List<EditCommand>) -> Unit,
                    onImeActionPerformed: (ImeAction) -> Unit
                ) = Unit

                override fun stopInput() = Unit
                override fun showSoftwareKeyboard() = Unit
                override fun hideSoftwareKeyboard() = Unit
                override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) = Unit
            }

            override fun accessibilityController(owner: SemanticsOwner) = object : AccessibilityController {
                override fun onSemanticsChange() = Unit
                override fun onLayoutChange(layoutNode: LayoutNode) = Unit
                override suspend fun syncLoop() = Unit
            }

            override fun setPointerIcon(pointerIcon: PointerIcon) = Unit
        }
    }
}