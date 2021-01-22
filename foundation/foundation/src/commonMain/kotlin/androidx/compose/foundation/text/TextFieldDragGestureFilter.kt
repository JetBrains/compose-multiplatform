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

package androidx.compose.foundation.text

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Helper composable for tracking drag position.
 */
@Suppress("ModifierInspectorInfo")
internal fun Modifier.dragPositionGestureFilter(
    onPress: (Offset) -> Unit,
    onRelease: (Offset) -> Unit,
    onTap: () -> Unit,
    interactionState: InteractionState?,
    enabled: Boolean = true
): Modifier = if (enabled) composed {
    val tracker = remember { DragEventTracker() }
    // TODO(shepshapard): PressIndicator doesn't seem to be the right thing to use here.  It
    //  actually may be functionally correct, but might mostly suggest that it should not
    //  actually be called PressIndicator, but instead something else.
    DisposableEffect(interactionState) {
        onDispose {
            interactionState?.removeInteraction(Interaction.Pressed)
        }
    }
    pointerInput {
        detectTapGestures(
            onTap = { onTap() },
            onPress = {
                interactionState?.addInteraction(Interaction.Pressed, it)
                tracker.init(it)
                onPress(it)
                val success = tryAwaitRelease()
                interactionState?.removeInteraction(Interaction.Pressed)
                if (success) onRelease(tracker.getPosition())
            }
        )
    }
        .dragGestureFilter(
            dragObserver = object :
                DragObserver {
                override fun onDrag(dragDistance: Offset): Offset {
                    tracker.onDrag(dragDistance)
                    return Offset.Zero
                }
            }
        )
} else this

/**
 * Helper class for tracking dragging event.
 */
internal class DragEventTracker {
    private var origin = Offset.Zero
    private var distance = Offset.Zero

    /**
     * Restart the tracking from given origin.
     *
     * @param origin The origin of the drag gesture.
     */
    fun init(origin: Offset) {
        this.origin = origin
    }

    /**
     * Pass distance parameter called by DragGestureDetector$onDrag callback
     *
     * @param distance The distance from the origin of the drag origin.
     */
    fun onDrag(distance: Offset) {
        this.distance = distance
    }

    /**
     * Returns the current position.
     *
     * @return The position of the current drag point.
     */
    fun getPosition(): Offset {
        return origin + distance
    }
}
