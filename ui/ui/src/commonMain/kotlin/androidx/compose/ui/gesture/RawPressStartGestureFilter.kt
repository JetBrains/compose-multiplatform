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

package androidx.compose.ui.gesture

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach

/**
 * Reacts if the first pointer input change it sees is an unconsumed down change, and if it reacts,
 * consumes all further down changes.
 *
 * This GestureDetector is not generally intended to be used directly, but is instead intended to be
 * used as a building block to create more complex GestureDetectors.
 *
 * This GestureDetector is a bit more experimental then the other GestureDetectors (the number and
 * types of GestureDetectors is still very much a work in progress) and is intended to be a
 * generically useful building block for more complicated GestureDetectors.
 *
 * The theory is that this GestureDetector can be reused in PressIndicatorGestureDetector, and there
 * could be a corresponding RawPressReleasedGestureDetector.
 *
 * @param onPressStart Called when the first pointer "presses" on the GestureDetector.  [Offset]
 * is the position of that first pointer on press.
 * @param enabled If false, this GestureDetector will effectively act as if it is not in the
 * hierarchy.
 * @param executionPass The [PointerEventPass] during which this GestureDetector will attempt to
 * react to and consume down changes.  Defaults to [PointerEventPass.Main].
 */
fun Modifier.rawPressStartGestureFilter(
    onPressStart: (Offset) -> Unit,
    enabled: Boolean = false,
    executionPass: PointerEventPass = PointerEventPass.Main
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "rawPressStartGestureFilter"
        properties["onPressStart"] = onPressStart
        properties["enabled"] = enabled
        properties["executionPass"] = executionPass
    }
) {
    val filter = remember { RawPressStartGestureFilter() }
    filter.onPressStart = onPressStart
    filter.setEnabled(enabled = enabled)
    filter.setExecutionPass(executionPass)
    PointerInputModifierImpl(filter)
}

internal class RawPressStartGestureFilter : PointerInputFilter() {

    lateinit var onPressStart: (Offset) -> Unit
    private var enabled: Boolean = true
    private var executionPass = PointerEventPass.Initial

    private var active = false

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == executionPass) {
            if (enabled && changes.all { it.changedToDown() }) {
                // If we have not yet started and all of the changes changed to down, we are
                // starting.
                active = true
                onPressStart(changes.first().position)
            } else if (changes.all { it.changedToUp() }) {
                // If we have started and all of the changes changed to up, we are stopping.
                active = false
            }

            if (active) {
                // If we have started, we should consume the down change on all changes.
                changes.fastForEach {
                    it.consumeDownChange()
                }
            }
        }
    }

    override fun onCancel() {
        active = false
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        // Whenever we are disabled, we can just go ahead and become inactive (which is the state we
        // should be in if we are to pretend that we aren't in the hierarchy.
        if (!enabled) {
            onCancel()
        }
    }

    fun setExecutionPass(executionPass: PointerEventPass) {
        this.executionPass = executionPass
    }
}