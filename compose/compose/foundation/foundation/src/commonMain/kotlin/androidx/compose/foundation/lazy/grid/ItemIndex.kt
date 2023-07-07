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

package androidx.compose.foundation.lazy.grid

/**
 * Represents a line index in the lazy grid.
 */
@Suppress("NOTHING_TO_INLINE")
@kotlin.jvm.JvmInline
internal value class LineIndex(val value: Int) {
    inline operator fun inc(): LineIndex = LineIndex(value + 1)
    inline operator fun dec(): LineIndex = LineIndex(value - 1)
    inline operator fun plus(i: Int): LineIndex = LineIndex(value + i)
    inline operator fun minus(i: Int): LineIndex = LineIndex(value - i)
    inline operator fun minus(i: LineIndex): LineIndex = LineIndex(value - i.value)
    inline operator fun compareTo(other: LineIndex): Int = value - other.value
}

/**
 * Represents an item index in the lazy grid.
 */
@Suppress("NOTHING_TO_INLINE")
@kotlin.jvm.JvmInline
internal value class ItemIndex(val value: Int) {
    inline operator fun inc(): ItemIndex = ItemIndex(value + 1)
    inline operator fun dec(): ItemIndex = ItemIndex(value - 1)
    inline operator fun plus(i: Int): ItemIndex = ItemIndex(value + i)
    inline operator fun minus(i: Int): ItemIndex = ItemIndex(value - i)
    inline operator fun minus(i: ItemIndex): ItemIndex = ItemIndex(value - i.value)
    inline operator fun compareTo(other: ItemIndex): Int = value - other.value
}
