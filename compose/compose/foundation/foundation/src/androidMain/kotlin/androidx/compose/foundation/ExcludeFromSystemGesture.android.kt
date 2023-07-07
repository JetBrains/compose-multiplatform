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

package androidx.compose.foundation

import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Excludes the layout rectangle from the system gesture.
 *
 * @see View.setSystemGestureExclusionRects
 */
@Deprecated("Use systemGestureExclusion", replaceWith = ReplaceWith("systemGestureExclusion"))
fun Modifier.excludeFromSystemGesture() = systemGestureExclusion()

/**
 * Excludes a rectangle within the local layout coordinates from the system gesture.
 * After layout, [exclusion] is called to determine the [Rect] to exclude from the system
 * gesture area.
 *
 * The [LayoutCoordinates] of the [Modifier]'s location in the layout is passed as passed as
 * [exclusion]'s parameter.
 *
 * @see View.setSystemGestureExclusionRects
 */
@Deprecated("Use systemGestureExclusion", replaceWith = ReplaceWith("systemGestureExclusion"))
fun Modifier.excludeFromSystemGesture(exclusion: (LayoutCoordinates) -> Rect) =
    systemGestureExclusion(exclusion)
