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

package androidx.compose.foundation.text

private const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000

internal actual fun StringBuilder.appendCodePointX(codePoint: Int): StringBuilder = apply {
    appendCodePoint(codePoint)
}

// Copy from https://github.com/JetBrains/kotlin/blob/7cd306950aad852e006715067435a4bbd9cd40d2/kotlin-native/runtime/src/main/kotlin/generated/_StringUppercase.kt#L26
private fun StringBuilder.appendCodePoint(codePoint: Int) {
    if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
        append(codePoint.toChar())
    } else {
        append(Char.MIN_HIGH_SURROGATE + ((codePoint - 0x10000) shr 10))
        append(Char.MIN_LOW_SURROGATE + (codePoint and 0x3ff))
    }
}
