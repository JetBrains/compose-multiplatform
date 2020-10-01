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

package androidx.compose.ui.platform

import android.os.Build
import android.view.ActionMode
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.actionmodecallback.FloatingTextActionModeCallback
import androidx.compose.ui.platform.actionmodecallback.PrimaryTextActionModeCallback
import androidx.compose.ui.platform.actionmodecallback.TextActionModeCallback

/**
 * Android implementation for [TextToolbar].
 */
internal class AndroidTextToolbar(private val view: View) : TextToolbar {
    private var actionMode: ActionMode? = null
    private var textToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: ActionCallback?,
        onPasteRequested: ActionCallback?,
        onCutRequested: ActionCallback?,
        onSelectAllRequested: ActionCallback?
    ) {
        textToolbarStatus = TextToolbarStatus.Shown
        if (Build.VERSION.SDK_INT >= 23) {
            val actionModeCallback =
                FloatingTextActionModeCallback(
                    TextActionModeCallback(
                        onCopyRequested = onCopyRequested,
                        onCutRequested = onCutRequested,
                        onPasteRequested = onPasteRequested,
                        onSelectAllRequested = onSelectAllRequested
                    )
                )
            actionModeCallback.setRect(rect)
            actionMode = view.startActionMode(
                actionModeCallback,
                ActionMode.TYPE_FLOATING
            )
        } else {
            val actionModeCallback =
                PrimaryTextActionModeCallback(
                    TextActionModeCallback(
                        onCopyRequested = onCopyRequested,
                        onPasteRequested = onPasteRequested,
                        onCutRequested = onCutRequested,
                        onSelectAllRequested = onSelectAllRequested
                    )
                )
            actionMode = view.startActionMode(actionModeCallback)
        }
    }

    override fun hide() {
        textToolbarStatus = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }

    override val status: TextToolbarStatus
        get() = textToolbarStatus
}
