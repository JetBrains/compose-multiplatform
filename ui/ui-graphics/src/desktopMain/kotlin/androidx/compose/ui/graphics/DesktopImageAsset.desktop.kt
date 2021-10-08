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
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Create an [ImageBitmap] from the given [Bitmap]. Note this does
 * not create a copy of the original [Bitmap] and changes to it
 * will modify the returned [ImageBitmap]
 */
@Deprecated("Use asComposeImageBitmap", replaceWith = ReplaceWith("asComposeImageBitmap()"))
fun Bitmap.asImageBitmap(): ImageBitmap = asComposeImageBitmap()

/**
 * Create an [ImageBitmap] from the given [Image].
 */
@Deprecated("Use toComposeImageBitmap", replaceWith = ReplaceWith("toComposeImageBitmap()"))
fun Image.asImageBitmap(): ImageBitmap = toComposeImageBitmap()

/**
 * @Throws UnsupportedOperationException if this [ImageBitmap] is not backed by an
 * org.jetbrains.skia.Image
 */
@Deprecated("Use asSkiaBitmap()", replaceWith = ReplaceWith("asSkiaBitmap()"))
fun ImageBitmap.asDesktopBitmap(): Bitmap = asSkiaBitmap()

internal actual fun ByteArray.putBytesInto(array: IntArray, offset: Int, length: Int) {
    ByteBuffer.wrap(this)
        .order(ByteOrder.LITTLE_ENDIAN) // to return ARGB
        .asIntBuffer()
        .get(array, offset, length)
}
