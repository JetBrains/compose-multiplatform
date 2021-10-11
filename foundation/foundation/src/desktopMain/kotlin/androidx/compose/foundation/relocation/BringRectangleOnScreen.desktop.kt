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

package androidx.compose.foundation.relocation

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect as ComposeRect

/**
 * Platform specific internal API to bring a rectangle into view.
 */
internal actual class BringRectangleOnScreenRequester {
    @Suppress("UNUSED_PARAMETER")
    actual fun bringRectangleOnScreen(rect: ComposeRect) {
        // TODO(b/203204124): Implement this if desktop has a
        //  concept similar to Android's View.requestRectangleOnScreen.
    }
}

/**
 * Companion Modifier to [BringRectangleOnScreenRequester].
 */
@Suppress("UNUSED_PARAMETER")
internal actual fun Modifier.bringRectangleOnScreenRequester(
    bringRectangleOnScreenRequester: BringRectangleOnScreenRequester
): Modifier = this