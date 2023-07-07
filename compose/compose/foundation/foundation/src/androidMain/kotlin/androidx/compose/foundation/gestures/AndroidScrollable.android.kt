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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.fastFold
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
internal actual fun platformScrollConfig(): ScrollConfig = AndroidConfig

private object AndroidConfig : ScrollConfig {
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        // 64 dp value is taken from ViewConfiguration.java, replace with better solution
        return event.changes.fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta } * -64.dp.toPx()
    }
}