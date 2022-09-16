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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.JPopupContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.text.TextContextMenu.TextManager
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.getSelectedText
import java.awt.Component
import javax.swing.JPopupMenu

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit
) {
    val state = remember { ContextMenuState() }
    if (DesktopPlatform.Current == DesktopPlatform.MacOS) {
        OpenMenuAdjuster(state) { manager.contextMenuOpenAdjustment(it) }
    }
    LocalTextContextMenu.current.Area(manager.textManager, state, content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
    manager: SelectionManager,
    content: @Composable () -> Unit
) {
    val state = remember { ContextMenuState() }
    if (DesktopPlatform.Current == DesktopPlatform.MacOS) {
        OpenMenuAdjuster(state) { manager.contextMenuOpenAdjustment(it) }
    }
    LocalTextContextMenu.current.Area(manager.textManager, state, content)
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

@OptIn(ExperimentalFoundationApi::class)
private val TextFieldSelectionManager.textManager get() = object : TextManager {
    override val selectedText get() = value.getSelectedText()

    val isPassword get() = visualTransformation is PasswordVisualTransformation

    override val cut: (() -> Unit)? get() =
        if (!value.selection.collapsed && editable && !isPassword) {
            {
                cut()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val copy: (() -> Unit)? get() =
        if (!value.selection.collapsed && !isPassword) {
            {
                copy(false)
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val paste: (() -> Unit)? get() =
        if (editable && clipboardManager?.getText() != null) {
            {
                paste()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val selectAll: (() -> Unit)? get() =
        if (value.selection.length != value.text.length) {
            {
                selectAll()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }
}

@OptIn(ExperimentalFoundationApi::class)
private val SelectionManager.textManager get() = object : TextManager {
    override val selectedText get() = getSelectedText() ?: AnnotatedString("")
    override val cut = null
    override val copy = { copy() }
    override val paste = null
    override val selectAll = null
}

/**
 * Composition local that keeps [TextContextMenu].
 */
@ExperimentalFoundationApi
val LocalTextContextMenu:
    ProvidableCompositionLocal<TextContextMenu> = staticCompositionLocalOf { TextContextMenu.Default }

/**
 * Describes how to show the text context menu for selectable texts and text fields.
 */
@ExperimentalFoundationApi
interface TextContextMenu {
    /**
     * Defines an area, that describes how to open and show text context menus.
     * Usually it uses [ContextMenuArea] as the implementation.
     *
     * @param textManager Provides useful methods and information for text for which we show the text context menu.
     * @param state [ContextMenuState] of menu controlled by this area.
     * @param content The content of the [ContextMenuArea].
     */
    @Composable
    fun Area(textManager: TextManager, state: ContextMenuState, content: @Composable () -> Unit)

    /**
     * Provides useful methods and information for text for which we show the text context menu.
     */
    @ExperimentalFoundationApi
    interface TextManager {
        /**
         * The current selected text.
         */
        val selectedText: AnnotatedString

        /**
         * Action for cutting the selected text to the clipboard. Null if there is no text to cut.
         */
        val cut: (() -> Unit)?

        /**
         * Action for copy the selected text to the clipboard. Null if there is no text to copy.
         */
        val copy: (() -> Unit)?

        /**
         * Action for pasting text from the clipboard. Null if there is no text in the clipboard.
         */
        val paste: (() -> Unit)?

        /**
         * Action for selecting the whole text. Null if the text is already selected.
         */
        val selectAll: (() -> Unit)?
    }

    companion object {
        /**
         * [TextContextMenu] that is used by default in Compose.
         */
        @ExperimentalFoundationApi
        val Default = object : TextContextMenu {
            @Composable
            override fun Area(textManager: TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
                val localization = LocalLocalization.current
                val items = {
                    listOfNotNull(
                        textManager.cut?.let {
                            ContextMenuItem(localization.cut, it)
                        },
                        textManager.copy?.let {
                            ContextMenuItem(localization.copy, it)
                        },
                        textManager.paste?.let {
                            ContextMenuItem(localization.paste, it)
                        },
                        textManager.selectAll?.let {
                            ContextMenuItem(localization.selectAll, it)
                        },
                    )
                }

                ContextMenuArea(items, state, content = content)
            }
        }
    }
}

/**
 * [TextContextMenu] that uses [JPopupMenu] to show the text context menu.
 *
 * You can use it by overriding [TextContextMenu] on the top level of your application.
 *
 * @param owner The root component that owns a context menu. Usually it is [ComposeWindow] or [ComposePanel].
 * @param createMenu Describes how to create [JPopupMenu] from [TextManager] and from list of custom [ContextMenuItem]
 * defined by [CompositionLocalProvider].
 */
@ExperimentalFoundationApi
class JPopupTextMenu(
    private val owner: Component,
    private val createMenu: (TextManager, List<ContextMenuItem>) -> JPopupMenu,
) : TextContextMenu {
    @Composable
    override fun Area(textManager: TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalContextMenuRepresentation provides JPopupContextMenuRepresentation(owner) {
                createMenu(textManager, it)
            }
        ) {
            // We pass emptyList, but it will be merged with the other custom items defined via ContextMenuDataProvider, and passed to createMenu
            ContextMenuArea({ emptyList() }, state, content = content)
        }
    }
}
