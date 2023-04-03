/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.unit.IntSize

/**
 * Modifier to catch pointer above platform interop view, like UIKitView.
 */
internal class InteropViewCatchPointerModifier : PointerInputFilter(), PointerInputModifier {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        // All touches for now is handled in iOS pointInside.
        // We may add implementation for Web and Desktop later.
    }

    override fun onCancel() {
        // All touches for now is handled in iOS pointInside.
        // We may add implementation for Web and Desktop later.
    }

    override val pointerInputFilter: PointerInputFilter = this
}
