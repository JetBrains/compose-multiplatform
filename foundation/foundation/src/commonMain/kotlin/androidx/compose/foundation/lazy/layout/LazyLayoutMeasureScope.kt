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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * The receiver scope of a [LazyLayout]'s measure lambda. The return value of the
 * measure lambda is [MeasureResult], which should be returned by [layout].
 *
 * Main difference from the regular flow of writing any custom layout is that you have a new
 * function [measure] which accepts item index and constraints, composes the item based and then
 * measures all the layouts emitted in the item content block.
 */
@Stable
@ExperimentalFoundationApi
sealed interface LazyLayoutMeasureScope : MeasureScope {
    /**
     * Subcompose and measure the item of lazy layout.
     *
     * @param index the item index. Should be no larger that [LazyLayoutItemProvider.itemCount].
     * @param constraints [Constraints] to measure the children emitted into an item content
     * composable specified via [LazyLayoutItemProvider.Item].
     *
     * @return Array of [Placeable]s. Note that if you emitted multiple children into the item
     * composable you will receive multiple placeables, each of them will be measured with
     * the passed [constraints].
     */
    fun measure(index: Int, constraints: Constraints): Array<Placeable>

    // Below overrides added to work around https://youtrack.jetbrains.com/issue/KT-51672
    // Must be kept in sync until resolved.

    @Stable
    override fun TextUnit.toDp(): Dp {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return Dp(value * fontScale)
    }

    @Stable
    override fun Int.toDp(): Dp = (this / density).dp

    @Stable
    override fun Float.toDp(): Dp = (this / density).dp

    @Stable
    override fun Float.toSp(): TextUnit = (this / (fontScale * density)).sp

    @Stable
    override fun Int.toSp(): TextUnit = (this / (fontScale * density)).sp

    @Stable
    override fun Dp.toSp(): TextUnit = (value / fontScale).sp

    @Stable
    override fun DpSize.toSize(): Size = if (isSpecified) {
        Size(width.toPx(), height.toPx())
    } else {
        Size.Unspecified
    }

    @Stable
    override fun Size.toDpSize(): DpSize = if (isSpecified) {
        DpSize(width.toDp(), height.toDp())
    } else {
        DpSize.Unspecified
    }
}

@ExperimentalFoundationApi
internal class LazyLayoutMeasureScopeImpl internal constructor(
    private val itemContentFactory: LazyLayoutItemContentFactory,
    private val subcomposeMeasureScope: SubcomposeMeasureScope
) : LazyLayoutMeasureScope, MeasureScope by subcomposeMeasureScope {

    /**
     * A cache of the previously composed items. It allows us to support [get]
     * re-executions with the same index during the same measure pass.
     */
    private val placeablesCache = hashMapOf<Int, Array<Placeable>>()

    override fun measure(index: Int, constraints: Constraints): Array<Placeable> {
        val cachedPlaceable = placeablesCache[index]
        return if (cachedPlaceable != null) {
            cachedPlaceable
        } else {
            val key = itemContentFactory.itemProvider().getKey(index)
            val itemContent = itemContentFactory.getContent(index, key)
            val measurables = subcomposeMeasureScope.subcompose(key, itemContent)
            Array(measurables.size) { i ->
                measurables[i].measure(constraints)
            }.also {
                placeablesCache[index] = it
            }
        }
    }

    /**
     * Below overrides added to work around https://youtrack.jetbrains.com/issue/KT-51672
     */

    override fun TextUnit.toDp(): Dp = with(subcomposeMeasureScope) { toDp() }

    override fun Int.toDp(): Dp = with(subcomposeMeasureScope) { toDp() }

    override fun Float.toDp(): Dp = with(subcomposeMeasureScope) { toDp() }

    override fun Float.toSp(): TextUnit = with(subcomposeMeasureScope) { toSp() }

    override fun Int.toSp(): TextUnit = with(subcomposeMeasureScope) { toSp() }

    override fun Dp.toSp(): TextUnit = with(subcomposeMeasureScope) { toSp() }

    override fun DpSize.toSize(): Size = with(subcomposeMeasureScope) { toSize() }

    override fun Size.toDpSize(): DpSize = with(subcomposeMeasureScope) { toDpSize() }
}
