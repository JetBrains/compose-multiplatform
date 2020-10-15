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

package androidx.compose.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.enforce
import androidx.compose.ui.unit.hasFixedHeight
import androidx.compose.ui.unit.hasFixedWidth
import kotlin.math.max

/**
 * A Container Box implementation used for selection children and handle layout
 */
@Composable
internal fun SimpleContainer(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    children: @Composable () -> Unit
) {
    Layout(children, modifier) { measurables, incomingConstraints ->
        val containerConstraints = Constraints()
            .copy(
                width?.toIntPx() ?: 0,
                width?.toIntPx() ?: Constraints.Infinity,
                height?.toIntPx() ?: 0,
                height?.toIntPx() ?: Constraints.Infinity
            )
            .enforce(incomingConstraints)
        val childConstraints = containerConstraints.copy(minWidth = 0, minHeight = 0)
        var placeable: Placeable? = null
        val containerWidth = if (
            containerConstraints.hasFixedWidth
        ) {
            containerConstraints.maxWidth
        } else {
            placeable = measurables.firstOrNull()?.measure(childConstraints)
            max((placeable?.width ?: 0), containerConstraints.minWidth)
        }
        val containerHeight = if (
            containerConstraints.hasFixedHeight
        ) {
            containerConstraints.maxHeight
        } else {
            if (placeable == null) {
                placeable = measurables.firstOrNull()?.measure(childConstraints)
            }
            max((placeable?.height ?: 0), containerConstraints.minHeight)
        }
        layout(containerWidth, containerHeight) {
            val p = placeable ?: measurables.firstOrNull()?.measure(childConstraints)
            p?.let {
                val position = Alignment.Center.align(
                    IntSize(
                        containerWidth - it.width,
                        containerHeight - it.height
                    )
                )
                it.placeRelative(
                    position.x,
                    position.y
                )
            }
        }
    }
}
