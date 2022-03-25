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
 * Strategies for painting shapes and paths on a canvas.
 *
 * See [Paint.style].
 */
@Immutable
@kotlin.jvm.JvmInline
value class PaintingStyle internal constructor(@Suppress("unused") private val value: Int) {

    companion object {

        /**
         * Apply the [Paint] to the inside of the shape. For example, when
         * applied to the [Canvas.drawCircle] call, this results in a disc
         * of the given size being painted.
         */
        val Fill = PaintingStyle(0)

        /**
         * Apply the [Paint] to the edge of the shape. For example, when
         * applied to the [Canvas.drawCircle] call, this results is a hoop
         * of the given size being painted. The line drawn on the edge will
         * be the width given by the [Paint.strokeWidth] property.
         */
        val Stroke = PaintingStyle(1)
    }

    override fun toString() = when (this) {
        Fill -> "Fill"
        Stroke -> "Stroke"
        else -> "Unknown"
    }
}