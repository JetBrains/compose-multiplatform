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

import java.awt.Component
import java.awt.event.MouseWheelEvent

private val EventComponent = object : Component() {}

internal fun awtWheelEvent(isScrollByPages: Boolean = false) = MouseWheelEvent(
    EventComponent,
    MouseWheelEvent.MOUSE_WHEEL,
    0,
    0,
    0,
    0,
    0,
    false,
    if (isScrollByPages) {
        MouseWheelEvent.WHEEL_BLOCK_SCROLL
    } else {
        MouseWheelEvent.WHEEL_UNIT_SCROLL
    },
    1,
    0
)