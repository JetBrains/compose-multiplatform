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
 * Defines how a list of points is interpreted when drawing a set of triangles.
 *
 * Used by [Canvas.drawVertices].
 */
@Immutable
@kotlin.jvm.JvmInline
value class VertexMode internal constructor(@Suppress("unused") private val value: Int) {

    companion object {
        /**
         * Draw each sequence of three points as the vertices of a triangle.
         */
        val Triangles = VertexMode(0)

        /**
         *  Draw each sliding window of three points as the vertices of a triangle.
         */
        val TriangleStrip = VertexMode(1)

        /**
         * Draw the first point and each sliding window of two points as the vertices of a triangle.
         */
        val TriangleFan = VertexMode(2)
    }

    override fun toString() = when (this) {
        Triangles -> "Triangles"
        TriangleStrip -> "TriangleStrip"
        TriangleFan -> "TriangleFan"
        else -> "Unknown"
    }
}