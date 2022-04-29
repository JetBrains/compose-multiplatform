/*
 * Copyright 2022 The Android Open Source Project
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

import android.graphics.Rect as AndroidRect
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberDefaultBringIntoViewParent(): BringIntoViewParent {
    val view = LocalView.current
    return remember(view) { AndroidBringIntoViewParent(view) }
}

/**
 * A [BringIntoViewParent] that delegates to the [View] hosting the composition.
 */
private class AndroidBringIntoViewParent(private val view: View) : BringIntoViewParent {
    override suspend fun bringChildIntoView(rect: Rect, childCoordinates: LayoutCoordinates) {
        val childOffset = childCoordinates.positionInRoot()
        val rootRect = rect.translate(childOffset)
        view.requestRectangleOnScreen(rootRect.toRect(), false)
    }
}

private fun Rect.toRect() = AndroidRect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())