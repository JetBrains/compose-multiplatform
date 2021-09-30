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

package androidx.compose.foundation.text

internal actual fun StringBuilder.appendCodePointX(codePoint: Int): StringBuilder =
    TODO("Implement native StringBuilder.appendCodePointX")
    // TODO: there is an appendCodePoint in K/N stdlib, but it is internal.
    // this.appendCodePoint(codePoint)

internal actual fun String.findPrecedingBreak(index: Int): Int {
    TODO("Implement native String.findPrecedingBreak")
/*
    val it = BreakIterator.getCharacterInstance()
    it.setText(this)
    return it.preceding(index)
*/
}

internal actual fun String.findFollowingBreak(index: Int): Int {
    TODO("Implement native String.findFollowingBreak")
/*
    val it = BreakIterator.getCharacterInstance()
    it.setText(this)
    return it.following(index)
*/
}
