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

package androidx.compose.foundation.text

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.flow.collect

@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit
) {
    val state = remember { ContextMenuState() }
    if (DesktopPlatform.Current == DesktopPlatform.MacOS) {
        OpenMenuAdjuster(state) { manager.contextMenuOpenAdjustment(it) }
    }
    ContextMenuArea(manager.contextMenuItems(), state, content = content)
}

@Composable
internal actual fun ContextMenuArea(
    manager: SelectionManager,
    content: @Composable () -> Unit
) {
    val state = remember { ContextMenuState() }
    if (DesktopPlatform.Current == DesktopPlatform.MacOS) {
        OpenMenuAdjuster(state) { manager.contextMenuOpenAdjustment(it) }
    }
    ContextMenuArea(manager.contextMenuItems(), state, content = content)
}

@Composable
internal fun OpenMenuAdjuster(state: ContextMenuState, adjustAction: (Offset) -> Unit) {
    LaunchedEffect(state) {
        snapshotFlow { state.status }.collect { status ->
            if (status is ContextMenuState.Status.Open) {
                adjustAction(status.rect.center)
            }
        }
    }
}

@Composable
internal fun TextFieldSelectionManager.contextMenuItems(): () -> List<ContextMenuItem> {
    val platformLocalization = LocalLocalization.current
    return {
        val result = mutableListOf<ContextMenuItem>()
        val isPassword = visualTransformation is PasswordVisualTransformation
        if (!value.selection.collapsed && !isPassword) {
            result.add(
                ContextMenuItem(platformLocalization.copy) {
                    copy(false)
                    focusRequester?.requestFocus()
                }
            )
        }

        if (!value.selection.collapsed && editable && !isPassword) {
            result.add(
                ContextMenuItem(platformLocalization.cut) {
                    cut()
                    focusRequester?.requestFocus()
                }
            )
        }

        if (editable && clipboardManager?.getText() != null) {
            result.add(
                ContextMenuItem(platformLocalization.paste) {
                    paste()
                    focusRequester?.requestFocus()
                }
            )
        }

        if (value.selection.length != value.text.length) {
            result.add(
                ContextMenuItem(platformLocalization.selectAll) {
                    selectAll()
                    focusRequester?.requestFocus()
                }
            )
        }
        result
    }
}

@Composable
internal fun SelectionManager.contextMenuItems(): () -> List<ContextMenuItem> {
    val localization = LocalLocalization.current
    return {
        listOf(
            ContextMenuItem(localization.copy) {
                copy()
            }
        )
    }
}