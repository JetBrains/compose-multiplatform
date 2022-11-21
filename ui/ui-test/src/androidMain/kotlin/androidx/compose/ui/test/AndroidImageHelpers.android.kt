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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.android.captureRegionToImage
import androidx.compose.ui.test.android.forceRedraw
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt

/**
 * Captures the underlying semantics node's surface into bitmap. This can be used to capture
 * nodes in a normal composable, a dialog if API >=28 and in a Popup. Note that the mechanism
 * used to capture the bitmap from a Popup is not the same as from a normal composable, since
 * a PopUp is in a different window.
 *
 * @throws IllegalArgumentException if we attempt to capture a bitmap of a dialog before API 28.
 */
@OptIn(ExperimentalTestApi::class)
@RequiresApi(Build.VERSION_CODES.O)
fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
    val node = fetchSemanticsNode("Failed to capture a node to bitmap.")
    // Validate we are in popup
    val popupParentMaybe = node.findClosestParentNode(includeSelf = true) {
        it.config.contains(SemanticsProperties.IsPopup)
    }
    if (popupParentMaybe != null) {
        return processMultiWindowScreenshot(node, testContext)
    }

    val view = (node.root as ViewRootForTest).view

    // If we are in dialog use its window to capture the bitmap
    val dialogParentNodeMaybe = node.findClosestParentNode(includeSelf = true) {
        it.config.contains(SemanticsProperties.IsDialog)
    }
    var dialogWindow: Window? = null
    if (dialogParentNodeMaybe != null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // TODO(b/163023027)
            throw IllegalArgumentException("Cannot currently capture dialogs on API lower than 28!")
        }

        dialogWindow = findDialogWindowProviderInParent(view)?.window
            ?: throw IllegalArgumentException(
                "Could not find a dialog window provider to capture its bitmap"
            )
    }

    val windowToUse = dialogWindow ?: view.context.getActivityWindow()

    val nodeBounds = node.boundsInRoot
    val nodeBoundsRect = Rect(
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

    return windowToUse.captureRegionToImage(testContext, nodeBoundsRect)
}

@ExperimentalTestApi
@RequiresApi(Build.VERSION_CODES.O)
private fun processMultiWindowScreenshot(
    node: SemanticsNode,
    testContext: TestContext
): ImageBitmap {

    (node.root as ViewRootForTest).view.forceRedraw(testContext)

    val nodePositionInScreen = findNodePosition(node)
    val nodeBoundsInRoot = node.boundsInRoot

    val combinedBitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()

    val finalBitmap = Bitmap.createBitmap(
        combinedBitmap,
        (nodePositionInScreen.x + nodeBoundsInRoot.left).roundToInt(),
        (nodePositionInScreen.y + nodeBoundsInRoot.top).roundToInt(),
        nodeBoundsInRoot.width.roundToInt(),
        nodeBoundsInRoot.height.roundToInt()
    )
    return finalBitmap.asImageBitmap()
}

@OptIn(InternalTestApi::class)
private fun findNodePosition(
    node: SemanticsNode
): Offset {
    val view = (node.root as ViewRootForTest).view
    val locationOnScreen = intArrayOf(0, 0)
    view.getLocationOnScreen(locationOnScreen)
    val x = locationOnScreen[0]
    val y = locationOnScreen[1]

    return Offset(x.toFloat(), y.toFloat())
}

internal fun findDialogWindowProviderInParent(view: View): DialogWindowProvider? {
    if (view is DialogWindowProvider) {
        return view
    }
    val parent = view.parent ?: return null
    if (parent is View) {
        return findDialogWindowProviderInParent(parent)
    }
    return null
}

private fun Context.getActivityWindow(): Window {
    fun Context.getActivity(): Activity {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> throw IllegalStateException(
                "Context is not an Activity context, but a ${javaClass.simpleName} context. " +
                    "An Activity context is required to get a Window instance"
            )
        }
    }
    return getActivity().window
}