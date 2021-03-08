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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastAny

/**  A set of vertex data used by [Canvas.drawVertices]. */
class Vertices(
    val vertexMode: VertexMode,
    positions: List<Offset>,
    textureCoordinates: List<Offset>,
    colors: List<Color>,
    indices: List<Int>
) /*extends NativeFieldWrapperClass2*/ {

    val positions: FloatArray
    val textureCoordinates: FloatArray
    val colors: IntArray
    val indices: ShortArray

    init {
        val outOfBounds: (Int) -> Boolean = { it < 0 || it >= positions.size }
        if (textureCoordinates.size != positions.size)
            throw IllegalArgumentException("positions and textureCoordinates lengths must match.")
        if (colors.size != positions.size)
            throw IllegalArgumentException("positions and colors lengths must match.")
        if (indices.fastAny(outOfBounds))
            throw IllegalArgumentException(
                "indices values must be valid indices " +
                    "in the positions list."
            )

        this.positions = encodePointList(positions)
        this.textureCoordinates = encodePointList(textureCoordinates)
        this.colors = encodeColorList(colors)
        this.indices = ShortArray(indices.size) {
            i ->
            indices[i].toShort()
        }
    }

    private fun encodeColorList(colors: List<Color>): IntArray {
        return IntArray(colors.size) {
            i ->
            colors[i].toArgb()
        }
    }

    private fun encodePointList(points: List<Offset>): FloatArray {
        return FloatArray(points.size * 2) { i ->
            val pointIndex = i / 2
            val point = points[pointIndex]
            if (i % 2 == 0) {
                point.x
            } else {
                point.y
            }
        }
    }
}