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
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.actionmodecallback.FloatingTextActionModeCallback
import androidx.compose.ui.platform.actionmodecallback.PrimaryTextActionModeCallback
import androidx.compose.ui.platform.actionmodecallback.TextActionModeCallback

/**
 * Android implementation for [TextToolbar].
 */
internal class AndroidTextToolbar(private val view: View) : TextToolbar {
    private var actionMode: ActionMode? = null
    private val textActionModeCallback: TextActionModeCallback = TextActionModeCallback(
        onActionModeDestroy = {
            actionMode = null
        }
    )
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        textActionModeCallback.rect = rect
        textActionModeCallback.onCopyRequested = onCopyRequested
        textActionModeCallback.onCutRequested = onCutRequested
        textActionModeCallback.onPasteRequested = onPasteRequested
        textActionModeCallback.onSelectAllRequested = onSelectAllRequested
        if (actionMode == null) {
            status = TextToolbarStatus.Shown
            actionMode = if (Build.VERSION.SDK_INT >= 23) {
                TextToolbarHelperMethods.startActionMode(
                    view,
                    FloatingTextActionModeCallback(textActionModeCallback),
                    ActionMode.TYPE_FLOATING
                )
            } else {
                view.startActionMode(
                    PrimaryTextActionModeCallback(textActionModeCallback)
                )
            }
        } else {
            actionMode?.invalidate()
        }
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(23)
internal object TextToolbarHelperMethods {
    @RequiresApi(23)
    @DoNotInline
    fun startActionMode(
        view: View,
        actionModeCallback: ActionMode.Callback,
        type: Int
    ): ActionMode? {
        return view.startActionMode(
            actionModeCallback,
            type
        )
    }

    @RequiresApi(23)
    @DoNotInline
    fun invalidateContentRect(actionMode: ActionMode) {
        actionMode.invalidateContentRect()
    }
}