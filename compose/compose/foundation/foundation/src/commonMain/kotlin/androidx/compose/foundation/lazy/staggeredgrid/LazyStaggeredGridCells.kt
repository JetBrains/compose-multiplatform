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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * This class describes the count and the sizes of columns in vertical staggered grids,
 * or rows in horizontal staggered grids.
 */
@ExperimentalFoundationApi
@Stable
interface StaggeredGridCells {
    /**
     * Calculates the number of cells and their cross axis size based on
     * [availableSize] and [spacing].
     *
     * For example, in vertical grids, [spacing] is passed from the grid's [Arrangement.Horizontal].
     * The [Arrangement.Horizontal] will also be used to arrange items in a row if the grid is wider
     * than the calculated sum of columns.
     *
     * Note that the calculated cross axis sizes will be considered in an RTL-aware manner --
     * if the staggered grid is vertical and the layout direction is RTL, the first width in the
     * returned list will correspond to the rightmost column.
     *
     * @param availableSize available size on cross axis, e.g. width of [LazyVerticalStaggeredGrid].
     * @param spacing cross axis spacing, e.g. horizontal spacing for [LazyVerticalStaggeredGrid].
     * The spacing is passed from the corresponding [Arrangement] param of the lazy grid.
     */
    fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int>

    /**
     * Defines a grid with fixed number of rows or columns.
     *
     * For example, for the vertical [LazyVerticalStaggeredGrid] Fixed(3) would mean that
     * there are 3 columns 1/3 of the parent width.
     */
    class Fixed(private val count: Int) : StaggeredGridCells {
        init {
            require(count > 0)
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int
        ): List<Int> {
            return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
        }

        override fun hashCode(): Int {
            return -count // Different sign from Adaptive.
        }

        override fun equals(other: Any?): Boolean {
            return other is Fixed && count == other.count
        }
    }

    /**
     * Defines a grid with as many rows or columns as possible on the condition that
     * every cell has at least [minSize] space and all extra space distributed evenly.
     *
     * For example, for the vertical [LazyVerticalStaggeredGrid] Adaptive(20.dp) would mean that
     * there will be as many columns as possible and every column will be at least 20.dp
     * and all the columns will have equal width. If the screen is 88.dp wide then
     * there will be 4 columns 22.dp each.
     */
    class Adaptive(private val minSize: Dp) : StaggeredGridCells {
        init {
            require(minSize > 0.dp)
        }

        override fun Density.calculateCrossAxisCellSizes(
            availableSize: Int,
            spacing: Int
        ): List<Int> {
            val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1)
            return calculateCellsCrossAxisSizeImpl(availableSize, count, spacing)
        }

        override fun hashCode(): Int {
            return minSize.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            return other is Adaptive && minSize == other.minSize
        }
    }
}

private fun calculateCellsCrossAxisSizeImpl(
    gridSize: Int,
    slotCount: Int,
    spacing: Int
): List<Int> {
    val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
    val slotSize = gridSizeWithoutSpacing / slotCount
    val remainingPixels = gridSizeWithoutSpacing % slotCount
    return List(slotCount) {
        slotSize + if (it < remainingPixels) 1 else 0
    }
}