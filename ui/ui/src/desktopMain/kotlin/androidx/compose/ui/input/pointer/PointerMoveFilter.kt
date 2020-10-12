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

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.PointerInputModifierImpl
import androidx.compose.ui.unit.IntSize

fun Modifier.pointerMoveFilter(
    onMove: (position: Offset) -> Boolean = { false },
    onExit: () -> Boolean = { false },
    onEnter: () -> Boolean
): Modifier = composed {
    val filter = remember(::PointerMoveEventFilter)
    filter.onEnterHandler = onEnter
    filter.onExitHandler = onExit
    filter.onMoveHandler = onMove
    PointerInputModifierImpl(filter)
}

internal class PointerMoveEventFilter : PointerInputFilter() {
    lateinit var onEnterHandler: () -> Boolean
    lateinit var onExitHandler: () -> Boolean
    lateinit var onMoveHandler: (position: Offset) -> Boolean

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) = Unit

    override fun onCancel() = Unit
}