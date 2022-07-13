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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * Interface for text-related toolbar.
 */
@JvmDefaultWithCompatibility
interface TextToolbar {
    /**
     * Show the floating toolbar(post-M) or primary toolbar(pre-M) for copying, cutting and pasting
     * text.
     * @param rect region of interest. The selected region around which the floating toolbar
     * should show. This rect is in global coordinates system.
     * @param onCopyRequested callback to copy text into ClipBoardManager.
     * @param onPasteRequested callback to get text from ClipBoardManager and paste it.
     * @param onCutRequested callback to cut text and copy the text into ClipBoardManager.
     */
    fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)? = null,
        onPasteRequested: (() -> Unit)? = null,
        onCutRequested: (() -> Unit)? = null,
        onSelectAllRequested: (() -> Unit)? = null
    )

    /**
     * Hide the floating toolbar(post-M) or primary toolbar(pre-M).
     */
    fun hide()

    /**
     * Return the [TextToolbarStatus] to check if the toolbar is shown or hidden.
     *
     * @return [TextToolbarStatus] of [TextToolbar].
     */
    val status: TextToolbarStatus
}