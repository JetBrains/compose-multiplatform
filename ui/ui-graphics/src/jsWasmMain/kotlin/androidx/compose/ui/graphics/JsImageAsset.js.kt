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

package androidx.compose.ui.graphics

internal actual fun ByteArray.putBytesInto(array: IntArray, offset: Int, length: Int) {
   if (offset < 0 || length < 0 || offset + length > array.size) {
      throw IndexOutOfBoundsException("Invalid offset or length")
   }

   if (length * 4 > this.size) {
      throw IndexOutOfBoundsException("ByteArray not big enough to hold the requested number of integers")
   }

   for (i in 0 until length) {
      val byteIndex = i * 4
      array[offset + i] = (this[byteIndex].toInt() and 0xFF) or
          ((this[byteIndex + 1].toInt() and 0xFF) shl 8) or
          ((this[byteIndex + 2].toInt() and 0xFF) shl 16) or
          ((this[byteIndex + 3].toInt() and 0xFF) shl 24)
   }
}

