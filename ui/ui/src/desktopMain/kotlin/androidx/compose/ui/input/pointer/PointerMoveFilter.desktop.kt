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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * Modifier allowing to track pointer (i.e. mouse or trackpad) move events.
 *  @param onMove The callback invoked when pointer is moved inside a component,
 *  relative position inside a component is passed
 *  @param onEnter The callback invoked when pointer enters the component
 *  @param onExit The callback invoked when pointer leaves the component
 */
@ExperimentalComposeUiApi
fun Modifier.pointerMoveFilter(
    onMove: (position: Offset) -> Boolean = { false },
    onExit: () -> Boolean = { false },
    onEnter: () -> Boolean = { false },
): Modifier = pointerInput(onMove, onExit, onEnter) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val consumed = when (event.type) {
                PointerEventType.Move -> {
                    onMove(event.changes.first().position)
                }
                PointerEventType.Enter -> {
                    onEnter()
                }
                PointerEventType.Exit -> {
                    onExit()
                }
                else -> false
            }
            if (consumed) {
                event.changes.forEach { it.consume() }
            }
        }
    }
}
