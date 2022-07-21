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

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset

/**
 * This exposes PointerInputEventData for testing purposes.
 */
@InternalComposeUiApi
class TestPointerInputEventData(
    val id: PointerId,
    val uptime: Long,
    val position: Offset,
    val down: Boolean
) {
    internal fun toPointerInputEventData() =
        PointerInputEventData(
            id,
            uptime,
            position,
            position,
            down,
            pressure = 1.0f,
            PointerType.Mouse
        )
}
