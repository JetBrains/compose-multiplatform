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
 * Styles to use for line joins.
 *
 * This only affects line joins for polygons drawn by [Canvas.drawPath] and
 * rectangles, not points drawn as lines with [Canvas.drawPoints].
 * See [Paint.strokeJoin].
 */
@Immutable
@kotlin.jvm.JvmInline
value class StrokeJoin internal constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Joins between line segments form sharp corners.
         */
        val Miter = StrokeJoin(0)

        /**
         * Joins between line segments are semi-circular.
         */
        val Round = StrokeJoin(1)

        /**
         * Joins between line segments connect the corners of the butt ends of the
         * line segments to give a beveled appearance.
         */
        val Bevel = StrokeJoin(2)
    }

    override fun toString() = when (this) {
        Miter -> "Miter"
        Round -> "Round"
        Bevel -> "Bevel"
        else -> "Unknown"
    }
}