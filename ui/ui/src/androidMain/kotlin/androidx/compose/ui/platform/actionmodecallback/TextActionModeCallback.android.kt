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

package androidx.compose.ui.platform.actionmodecallback

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ActionCallback

internal const val MENU_ITEM_COPY = 0
internal const val MENU_ITEM_PASTE = 1
internal const val MENU_ITEM_CUT = 2
internal const val MENU_ITEM_SELECT_ALL = 3

internal class TextActionModeCallback(
    var rect: Rect = Rect.Zero,
    var onCopyRequested: ActionCallback? = null,
    var onPasteRequested: ActionCallback? = null,
    var onCutRequested: ActionCallback? = null,
    var onSelectAllRequested: ActionCallback? = null
) {
    fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        requireNotNull(menu)
        requireNotNull(mode)

        onCopyRequested?.let {
            menu.add(0, MENU_ITEM_COPY, 0, android.R.string.copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }

        onPasteRequested?.let {
            menu.add(0, MENU_ITEM_PASTE, 1, android.R.string.paste)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }

        onCutRequested?.let {
            menu.add(0, MENU_ITEM_CUT, 2, android.R.string.cut)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }

        onSelectAllRequested?.let {
            menu.add(0, MENU_ITEM_SELECT_ALL, 3, android.R.string.selectAll)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        return true
    }

    fun onPrepareActionMode(): Boolean {
        return false
    }

    fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item!!.itemId) {
            MENU_ITEM_COPY -> onCopyRequested?.invoke()
            MENU_ITEM_PASTE -> onPasteRequested?.invoke()
            MENU_ITEM_CUT -> onCutRequested?.invoke()
            MENU_ITEM_SELECT_ALL -> onSelectAllRequested?.invoke()
            else -> return false
        }
        mode?.finish()
        return true
    }

    fun onDestroyActionMode() {}
}
