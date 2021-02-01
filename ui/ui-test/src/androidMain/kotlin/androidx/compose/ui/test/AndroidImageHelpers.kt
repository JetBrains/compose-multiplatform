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

package androidx.compose.ui.test

import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.android.captureRegionToImage
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt

/**
 * Captures the underlying semantics node's surface into bitmap.
 *
 * This has a limitation that if there is another window covering part of this node, such a
 * window won't occur in this bitmap.
 *
 * @throws IllegalArgumentException if a bitmap is taken inside of a popup.
*/
@RequiresApi(Build.VERSION_CODES.O)
fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
    val node = fetchSemanticsNode("Failed to capture a node to bitmap.")
    // TODO(pavlis): Consider doing assertIsDisplayed here. Will need to move things around.
    var windowToUse: Window? = null

    // Validate we are not in popup
    val popupParentMaybe = node.findClosestParentNode(includeSelf = true) {
        it.config.contains(SemanticsProperties.IsPopup)
    }
    if (popupParentMaybe != null) {
        // We do not support capturing popups to bitmap
        throw IllegalArgumentException(
            "The node that is being captured to bitmap is in " +
                "a popup or is a popup itself. Popups currently cannot be captured to bitmap."
        )
    }

    val view = (node.root as ViewRootForTest).view

    // If we are in dialog use its window to capture the bitmap
    val dialogParentNodeMaybe = node.findClosestParentNode(includeSelf = true) {
        it.config.contains(SemanticsProperties.IsDialog)
    }
    if (dialogParentNodeMaybe != null) {
        val dialogProvider = findDialogWindowProviderInParent(view)
            ?: throw IllegalArgumentException(
                "Could not find a dialog window provider to capture" +
                    " its bitmap"
            )
        windowToUse = dialogProvider.window

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // b/163023027
            throw IllegalArgumentException("Cannot currently capture dialogs on API lower than 28!")
        }
    }

    val nodeBounds = node.boundsInRoot
    val nodeBoundsRect = android.graphics.Rect(
        nodeBounds.left.roundToInt(),
        nodeBounds.top.roundToInt(),
        nodeBounds.right.roundToInt(),
        nodeBounds.bottom.roundToInt()
    )

    val locationInWindow = intArrayOf(0, 0)
    view.getLocationInWindow(locationInWindow)
    val x = locationInWindow[0]
    val y = locationInWindow[1]

    // Now these are bounds in window
    nodeBoundsRect.offset(x, y)

    return captureRegionToImage(testContext, nodeBoundsRect, view, windowToUse)
}

private fun findDialogWindowProviderInParent(view: View): DialogWindowProvider? {
    if (view is DialogWindowProvider) {
        return view
    }
    val parent = view.parent ?: return null
    if (parent is View) {
        return findDialogWindowProviderInParent(parent)
    }
    return null
}
