/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * The DSL implementation of a lazy grid layout. It composes only visible rows of the grid.
 * This API is not stable, please consider using stable components like [LazyColumnFor] and [Row]
 * to achieve the same result.
 *
 * @param columns a fixed number of columns of the grid
 * @param modifier the modifier to apply to this layout
 * @param contentPadding specify a padding around the whole content
 * @param content the [LazyListScope] which describes the content
 */
@Composable
internal fun LazyGrid(
    columns: Int,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit
) {
    val scope = LazyListScopeImpl()
    scope.apply(content)

    val rows = (scope.totalSize + columns - 1) / columns
    LazyList(
        itemsCount = rows,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        isVertical = true,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        reverseLayout = false
    ) { rowIndex ->
        {
            GridRow {
                for (columnIndex in 0 until columns) {
                    val itemIndex = rowIndex * columns + columnIndex
                    if (itemIndex < scope.totalSize) {
                        scope.contentFor(itemIndex, this).invoke()
                    } else {
                        Spacer(Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun GridRow(
    content: @Composable () -> Unit
) {
    // TODO: Implement customisable column widths.
    Layout(
        content = content
    ) { measurables, constraints ->

        // TODO: Avoid int rounding to fill all the width pixels.
        val itemConstraint = Constraints.fixedWidth(constraints.maxWidth / measurables.size)
        var maxItemHeight = 0
        val placeables = measurables.map { measurable ->
            measurable.measure(itemConstraint)
                .also {
                    if (it.height > maxItemHeight) {
                        maxItemHeight = it.height
                    }
                }
        }

        layout(constraints.maxWidth, maxItemHeight) {
            var currentXPosition = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x = currentXPosition, y = 0)
                currentXPosition += placeable.width
            }
        }
    }
}
