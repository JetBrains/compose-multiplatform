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

package androidx.compose.ui.graphics

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.addressOf
import platform.posix.memcpy

internal actual fun ByteArray.putBytesInto(array: IntArray, offset: Int, length: Int) {
    this.usePinned { bytes ->
        array.usePinned { ints ->
            // Assuming little endian.
            memcpy(ints.addressOf(offset), bytes.addressOf(0), (length*4).toULong())
        }
    }
}

