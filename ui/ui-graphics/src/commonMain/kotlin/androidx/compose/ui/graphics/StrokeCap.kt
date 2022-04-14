/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.compose.runtime.Immutable

/**
 * Styles to use for line endings.
 * See [Paint.strokeCap].
 */
@Immutable
@kotlin.jvm.JvmInline
value class StrokeCap internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Begin and end contours with a flat edge and no extension.
         */
        val Butt = StrokeCap(0)

        /**
         * Begin and end contours with a semi-circle extension.
         */
        val Round = StrokeCap(1)

        /**
         * Begin and end contours with a half square extension. This is
         * similar to extending each contour by half the stroke width (as
         * given by [Paint.strokeWidth]).
         */
        val Square = StrokeCap(2)
    }

    override fun toString() = when (this) {
        Butt -> "Butt"
        Round -> "Round"
        Square -> "Square"
        else -> "Unknown"
    }
}
