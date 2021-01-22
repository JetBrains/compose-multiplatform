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

package androidx.compose.foundation.text

import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.LongPressDragObserver
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.gesture.longPressDragGestureFilter

// Touch selection
internal fun Modifier.longPressDragGestureFilter(
    observer: LongPressDragObserver,
    enabled: Boolean
) = if (enabled) this.then(longPressDragGestureFilter(observer)) else this

// Focus modifiers
internal fun Modifier.textFieldFocusModifier(
    enabled: Boolean,
    focusRequester: FocusRequester,
    interactionState: InteractionState?,
    onFocusChanged: (FocusState) -> Unit
): Modifier {
    if (!enabled) return this

    return this
        .focusRequester(focusRequester)
        .onFocusChanged(onFocusChanged)
        .focusable(interactionState = interactionState)
}

// Mouse
internal fun Modifier.mouseDragGestureFilter(
    dragObserver: DragObserver,
    enabled: Boolean
) = if (enabled) this.dragGestureFilter(dragObserver, startDragImmediately = true) else this