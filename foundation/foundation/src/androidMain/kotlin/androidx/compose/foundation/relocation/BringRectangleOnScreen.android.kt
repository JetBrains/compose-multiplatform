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

package androidx.compose.foundation.relocation

import android.view.View
import androidx.compose.ui.Modifier
import android.graphics.Rect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.geometry.Rect as ComposeRect

/**
 * Platform specific internal API to bring a rectangle into view.
 */
internal actual class BringRectangleOnScreenRequester {
    internal var view: View? = null
    actual fun bringRectangleOnScreen(rect: ComposeRect) {
        view?.requestRectangleOnScreen(rect.toRect(), false)
    }
}

/**
 * Companion Modifier to [BringRectangleOnScreenRequester].
 */
internal actual fun Modifier.bringRectangleOnScreenRequester(
    bringRectangleOnScreenRequester: BringRectangleOnScreenRequester
): Modifier = composed(
    debugInspectorInfo {
        name = "bringRectangleOnScreenRequester"
        properties["bringRectangleOnScreenRequester"] = bringRectangleOnScreenRequester
    }
) {
    val view = LocalView.current
    DisposableEffect(view) {
        bringRectangleOnScreenRequester.view = view
        onDispose {
            bringRectangleOnScreenRequester.view = null
        }
    }
    Modifier
}

private fun ComposeRect.toRect(): Rect {
    return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
}