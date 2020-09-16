/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.demos.gestures

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

internal val DefaultBackgroundColor = Color(0xffffffff)
internal val PressedColor = Color(0x1f000000)
internal val BorderColor = Color(0x1f000000)

internal val Red = Color(0xFFf44336)
internal val Pink = Color(0xFFe91e63)
internal val Purple = Color(0xFF9c27b0)
internal val DeepPurple = Color(0xFF673ab7)
internal val Indigo = Color(0xFF3f51b5)
internal val Blue = Color(0xFF2196f3)
internal val LightBlue = Color(0xFF03a9f4)
internal val Cyan = Color(0xFF00bcd4)
internal val Teal = Color(0xFF009688)
internal val Green = Color(0xFF4caf50)
internal val LightGreen = Color(0xFF8bc34a)
internal val Lime = Color(0xFFcddc39)
internal val Yellow = Color(0xFFffeb3b)
internal val Amber = Color(0xFFffc107)
internal val Orange = Color(0xFFff9800)
internal val DeepOrange = Color(0xFFff5722)
internal val Brown = Color(0xFF795548)
internal val Grey = Color(0xFF9e9e9e)
internal val BlueGrey = Color(0xFF607d8b)

internal val Colors = listOf(
    Red,
    Pink,
    Purple,
    DeepPurple,
    Indigo,
    Blue,
    LightBlue,
    Cyan,
    Teal,
    Green,
    LightGreen,
    Lime,
    Yellow,
    Amber,
    Orange,
    DeepOrange,
    Brown,
    Grey,
    BlueGrey
)

internal fun Color.anotherRandomColor() = Colors.random(this)

internal fun Color.next() = Colors.inOrder(this, true)
internal fun Color.prev() = Colors.inOrder(this, false)

private fun List<Color>.random(exclude: Color?): Color {
    val excludeIndex = indexOf(exclude)

    val max = size - if (excludeIndex >= 0) 1 else 0

    val random = Random.nextInt(max).run {
        if (excludeIndex >= 0 && this >= excludeIndex) {
            this + 1
        } else {
            this
        }
    }

    return this[random]
}

private fun List<Color>.inOrder(current: Color?, forward: Boolean): Color {
    val currentIndex = indexOf(current)

    val next =
        if (forward) {
            if (currentIndex == -1) {
                0
            } else {
                (currentIndex + 1) % size
            }
        } else {
            if (currentIndex == -1) {
                size - 1
            } else {
                (currentIndex - 1 + size) % size
            }
        }

    return this[next]
}
