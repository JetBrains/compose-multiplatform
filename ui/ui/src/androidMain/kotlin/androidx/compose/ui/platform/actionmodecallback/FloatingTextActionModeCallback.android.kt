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

import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi

@RequiresApi(23)
internal class FloatingTextActionModeCallback(
    private val callback: TextActionModeCallback
) : ActionMode.Callback2() {
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return callback.onActionItemClicked(mode, item)
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return callback.onCreateActionMode(mode, menu)
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return callback.onPrepareActionMode(mode, menu)
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        callback.onDestroyActionMode()
    }

    override fun onGetContentRect(
        mode: ActionMode?,
        view: View?,
        outRect: Rect?
    ) {
        val rect = callback.rect
        outRect?.set(
            rect.left.toInt(),
            rect.top.toInt(),
            rect.right.toInt(),
            rect.bottom.toInt()
        )
    }
}
