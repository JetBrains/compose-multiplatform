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

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.platform.debugInspectorInfo

@ExperimentalComposeUiApi
object PointerIconDefaults {
    val Default = pointerIconDefault
    val Crosshair = pointerIconCrosshair
    val Text = pointerIconText
    val Hand = pointerIconHand
}

/**
 * Represents a pointer icon to use in [Modifier.pointerHoverIcon]
 */
@Stable
interface PointerIcon

internal expect val pointerIconDefault: PointerIcon
internal expect val pointerIconCrosshair: PointerIcon
internal expect val pointerIconText: PointerIcon
internal expect val pointerIconHand: PointerIcon

internal interface PointerIconService {
    var current: PointerIcon
}

/**
 * Creates modifier which specifies desired pointer icon when the cursor is over the modified
 * element.
 *
 * @sample androidx.compose.ui.samples.PointerIconSample
 *
 * @param icon The icon to set
 * @param overrideDescendants when false (by default) descendants are able to set their own pointer
 * icon. if true it overrides descendants' icon.
 */
@Stable
fun Modifier.pointerHoverIcon(icon: PointerIcon, overrideDescendants: Boolean = false) =
    composed(
        inspectorInfo = debugInspectorInfo {
            name = "pointerHoverIcon"
            properties["icon"] = icon
            properties["overrideDescendants"] = overrideDescendants
        }
    ) {
        val pointerIconService = LocalPointerIconService.current
        if (pointerIconService == null) {
            Modifier
        } else {
            this.pointerInput(icon, overrideDescendants) {
                awaitPointerEventScope {
                    while (true) {
                        val pass = if (overrideDescendants)
                            PointerEventPass.Main
                        else
                            PointerEventPass.Initial
                        val event = awaitPointerEvent(pass)
                        val isOutsideRelease = event.type == PointerEventType.Release &&
                            event.changes[0].isOutOfBounds(size, Size.Zero)
                        if (event.type != PointerEventType.Exit && !isOutsideRelease) {
                            pointerIconService.current = icon
                        }
                    }
                }
            }
        }
    }