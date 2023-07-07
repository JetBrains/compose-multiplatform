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

package androidx.testutils

import android.util.Pair

class WeightStyleFont {
    fun getWideCharacter(weight: Int, italic: Boolean): Char {
        return when (weight to italic) {
            100 to true -> 0x61.toChar()
            100 to false -> 0x62.toChar()
            200 to true -> 0x63.toChar()
            200 to false -> 0x64.toChar()
            300 to true -> 0x65.toChar()
            300 to false -> 0x66.toChar()
            400 to true -> 0x67.toChar()
            400 to false -> 0x68.toChar()
            500 to true -> 0x69.toChar()
            500 to false -> 0x6A.toChar()
            600 to true -> 0x6B.toChar()
            600 to false -> 0x6C.toChar()
            700 to true -> 0x6D.toChar()
            700 to false -> 0x6E.toChar()
            800 to true -> 0x6F.toChar()
            800 to false -> 0x70.toChar()
            900 to true -> 0x71.toChar()
            900 to false -> 0x72.toChar()
            else -> throw RuntimeException("Unknown weight and italic ($weight, $italic)")
        }
    }

    companion object TtxWeights {
        const val Narrow = 1000
        const val Wide = 3000
        const val SkinnyChar = 0x73.toChar()
    }
}

private infix fun Int.to(italic: Boolean): Pair<Int, Boolean> = Pair(this, italic)
